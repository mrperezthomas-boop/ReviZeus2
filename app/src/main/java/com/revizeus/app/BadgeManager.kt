package com.revizeus.app

import android.content.Context
import android.content.SharedPreferences
import com.revizeus.app.core.XpCalculator
import com.revizeus.app.models.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar


/**
 * ============================================================
 * BadgeManager.kt — RéviZeus v10
 * Moteur du système de succès/badges.
 *
 * RESPONSABILITÉS :
 * - Stocker quels badges sont débloqués (SharedPreferences)
 * - Évaluer les conditions de déverrouillage (102 badges)
 * - Accorder l'XP bonus associé à chaque badge
 * - Notifier l'UI via callbacks quand un badge est obtenu
 *
 * DONNÉES NÉCESSAIRES (BadgeEvalContext) :
 * Ces données sont rassemblées UNE SEULE FOIS depuis la BDD
 * et passées à evaluateAll(). Pas de BDD appelée en boucle.
 * ============================================================
 */

/**
 * Contexte d'évaluation : toutes les données du héros
 * rassemblées en un seul objet pour évaluer les conditions.
 */
data class BadgeEvalContext(
    val level: Int,
    val totalXp: Int,
    val streak: Int,
    val totalScans: Int,
    val subjectsScanned: Set<String>,   // matières scannées ≥ 1 cours
    val totalQuizDone: Int,
    val totalPerfectScore: Int,         // nb de quiz avec score = 100%
    val consecutivePerfect: Int,        // parfaits consécutifs en cours
    val totalEpreuveUltime: Int,
    val quizParMatiere: Map<String, Int>,// matière → nb quiz complétés
    val quizDoneHeure: Int,             // heure locale du quiz (0-23)
    val quizDoneMinute: Int,            // minute locale du quiz (0-59)
    val quizDurationSeconds: Int,       // durée du dernier quiz en secondes
    val quizScorePercent: Int,          // score en % du dernier quiz
    val sessionQuizCount: Int,          // nb de quiz dans la session
    val lastLoginDaysAgo: Int,          // jours depuis dernière connexion
    val tutorialCompleted: Boolean,
    val hasVisitedProfile: Boolean,
    val hasVisitedSettings: Boolean,

    // --- NOUVEAUTÉS v10 (Éveil de l'Olympe) ---
    val starsEarned: Int,               // Étoiles obtenues (1-6)
    val isAresChallenge: Boolean,       // Est-ce un défi d'Arès ?
    val aresSuccess: Boolean,           // Arès vaincu ?
    val aresProvocationsToday: Int,     // Nombre de provocations Arès aujourd'hui
    val itemsInInventoryCount: Int,     // Nombre d'objets forgés différents
    val totalItemsForged: Int,          // Nombre total de crafts
    val fragmentsTotal: Int,            // Total de fragments
    val fragmentsMaxInOne: Int,         // Max de fragments dans une seule matière
    val forgeClickFailCount: Int,       // Clics impatients sans ressource
    val mnemoClickCount: Int,           // Clics sur les astuces mnemo
    val apollonLyreCount: Int,          // Utilisation d'Apollon
    val prometheeHelpCount: Int,        // Aides demandées à Prométhée via HUD
    val isVolumeZero: Boolean,          // Volume coupé
    val isVolumeMax: Boolean,           // Volume à fond
    val inventoryOpenCount: Int,        // Spam inventaire
    val noCorrectionClicked: Boolean    // Mode Berserker
)

object BadgeManager {

    private const val PREFS_NAME    = "revizeus_badges"
    private const val KEY_UNLOCKED  = "unlocked_"        // + badge id
    private const val KEY_UNLOCK_TS = "timestamp_"       // + badge id (ms)

    // Stats basiques
    private const val KEY_QUIZ_COUNT        = "stat_quiz_total"
    private const val KEY_PERFECT_COUNT     = "stat_perfect_total"
    private const val KEY_CONSEC_PERFECT    = "stat_perfect_consec"
    private const val KEY_ULTIME_COUNT      = "stat_ultime_total"
    private const val KEY_LAST_LOGIN_MS     = "stat_last_login_ms"
    private const val KEY_SESSION_QUIZ      = "stat_session_quiz_count"
    private const val KEY_QUIZ_MAT_PREFIX   = "stat_quiz_mat_"

    // Stats v10 (Olympe)
    private const val STAT_ITEMS_FORGED     = "stat_items_forged_total"
    private const val STAT_FORGE_FAIL       = "stat_forge_fail_click"
    private const val STAT_MNEMO_COUNT      = "stat_mnemo_click"
    private const val STAT_APOLLON_COUNT    = "stat_apollon_count"
    private const val STAT_PROMETHEE_COUNT  = "stat_promethee_count"
    private const val STAT_ARES_PROV        = "stat_ares_provoc_today"
    private const val STAT_INV_OPEN         = "stat_inv_open_count"

    // ══════════════════════════════════════════════════════════
    // PREFS — Accès
    // ══════════════════════════════════════════════════════════

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Badges débloqués ──────────────────────────────────────

    fun isUnlocked(context: Context, badgeId: String): Boolean =
        prefs(context).getBoolean(KEY_UNLOCKED + badgeId, false)

    fun getUnlockTimestamp(context: Context, badgeId: String): Long =
        prefs(context).getLong(KEY_UNLOCK_TS + badgeId, 0L)

    fun getUnlockedIds(context: Context): Set<String> =
        BadgeCatalogue.tous
            .filter { isUnlocked(context, it.id) }
            .map { it.id }
            .toSet()

    fun getUnlockedBadges(context: Context): List<BadgeDefinition> =
        BadgeCatalogue.tous.filter { isUnlocked(context, it.id) }

    fun getLockedBadges(context: Context): List<BadgeDefinition> =
        BadgeCatalogue.tous.filter { !isUnlocked(context, it.id) }

    fun getUnlockedCount(context: Context): Int =
        BadgeCatalogue.tous.count { isUnlocked(context, it.id) }

    private fun unlock(context: Context, badgeId: String) {
        prefs(context).edit()
            .putBoolean(KEY_UNLOCKED + badgeId, true)
            .putLong(KEY_UNLOCK_TS + badgeId, System.currentTimeMillis())
            .apply()
    }

    // ── Stats mises à jour par les Activities ─────────────────

    fun recordQuizCompleted(
        context: Context,
        matiere: String,
        scorePercent: Int,
        durationSeconds: Int,
        isEpreuveUltime: Boolean
    ) {
        val p = prefs(context)
        val editor = p.edit()

        editor.putInt(KEY_QUIZ_COUNT, p.getInt(KEY_QUIZ_COUNT, 0) + 1)
        editor.putInt(KEY_SESSION_QUIZ, p.getInt(KEY_SESSION_QUIZ, 0) + 1)

        if (isEpreuveUltime) {
            editor.putInt(KEY_ULTIME_COUNT, p.getInt(KEY_ULTIME_COUNT, 0) + 1)
        }

        if (scorePercent == 100) {
            editor.putInt(KEY_PERFECT_COUNT, p.getInt(KEY_PERFECT_COUNT, 0) + 1)
            editor.putInt(KEY_CONSEC_PERFECT, p.getInt(KEY_CONSEC_PERFECT, 0) + 1)
        } else {
            editor.putInt(KEY_CONSEC_PERFECT, 0)
        }

        val keyMat = KEY_QUIZ_MAT_PREFIX + matiere
        editor.putInt(keyMat, p.getInt(keyMat, 0) + 1)

        editor.apply()
    }

    fun recordLogin(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_LOGIN_MS, System.currentTimeMillis())
            .putInt(KEY_SESSION_QUIZ, 0) // Reset session count
            .putInt(STAT_INV_OPEN, 0)
            .apply()
    }

    fun incrementStat(context: Context, statKey: String) {
        val p = prefs(context)
        p.edit().putInt(statKey, p.getInt(statKey, 0) + 1).apply()
    }

    /**
     * Enregistre un craft réussi dans les stats de forge.
     *
     * Appelé depuis ForgeActivity.onForgeClic() après chaque insertion
     * ou incrémentation réussie en inventaire.
     *
     * Incrémente STAT_ITEMS_FORGED (clé : "stat_items_forged_total"),
     * qui est lue par buildContext() → totalItemsForged → evaluateAll()
     * pour déclencher : forge_first, forge_5, forge_10, forge_master.
     *
     * Thread-safe : SharedPreferences.apply() est asynchrone et sûr
     * depuis n'importe quel thread.
     */
    fun recordItemForged(context: Context) {
        val p = prefs(context)
        p.edit().putInt(STAT_ITEMS_FORGED, p.getInt(STAT_ITEMS_FORGED, 0) + 1).apply()
    }

    fun getLastLoginDaysAgo(context: Context): Int {
        val lastMs = prefs(context).getLong(KEY_LAST_LOGIN_MS, 0L)
        if (lastMs == 0L) return 0
        val diffMs = System.currentTimeMillis() - lastMs
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getQuizCount(context: Context): Int = prefs(context).getInt(KEY_QUIZ_COUNT, 0)
    fun getPerfectCount(context: Context): Int = prefs(context).getInt(KEY_PERFECT_COUNT, 0)
    fun getConsecPerfect(context: Context): Int = prefs(context).getInt(KEY_CONSEC_PERFECT, 0)
    fun getUltimeCount(context: Context): Int = prefs(context).getInt(KEY_ULTIME_COUNT, 0)
    fun getQuizCountForMatiere(context: Context, matiere: String): Int = prefs(context).getInt(KEY_QUIZ_MAT_PREFIX + matiere, 0)

    // ══════════════════════════════════════════════════════════
    // CONTEXT BUILDER
    // ══════════════════════════════════════════════════════════

    suspend fun buildContext(
        context: Context,
        quizDoneHeure: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        quizDoneMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
        quizDurationSeconds: Int = 0,
        quizScorePercent: Int = 0,
        hasVisitedProfile: Boolean = false,
        hasVisitedSettings: Boolean = false,
        starsEarned: Int = 0,
        isAresChallenge: Boolean = false,
        aresSuccess: Boolean = false,
        isVolumeZero: Boolean = false,
        isVolumeMax: Boolean = false,
        noCorrectionClicked: Boolean = false
    ): BadgeEvalContext = withContext(Dispatchers.IO) {

        val db = AppDatabase.getDatabase(context)
        val profil = db.iAristoteDao().getUserProfile()
        val subjects = db.iAristoteDao().getDistinctSubjects()
        val totalScans = db.iAristoteDao().countCourses()
        val p = prefs(context)

        val quizParMatiere = GodManager.pantheon.associate { god ->
            god.matiere to getQuizCountForMatiere(context, god.matiere)
        }

        // Mock pour la forge (devra être connecté à ta table Inventory/Fragments)
        val totalForged = p.getInt(STAT_ITEMS_FORGED, 0)
        val fragmentsMax = 0 // A lier à ton Dao
        val fragmentsTot = 0 // A lier à ton Dao
        val invCount = 0     // A lier à ton Dao

        BadgeEvalContext(
            level                = profil?.level ?: 1,
            totalXp              = profil?.xp ?: 0,
            streak               = profil?.streak ?: 0,
            totalScans           = totalScans,
            subjectsScanned      = subjects.toSet(),
            totalQuizDone        = getQuizCount(context),
            totalPerfectScore    = getPerfectCount(context),
            consecutivePerfect   = getConsecPerfect(context),
            totalEpreuveUltime   = getUltimeCount(context),
            quizParMatiere       = quizParMatiere,
            quizDoneHeure        = quizDoneHeure,
            quizDoneMinute       = quizDoneMinute,
            quizDurationSeconds  = quizDurationSeconds,
            quizScorePercent     = quizScorePercent,
            sessionQuizCount     = p.getInt(KEY_SESSION_QUIZ, 0),
            lastLoginDaysAgo     = getLastLoginDaysAgo(context),
            tutorialCompleted    = TutorialManager.isTutorialTermine(context),
            hasVisitedProfile    = hasVisitedProfile,
            hasVisitedSettings   = hasVisitedSettings,
            starsEarned          = starsEarned,
            isAresChallenge      = isAresChallenge,
            aresSuccess          = aresSuccess,
            aresProvocationsToday= p.getInt(STAT_ARES_PROV, 0),
            itemsInInventoryCount= invCount,
            totalItemsForged     = totalForged,
            fragmentsTotal       = fragmentsTot,
            fragmentsMaxInOne    = fragmentsMax,
            forgeClickFailCount  = p.getInt(STAT_FORGE_FAIL, 0),
            mnemoClickCount      = p.getInt(STAT_MNEMO_COUNT, 0),
            apollonLyreCount     = p.getInt(STAT_APOLLON_COUNT, 0),
            prometheeHelpCount   = p.getInt(STAT_PROMETHEE_COUNT, 0),
            isVolumeZero         = isVolumeZero,
            isVolumeMax          = isVolumeMax,
            inventoryOpenCount   = p.getInt(STAT_INV_OPEN, 0),
            noCorrectionClicked  = noCorrectionClicked
        )
    }

    // ══════════════════════════════════════════════════════════
    // ÉVALUATION — 102 Badges
    // ══════════════════════════════════════════════════════════

    suspend fun evaluateAll(
        context: Context,
        ctx: BadgeEvalContext
    ): List<BadgeDefinition> = withContext(Dispatchers.IO) {

        val nouveaux = mutableListOf<BadgeDefinition>()

        suspend fun check(badgeId: String, condition: Boolean) {
            if (condition && !isUnlocked(context, badgeId)) {
                unlock(context, badgeId)
                BadgeCatalogue.parId(badgeId)?.let { badge ->
                    nouveaux.add(badge)
                    if (badge.xpRecompense > 0) {
                        val db = AppDatabase.getDatabase(context)
                        val profil = db.iAristoteDao().getUserProfile()
                        if (profil != null) {
                            val newXp    = profil.xp + badge.xpRecompense
                            val newLevel = XpCalculator.calculateLevel(newXp)
                            db.iAristoteDao().updateXpAndLevel(newXp, newLevel)
                        }
                    }
                }
            }
        }

        val allSubjects = GodManager.pantheon.map { it.matiere }.toSet()

        // ── STREAK ────────────────────────────────────────────
        check("streak_first",  ctx.streak >= 1)
        check("streak_3",      ctx.streak >= 3)
        check("streak_7",      ctx.streak >= 7)
        check("streak_14",     ctx.streak >= 14)
        check("streak_30",     ctx.streak >= 30)
        check("streak_60",     ctx.streak >= 60)
        check("streak_100",    ctx.streak >= 100)
        check("streak_200",    ctx.streak >= 200)
        check("streak_365",    ctx.streak >= 365)

        // ── XP / NIVEAU ───────────────────────────────────────
        val levels = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100)
        levels.forEach { lv -> check("level_$lv", ctx.level >= lv) }
        check("xp_1000",   ctx.totalXp >= 1000)
        check("xp_5000",   ctx.totalXp >= 5000)
        check("xp_20000",  ctx.totalXp >= 20000)

        // ── ORACLE ────────────────────────────────────
        check("scan_first",       ctx.totalScans >= 1)
        check("scan_5",           ctx.totalScans >= 5)
        check("scan_10",          ctx.totalScans >= 10)
        check("scan_25",          ctx.totalScans >= 25)
        check("scan_50",          ctx.totalScans >= 50)
        check("scan_100",         ctx.totalScans >= 100)
        check("scan_200",         ctx.totalScans >= 200)
        check("scan_all_subjects", allSubjects.all { it in ctx.subjectsScanned })

        // ── QUIZ & ARÈS ──────────────────────────────────────────────
        check("quiz_first",          ctx.totalQuizDone >= 1)
        check("quiz_5",              ctx.totalQuizDone >= 5)
        check("quiz_10",             ctx.totalQuizDone >= 10)
        check("quiz_50",             ctx.totalQuizDone >= 50)
        check("quiz_100",            ctx.totalQuizDone >= 100)
        check("quiz_250",            ctx.totalQuizDone >= 250)
        check("quiz_500",            ctx.totalQuizDone >= 500)
        check("perfect_score",       ctx.totalPerfectScore >= 1)
        check("perfect_score_3",     ctx.consecutivePerfect >= 3)
        check("epreuve_ultime_first",ctx.totalEpreuveUltime >= 1)
        check("epreuve_ultime_10",   ctx.totalEpreuveUltime >= 10)

        check("star_6",              ctx.starsEarned == 6)
        check("star_chain_5",        ctx.consecutivePerfect >= 5)
        check("ares_challenge_win",  ctx.isAresChallenge && ctx.aresSuccess)
        check("ares_rage",           ctx.isAresChallenge && !ctx.aresSuccess)
        check("ares_triple",         ctx.aresProvocationsToday >= 3)

        // ── PANTHÉON ───────────────────────────────
        check("pantheon_zeus",      (ctx.quizParMatiere["Mathématiques"] ?: 0) >= 5)
        check("pantheon_athena",    (ctx.quizParMatiere["Français"]       ?: 0) >= 5)
        check("pantheon_poseidon",  (ctx.quizParMatiere["SVT"]            ?: 0) >= 5)
        check("pantheon_ares",      (ctx.quizParMatiere["Histoire"]       ?: 0) >= 5)
        check("pantheon_aphrodite", (ctx.quizParMatiere["Art/Musique"]    ?: 0) >= 5)
        check("pantheon_hermes",    (ctx.quizParMatiere["Anglais"]        ?: 0) >= 5)
        check("pantheon_demeter",   (ctx.quizParMatiere["Géographie"]     ?: 0) >= 5)
        check("pantheon_hephaistos",(ctx.quizParMatiere["Physique-Chimie"]?: 0) >= 5)
        check("pantheon_apollon",   (ctx.quizParMatiere["Philo/SES"]      ?: 0) >= 5)
        check("pantheon_promethee", (ctx.quizParMatiere["Vie & Projets"]  ?: 0) >= 5)
        check("pantheon_complet",   allSubjects.all { (ctx.quizParMatiere[it] ?: 0) >= 1 })

        // ── FORGE ──────────────────────────────────────────
        check("forge_first",        ctx.totalItemsForged >= 1)
        check("forge_5",            ctx.totalItemsForged >= 5)
        check("forge_10",           ctx.totalItemsForged >= 10)
        check("forge_full_inv",     ctx.itemsInInventoryCount >= 10)
        check("fragment_100",       ctx.fragmentsMaxInOne >= 100)
        check("fragment_1000",      ctx.fragmentsTotal >= 1000)
        check("forge_impatient",    ctx.forgeClickFailCount >= 10)
        check("forge_night",        ctx.totalItemsForged > 0 && ctx.quizDoneHeure in 0..4)

        // ── DIVIN / INTERACTIONS ──────────────────────────────────────────
        check("divin_mnemo",        ctx.mnemoClickCount >= 1)
        check("divin_mnemo_20",     ctx.mnemoClickCount >= 20)
        check("apollon_lyre",       ctx.apollonLyreCount >= 5)
        check("promethee_help",     ctx.prometheeHelpCount >= 5)
        check("zeus_strict",        ctx.quizScorePercent in 1..49 && ctx.totalQuizDone > 0)

        // ── SPÉCIAUX & WTF ──────────────────────────────────────────
        check("first_connection",   true)
        check("tutorial_done",      ctx.tutorialCompleted)
        check("night_owl",          ctx.quizDoneHeure == 23 || ctx.quizDoneHeure == 0)
        check("early_bird",         ctx.quizDoneHeure in 5..6)
        check("settings_explorer",  ctx.hasVisitedSettings)
        check("profile_visited",    ctx.hasVisitedProfile)
        check("comeback",           ctx.lastLoginDaysAgo >= 7 && ctx.totalQuizDone > 0)
        check("speed_quiz",         ctx.quizDurationSeconds in 1..119 && ctx.totalQuizDone > 0)
        check("quiz_at_3am",        ctx.quizDoneHeure == 3)
        check("rage_fail",          ctx.quizScorePercent in 0..19 && ctx.totalQuizDone > 0)
        check("marathon_quiz",      ctx.sessionQuizCount >= 10)

        check("wtf_speed_fail",     ctx.quizDurationSeconds < 30 && ctx.quizScorePercent < 20 && ctx.totalQuizDone > 0)
        check("wtf_lucky",          ctx.quizScorePercent == 51)
        check("wtf_mute",           ctx.isVolumeZero && ctx.totalQuizDone > 0)
        check("wtf_volume_max",     ctx.isVolumeMax && ctx.quizScorePercent == 100)
        check("wtf_inv_spam",       ctx.inventoryOpenCount >= 20)
        check("wtf_no_stop",        ctx.noCorrectionClicked && ctx.sessionQuizCount >= 3)
        check("wtf_slow_win",       ctx.quizDurationSeconds > 1200 && ctx.quizScorePercent == 100) // 20 min
        check("wtf_perfect_333",    ctx.quizDoneHeure == 3 && ctx.quizDoneMinute == 33 && ctx.quizScorePercent == 100)

        // Ultime Check : Panthéon Absolu
        val unlockedCount = BadgeCatalogue.tous.count { isUnlocked(context, it.id) }
        check("all_badges", unlockedCount >= BadgeCatalogue.totalCount - 1)

        nouveaux.toList()
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS UI (Conservés intacts !)
    // ══════════════════════════════════════════════════════════

    fun getBadgesPourAffichage(context: Context): List<Pair<BadgeDefinition, Boolean>> {
        val unlocked = getUnlockedIds(context)
        val debloquesRecents = BadgeCatalogue.tous
            .filter { it.id in unlocked }
            .sortedByDescending { getUnlockTimestamp(context, it.id) }
        val verrouilles = BadgeCatalogue.tous
            .filter { it.id !in unlocked }

        return (debloquesRecents + verrouilles).map { badge ->
            Pair(badge, badge.id in unlocked)
        }
    }

    fun getBadgesParCategorie(
        context: Context,
        categorie: BadgeCategorie
    ): List<Pair<BadgeDefinition, Boolean>> {
        val unlocked = getUnlockedIds(context)
        return BadgeCatalogue.parCategorie(categorie).map { badge ->
            Pair(badge, badge.id in unlocked)
        }
    }

    fun getResume(context: Context): Triple<Int, Int, Int> {
        val debloques = getUnlockedCount(context)
        val total = BadgeCatalogue.totalCount
        val pct = if (total > 0) (debloques * 100 / total) else 0
        return Triple(debloques, total, pct)
    }

    fun getDerniersDebloques(context: Context, n: Int = 3): List<BadgeDefinition> =
        getUnlockedBadges(context)
            .sortedByDescending { getUnlockTimestamp(context, it.id) }
            .take(n)

    fun getBadgePrestige(context: Context): BadgeDefinition? {
        val unlocked = getUnlockedIds(context)
        val ordreRarete = listOf(
            BadgeRarete.LEGENDAIRE,
            BadgeRarete.EPIQUE,
            BadgeRarete.RARE,
            BadgeRarete.COMMUN
        )
        for (rarete in ordreRarete) {
            val badge = BadgeCatalogue.parRarete(rarete)
                .firstOrNull { it.id in unlocked }
            if (badge != null) return badge
        }
        return null
    }

    // ══════════════════════════════════════════════════════════
    // RESET
    // ══════════════════════════════════════════════════════════

    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
