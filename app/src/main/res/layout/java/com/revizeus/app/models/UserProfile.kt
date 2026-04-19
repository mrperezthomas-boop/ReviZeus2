package com.revizeus.app.models

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.revizeus.app.core.XpCalculator
import org.json.JSONObject

/**
 * Modèle UserProfile — RéviZeus
 *
 * Correctifs cumulés :
 * - monnaies du jeu
 * - avatar par nom de ressource pour une vraie persistance
 * - day streak / win streak sans casser le champ legacy `streak`
 *
 * CONSOLIDATION :
 * - Aucune variable supprimée.
 * - Aucune mécanique supprimée.
 * - Sécurisation du fallback avatar pour éviter un resId invalide.
 *
 * PHASE 1 — COMPTE DIVIN FIREBASE :
 * - Ajout des champs d'identité cloud et parentaux
 * - Tous les nouveaux champs ont une valeur par défaut
 * - Cela évite de casser les instanciations existantes de UserProfile
 *
 * SYSTÈME MULTI-COMPTES v7 :
 * - Ajout du champ totalPlayTimeSeconds pour le suivi du temps de jeu
 *   affiché sur l'écran de sélection de compte (AccountSelectActivity).
 */
@Entity(tableName = "user_profile")
data class UserProfile(

    @PrimaryKey val id: Int = 1,

    var age: Int = 15,
    var classLevel: String = "Terminale",
    var mood: String = "Prêt",
    var xp: Int = 0,
    var streak: Int = 0,
    var cognitivePattern: String = "Neutral",

    var statLogique: Int = 1,
    var statMemoire: Int = 1,
    var statAnalyse: Int = 1,
    var statCreativite: Int = 1,
    var statExpression: Int = 1,
    var statConcentration: Int = 1,
    var statEndurance: Int = 1,

    var logicalPrecisionScore: Float = 1.0f,
    var errorPatternFrequency: Float = 0.0f,
    var fatigueIndex: Float = 0.0f,
    var biasSusceptibility: Float = 0.5f,
    var retentionDecayRate: Float = 1.0f,
    var adaptabilityCoefficient: Float = 1.0f,

    @ColumnInfo(name = "pseudo")
    var pseudo: String = "Héros",

    @ColumnInfo(name = "level")
    var level: Int = 1,

    @ColumnInfo(name = "avatarResName")
    var avatarResName: String = "avatar_hero1",

    @ColumnInfo(name = "title_equipped")
    var titleEquipped: String = "Novice de l'Olympe",

    @ColumnInfo(name = "total_xp_earned")
    var totalXpEarned: Int = 0,

    @ColumnInfo(name = "best_streak_ever")
    var bestStreakEver: Int = 0,

    @ColumnInfo(name = "last_login_at")
    var lastLoginAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "total_quiz_done")
    var totalQuizDone: Int = 0,

    @ColumnInfo(name = "eclats_savoir")
    var eclatsSavoir: Int = 0,

    @ColumnInfo(name = "ambroisie")
    var ambroisie: Int = 0,

    @ColumnInfo(name = "day_streak")
    var dayStreak: Int = 0,

    @ColumnInfo(name = "best_day_streak")
    var bestDayStreak: Int = 0,

    @ColumnInfo(name = "win_streak")
    var winStreak: Int = 0,

    @ColumnInfo(name = "best_win_streak")
    var bestWinStreak: Int = 0,

    @ColumnInfo(name = "last_login_day_key")
    var lastLoginDayKey: String = "",

    @ColumnInfo(name = "last_win_quiz_at")
    var lastWinQuizAt: Long = 0L,

    /**
     * PHASE C — FORGE D'HÉPHAÏSTOS
     * Stock de Fragments de Connaissance par matière.
     *
     * Format JSON sérialisé : {"Mathématiques": 42, "Histoire": 17, ...}
     * Stocké en String pour éviter une table supplémentaire et rester
     * compatible avec les migrations Room sans friction.
     *
     * RÈGLES :
     * - Ne jamais écrire directement dans ce champ depuis l'extérieur.
     * - Utiliser UNIQUEMENT getFragmentCount() et addFragments() pour
     *   garantir la cohérence du JSON et éviter les corruptions.
     *
     * ÉVOLUTION FUTURE :
     * - Migrer vers une table dédiée FragmentEntry si le besoin d'indexation
     *   ou de requêtes SQL sur les fragments devient nécessaire.
     */
    @ColumnInfo(name = "knowledge_fragments")
    var knowledgeFragments: String = "{}",

    /**
     * PHASE 1 — COMPTE DIVIN FIREBASE
     * Email principal du compte cloud.
     */
    @ColumnInfo(name = "account_email")
    var accountEmail: String = "",

    /**
     * Email de récupération conservé séparément si besoin d'évolution future.
     * Pour l'instant, on l'aligne généralement sur accountEmail.
     */
    @ColumnInfo(name = "recovery_email")
    var recoveryEmail: String = "",

    /**
     * UID Firebase de l'utilisateur.
     */
    @ColumnInfo(name = "firebase_uid")
    var firebaseUid: String = "",

    /**
     * Indique si l'email du compte a été vérifié côté Firebase.
     */
    @ColumnInfo(name = "is_email_verified")
    var isEmailVerified: Boolean = false,

    /**
     * Email parent optionnel — préparé pour la future Phase Parents.
     */
    @ColumnInfo(name = "parent_email")
    var parentEmail: String = "",

    /**
     * Active ou non les futurs résumés hebdomadaires parentaux.
     */
    @ColumnInfo(name = "is_parent_summary_enabled")
    var isParentSummaryEnabled: Boolean = false,

    /**
     * Horodatage du dernier envoi hebdomadaire parent.
     */
    @ColumnInfo(name = "last_weekly_summary_sent_at")
    var lastWeeklySummarySentAt: Long = 0L,

    /**
     * SYSTÈME MULTI-COMPTES v7 — Temps total de jeu en secondes.
     *
     * Incrémenté lors de chaque déconnexion via AccountRegistry.endSessionAndSaveTime().
     * Affiché sur l'écran de sélection de compte (AccountSelectActivity)
     * pour aider la famille à identifier rapidement quel profil est le sien.
     *
     * Valeur par défaut : 0L (nouveau compte)
     *
     * ÉVOLUTION FUTURE :
     * - Déclencher un badge "Marathonien de l'Olympe" à 100h de jeu.
     * - Afficher le temps moyen par session dans HeroProfileActivity.
     */
    @ColumnInfo(name = "total_play_time_seconds")
    var totalPlayTimeSeconds: Long = 0L

) {
    @get:Ignore
    val rang: String
        get() = when {
            level >= 50 -> "Olympien"
            level >= 20 -> "Demi-Dieu"
            level >= 10 -> "Héros Confirmé"
            level >= 5  -> "Initié"
            else        -> "Mortel"
        }

    @get:Ignore
    val userClass: String
        get() = classLevel

    @get:Ignore
    val xpDansNiveau: Int
        get() = XpCalculator.xpInCurrentLevel(xp)

    @get:Ignore
    val xpSeuilNiveau: Int
        get() = XpCalculator.xpThresholdForLevel(level)

    @get:Ignore
    val progressionNiveauPct: Int
        get() = XpCalculator.progressToNextLevel(xp)

    @Ignore
    fun getAvatarResId(context: Context): Int {
        // Fallback ultra sûr : si le nom est vide ou introuvable, on renvoie une icône système.
        if (avatarResName.isBlank()) return android.R.drawable.sym_def_app_icon

        val resId = context.resources.getIdentifier(
            avatarResName,
            "drawable",
            context.packageName
        )

        return if (resId != 0) {
            resId
        } else {
            android.R.drawable.sym_def_app_icon
        }
    }

    // ══════════════════════════════════════════════════════════
    // PHASE C — FORGE D'HÉPHAÏSTOS — HELPERS FRAGMENTS
    // ══════════════════════════════════════════════════════════

    /**
     * Retourne le nombre de Fragments de Connaissance pour une matière.
     */
    @Ignore
    fun getFragmentCount(matiere: String): Int {
        return try {
            val json = JSONObject(knowledgeFragments)
            json.optInt(matiere, 0).coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Ajoute (ou déduit) des Fragments de Connaissance pour une matière.
     */
    @Ignore
    fun addFragments(matiere: String, delta: Int) {
        try {
            val json = if (knowledgeFragments.isBlank()) JSONObject() else JSONObject(knowledgeFragments)
            val actuel = json.optInt(matiere, 0).coerceAtLeast(0)
            val nouveau = (actuel + delta).coerceAtLeast(0)
            json.put(matiere, nouveau)
            knowledgeFragments = json.toString()
        } catch (e: Exception) {
            try {
                val fallback = JSONObject()
                fallback.put(matiere, delta.coerceAtLeast(0))
                knowledgeFragments = fallback.toString()
            } catch (_: Exception) {}
        }
    }
}
