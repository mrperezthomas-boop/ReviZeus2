package com.revizeus.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GameUpdateManager
 * ─────────────────────────────────────────────────────────────
 * V2 PROPRE
 *
 * OBJECTIFS :
 * - Ne jamais afficher l'écran de MAJ au premier lancement.
 * - Ne jamais afficher l'écran de MAJ en build debug / app debuggable.
 * - Ne jamais dépendre de maj.txt ici.
 * - Ne jamais bloquer le boot : timeout strict.
 * - Préserver strictement les données sacrées du joueur.
 *
 * DONNÉES SACRÉES PRÉSERVÉES :
 * - UserProfile
 * - CourseEntry
 * - MemoryScore
 * - Inventory
 * - XP / niveau
 * - Éclats / Ambroisie
 * - avatar
 * - fragments
 *
 * IMPORTANT :
 * Ce système ne remplace PAS les migrations Room.
 * Toute évolution du schéma Room doit conserver sa migration dédiée.
 */
object GameUpdateManager {

    private const val PREFS_NAME = "ReviZeusGameUpdate"
    private const val KEY_LAST_APPLIED_VERSION_CODE = "last_applied_version_code"
    private const val KEY_LAST_APPLIED_VERSION_NAME = "last_applied_version_name"
    private const val KEY_SNOOZED_VERSION_CODE = "snoozed_version_code"
    private const val KEY_UPDATE_IN_PROGRESS = "update_in_progress"
    private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_FIRST_INSTALL_SEALED = "first_install_sealed"

    /**
     * Timeout strict pour éviter tout boot bloqué.
     * Si une étape traîne, on échoue proprement au lieu de laisser l'écran tourner.
     */
    private const val APPLY_TIMEOUT_MS = 8000L

    data class UpdateGate(
        val shouldShow: Boolean,
        val isRecoveryMode: Boolean,
        val currentVersionCode: Int,
        val currentVersionName: String,
        val lastAppliedVersionCode: Int,
        val reason: String
    )

    data class PreservedState(
        val hasProfile: Boolean,
        val pseudo: String,
        val avatarResName: String,
        val eclatsSavoir: Int,
        val ambroisie: Int,
        val knowledgeFragmentsJson: String,
        val level: Int,
        val xp: Int,
        val courseCount: Int,
        val inventoryCount: Int,
        val memoryScoreCount: Int
    )

    data class UpdateResult(
        val success: Boolean,
        val message: String,
        val beforeState: PreservedState?,
        val afterState: PreservedState?,
        val backupFolderPath: String?
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Lecture robuste du versionCode sans dépendre de BuildConfig.
     */
    private fun currentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    /**
     * Lecture robuste du versionName sans dépendre de BuildConfig.
     */
    private fun currentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    /**
     * Détection d'une app "developer friendly".
     * Tant que l'app est installée en mode debuggable, on bypass l'écran.
     */
    private fun isDebuggableApp(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Détection très prudente du premier lancement.
     *
     * Ici, on considère "première installation" si :
     * - aucune version n'a encore été scellée
     * - aucune DB RéviZeus n'existe
     *
     * Dans ce cas on marque silencieusement la version comme appliquée
     * et on ne montre jamais l'écran.
     */
    private fun isFreshInstall(context: Context): Boolean {
        val prefs = prefs(context)
        val alreadySealed = prefs.getBoolean(KEY_FIRST_INSTALL_SEALED, false)
        if (alreadySealed) return false

        val hasAnyRevizeusDb = context.databaseList().any { dbName ->
            dbName.startsWith("revizeus_database")
        }

        return !hasAnyRevizeusDb
    }

    /**
     * Scelle silencieusement la version courante sans afficher d'écran.
     * Utilisé pour :
     * - premier lancement
     * - app debuggable côté dev
     */
    private fun markAppliedSilently(context: Context) {
        val appContext = context.applicationContext
        prefs(appContext).edit()
            .putInt(KEY_LAST_APPLIED_VERSION_CODE, currentVersionCode(appContext))
            .putString(KEY_LAST_APPLIED_VERSION_NAME, currentVersionName(appContext))
            .putBoolean(KEY_FIRST_INSTALL_SEALED, true)
            .putBoolean(KEY_UPDATE_IN_PROGRESS, false)
            .remove(KEY_SNOOZED_VERSION_CODE)
            .remove(KEY_LAST_ERROR)
            .apply()
    }

    /**
     * Gate d'affichage.
     *
     * RÈGLES V2 :
     * - debug/dev => jamais d'écran
     * - premier lancement => jamais d'écran
     * - release déjà snoozée => pas d'écran
     * - recovery interrompu => écran obligatoire
     * - nouvelle release réelle => écran autorisé
     */
    fun evaluate(context: Context): UpdateGate {
        val appContext = context.applicationContext
        val prefs = prefs(appContext)

        val versionCode = currentVersionCode(appContext)
        val versionName = currentVersionName(appContext)
        val lastApplied = prefs.getInt(KEY_LAST_APPLIED_VERSION_CODE, 0)
        val snoozed = prefs.getInt(KEY_SNOOZED_VERSION_CODE, -1)
        val inProgress = prefs.getBoolean(KEY_UPDATE_IN_PROGRESS, false)

        if (isDebuggableApp(appContext)) {
            markAppliedSilently(appContext)
            return UpdateGate(
                shouldShow = false,
                isRecoveryMode = false,
                currentVersionCode = versionCode,
                currentVersionName = versionName,
                lastAppliedVersionCode = versionCode,
                reason = "Bypass développeur actif."
            )
        }

        if (isFreshInstall(appContext)) {
            markAppliedSilently(appContext)
            return UpdateGate(
                shouldShow = false,
                isRecoveryMode = false,
                currentVersionCode = versionCode,
                currentVersionName = versionName,
                lastAppliedVersionCode = versionCode,
                reason = "Première installation scellée silencieusement."
            )
        }

        val isRecoveryMode = inProgress
        val hasNewRelease = versionCode > lastApplied

        val shouldShow = when {
            isRecoveryMode -> true
            snoozed == versionCode -> false
            hasNewRelease -> true
            else -> false
        }

        val reason = when {
            isRecoveryMode ->
                "Une mise à jour précédente a été interrompue. L’Olympe doit terminer son réalignement."
            hasNewRelease ->
                "Une nouvelle release a été détectée. Les systèmes secondaires du monde peuvent être réalignés."
            else ->
                "Aucune mise à jour divine requise."
        }

        return UpdateGate(
            shouldShow = shouldShow,
            isRecoveryMode = isRecoveryMode,
            currentVersionCode = versionCode,
            currentVersionName = versionName,
            lastAppliedVersionCode = lastApplied,
            reason = reason
        )
    }

    /**
     * Report uniquement pour la release courante.
     */
    fun snoozeCurrentVersion(context: Context) {
        val appContext = context.applicationContext
        prefs(appContext).edit()
            .putInt(KEY_SNOOZED_VERSION_CODE, currentVersionCode(appContext))
            .apply()
    }

    /**
     * Texte de rassurance uniquement.
     * Aucune lecture de maj.txt ici.
     */
    suspend fun loadPreservationSummaryText(context: Context): String = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val state = snapshotPreservedState(db)
            buildPreservationSummary(state)
        }.getOrElse {
            "Aucune archive joueur détectée ou base encore vide."
        }
    }

    /**
     * Application réelle, encapsulée dans un timeout strict.
     * Pas de freeze interminable : si ça dépasse, on sort en échec propre.
     */
    suspend fun applyUpdate(
        context: Context,
        onProgress: suspend (progress: Int, message: String) -> Unit
    ): UpdateResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = prefs(appContext)

        prefs.edit()
            .putBoolean(KEY_UPDATE_IN_PROGRESS, true)
            .putLong(KEY_LAST_ATTEMPT_AT, System.currentTimeMillis())
            .remove(KEY_LAST_ERROR)
            .apply()

        var beforeState: PreservedState? = null
        var afterState: PreservedState? = null
        var backupFolder: File? = null

        try {
            val result = withTimeout(APPLY_TIMEOUT_MS) {
                onProgress(10, "Prométhée sécurise les archives du héros...")
                backupFolder = createDatabaseBackup(appContext)

                onProgress(35, "Athéna inspecte les données sacrées...")
                val db = AppDatabase.getDatabase(appContext)
                beforeState = snapshotPreservedState(db)

                onProgress(60, "Zeus dissipe les caches temporaires du Chaos...")
                cleanupDerivedSharedPrefs(appContext)

                onProgress(85, "Apollon confirme que rien de précieux n’a été perdu...")
                afterState = snapshotPreservedState(db)
                validatePreservedState(beforeState, afterState)

                onProgress(100, "L’Olympe est réaligné.")

                prefs.edit()
                    .putInt(KEY_LAST_APPLIED_VERSION_CODE, currentVersionCode(appContext))
                    .putString(KEY_LAST_APPLIED_VERSION_NAME, currentVersionName(appContext))
                    .putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
                    .putBoolean(KEY_UPDATE_IN_PROGRESS, false)
                    .putBoolean(KEY_FIRST_INSTALL_SEALED, true)
                    .remove(KEY_SNOOZED_VERSION_CODE)
                    .remove(KEY_LAST_ERROR)
                    .apply()

                UpdateResult(
                    success = true,
                    message = "Mise à jour divine appliquée sans perte de progression sacrée.",
                    beforeState = beforeState,
                    afterState = afterState,
                    backupFolderPath = backupFolder?.absolutePath
                )
            }

            result
        } catch (t: Throwable) {
            prefs.edit()
                .putBoolean(KEY_UPDATE_IN_PROGRESS, false)
                .putString(KEY_LAST_ERROR, t.message ?: t.javaClass.simpleName)
                .apply()

            UpdateResult(
                success = false,
                message = t.message ?: "Le réalignement a été interrompu avant sa validation.",
                beforeState = beforeState,
                afterState = afterState,
                backupFolderPath = backupFolder?.absolutePath
            )
        }
    }

    private suspend fun snapshotPreservedState(db: AppDatabase): PreservedState {
        val dao = db.iAristoteDao()
        val profile = dao.getUserProfile()

        return PreservedState(
            hasProfile = profile != null,
            pseudo = profile?.pseudo ?: "",
            avatarResName = profile?.avatarResName ?: "",
            eclatsSavoir = profile?.eclatsSavoir ?: 0,
            ambroisie = profile?.ambroisie ?: 0,
            knowledgeFragmentsJson = profile?.knowledgeFragments ?: "{}",
            level = profile?.level ?: 0,
            xp = profile?.xp ?: 0,
            courseCount = runCatching { dao.countCourses() }.getOrDefault(0),
            inventoryCount = runCatching { dao.countTotalItemQuantity() ?: 0 }.getOrDefault(0),
            memoryScoreCount = runCatching { dao.getAllMemoryScores().size }.getOrDefault(0)
        )
    }

    private fun validatePreservedState(before: PreservedState?, after: PreservedState?) {
        if (before == null || after == null) {
            throw IllegalStateException("Validation impossible : état joueur introuvable.")
        }

        if (before.hasProfile != after.hasProfile) {
            throw IllegalStateException("Le profil héros n’a pas survécu au réalignement.")
        }

        if (before.hasProfile) {
            if (before.pseudo != after.pseudo) {
                throw IllegalStateException("Le pseudo a changé pendant la mise à jour.")
            }
            if (before.avatarResName != after.avatarResName) {
                throw IllegalStateException("L’avatar a changé pendant la mise à jour.")
            }
            if (before.eclatsSavoir != after.eclatsSavoir) {
                throw IllegalStateException("Les Éclats de Savoir ont changé pendant la mise à jour.")
            }
            if (before.ambroisie != after.ambroisie) {
                throw IllegalStateException("L’Ambroisie a changé pendant la mise à jour.")
            }
            if (before.knowledgeFragmentsJson != after.knowledgeFragmentsJson) {
                throw IllegalStateException("Les fragments ont changé pendant la mise à jour.")
            }
            if (before.level != after.level || before.xp != after.xp) {
                throw IllegalStateException("L’XP ou le niveau a changé pendant la mise à jour.")
            }
        }

        if (before.courseCount != after.courseCount) {
            throw IllegalStateException("Le nombre de savoirs sauvegardés a changé.")
        }
        if (before.inventoryCount != after.inventoryCount) {
            throw IllegalStateException("Le contenu de l’inventaire a changé.")
        }
        if (before.memoryScoreCount != after.memoryScoreCount) {
            throw IllegalStateException("Les scores mémoire ont changé.")
        }
    }

    /**
     * Nettoyage ciblé.
     * Aucun clear() sauvage.
     * Aucune suppression de données sacrées.
     */
    private fun cleanupDerivedSharedPrefs(context: Context) {
        val prefNames = listOf(
            "ReviZeusPrefs",
            "revizeus_settings",
            "revizeus_tutorial"
        )

        prefNames.forEach { prefName ->
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val authSensitiveKeys = setOf(
                "ACCOUNT_EMAIL",
                "RECOVERY_EMAIL",
                "FIREBASE_UID",
                "HAS_ACCOUNT",
                "IS_REGISTERED",
                "IS_EMAIL_VERIFIED",
                "PENDING_ACCOUNT_EMAIL",
                "ONBOARDING_EMAIL",
                "ONBOARDING_PASSWORD",
                "ONBOARDING_MODE"
            )

            val keysToRemove = prefs.all.keys.filter { key ->
                val isAuthSensitive = key in authSensitiveKeys

                !isAuthSensitive && (
                    key.startsWith("QUIZ_Q_") ||
                        key.startsWith("TEMP_") ||
                        key.startsWith("CACHE_") ||
                        key.startsWith("OCR_") ||
                        key.startsWith("RESULT_DRAFT_") ||
                        key.startsWith("PENDING_") ||
                        key.startsWith("UPDATE_TMP_") ||
                        key == "LAST_GENERATED_SUMMARY" ||
                        key == "LAST_GENERATED_QUIZ" ||
                        key == "LAST_ORACLE_TEXT"
                )
            }

            keysToRemove.forEach { editor.remove(it) }
            editor.apply()
        }

        // Correction auth :
        // ne jamais vider OnboardingSession ici.
        // Une mise à jour ne doit pas casser un flux d'inscription encore en mémoire.
    }

    fun buildPreservationSummary(state: PreservedState?): String {
        if (state == null) {
            return "Aucune donnée joueur détectée pour cette installation."
        }

        val fragmentsPreview = try {
            val json = JSONObject(state.knowledgeFragmentsJson)
            val pairs = mutableListOf<String>()
            val iterator = json.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                pairs += "$key: ${json.optInt(key, 0)}"
            }
            if (pairs.isEmpty()) "aucun fragment" else pairs.take(4).joinToString(" • ")
        } catch (_: Exception) {
            "fragments illisibles"
        }

        return buildString {
            append("Pseudo : ${state.pseudo.ifBlank { "Héros" }}\n")
            append("Avatar : ${state.avatarResName.ifBlank { "non défini" }}\n")
            append("Éclats : ${state.eclatsSavoir} • Ambroisie : ${state.ambroisie}\n")
            append("XP : ${state.xp} • Niveau : ${state.level}\n")
            append("Savoirs : ${state.courseCount} • Inventaire : ${state.inventoryCount} • Mémoire : ${state.memoryScoreCount}\n")
            append("Fragments : $fragmentsPreview")
        }
    }

    private fun createDatabaseBackup(context: Context): File? {
        val dbName = resolveCurrentDbName(context)
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return null

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date())
        val backupFolder = File(context.filesDir, "update_backups/$timestamp").apply { mkdirs() }

        copyIfExists(dbFile, File(backupFolder, dbFile.name))
        copyIfExists(File(dbFile.absolutePath + "-wal"), File(backupFolder, dbFile.name + "-wal"))
        copyIfExists(File(dbFile.absolutePath + "-shm"), File(backupFolder, dbFile.name + "-shm"))

        val meta = JSONObject().apply {
            put("db_name", dbName)
            put("created_at", System.currentTimeMillis())
            put("app_version_code", currentVersionCode(context))
            put("app_version_name", currentVersionName(context))
            put("android_sdk", Build.VERSION.SDK_INT)
        }
        File(backupFolder, "meta.json").writeText(meta.toString(2))

        return backupFolder
    }

    private fun resolveCurrentDbName(context: Context): String {
        val prefs = context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("FIREBASE_UID", "") ?: ""

        val slot = context
            .getSharedPreferences("ReviZeusAccounts", Context.MODE_PRIVATE)
            .getInt("active_slot_$uid", 1)
            .coerceIn(1, 3)

        return when {
            uid.isBlank() -> "revizeus_database"
            slot == 1 -> "revizeus_database_$uid"
            else -> "revizeus_database_${uid}_slot$slot"
        }
    }

    private fun copyIfExists(source: File, target: File) {
        if (!source.exists()) return
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }
}