package com.revizeus.app

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * ════════════════════════════════════════════════════════════════
 * LyriaManager.kt — RéviZeus
 * Version COMPATIBILITÉ TOTALE GodMatiereActivity premium/fallback
 * ════════════════════════════════════════════════════════════════
 *
 * Objectif :
 * - corriger les imports Firebase Functions
 * - conserver la génération musicale réelle via Cloud Functions
 * - sécuriser le quota côté Android (anti double invocation locale)
 * - rester compatible avec les anciennes versions de GodMatiereActivity
 *   qui attendent certaines fonctions / enums supplémentaires
 */
object LyriaManager {

    private const val FUNCTIONS_REGION = "us-central1"

    @Volatile
    private var isGenerationInFlight: Boolean = false

    private val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
    }

    data class LyriaResult(
        val success: Boolean,
        val audioUrl: String? = null,
        val localFilePath: String? = null,
        val remainingQuota: Int = 0,
        val message: String = "",
        val error: LyriaError? = null
    )

    /**
     * Enum enrichi pour rester compatible avec plusieurs générations
     * de code GodMatiereActivity déjà envoyées.
     */
    enum class LyriaError {
        QUOTA_USER_EXCEEDED,
        QUOTA_GLOBAL_EXCEEDED,
        UNAUTHENTICATED,
        NETWORK_ERROR,

        // Noms récents
        INVALID_REQUEST,
        ALREADY_RUNNING,

        // Alias legacy attendus par certains packs déjà intégrés
        INVALID_ARGUMENT,
        GENERATION_ALREADY_RUNNING,
        SERVER_REFUSED,

        UNKNOWN
    }

    /**
     * Normalise légèrement les paroles sans en altérer le contenu sémantique.
     *
     * Objectif :
     * - conserver EXACTEMENT le texte affiché à l'écran comme source de vérité ;
     * - nettoyer les espaces parasites qui perturbent parfois la diction côté backend ;
     * - ne jamais réécrire le sens, ni injecter d'autres mots.
     */
    fun normalizeLyricsForSinging(lyrics: String): String {
        return lyrics
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[\\t ]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    suspend fun generateDivineMusic(
        godName: String,
        lyrics: String,
        musicalStyle: String,
        courseName: String? = null,
        context: Context
    ): LyriaResult = suspendCancellableCoroutine { continuation ->

        if (isGenerationInFlight) {
            continuation.resume(
                LyriaResult(
                    success = false,
                    message = "La lyre est déjà en train de vibrer. Attends la fin de l'invocation en cours.",
                    error = LyriaError.ALREADY_RUNNING
                )
            )
            return@suspendCancellableCoroutine
        }

        isGenerationInFlight = true

        val normalizedLyrics = normalizeLyricsForSinging(lyrics)

        val data = hashMapOf(
            "godName" to godName,
            "lyrics" to normalizedLyrics,
            "musicalStyle" to musicalStyle
        )

        if (!courseName.isNullOrBlank()) {
            data["courseName"] = courseName
        }

        functions
            .getHttpsCallable("generateDivineMusic")
            .call(data)
            .addOnSuccessListener { result ->
                try {
                    val response = result.data as? Map<*, *>
                    val success = response?.get("success") as? Boolean ?: false
                    val audioUrl = response?.get("audioUrl") as? String
                    val remainingQuota = (response?.get("remainingQuota") as? Number)?.toInt() ?: 0
                    val message = response?.get("message") as? String ?: ""

                    if (success && !audioUrl.isNullOrBlank()) {
                        downloadAudio(audioUrl, context) { localPath ->
                            isGenerationInFlight = false

                            if (localPath != null) {
                                continuation.resume(
                                    LyriaResult(
                                        success = true,
                                        audioUrl = audioUrl,
                                        localFilePath = localPath,
                                        remainingQuota = remainingQuota,
                                        message = message
                                    )
                                )
                            } else {
                                continuation.resume(
                                    LyriaResult(
                                        success = false,
                                        message = "La musique divine a bien été créée, mais son téléchargement a échoué.",
                                        error = LyriaError.NETWORK_ERROR
                                    )
                                )
                            }
                        }
                    } else {
                        isGenerationInFlight = false
                        continuation.resume(
                            LyriaResult(
                                success = false,
                                message = if (message.isNotBlank()) message else "Lyria n'a pas pu créer de musique.",
                                error = LyriaError.UNKNOWN
                            )
                        )
                    }
                } catch (e: Exception) {
                    isGenerationInFlight = false
                    Log.e("LyriaManager", "Erreur parsing réponse", e)
                    continuation.resume(
                        LyriaResult(
                            success = false,
                            message = "Erreur divine : ${e.message}",
                            error = LyriaError.UNKNOWN
                        )
                    )
                }
            }
            .addOnFailureListener { exception ->
                isGenerationInFlight = false
                Log.e("LyriaManager", "Erreur Cloud Function", exception)

                val mappedError = mapFirebaseFunctionsError(exception)
                continuation.resume(
                    LyriaResult(
                        success = false,
                        message = exception.message ?: "Erreur inconnue",
                        error = mappedError
                    )
                )
            }

        continuation.invokeOnCancellation {
            isGenerationInFlight = false
        }
    }

    private fun downloadAudio(
        audioUrl: String,
        context: Context,
        callback: (String?) -> Unit
    ) {
        Thread {
            try {
                val url = URL(audioUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("LyriaManager", "HTTP ${connection.responseCode}")
                    callback(null)
                    return@Thread
                }

                val fileName = "divine_music_${System.currentTimeMillis()}.mp3"
                val outputFile = File(context.cacheDir, fileName)

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d("LyriaManager", "Audio téléchargé : ${outputFile.absolutePath}")
                callback(outputFile.absolutePath)
            } catch (e: Exception) {
                Log.e("LyriaManager", "Erreur téléchargement audio", e)
                callback(null)
            }
        }.start()
    }

    fun playDivineMusic(
        filePath: String,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): MediaPlayer? {
        return try {
            val audioFile = File(filePath)
            if (!audioFile.exists() || !audioFile.isFile) {
                onError?.invoke("Le fichier audio divin est introuvable.")
                return null
            }

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(audioFile.absolutePath)
            mediaPlayer.setOnCompletionListener {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                } catch (_: Exception) {
                }
                onComplete?.invoke()
                try {
                    it.reset()
                } catch (_: Exception) {
                }
                try {
                    it.release()
                } catch (_: Exception) {
                }
            }

            mediaPlayer.setOnErrorListener { player, what, extra ->
                Log.e("LyriaManager", "MediaPlayer error: what=$what extra=$extra")
                try {
                    player.reset()
                } catch (_: Exception) {
                }
                try {
                    player.release()
                } catch (_: Exception) {
                }
                onError?.invoke("Erreur lecture audio")
                true
            }

            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer
        } catch (e: Exception) {
            Log.e("LyriaManager", "Erreur MediaPlayer", e)
            onError?.invoke("Impossible de jouer la musique divine : ${e.message}")
            null
        }
    }

    /**
     * Fallback premium : vrai/faux.
     * On garde les quotas / auth / requêtes invalides hors fallback.
     * On permet le fallback pour les pannes réseau / serveur / inconnues.
     */
    fun shouldUsePremiumFallback(error: LyriaError?): Boolean {
        return when (error) {
            null -> true
            LyriaError.NETWORK_ERROR,
            LyriaError.SERVER_REFUSED,
            LyriaError.UNKNOWN -> true

            LyriaError.QUOTA_USER_EXCEEDED,
            LyriaError.QUOTA_GLOBAL_EXCEEDED,
            LyriaError.UNAUTHENTICATED,
            LyriaError.INVALID_REQUEST,
            LyriaError.INVALID_ARGUMENT,
            LyriaError.ALREADY_RUNNING,
            LyriaError.GENERATION_ALREADY_RUNNING -> false
        }
    }

    /**
     * Petit label premium affichable dans le fallback.
     */
    fun getPremiumFallbackLabel(error: LyriaError?, message: String?): String {
        return when (error) {
            LyriaError.NETWORK_ERROR -> "TRANSMISSION DIVINE INTERROMPUE"
            LyriaError.SERVER_REFUSED -> "CHANT ÉTHÉRÉ NON MATÉRIALISÉ"
            LyriaError.UNKNOWN, null -> "CHANT DU SAVOIR MANIFESTÉ"
            else -> if (message.isNullOrBlank()) {
                "CHANT DU SAVOIR"
            } else {
                "CHANT DU SAVOIR — ${message.take(42)}"
            }
        }
    }

    /**
     * Message diégétique premium si le son ne peut pas être matérialisé.
     */
    fun getPremiumFallbackMessage(godName: String): String {
        return listOf(
            "$godName a bien inspiré les paroles sacrées, mais la musique refuse encore de descendre de l'Olympe.",
            "$godName murmure que le chant existe déjà dans les sphères célestes, même si tes oreilles mortelles ne peuvent pas encore le saisir.",
            "Les Muses ont terminé la composition pour $godName, mais l'écho divin s'est dissipé avant d'atteindre ton temple.",
            "$godName a gravé cette chanson dans la mémoire du ciel. Le son s'est perdu, mais le savoir, lui, demeure."
        ).random()
    }

    fun getDivineFunnyMessage(error: LyriaError, godName: String): String {
        return when (error) {
            LyriaError.QUOTA_USER_EXCEEDED -> listOf(
                "$godName a déjà offert son concert du jour. Une seule grande musique divine par mortel et par jour.",
                "La lyre de $godName doit refroidir jusqu'à demain. Reviens au prochain lever d'Hélios.",
                "$godName dit : \"Une seule composition céleste par jour. Même les Muses ont un planning.\""
            ).random()

            LyriaError.QUOTA_GLOBAL_EXCEEDED -> listOf(
                "Le Panthéon a atteint sa limite musicale du jour. Les Muses ferment la scène jusqu'à demain.",
                "L'Olympe tout entier a trop chanté aujourd'hui. Silence sacré jusqu'à demain."
            ).random()

            LyriaError.UNAUTHENTICATED ->
                "$godName refuse de jouer pour un inconnu. Connecte-toi d'abord à l'Olympe."

            LyriaError.NETWORK_ERROR ->
                "Hermès a perdu la partition en route. Vérifie ta connexion à l'Olympe."

            LyriaError.INVALID_REQUEST,
            LyriaError.INVALID_ARGUMENT ->
                "Les Muses disent que l'invocation est incomplète. Il manque des éléments au chant sacré."

            LyriaError.ALREADY_RUNNING,
            LyriaError.GENERATION_ALREADY_RUNNING ->
                "La lyre est déjà en train de vibrer. Une seule invocation musicale à la fois."

            LyriaError.SERVER_REFUSED ->
                "$godName a bien tenté d'invoquer la scène céleste, mais le théâtre divin a refusé l'entrée pour un instant."

            LyriaError.UNKNOWN ->
                "$godName a trébuché sur une corde de sa lyre. Réessaie dans un instant."
        }
    }

    private fun mapFirebaseFunctionsError(exception: Exception): LyriaError {
        val functionsException = exception as? FirebaseFunctionsException
        val code = functionsException?.code
        val detailsText = buildString {
            append(exception.message.orEmpty())
            append(" ")
            append(functionsException?.details?.toString().orEmpty())
        }.lowercase()

        return when {
            code == FirebaseFunctionsException.Code.UNAUTHENTICATED ||
                detailsText.contains("unauthenticated") -> {
                LyriaError.UNAUTHENTICATED
            }

            code == FirebaseFunctionsException.Code.INVALID_ARGUMENT ||
                detailsText.contains("invalid-argument") ||
                detailsText.contains("invalid argument") -> {
                LyriaError.INVALID_ARGUMENT
            }

            code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED &&
                detailsText.contains("quota_user_exceeded") -> {
                LyriaError.QUOTA_USER_EXCEEDED
            }

            code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED &&
                detailsText.contains("quota_global_exceeded") -> {
                LyriaError.QUOTA_GLOBAL_EXCEEDED
            }

            code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> {
                LyriaError.SERVER_REFUSED
            }

            code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ||
                code == FirebaseFunctionsException.Code.UNAVAILABLE ||
                detailsText.contains("network") ||
                detailsText.contains("timeout") -> {
                LyriaError.NETWORK_ERROR
            }

            detailsText.contains("already running") ||
                detailsText.contains("already_running") ||
                detailsText.contains("invocation en cours") -> {
                LyriaError.GENERATION_ALREADY_RUNNING
            }

            else -> LyriaError.UNKNOWN
        }
    }
}
