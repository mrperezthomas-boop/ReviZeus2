package com.revizeus.app

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * AccountRecoveryManager — RéviZeus
 * ─────────────────────────────────────────────────────────────
 * OBJECTIF :
 * - Ajouter un vrai code de secours par compte Firebase (UID)
 * - Ne JAMAIS casser la logique actuelle 1 email = jusqu'à 3 héros
 * - Conserver la séparation :
 *      Firebase Auth = identité cloud
 *      Room         = gameplay local
 *      RecoveryCode = filet de sécurité compte
 *
 * ÉVOLUTION v2 :
 * - stockage local du code brut pour pouvoir le réafficher depuis les réglages
 * - drapeau "acknowledged" pour forcer l'affichage du popup tant que
 *   l'utilisateur n'a pas explicitement confirmé qu'il a bien conservé le code
 */
object AccountRecoveryManager {

    private const val PREFS_NAME = "ReviZeusRecovery"
    private const val KEY_HASH_PREFIX = "recovery_hash_"
    private const val KEY_HINT_PREFIX = "recovery_hint_"
    private const val KEY_EMAIL_PREFIX = "recovery_email_"
    private const val KEY_SYNC_AT_PREFIX = "recovery_sync_at_"
    private const val KEY_RAW_PREFIX = "recovery_raw_"
    private const val KEY_ACK_PREFIX = "recovery_ack_"

    private val secureRandom = SecureRandom()

    /**
     * Génère un code de secours lisible par l'utilisateur.
     *
     * Format choisi :
     * REVZ-ABCD-EFGH-IJKL
     */
    fun generateRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        fun block(size: Int): String = buildString {
            repeat(size) { append(alphabet[secureRandom.nextInt(alphabet.length)]) }
        }
        return "REVZ-${block(4)}-${block(4)}-${block(4)}"
    }

    /**
     * Normalise une saisie utilisateur.
     * On retire espaces et tirets parasites pour éviter les faux échecs.
     */
    fun normalizeRecoveryCode(raw: String): String {
        return raw
            .uppercase()
            .replace("\\s".toRegex(), "")
            .replace("-", "")
            .trim()
    }

    /**
     * Hash SHA-256 simple et stable.
     * Suffisant ici pour éviter de stocker le code brut dans Firestore.
     */
    fun hashRecoveryCode(raw: String): String {
        val normalized = normalizeRecoveryCode(raw)
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Retourne un indice d'affichage non sensible.
     */
    fun buildHint(raw: String): String {
        val normalized = normalizeRecoveryCode(raw)
        return if (normalized.length >= 4) "••••-${normalized.takeLast(4)}" else "••••"
    }

    /**
     * Indique si un code de secours local existe déjà pour ce compte.
     */
    fun hasLocalRecoveryCode(context: Context, firebaseUid: String): Boolean {
        if (firebaseUid.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HASH_PREFIX + firebaseUid, "").orEmpty().isNotBlank()
    }

    /**
     * Retourne le code brut local si cet appareil le possède encore.
     */
    fun getLocalRecoveryCode(context: Context, firebaseUid: String): String {
        if (firebaseUid.isBlank()) return ""
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RAW_PREFIX + firebaseUid, "").orEmpty()
    }

    /**
     * Retourne l'indice local du code de secours.
     */
    fun getLocalRecoveryHint(context: Context, firebaseUid: String): String {
        if (firebaseUid.isBlank()) return ""
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HINT_PREFIX + firebaseUid, "").orEmpty()
    }

    /**
     * Le popup a-t-il été explicitement validé par l'utilisateur ?
     */
    fun isRecoveryCodeAcknowledged(context: Context, firebaseUid: String): Boolean {
        if (firebaseUid.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACK_PREFIX + firebaseUid, false)
    }

    /**
     * Marque le code comme bien conservé par l'utilisateur.
     */
    fun markRecoveryCodeAcknowledged(context: Context, firebaseUid: String) {
        if (firebaseUid.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ACK_PREFIX + firebaseUid, true).apply()
    }

    /**
     * Réinitialise l'acquittement si un futur flow exige de remontrer le code.
     */
    fun clearRecoveryCodeAcknowledged(context: Context, firebaseUid: String) {
        if (firebaseUid.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ACK_PREFIX + firebaseUid, false).apply()
    }

    /**
     * Vérifie localement un code saisi contre le compte indiqué.
     */
    fun verifyLocalRecoveryCode(context: Context, firebaseUid: String, rawCode: String): Boolean {
        if (firebaseUid.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(KEY_HASH_PREFIX + firebaseUid, "").orEmpty()
        if (savedHash.isBlank()) return false
        return savedHash == hashRecoveryCode(rawCode)
    }

    /**
     * Sauvegarde :
     * - localement dans SharedPreferences
     * - puis dans Firestore sous users/{uid}
     *
     * IMPORTANT :
     * - le brut reste seulement local à l'appareil
     * - Firestore ne reçoit que le hash + l'indice
     * - un nouveau code doit être explicitement revalidé par l'utilisateur
     */
    fun saveRecoveryCode(
        context: Context,
        firebaseUid: String,
        accountEmail: String,
        rawCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (firebaseUid.isBlank()) {
            onError("UID Firebase introuvable pour enregistrer le code de secours.")
            return
        }

        val hash = hashRecoveryCode(rawCode)
        val hint = buildHint(rawCode)

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_HASH_PREFIX + firebaseUid, hash)
                .putString(KEY_HINT_PREFIX + firebaseUid, hint)
                .putString(KEY_EMAIL_PREFIX + firebaseUid, accountEmail)
                .putString(KEY_RAW_PREFIX + firebaseUid, rawCode)
                .putBoolean(KEY_ACK_PREFIX + firebaseUid, false)
                .putLong(KEY_SYNC_AT_PREFIX + firebaseUid, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e("RecoveryCode", "Erreur sauvegarde locale", e)
            onError("Le code a été généré, mais sa sauvegarde locale a échoué.")
            return
        }

        val payload = hashMapOf(
            "recoveryEnabled" to true,
            "recoveryCodeHash" to hash,
            "recoveryCodeHint" to hint,
            "recoveryEmailSnapshot" to accountEmail,
            "recoveryUpdatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(firebaseUid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                Log.e("RecoveryCode", "Erreur sync Firestore", error)
                onError(error.localizedMessage ?: "Le code local a été créé, mais la synchronisation cloud a échoué.")
            }
    }
}
