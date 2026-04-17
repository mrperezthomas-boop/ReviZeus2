package com.revizeus.app

/**
 * ============================================================
 * BadgeDefinition.kt — RéviZeus v10 (L'Éveil de l'Olympe)
 * Définitions statiques de tous les badges/succès du jeu.
 * * 102 Badges au total (52 Classiques + 50 Nouvelle Ère)
 *
 * Chaque BadgeDefinition est IMMUABLE — elle décrit ce qu'est
 * un badge. L'état "débloqué/verrouillé" est stocké ailleurs
 * (dans BadgeManager via SharedPreferences).
 *
 * Raretés :
 * COMMUN      → gris/argent  — facile à obtenir
 * RARE        → bleu         — effort modéré
 * EPIQUE      → violet       — effort soutenu
 * LEGENDAIRE  → or brillant  — exploit exceptionnel
 * ============================================================
 */

enum class BadgeRarete(
    val label: String,
    val colorHex: String,
    val frameDrawable: Int
) {
    COMMUN    ("Commun",     "#C0C0C0", R.drawable.bg_badge_frame_commun),
    RARE      ("Rare",       "#1E90FF", R.drawable.bg_badge_frame_rare),
    EPIQUE    ("Épique",     "#9B59B6", R.drawable.bg_badge_frame_epique),
    LEGENDAIRE("Légendaire", "#FFD700", R.drawable.bg_badge_frame_legendaire)
}

enum class BadgeCategorie(val label: String, val emoji: String) {
    STREAK    ("Persévérance",  "🔥"),
    XP_NIVEAU ("Progression",  "⚡"),
    ORACLE    ("Oracle",        "👁"),
    QUIZ      ("Combat",        "⚔"),
    PANTHEON  ("Panthéon",      "🏛"),
    FORGE     ("Artisanat",     "⚒️"),  // NOUVEAU
    DIVIN     ("Interactions",  "💬"), // NOUVEAU
    SPECIAL   ("Spécial",       "✨")
}

data class BadgeDefinition(
    val id: String,                    // identifiant unique ex: "streak_7"
    val nom: String,                   // "Flamme de 7 Jours"
    val description: String,           // "Connecte-toi 7 jours de suite"
    val descriptionBloquee: String,    // "??? — Continue ta série"
    val iconDrawable: Int,             // R.drawable.badge_streak_7
    val rarete: BadgeRarete,
    val categorie: BadgeCategorie,
    val xpRecompense: Int = 0          // XP bonus accordé à l'obtention
)

/**
 * Catalogue complet des 102 badges de RéviZeus.
 * Accès : BadgeCatalogue.tous, BadgeCatalogue.parCategorie(), etc.
 */
object BadgeCatalogue {

    val tous: List<BadgeDefinition> = listOf(

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : STREAK — Persévérance
        // ═══════════════════════════════════════════════════

        BadgeDefinition("streak_first", "L'Éveil du Héros", "Tu as effectué ta première session.", "??? — Reviens demain.", R.drawable.badge_streak_first, BadgeRarete.COMMUN, BadgeCategorie.STREAK, 50),
        BadgeDefinition("streak_3", "Rituel de 3 Jours", "3 jours consécutifs sur l'Olympe.", "??? — La constance forge les dieux.", R.drawable.badge_streak_3, BadgeRarete.COMMUN, BadgeCategorie.STREAK, 100),
        BadgeDefinition("streak_7", "Flamme de 7 Jours", "7 jours consécutifs sans faillir.", "??? — La flamme demande une semaine.", R.drawable.badge_streak_7, BadgeRarete.RARE, BadgeCategorie.STREAK, 200),
        BadgeDefinition("streak_14", "Deux Semaines de Feu", "14 jours de révision ininterrompus.", "??? — Deux semaines sur l'Olympe.", R.drawable.badge_streak_14, BadgeRarete.RARE, BadgeCategorie.STREAK, 350),
        BadgeDefinition("streak_30", "Gardien de la Flamme", "30 jours consécutifs. Chronos t'honore.", "??? — Un mois sans faillir.", R.drawable.badge_streak_30, BadgeRarete.EPIQUE, BadgeCategorie.STREAK, 750),
        BadgeDefinition("streak_60", "Serviteur de Chronos", "60 jours. Le temps lui-même te respecte.", "??? — Deux mois de dévotion.", R.drawable.badge_streak_60, BadgeRarete.EPIQUE, BadgeCategorie.STREAK, 1500),
        BadgeDefinition("streak_100", "Immortel", "100 jours consécutifs. Tu es au-delà des mortels.", "??? — Les immortels seuls connaissent ce chemin.", R.drawable.badge_streak_100, BadgeRarete.LEGENDAIRE, BadgeCategorie.STREAK, 3000),
        BadgeDefinition("streak_200", "Flamme Éternelle", "200 jours consécutifs.", "??? — La flamme éternelle.", R.drawable.badge_streak_200, BadgeRarete.LEGENDAIRE, BadgeCategorie.STREAK, 5000),
        BadgeDefinition("streak_365", "Cycle des Dieux", "365 jours consécutifs.", "??? — Une année parfaite.", R.drawable.badge_streak_365, BadgeRarete.LEGENDAIRE, BadgeCategorie.STREAK, 10000),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : XP / NIVEAU — Progression
        // ═══════════════════════════════════════════════════

        BadgeDefinition("level_5", "Initié", "Tu as atteint le niveau 5.", "??? — Progresse jusqu'au niveau 5.", R.drawable.badge_level_5, BadgeRarete.COMMUN, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_10", "Héros Confirmé", "Niveau 10 atteint. Les dieux te remarquent.", "??? — Le niveau 10 est ton prochain horizon.", R.drawable.badge_level_10, BadgeRarete.RARE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_15", "Guerrier Ascendant", "Tu as atteint le niveau 15.", "??? — L'ascension continue.", R.drawable.badge_level_15, BadgeRarete.RARE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_20", "Demi-Dieu", "Niveau 20. Tu transcendes la condition mortelle.", "??? — Seuls les demi-dieux franchissent ce seuil.", R.drawable.badge_level_20, BadgeRarete.EPIQUE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_25", "Champion de l'Olympe", "Niveau 25 atteint.", "??? — Les champions atteignent ce niveau.", R.drawable.badge_level_25, BadgeRarete.EPIQUE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_30", "Ascension Divine", "Niveau 30.", "??? — L'ascension continue.", R.drawable.badge_level_30, BadgeRarete.EPIQUE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_35", "Héritier de Zeus", "Niveau 35 atteint.", "??? — Zeus observe.", R.drawable.badge_level_35, BadgeRarete.EPIQUE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_40", "Élu de l'Olympe", "Niveau 40.", "??? — Les élus seuls arrivent ici.", R.drawable.badge_level_40, BadgeRarete.EPIQUE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_45", "Foudre Montante", "Niveau 45.", "??? — La foudre gronde.", R.drawable.badge_level_45, BadgeRarete.EPIQUE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_50", "Ascension", "Niveau 50. L'Olympe t'accueille parmi ses pairs.", "??? — L'ascension est réservée aux plus grands.", R.drawable.badge_level_50, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU, 1000),
        BadgeDefinition("level_55", "Oracle Vivant", "Niveau 55.", "??? — La sagesse grandit.", R.drawable.badge_level_55, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_60", "Colosse de Savoir", "Niveau 60.", "??? — Un colosse se forme.", R.drawable.badge_level_60, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_65", "Titan de Mémoire", "Niveau 65.", "??? — Les titans émergent.", R.drawable.badge_level_65, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_70", "Dieu Mineur", "Niveau 70.", "??? — Tu approches du Panthéon.", R.drawable.badge_level_70, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_75", "Divinité Savante", "Niveau 75.", "??? — Les dieux observent.", R.drawable.badge_level_75, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_80", "Seigneur du Savoir", "Niveau 80.", "??? — La maîtrise approche.", R.drawable.badge_level_80, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_85", "Oracle Suprême", "Niveau 85.", "??? — Les sages atteignent ce seuil.", R.drawable.badge_level_85, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_90", "Archonte du Savoir", "Niveau 90.", "??? — Le sommet approche.", R.drawable.badge_level_90, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_95", "Sage Cosmique", "Niveau 95.", "??? — Peu atteignent cette sagesse.", R.drawable.badge_level_95, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU),
        BadgeDefinition("level_100", "Zeus du Savoir", "Niveau 100 atteint.", "??? — Seuls les immortels arrivent ici.", R.drawable.badge_level_100, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU, 5000),
        BadgeDefinition("xp_1000", "Mille Éclairs", "1 000 XP accumulés au total.", "??? — Accumule 1000 XP.", R.drawable.badge_xp_1000, BadgeRarete.COMMUN, BadgeCategorie.XP_NIVEAU, 50),
        BadgeDefinition("xp_5000", "Trésor de l'Olympe", "5 000 XP. La richesse divine est en toi.", "??? — 5000 XP sont nécessaires.", R.drawable.badge_xp_5000, BadgeRarete.RARE, BadgeCategorie.XP_NIVEAU, 100),
        BadgeDefinition("xp_20000", "Légende Vivante", "20 000 XP. Ton nom sera gravé pour l'éternité.", "??? — Un trésor de 20 000 XP attend.", R.drawable.badge_xp_20000, BadgeRarete.LEGENDAIRE, BadgeCategorie.XP_NIVEAU, 500),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : ORACLE — Cours scannés
        // ═══════════════════════════════════════════════════

        BadgeDefinition("scan_first", "Premier Regard", "Tu as utilisé l'Oracle pour la première fois.", "??? — L'Oracle attend ton premier regard.", R.drawable.badge_first_scan, BadgeRarete.COMMUN, BadgeCategorie.ORACLE, 50),
        BadgeDefinition("scan_5", "Curieux Éveillé", "5 cours scannés par l'Oracle.", "??? — L'Oracle attend 5 offrandes.", R.drawable.badge_scan_5, BadgeRarete.COMMUN, BadgeCategorie.ORACLE, 100),
        BadgeDefinition("scan_10", "Lecteur Assidu", "10 cours dans la bibliothèque divine.", "??? — 10 parchemins dans le Savoir.", R.drawable.badge_scan_10, BadgeRarete.RARE, BadgeCategorie.ORACLE, 200),
        BadgeDefinition("scan_25", "Archiviste Divin", "25 cours capturés. Aristote est impressionné.", "??? — L'archive divine demande 25 parchemins.", R.drawable.badge_scan_25, BadgeRarete.RARE, BadgeCategorie.ORACLE, 300),
        BadgeDefinition("scan_50", "Bibliothécaire de l'Olympe", "50 cours. Ton Savoir rivalise avec celui d'Athéna.", "??? — 50 cours pour égaler la déesse.", R.drawable.badge_scan_50, BadgeRarete.EPIQUE, BadgeCategorie.ORACLE, 500),
        BadgeDefinition("scan_100", "Grand Archiviste", "100 cours scannés.", "??? — L'archive grandit.", R.drawable.badge_scan_100, BadgeRarete.EPIQUE, BadgeCategorie.ORACLE),
        BadgeDefinition("scan_200", "Oracle Absolu", "200 cours scannés.", "??? — L'oracle sait tout.", R.drawable.badge_scan_200, BadgeRarete.LEGENDAIRE, BadgeCategorie.ORACLE),
        BadgeDefinition("scan_all_subjects", "Encyclopédie Vivante", "Au moins 1 cours scanné dans chaque matière.", "??? — Explore toutes les matières du Panthéon.", R.drawable.badge_all_subjects, BadgeRarete.LEGENDAIRE, BadgeCategorie.ORACLE, 1000),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : QUIZ / COMBAT & ARÈS
        // ═══════════════════════════════════════════════════

        BadgeDefinition("quiz_first", "Premier Combat", "Tu as affronté ton premier quiz.", "??? — Entre dans l'arène pour la première fois.", R.drawable.badge_first_quiz, BadgeRarete.COMMUN, BadgeCategorie.QUIZ, 50),
        BadgeDefinition("quiz_5", "Combattant Régulier", "5 quiz complétés. L'arène te connaît.", "??? — 5 combats dans l'arène.", R.drawable.badge_quiz_5, BadgeRarete.COMMUN, BadgeCategorie.QUIZ, 100),
        BadgeDefinition("quiz_10", "Arène Confirmée", "10 quiz réalisés. Zeus est satisfait.", "??? — 10 combats sont requis.", R.drawable.badge_quiz_10, BadgeRarete.RARE, BadgeCategorie.QUIZ, 200),
        BadgeDefinition("quiz_50", "Gladiateur de l'Olympe", "50 quiz. Tu es une légende de l'arène.", "??? — 50 combats forment un gladiateur.", R.drawable.badge_quiz_50, BadgeRarete.EPIQUE, BadgeCategorie.QUIZ, 750),
        BadgeDefinition("quiz_100", "Maître de l'Arène", "100 quiz. L'Olympe entier s'incline.", "??? — 100 combats pour maîtriser l'arène.", R.drawable.badge_quiz_100, BadgeRarete.LEGENDAIRE, BadgeCategorie.QUIZ, 2000),
        BadgeDefinition("quiz_250", "Champion de l'Arène", "250 quiz complétés.", "??? — Continue les combats.", R.drawable.badge_quiz_250, BadgeRarete.EPIQUE, BadgeCategorie.QUIZ),
        BadgeDefinition("quiz_500", "Seigneur de l'Arène", "500 quiz complétés.", "??? — Un seigneur combat sans cesse.", R.drawable.badge_quiz_500, BadgeRarete.LEGENDAIRE, BadgeCategorie.QUIZ),
        BadgeDefinition("perfect_score", "Perfection Divine", "Score parfait (100%) à un quiz.", "??? — Aucune erreur. La perfection existe.", R.drawable.badge_perfect_score, BadgeRarete.EPIQUE, BadgeCategorie.QUIZ, 500),
        BadgeDefinition("perfect_score_3", "Trilogie Parfaite", "3 scores parfaits consécutifs.", "??? — Trois fois parfait de suite.", R.drawable.badge_perfect_score_3, BadgeRarete.LEGENDAIRE, BadgeCategorie.QUIZ, 1500),
        BadgeDefinition("epreuve_ultime_first", "Téméraire", "Tu as osé l'Épreuve Ultime.", "??? — L'Épreuve Ultime n'est pas pour les timides.", R.drawable.badge_ultime_first, BadgeRarete.RARE, BadgeCategorie.QUIZ, 300),
        BadgeDefinition("epreuve_ultime_10", "Guerrier Éternel", "10 Épreuves Ultimes complétées.", "??? — 10 fois face au défi ultime.", R.drawable.badge_ultime_10, BadgeRarete.LEGENDAIRE, BadgeCategorie.QUIZ, 2500),
        // Nouveautés Arès & Étoiles :
        BadgeDefinition("star_6", "Éclat Divin", "Obtenir la 6ème étoile mythique.", "??? — Atteins la perfection absolue.", R.drawable.badge_star_6, BadgeRarete.LEGENDAIRE, BadgeCategorie.QUIZ, 1000),
        BadgeDefinition("star_chain_5", "Constellation", "Obtenir 5 étoiles sur 5 quiz d'affilée.", "??? — Le ciel s'aligne.", R.drawable.badge_star_5x5, BadgeRarete.EPIQUE, BadgeCategorie.QUIZ, 600),
        BadgeDefinition("ares_challenge_win", "Dompteur de Guerre", "Réussir un défi lancé par Arès.", "??? — Arès t'observe.", R.drawable.badge_ares_win, BadgeRarete.EPIQUE, BadgeCategorie.QUIZ, 750),
        BadgeDefinition("ares_rage", "Colère d'Arès", "Échouer à un défi d'Arès.", "??? — Tu as déçu le Dieu de la Guerre.", R.drawable.badge_ares_fail, BadgeRarete.COMMUN, BadgeCategorie.QUIZ, 50),
        BadgeDefinition("ares_triple", "Cible Mouvante", "Provoquer Arès 3 fois en une journée.", "??? — Tu aimes le danger.", R.drawable.badge_ares_3, BadgeRarete.LEGENDAIRE, BadgeCategorie.QUIZ, 1200),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : PANTHÉON — Maîtrise des matières
        // ═══════════════════════════════════════════════════

        BadgeDefinition("pantheon_zeus", "Disciple de Zeus", "5 quiz de Mathématiques complétés.", "??? — Prouve ta valeur à Zeus.", R.drawable.badge_zeus_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_athena", "Voix d'Athéna", "5 quiz de Français complétés.", "??? — La stratège du Verbe t'attend.", R.drawable.badge_athena_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_poseidon", "Marin des Cellules", "5 quiz de SVT complétés.", "??? — Explore les profondeurs du vivant.", R.drawable.badge_poseidon_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_ares", "Gardien du Passé", "5 quiz d'Histoire complétés.", "??? — Arès juge ceux qui ignorent l'Histoire.", R.drawable.badge_ares_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_aphrodite", "Âme Créatrice", "5 quiz d'Art/Musique complétés.", "??? — L'Inspiratrice récompense les sensibles.", R.drawable.badge_aphrodite_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_hermes", "Messager Agile", "5 quiz d'Anglais complétés.", "??? — Hermès teste tes mots.", R.drawable.badge_hermes_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_demeter", "Arpenteur des Terres", "5 quiz de Géographie complétés.", "??? — Déméter attend que tu cartographies.", R.drawable.badge_demeter_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_hephaistos", "Forgeron de la Science", "5 quiz de Physique-Chimie complétés.", "??? — L'atelier d'Héphaïstos t'attend.", R.drawable.badge_hephaistos_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_apollon", "Chercheur de Vérité", "5 quiz de Philo/SES complétés.", "??? — Apollon n'ouvre sa porte qu'aux réfléchis.", R.drawable.badge_apollon_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_promethee", "Porteur de Flamme", "5 quiz de Vie & Projets complétés.", "??? — Prométhée croit en ceux qui osent.", R.drawable.badge_promethee_master, BadgeRarete.RARE, BadgeCategorie.PANTHEON, 200),
        BadgeDefinition("pantheon_complet", "Panthéon Complet", "Au moins 1 quiz complété dans chaque matière.", "??? — Tous les dieux réclament ton attention.", R.drawable.badge_all_subjects, BadgeRarete.LEGENDAIRE, BadgeCategorie.PANTHEON, 2000),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : FORGE & FRAGMENTS (NOUVEAU)
        // ═══════════════════════════════════════════════════
        BadgeDefinition("forge_first", "Apprenti Forgeron", "Premier objet forgé.", "??? — L'enclume t'attend.", R.drawable.badge_forge_first, BadgeRarete.COMMUN, BadgeCategorie.FORGE, 100),
        BadgeDefinition("forge_5", "Main de Fer", "5 objets forgés.", "??? — Frappe encore.", R.drawable.badge_forge_5, BadgeRarete.RARE, BadgeCategorie.FORGE, 250),
        BadgeDefinition("forge_10", "Maître Artisan", "10 objets forgés.", "??? — L'atelier est ton domaine.", R.drawable.badge_forge_10, BadgeRarete.EPIQUE, BadgeCategorie.FORGE, 500),
        BadgeDefinition("forge_full_inv", "Collectionneur de Reliques", "Avoir 10 objets différents dans l'inventaire.", "??? — Remplis ta sacoche.", R.drawable.badge_forge_full, BadgeRarete.EPIQUE, BadgeCategorie.FORGE, 600),
        BadgeDefinition("fragment_100", "Amas de Savoir", "Posséder 100 fragments d'une même matière.", "??? — Accumule la matière brute.", R.drawable.badge_frag_100, BadgeRarete.COMMUN, BadgeCategorie.FORGE, 100),
        BadgeDefinition("fragment_1000", "Magnat des Éclats", "Posséder 1000 fragments au total.", "??? — Richesse intellectuelle.", R.drawable.badge_frag_1000, BadgeRarete.LEGENDAIRE, BadgeCategorie.FORGE, 1000),
        BadgeDefinition("forge_impatient", "Le Marteau Vide", "Cliquer 10 fois sur Forger sans les ressources.", "??? — Héphaïstos s'énerve.", R.drawable.badge_forge_impatient, BadgeRarete.COMMUN, BadgeCategorie.FORGE, 50),
        BadgeDefinition("artefact_pythagore", "Géomètre Sacré", "Forger le Bouclier de Pythagore.", "??? — Recette mathématique requise.", R.drawable.badge_art_pyth, BadgeRarete.RARE, BadgeCategorie.FORGE, 300),
        BadgeDefinition("artefact_athena", "Plume de Sagesse", "Forger la Plume d'Athéna.", "??? — Recette de français requise.", R.drawable.badge_art_athena, BadgeRarete.RARE, BadgeCategorie.FORGE, 300),
        BadgeDefinition("artefact_ares", "Glaive du Passé", "Forger le Glaive d'Arès.", "??? — Recette d'Histoire requise.", R.drawable.badge_art_ares, BadgeRarete.RARE, BadgeCategorie.FORGE, 300),
        BadgeDefinition("forge_night", "Forgeron de Nuit", "Forger un objet entre 0h et 4h du matin.", "??? — Le feu ne dort jamais.", R.drawable.badge_forge_night, BadgeRarete.EPIQUE, BadgeCategorie.FORGE, 400),
        BadgeDefinition("forge_vibration", "Résonance", "Ressentir les 3 vibrations de forge.", "??? — Écoute le métal.", R.drawable.badge_forge_vib, BadgeRarete.COMMUN, BadgeCategorie.FORGE, 50),
        BadgeDefinition("forge_master", "Héritier d'Héphaïstos", "Débloquer toutes les recettes de forge.", "??? — Le catalogue est vaste.", R.drawable.badge_forge_master, BadgeRarete.LEGENDAIRE, BadgeCategorie.FORGE, 2000),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : DIVIN / INTERACTIONS (NOUVEAU)
        // ═══════════════════════════════════════════════════
        BadgeDefinition("divin_talk_10", "Disciple Attentif", "Entendre 10 feedbacks personnalisés des Dieux.", "??? — Écoute les voix d'en haut.", R.drawable.badge_talk_10, BadgeRarete.RARE, BadgeCategorie.DIVIN, 200),
        BadgeDefinition("divin_mnemo", "Mémoire d'Éléphant", "Cliquer sur une astuce mnémotechnique secrète.", "??? — Les Dieux ont des secrets.", R.drawable.badge_mnemo, BadgeRarete.COMMUN, BadgeCategorie.DIVIN, 100),
        BadgeDefinition("demeter_water", "Main Verte", "Arroser un cours fané après l'alerte de Déméter.", "??? — Ne laisse pas mourir le savoir.", R.drawable.badge_demeter_water, BadgeRarete.RARE, BadgeCategorie.DIVIN, 250),
        BadgeDefinition("apollon_lyre", "Poète de l'Olympe", "Transformer 5 cours en hymnes avec Apollon.", "??? — Chante tes révisions.", R.drawable.badge_lyre_5, BadgeRarete.RARE, BadgeCategorie.DIVIN, 300),
        BadgeDefinition("promethee_help", "Curiosité de Pandore", "Demander 5 explications à Prométhée via le HUD.", "??? — Clique sur les symboles.", R.drawable.badge_promethee_help, BadgeRarete.COMMUN, BadgeCategorie.DIVIN, 100),
        BadgeDefinition("zeus_strict", "Foudre de la Rigueur", "Recevoir un feedback exigeant de Zeus (score < 50%).", "??? — Zeus n'est pas content.", R.drawable.badge_zeus_angry, BadgeRarete.COMMUN, BadgeCategorie.DIVIN, 50),
        BadgeDefinition("divin_wait", "Sagesse de l'Attente", "Attendre patiemment la fin d'un chargement IA long.", "??? — Le savoir prend du temps.", R.drawable.badge_ia_wait, BadgeRarete.COMMUN, BadgeCategorie.DIVIN, 50),
        BadgeDefinition("divin_all_gods", "Cercle des Immortels", "Parler à chaque Dieu au moins une fois.", "??? — Sois sociable.", R.drawable.badge_all_gods, BadgeRarete.LEGENDAIRE, BadgeCategorie.DIVIN, 1500),
        BadgeDefinition("divin_mnemo_20", "Mémoire Infinie", "Utiliser 20 astuces mnémotechniques.", "??? — Stocke les astuces.", R.drawable.badge_mnemo_20, BadgeRarete.EPIQUE, BadgeCategorie.DIVIN, 750),
        BadgeDefinition("demeter_return", "Retour au Temple", "Répondre à Déméter après 7j.", "??? — Ton jardin revit.", R.drawable.badge_demeter_return, BadgeRarete.RARE, BadgeCategorie.DIVIN, 200),
        BadgeDefinition("hephaistos_verdict", "Jugement du Fer", "Recevoir un verdict de craft d'Héphaïstos.", "??? — Le métal a parlé.", R.drawable.badge_heph_verdict, BadgeRarete.COMMUN, BadgeCategorie.DIVIN, 50),
        BadgeDefinition("athena_pedagogue", "Leçon de Stratégie", "Recevoir une leçon pédagogique d'Athéna.", "??? — Écoute la sagesse.", R.drawable.badge_athena_talk, BadgeRarete.COMMUN, BadgeCategorie.DIVIN, 50),

        // ═══════════════════════════════════════════════════
        // CATÉGORIE : SPÉCIAL & WTF
        // ═══════════════════════════════════════════════════

        BadgeDefinition("first_connection", "L'Appel de l'Olympe", "Tu as répondu à l'appel de Zeus.", "??? — L'Olympe n'attend que toi.", R.drawable.badge_first_day, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 100),
        BadgeDefinition("tutorial_done", "Initié de l'Olympe", "Tu as lu toutes les leçons du tutoriel.", "??? — Les sages lisent avant d'agir.", R.drawable.badge_tutorial_done, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 150),
        BadgeDefinition("night_owl", "Chouette de Minuit", "Session réalisée entre 23h et 1h du matin.", "??? — Les dieux observent même la nuit.", R.drawable.badge_night_owl, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 150),
        BadgeDefinition("early_bird", "Aube Dorée", "Session réalisée avant 7h du matin.", "??? — L'Olympe s'éveille à l'aurore.", R.drawable.badge_early_bird, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 150),
        BadgeDefinition("settings_explorer", "Architecte de l'Olympe", "Tu as ouvert les Réglages.", "??? — L'atelier d'Héphaïstos est dans les Réglages.", R.drawable.badge_settings_explorer, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 25),
        BadgeDefinition("profile_visited", "Connais-toi Toi-même", "Tu as consulté ton profil héros.", "??? — Socrate avait raison.", R.drawable.badge_profile_visited, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 25),
        BadgeDefinition("speed_quiz", "Éclair de Zeus", "Quiz complété en moins de 2 minutes.", "??? — Même Zeus n'est pas si rapide.", R.drawable.badge_speed_quiz, BadgeRarete.EPIQUE, BadgeCategorie.SPECIAL, 400),
        BadgeDefinition("comeback", "Phénix de l'Olympe", "Retour après 7 jours d'absence.", "??? — Même les phénix renaissent.", R.drawable.badge_comeback, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 200),
        BadgeDefinition("quiz_at_3am", "Fantôme de 3h du Matin", "Quiz lancé à 3h du matin.", "??? — Les fantômes étudient tard.", R.drawable.badge_quiz_at_3am, BadgeRarete.EPIQUE, BadgeCategorie.SPECIAL),
        BadgeDefinition("rage_fail", "Crash Olympien", "Score inférieur à 20%.", "??? — Même les héros chutent.", R.drawable.badge_rage_fail, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL),
        BadgeDefinition("marathon_quiz", "Marathon de l'Olympe", "10 quiz dans la même session.", "??? — L'endurance divine.", R.drawable.badge_marathon_quiz, BadgeRarete.EPIQUE, BadgeCategorie.SPECIAL),

        // Nouveautés WTF :
        BadgeDefinition("wtf_speed_fail", "Icare", "Échouer à un quiz en moins de 30 secondes.", "??? — Brûlé par le soleil.", R.drawable.badge_icare, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 100),
        BadgeDefinition("wtf_lucky", "Chance de Tyché", "Réussir un quiz difficile avec exactement 51%.", "??? — Sur le fil.", R.drawable.badge_lucky, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 200),
        BadgeDefinition("wtf_mute", "Silence Olympien", "Finir un quiz avec le volume à zéro.", "??? — Dans le calme des cieux.", R.drawable.badge_mute, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 50),
        BadgeDefinition("wtf_volume_max", "Tonnerre", "Finir une Victoire Divine (100%) avec le son au max.", "??? — Que l'Olympe entende !", R.drawable.badge_thunder, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 150),
        BadgeDefinition("wtf_inv_spam", "Obsessionnel", "Ouvrir son inventaire 20 fois en une session.", "??? — Tes trésors sont toujours là.", R.drawable.badge_inv_spam, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 50),
        BadgeDefinition("wtf_screenshot", "Souvenir des Dieux", "Prendre un screenshot d'un résultat 6 étoiles.", "??? — Capture l'instant divin.", R.drawable.badge_screenshot, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 200),
        BadgeDefinition("wtf_no_stop", "Berserker", "Faire 3 quiz sans jamais cliquer sur 'Correction'.", "??? — Pas le temps pour les regrets.", R.drawable.badge_no_stop, BadgeRarete.EPIQUE, BadgeCategorie.SPECIAL, 500),
        BadgeDefinition("wtf_slow_win", "Tortue d'Hermès", "Mettre plus de 20 min pour un quiz et avoir 100%.", "??? — La réflexion est longue.", R.drawable.badge_slow_win, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 200),
        BadgeDefinition("wtf_perfect_333", "Cauchemar de Tartare", "Réussir un 100% à 3h33 du matin précisément.", "??? — Une heure maudite.", R.drawable.badge_333, BadgeRarete.LEGENDAIRE, BadgeCategorie.SPECIAL, 3333),
        BadgeDefinition("wtf_shake", "Séisme de Poséidon", "Secouer son téléphone violemment sur l'écran de défaite.", "??? — Calme ta rage.", R.drawable.badge_shake, BadgeRarete.RARE, BadgeCategorie.SPECIAL, 100),
        BadgeDefinition("wtf_tap_spam", "Doigts de Foudre", "Cliquer plus de 50 fois pendant le chargement IA.", "??? — La patience est une vertu.", R.drawable.badge_tap_spam, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 10),
        BadgeDefinition("wtf_oracle_spam", "Aveuglement", "Scanner le même cours 5 fois de suite.", "??? — L'Oracle a bien vu.", R.drawable.badge_oracle_spam, BadgeRarete.COMMUN, BadgeCategorie.SPECIAL, 25),

        BadgeDefinition("all_badges", "Panthéon Absolu", "Tous les badges débloqués.", "????", R.drawable.all_badges, BadgeRarete.LEGENDAIRE, BadgeCategorie.SPECIAL, 10000)
    )

    // ── Accès rapides ─────────────────────────────────────────

    fun parId(id: String): BadgeDefinition? = tous.firstOrNull { it.id == id }

    fun parCategorie(cat: BadgeCategorie): List<BadgeDefinition> =
        tous.filter { it.categorie == cat }

    fun parRarete(rarete: BadgeRarete): List<BadgeDefinition> =
        tous.filter { it.rarete == rarete }

    val totalCount: Int get() = tous.size
}