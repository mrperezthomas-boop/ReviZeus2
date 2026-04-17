package com.revizeus.app

import android.content.Context
import android.content.SharedPreferences

/**
 * ============================================================
 * TutorialManager.kt — RéviZeus v9
 * Gestionnaire du tutoriel "Ce que Zeus t'a dit à la première
 * connexion".
 *
 * Rôle :
 *  - Stocker quelles étapes du tutoriel ont été vues
 *  - Fournir les textes de chaque étape (dieu + message)
 *  - Permettre à SettingsActivity d'afficher le tutoriel
 *    complet dans l'onglet dédié
 *  - Permettre de rejouer le tutoriel depuis le début
 *
 * Structure d'une étape :
 *   TutorialStep(
 *     id         : identifiant unique
 *     godName    : nom du dieu qui parle
 *     godDrawable: ressource avatar dialog du dieu
 *     godColor   : couleur hex du dieu
 *     title      : titre court de l'étape
 *     message    : texte complet du dialogue
 *     screen     : écran où cette étape apparaît normalement
 *   )
 * ============================================================
 */

data class TutorialStep(
    val id: String,
    val godName: String,
    val godDrawable: Int,
    val godColor: String,
    val title: String,
    val message: String,
    val screen: String
)

object TutorialManager {

    private const val PREFS_NAME = "revizeus_tutorial"
    private const val KEY_PREFIX_SEEN = "seen_"
    private const val KEY_TUTORIAL_DONE = "tutorial_completed"
    private const val KEY_PREFIX_HERO_FEATURE_SEEN = "hero_feature_seen_"

    // ══════════════════════════════════════════════════════════
    // CATALOGUE COMPLET DES ÉTAPES DU TUTORIEL
    // (dans l'ordre de la première connexion)
    // ══════════════════════════════════════════════════════════

    fun getSteps(context: Context): List<TutorialStep> = listOf(

        // ── 1. SPLASH / ACCUEIL ───────────────────────────────
        TutorialStep(
            id = "splash_bienvenue",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Bienvenue sur l'Olympe",
            message = "Mortel ! Tu as osé frapper aux portes de l'Olympe. " +
                    "Je suis Zeus, Seigneur des dieux et gardien de la connaissance. " +
                    "Ici, l'effort se transforme en puissance divine. " +
                    "Prépare-toi à une quête comme aucune autre.",
            screen = "SplashActivity"
        ),

        // ── 2. CRÉATION DU PROFIL ─────────────────────────────
        TutorialStep(
            id = "auth_profil",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Forge ton identité",
            message = "Dis-moi ton nom, héros. Un guerrier sans nom est " +
                    "un guerrier oublié. Ton pseudo sera gravé dans le " +
                    "marbre de l'Olympe pour l'éternité... ou jusqu'à " +
                    "ce que tu le changes dans les réglages.",
            screen = "AuthActivity"
        ),

        // ── 3. CHOIX DU GENRE ─────────────────────────────────
        TutorialStep(
            id = "gender_choix",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Héros ou Héroïne ?",
            message = "Sur l'Olympe, tous les chemins mènent à la gloire. " +
                    "Choisis la forme que prendra ton avatar. " +
                    "Cette décision forgera ton apparence dans tous " +
                    "les écrans de ta quête.",
            screen = "GenderActivity"
        ),

        // ── 4. CHOIX DE L'AVATAR ──────────────────────────────
        TutorialStep(
            id = "avatar_selection",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Choisis ton incarnation",
            message = "Cinq avatars t'attendent, chacun portant l'énergie " +
                    "d'un héros différent. Celui que tu choisis sera " +
                    "affiché sur ton tableau de bord chaque jour. " +
                    "Tu pourras en changer depuis les Réglages.",
            screen = "AvatarActivity"
        ),

        // ── 5. HUMEUR QUOTIDIENNE ─────────────────────────────
        TutorialStep(
            id = "mood_humeur",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "L'humeur du jour",
            message = "Chaque matin, l'Olympe s'adapte à ton état d'esprit. " +
                    "Joyeux ? Je te prépare des défis stimulants. " +
                    "Fatigué ? Je module la difficulté pour toi. " +
                    "Stressé ? Je te guide pas à pas. Sois honnête, " +
                    "car les dieux voient tout.",
            screen = "MoodActivity"
        ),

        // ── 6. LE DASHBOARD ───────────────────────────────────
        TutorialStep(
            id = "dashboard_presentation",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Ton Panthéon personnel",
            message = "Voici ton tableau de bord, héros. En haut : " +
                    "ton niveau, ton XP et ta série de jours consécutifs. " +
                    "Au centre : ton avatar, reflet de ta progression. " +
                    "En bas : trois portails sacrés — l'Oracle, " +
                    "l'Entraînement, et la Bibliothèque du Savoir.",
            screen = "DashboardActivity"
        ),

        // ── 7. XP ET NIVEAUX ──────────────────────────────────
        TutorialStep(
            id = "dashboard_xp",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "L'expérience divine (XP)",
            message = "Chaque bonne réponse te rapporte de l'XP. " +
                    "500 XP = niveau suivant. Plus tu montes en niveau, " +
                    "plus tu te rapproches du statut de Demi-Dieu. " +
                    "La difficulté 'Maître' multiplie tes gains d'XP. " +
                    "La paresse, elle, les réduit.",
            screen = "DashboardActivity"
        ),

        // ── 8. LE STREAK ─────────────────────────────────────
        TutorialStep(
            id = "dashboard_streak",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "La série de feu 🔥",
            message = "Reviens chaque jour et ta flamme grandit. " +
                    "Un seul jour manqué et tout s'effondre. " +
                    "Chronos, dieu du Temps, veille sur ta constance. " +
                    "Active les notifications pour ne jamais briser " +
                    "ta série — les dieux récompensent la régularité.",
            screen = "DashboardActivity"
        ),

        // ── 9. L'ORACLE (Scanner) ─────────────────────────────
        TutorialStep(
            id = "oracle_scanner",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "L'Oracle — Œil Divin",
            message = "Appuie sur l'œil géant pour invoquer l'Oracle. " +
                    "Pointe ta caméra vers n'importe quel cours, " +
                    "exercice ou chapitre. L'Oracle — alimenté par " +
                    "Aristote et la magie de Gemini — analyse, résume " +
                    "et génère des questions sur ce qu'il voit. " +
                    "C'est ta forge principale de savoir.",
            screen = "OracleActivity"
        ),

        // ── 10. LES RÉSULTATS DE SCAN ─────────────────────────
        TutorialStep(
            id = "result_analyse",
            godName = "ATHÉNA",
            godDrawable = R.drawable.avatar_athena_dialog,
            godColor = "#FFD700",
            title = "Le Résumé d'Athéna",
            message = "Après chaque scan, je te livre un résumé structuré " +
                    "et les concepts-clés à retenir. Ce résumé est sauvegardé " +
                    "dans ta Bibliothèque. En bas, tu trouveras les questions " +
                    "générées par l'Oracle. Commence le quiz quand tu te sens prêt.",
            screen = "ResultActivity"
        ),

        // ── 11. LE QUIZ ───────────────────────────────────────
        TutorialStep(
            id = "quiz_regles",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Les règles du combat",
            message = "Le quiz te propose plusieurs choix. Un seul est juste. " +
                    "Bonne réponse : tu gagnes de l'XP et le dieu de " +
                    "la matière te félicite. Mauvaise réponse : le dieu " +
                    "t'explique où tu t'es trompé. " +
                    "Lis les corrections — elles forgent ta mémoire.",
            screen = "QuizActivity"
        ),

        // ── 12. LA BIBLIOTHÈQUE ───────────────────────────────
        TutorialStep(
            id = "savoir_bibliotheque",
            godName = "ATHÉNA",
            godDrawable = R.drawable.avatar_athena_dialog,
            godColor = "#FFD700",
            title = "La Bibliothèque du Savoir",
            message = "Tous tes cours scannés sont ici, classés par matière " +
                    "et par dieu tutélaire. Chaque parchemin contient " +
                    "le résumé et les concepts que l'Oracle a extraits. " +
                    "Consulte-les avant un quiz pour rafraîchir ta mémoire. " +
                    "La connaissance stockée est une arme.",
            screen = "SavoirActivity"
        ),

        // ── 13. L'ENTRAÎNEMENT ────────────────────────────────
        TutorialStep(
            id = "training_entrainement",
            godName = "ARÈS",
            godDrawable = R.drawable.avatar_ares_dialog,
            godColor = "#DAA520",
            title = "L'Arène d'Arès",
            message = "L'Entraînement te permet de te tester matière par matière " +
                    "sans attendre un nouveau scan. Choisis ton dieu, " +
                    "affronte ses questions. L'Épreuve Ultime, elle, " +
                    "mélange toutes les matières pour les vrais guerriers. " +
                    "Seule l'Épreuve Ultime rapporte de l'XP.",
            screen = "TrainingSelectActivity"
        ),

        // ── 14. LA RÉVISION ESPACÉE ───────────────────────────
        TutorialStep(
            id = "chronos_revision",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Chronos et la mémoire",
            message = "Chronos, dieu du Temps, surveille tes performances. " +
                    "Les concepts que tu maîtrises mal remontent " +
                    "automatiquement en priorité dans les quiz. " +
                    "C'est la révision espacée — la méthode la plus " +
                    "efficace pour ancrer la connaissance dans la durée. " +
                    "Tu peux l'activer ou désactiver dans les Réglages.",
            screen = "DashboardActivity"
        ),

        // ── 15. LES RÉGLAGES ──────────────────────────────────
        TutorialStep(
            id = "settings_reglages",
            godName = "HÉPHAÏSTOS",
            godDrawable = R.drawable.avatar_hephaistos_dialog,
            godColor = "#FF8C00",
            title = "L'Atelier d'Héphaïstos",
            message = "Les Réglages sont mon domaine. " +
                    "Audio, affichage, accessibilité, notifications, " +
                    "difficulté, nombre de questions, timer... " +
                    "Tout est ajustable. C'est ici que tu personnalises " +
                    "ton Olympe selon tes besoins. " +
                    "N'oublie pas — un outil bien réglé forge mieux.",
            screen = "SettingsActivity"
        )
    )

    // ══════════════════════════════════════════════════════════
    // GESTION DE L'ÉTAT "VU / NON VU"
    // ══════════════════════════════════════════════════════════

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Marque une étape comme vue */
    fun marquerVue(context: Context, stepId: String) {
        getPrefs(context).edit().putBoolean(KEY_PREFIX_SEEN + stepId, true).apply()
    }

    /** Vérifie si une étape a déjà été vue */
    fun estVue(context: Context, stepId: String): Boolean =
        getPrefs(context).getBoolean(KEY_PREFIX_SEEN + stepId, false)

    /** Marque le tutoriel complet comme terminé */
    fun marquerTutorialTermine(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_TUTORIAL_DONE, true).apply()
    }

    /** Vérifie si le tutoriel d'intro a été complété */
    fun isTutorialTermine(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_TUTORIAL_DONE, false)

    /**
     * Retourne les étapes non encore vues — utile pour les
     * bulles "Tu n'as pas encore lu ça !" dans le jeu.
     */
    fun getEtapesNonVues(context: Context): List<TutorialStep> =
        getSteps(context).filter { !estVue(context, it.id) }

    /**
     * Réinitialise tout le tutoriel (toutes les étapes repassent
     * en "non vu"). Utilisé depuis les Réglages → "Rejouer le tutoriel".
     */
    fun reinitialiserTutorial(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        getSteps(context).forEach { editor.remove(KEY_PREFIX_SEEN + it.id) }
        editor.remove(KEY_TUTORIAL_DONE)
        editor.apply()
    }

    /**
     * Retourne le nombre d'étapes vues sur le total.
     * Ex : "12 / 15"
     */
    fun getProgression(context: Context): Pair<Int, Int> {
        val steps = getSteps(context)
        val vues  = steps.count { estVue(context, it.id) }
        return Pair(vues, steps.size)
    }

    /**
     * Retourne les étapes filtrées par écran.
     * Utile pour n'afficher que les conseils de l'écran en cours.
     */
    fun getStepsPourEcran(context: Context, screenName: String): List<TutorialStep> =
        getSteps(context).filter { it.screen == screenName }

    /**
     * Retourne les étapes filtrées par dieu.
     * Utile pour l'onglet Tutoriel (filtrer par dieu).
     */
    fun getStepsPourDieu(context: Context, godName: String): List<TutorialStep> =
        getSteps(context).filter { it.godName == godName }


    // ══════════════════════════════════════════════════════════
    // TUTORIELS CONTEXTUELS PAR HÉROS / HÉROÏNE
    // ══════════════════════════════════════════════════════════

    /**
     * Scope persistant du héros actif.
     * Format : uid_slotX. Fallback local si aucun héros n'est encore actif.
     */
    fun getActiveHeroScope(context: Context): String {
        val uid = try { AccountRegistry.getActiveUid(context) } catch (_: Exception) { "" }
        val safeUid = uid.trim().ifBlank { "local_hero" }
        val slot = try { AccountRegistry.getActiveSlot(context, uid) } catch (_: Exception) { 1 }
        return "${safeUid}_slot${slot.coerceAtLeast(1)}"
    }

    private fun getHeroStepKey(context: Context, stepId: String): String {
        return "hero_${getActiveHeroScope(context)}_${KEY_PREFIX_SEEN}$stepId"
    }

    fun marquerVuePourHero(context: Context, stepId: String) {
        getPrefs(context).edit().putBoolean(getHeroStepKey(context, stepId), true).apply()
    }

    fun estVuePourHero(context: Context, stepId: String): Boolean {
        return getPrefs(context).getBoolean(getHeroStepKey(context, stepId), false)
    }

    fun reinitialiserTutorielsDuHeroActif(context: Context) {
        val prefs = getPrefs(context)
        val scopePrefix = "hero_${getActiveHeroScope(context)}_${KEY_PREFIX_SEEN}"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(scopePrefix) }.forEach { editor.remove(it) }
        editor.apply()
    }

    /**
     * Helper central pour afficher une explication contextuelle une seule fois par héros.
     * Conserve le rendu RPG premium existant via DialogRPGManager.
     */
    fun showHeroTutorialIfNeeded(
        activity: androidx.appcompat.app.AppCompatActivity,
        stepId: String,
        godId: String,
        title: String,
        message: String,
        onDismiss: (() -> Unit)? = null
    ) {
        if (estVuePourHero(activity, stepId)) {
            onDismiss?.invoke()
            return
        }
        marquerVuePourHero(activity, stepId)
        DialogRPGManager.showInfo(
            activity = activity,
            godId = godId,
            title = title,
            message = message,
            onDismiss = onDismiss
        )
    }


    private fun getHeroFeatureStep(featureId: String): TutorialStep? = when (featureId) {
        "dashboard" -> TutorialStep(
            id = "feature_dashboard",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Le Panthéon du héros",
            message = "Voici le coeur de ton aventure. Depuis ce tableau sacré, tu surveilles ta progression, ton humeur, tes richesses et les grands portails de RéviZeus. Chaque retour ici est un recentrage avant le prochain exploit.",
            screen = "DashboardActivity"
        )
        "oracle" -> TutorialStep(
            id = "feature_oracle",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "L'Oracle",
            message = "L'Oracle observe tes cours, invoque l'IA et transforme la matière brute en savoir exploitable. Tu y analyses un contenu, tu obtiens un résumé, puis tu lances le combat du quiz pour graver la leçon dans ta mémoire.",
            screen = "OracleActivity"
        )
        "library" -> TutorialStep(
            id = "feature_library",
            godName = "ATHÉNA",
            godDrawable = R.drawable.avatar_athena_dialog,
            godColor = "#FFD700",
            title = "La Bibliothèque du Savoir",
            message = "Ici reposent tous les savoirs que tu as déjà sauvés. Tu peux les relire, les trier, les retravailler, lancer leurs quiz et, plus tard, invoquer d'autres pouvoirs divins autour d'eux.",
            screen = "SavoirActivity"
        )
        "training" -> TutorialStep(
            id = "feature_training",
            godName = "ARES",
            godDrawable = R.drawable.avatar_ares_dialog,
            godColor = "#C62828",
            title = "L'Entraînement",
            message = "L'entraînement est ton arène. Tu y affrontes des questions ciblées pour gagner en maîtrise, en fragments et en réflexes. Plus tu t'y rends, plus ton héros devient dangereux face à l'oubli.",
            screen = "TrainingSelectActivity"
        )
        "forge" -> TutorialStep(
            id = "feature_forge",
            godName = "HEPHAISTOS",
            godDrawable = R.drawable.avatar_hephaistos_dialog,
            godColor = "#FF8C00",
            title = "La Forge",
            message = "À la Forge, les savoirs et les ressources se transforment. C'est l'atelier des créations divines, des objets utiles et des mécaniques avancées qui enrichissent ta progression.",
            screen = "ForgeActivity"
        )
        "inventory" -> TutorialStep(
            id = "feature_inventory",
            godName = "HERMES",
            godDrawable = R.drawable.avatar_hermes_dialog,
            godColor = "#87CEEB",
            title = "Inventaire",
            message = "Ton Inventaire rassemble les objets déjà obtenus. Tu y vérifies ce que tu possèdes, ce qui peut être utilisé plus tard et ce qui vient d'être forgé. C'est la mémoire matérielle de ton aventure dans RéviZeus.",
            screen = "InventoryActivity"
        )
        "mood" -> TutorialStep(
            id = "feature_mood",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Humeur du héros",
            message = "Ton humeur n'est pas décorative. Elle influence l'ambiance sonore et aide l'IA à ajuster son ton, sa difficulté et son accompagnement. Quand ton état change, viens ici pour que l'Olympe s'accorde vraiment à toi.",
            screen = "MoodActivity"
        )
        "adventure_locked" -> TutorialStep(
            id = "feature_adventure_locked",
            godName = "ZEUS",
            godDrawable = R.drawable.avatar_zeus_dialog,
            godColor = "#1E90FF",
            title = "Route d'aventure",
            message = "Cette route n'est pas encore ouverte, mais elle accueillera la grande progression scénarisée de RéviZeus. Plus tard, elle reliera tes temples, tes exploits et l'évolution vivante de l'Olympe. Pour l'instant, concentre-toi sur les savoirs, l'entraînement et la forge.",
            screen = "DashboardActivity"
        )
        else -> null
    }

    fun hasHeroSeenFeature(context: Context, featureId: String): Boolean {
        val heroScope = getActiveHeroScope(context)
        return getPrefs(context).getBoolean("$KEY_PREFIX_HERO_FEATURE_SEEN${heroScope}_$featureId", false)
    }

    fun markHeroFeatureSeen(context: Context, featureId: String) {
        val heroScope = getActiveHeroScope(context)
        getPrefs(context).edit()
            .putBoolean("$KEY_PREFIX_HERO_FEATURE_SEEN${heroScope}_$featureId", true)
            .apply()
    }

    fun resetHeroFeature(context: Context, featureId: String) {
        val heroScope = getActiveHeroScope(context)
        getPrefs(context).edit()
            .remove("$KEY_PREFIX_HERO_FEATURE_SEEN${heroScope}_$featureId")
            .apply()
    }

    fun runHeroFirstTimeFeature(
        activity: androidx.appcompat.app.AppCompatActivity,
        featureId: String,
        onContinue: () -> Unit
    ) {
        val step = getHeroFeatureStep(featureId)
        if (step == null || hasHeroSeenFeature(activity, featureId)) {
            onContinue()
            return
        }

        DialogRPGManager.showInfo(
            activity = activity,
            godId = when (step.godName.uppercase()) {
                "ATHÉNA" -> "athena"
                "ARES", "ARÈS" -> "ares"
                "HEPHAISTOS", "HÉPHAÏSTOS" -> "hephaistos"
                "HERMES", "HERMÈS" -> "hermes"
                else -> "zeus"
            },
            title = step.title,
            message = step.message,
            onDismiss = {
                markHeroFeatureSeen(activity, featureId)
                onContinue()
            }
        )
    }

}