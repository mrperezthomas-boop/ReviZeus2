package com.revizeus.app

import android.content.Context
import android.util.Log
import com.revizeus.app.models.AppDatabase
import com.revizeus.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * CURRENCY MANAGER — RéviZeus
 * ═══════════════════════════════════════════════════════════════
 * Gère les monnaies du jeu :
 * - Éclats de savoir (monnaie normale)
 * - Ambroisie (monnaie rare)
 * ═══════════════════════════════════════════════════════════════
 */
object CurrencyManager {

    data class CurrencyReward(
        val eclatsSavoir: Int,
        val ambroisie: Int
    )

    /**
     * Calcule la récompense d'un quiz.
     *
     * Barème de départ :
     * - Quiz terminé : +10 Éclats
     * - Score >= 75% : +15 Éclats
     * - Score = 100% : +25 Éclats supplémentaires
     * - Épreuve Ultime : +20 Éclats supplémentaires
     *
     * Ambroisie :
     * - Score = 100% : +1
     * - Épreuve Ultime >= 75% : +1
     * - Épreuve Ultime = 100% : +1 supplémentaire
     */
    fun computeQuizReward(
        score: Int,
        total: Int,
        isEpreuveUltime: Boolean
    ): CurrencyReward {
        val percentage = if (total > 0) (score * 100) / total else 0

        var eclats = 10
        if (percentage >= 75) eclats += 15
        if (percentage == 100) eclats += 25
        if (isEpreuveUltime) eclats += 20

        var ambroisie = 0
        if (percentage == 100) ambroisie += 1
        if (isEpreuveUltime && percentage >= 75) ambroisie += 1
        if (isEpreuveUltime && percentage == 100) ambroisie += 1

        return CurrencyReward(
            eclatsSavoir = eclats,
            ambroisie = ambroisie
        )
    }

    /**
     * Applique les monnaies au profil utilisateur.
     */
    suspend fun grantReward(
        context: Context,
        reward: CurrencyReward
    ) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            var profile = db.iAristoteDao().getUserProfile()

            if (profile == null) {
                profile = UserProfile(
                    id = 1,
                    age = 15,
                    classLevel = "Terminale",
                    mood = "Prêt",
                    xp = 0,
                    streak = 0,
                    cognitivePattern = "Neutral"
                )
                db.iAristoteDao().saveUserProfile(profile)
            }

            profile.eclatsSavoir += reward.eclatsSavoir
            profile.ambroisie += reward.ambroisie

            db.iAristoteDao().updateUserProfile(profile)
        } catch (e: Exception) {
            Log.e("REVIZEUS", "Erreur CurrencyManager.grantReward : ${e.message}")
        }
    }
}