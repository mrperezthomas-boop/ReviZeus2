package com.revizeus.app

import android.content.Context
import android.util.Patterns
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ============================================================
 * ParentSummaryManager.kt — RéviZeus
 * Gestionnaire local de la phase Parents
 *
 * Utilité :
 * - Stockage local du mail parent
 * - Activation / désactivation du résumé hebdo
 * - Synchronisation douce vers UserProfile sans casser l'existant
 *
 * Connexions :
 * - AuthActivity
 * - SettingsActivity
 * - UserProfile / AppDatabase
 *
 * NOTE :
 * - Ce manager prépare la couche backend future.
 * - Il ne remplace pas la future Cloud Function.
 * ============================================================
 */
object ParentSummaryManager {

    private const val PREFS_NAME = "ReviZeusPrefs"
    private const val KEY_PARENT_EMAIL = "PARENT_EMAIL"
    private const val KEY_PARENT_SUMMARY_ENABLED = "PARENT_SUMMARY_ENABLED"

    fun getParentEmail(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PARENT_EMAIL, "") ?: ""
    }

    fun isParentSummaryEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PARENT_SUMMARY_ENABLED, false)
    }

    fun isValidParentEmail(email: String): Boolean {
        return email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun saveToPrefs(
        context: Context,
        parentEmail: String,
        enabled: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PARENT_EMAIL, parentEmail.trim())
            .putBoolean(KEY_PARENT_SUMMARY_ENABLED, enabled)
            .apply()
    }

    suspend fun syncToRoom(
        context: Context,
        parentEmail: String,
        enabled: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.iAristoteDao()
            val profile = dao.getUserProfile() ?: return@withContext
            profile.parentEmail = parentEmail.trim()
            profile.isParentSummaryEnabled = enabled
            dao.updateUserProfile(profile)
        }
    }
}
