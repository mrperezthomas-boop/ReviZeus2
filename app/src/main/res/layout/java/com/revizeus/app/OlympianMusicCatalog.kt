package com.revizeus.app

/**
 * ============================================================
 * OlympianMusicCatalog.kt — RéviZeus
 * Catalogue central de toutes les BGM du jeu
 *
 * Utilité :
 * - Source unique pour le jukebox
 * - Référence stable pour les futures extensions audio
 *
 * Connexions :
 * - JukeboxAdapter
 * - SettingsActivity
 * - futurs écrans audio / debug / QA
 *
 * IMPORTANT :
 * - On référence ici les noms EXACTS fournis pour les ressources raw
 * - En Kotlin, on appelle toujours R.raw.nom_sans_extension
 * ============================================================
 */
object OlympianMusicCatalog {

    /**
     * Retourne la liste complète des BGM connues du projet.
     *
     * Pour ajouter une future piste :
     * 1. déposer le fichier dans res/raw/
     * 2. garder un nom Android valide
     * 3. ajouter une ligne MusicTrackItem ici
     */
    fun buildTracks(): List<MusicTrackItem> {
        return listOf(
            MusicTrackItem(title = "Égide de RéviZeus", description = "Les fondations du savoir et l'essence même de l'Olympe.", resId = R.raw.info_revizeus),
            MusicTrackItem(title = "Lyre d'Apollon", description = "Ambiance poétique et divine du temple d'Apollon.", resId = R.raw.bgm_apollo_lyre),
            MusicTrackItem(title = "Grâce d'Artémis", description = "Variation mélodique dédiée à l'avatar féminin.", resId = R.raw.bgm_avatar_fille),
            MusicTrackItem(title = "Force d'Héraclès", description = "Variation mélodique dédiée à l'avatar masculin.", resId = R.raw.bgm_avatar_homme),
            MusicTrackItem(title = "L'Appel du Héros", description = "Choisissez le visage de celui qui gravira le mont Olympe.", resId = R.raw.bgm_avatar_selection),
            MusicTrackItem(title = "Lignée de Théo", description = "Thème unique résonnant avec l'identité de Théo.", resId = R.raw.bgm_avatar_theo),
            MusicTrackItem(title = "Agora du Dashboard", description = "Le cœur battant de votre progression sous l'œil des Dieux.", resId = R.raw.bgm_dashboard),
            MusicTrackItem(title = "Allégresse d'Hermès", description = "Une brise légère et motivante souffle sur vos accomplissements.", resId = R.raw.bgm_dashboard_happy),
            MusicTrackItem(title = "Tourmente du Tartare", description = "Une tension sourde pour les moments de révision intense.", resId = R.raw.bgm_dashboard_stressed),
            MusicTrackItem(title = "Repos de l'Héros", description = "Une mélodie apaisante pour une fin de journée de labeur.", resId = R.raw.bgm_dashboard_tired),
            MusicTrackItem(title = "Mécanique d'Héphaïstos", description = "Analyse des rouages internes et maintenance du sanctuaire.", resId = R.raw.bgm_debug),
            MusicTrackItem(title = "Le Souffle des Muses", description = "Choisissez l'état d'esprit qui guidera votre session de révision.", resId = R.raw.bgm_mood_selection),
            MusicTrackItem(title = "Rituel des Prémices", description = "Un moment sacré de dévotion pour obtenir la faveur divine.", resId = R.raw.bgm_offrande),
            MusicTrackItem(title = "Murmure de la Pythie", description = "L'analyse mystique de vos performances et de votre avenir.", resId = R.raw.bgm_oracle),
            MusicTrackItem(title = "Faveur Divine", description = "Une réussite claire qui honore les habitants de l'Olympe.", resId = R.raw.bgm_result_bien),
            MusicTrackItem(title = "Courroux de Zeus", description = "Un verdict sévère qui appelle à plus de rigueur.", resId = R.raw.bgm_result_fail),
            MusicTrackItem(title = "Équilibre d'Hestia", description = "Un résultat honorable, mais le chemin vers l'Olympe continue.", resId = R.raw.bgm_result_moyen),
            MusicTrackItem(title = "Apothéose", description = "Triomphe absolu : vous avez atteint la perfection divine.", resId = R.raw.bgm_result_parfait),
            MusicTrackItem(title = "Sanctuaire des Savoirs", description = "Immersion totale dans la quiétude de la connaissance pure.", resId = R.raw.bgm_savoir),
            MusicTrackItem(title = "Messager d'Hermès", description = "Maîtrisez les langues étrangères dans le temple du voyage.", resId = R.raw.bgm_select_anglais_hermes),
            MusicTrackItem(title = "Esthétique d'Aphrodite", description = "Laissez l'inspiration artistique guider votre apprentissage.", resId = R.raw.bgm_select_art_aphrodite),
            MusicTrackItem(title = "Sagesse d'Athéna", description = "Stratégie et rigueur au sein du temple de la Raison.", resId = R.raw.bgm_select_francais_athena),
            MusicTrackItem(title = "Sillons de Déméter", description = "Explorez les terres et les cycles du monde terrestre.", resId = R.raw.bgm_select_geographie_demeter),
            MusicTrackItem(title = "Chronique d'Arès", description = "Revivez les batailles et les époques marquantes du passé.", resId = R.raw.bgm_select_histoire_ares),
            MusicTrackItem(title = "Foudre de Zeus", description = "La puissance du calcul souverain et de l'ordre universel.", resId = R.raw.bgm_select_maths_zeus),
            MusicTrackItem(title = "Éclat d'Apollon", description = "Lumière et réflexion profonde sur la nature de l'existence.", resId = R.raw.bgm_select_philo_apollon),
            MusicTrackItem(title = "Fournaise d'Héphaïstos", description = "Comprendre les lois de la matière et de l'énergie.", resId = R.raw.bgm_select_physique_hephaistos),
            MusicTrackItem(title = "Profondeurs de Poséidon", description = "Étude du vivant, des océans et des forces de la nature.", resId = R.raw.bgm_select_svt_poseidon),
            MusicTrackItem(title = "Étincelle de Prométhée", description = "Le savoir pratique et le don de la connaissance humaine.", resId = R.raw.bgm_select_vie_promethee),
            MusicTrackItem(title = "L'Épreuve des Écoles", description = "Rythme soutenu pour un entraînement sans relâche.", resId = R.raw.bgm_training_quiz),
            MusicTrackItem(title = "Préparatifs du Combat", description = "Concentration avant d'affronter les épreuves du savoir.", resId = R.raw.bgm_training_select),
            MusicTrackItem(title = "Destinée Héroïque", description = "Définissez l'essence de votre incarnation divine.", resId = R.raw.music_gender_selection),
            MusicTrackItem(title = "Le Jugement des Dieux", description = "Ambiance de défi ultime pour tester vos connaissances.", resId = R.raw.music_quiz),
            MusicTrackItem(title = "Majesté de l'Olympe", description = "Le grand hymne célébrant la splendeur du domaine des Dieux.", resId = R.raw.olympus_theme),
            MusicTrackItem(title = "Échos de RéviZeus I", description = "Variation symphonique de l'odyssée pédagogique.", resId = R.raw.revizeus1),
            MusicTrackItem(title = "Échos de RéviZeus II", description = "Variation symphonique de l'odyssée pédagogique.", resId = R.raw.revizeus2),
            MusicTrackItem(title = "Codex des Exploits", description = "Le récit musical de vos hauts faits et trophées divins.", resId = R.raw.bgm_badge),
            MusicTrackItem(title = "Trésor de l'Olympe", description = "Revue de vos reliques et objets sacrés collectés.", resId = R.raw.bgm_inventaire),
            MusicTrackItem(title = "L'Éveil du Héros", description = "Le murmure du destin qui s'éveille aux premières lueurs du mont Olympe.", resId = R.raw.bgm_debut),
            MusicTrackItem(title = "L'Appel de l'Aventure", description = "Franchissez le seuil et lancez-vous dans une quête dont les bardes chanteront la gloire.", resId = R.raw.bgm_demarrer),
            MusicTrackItem(title = "Le Choix des Destinées", description = "Parcourez les récits oubliés et choisissez le nom qui marquera l'histoire.", resId = R.raw.bgm_title_select),
            MusicTrackItem(title = "Le Souffle de la Pythie", description = "Une mélodie mystique pour percer le voile de l'avenir et écouter la sagesse des dieux.", resId = R.raw.bgm_oracle_conseil),
            MusicTrackItem(title = "Migration du Renouveau", description = "L'évolution de l'Olympe porte son souffle par delà les univers", resId = R.raw.bgm_migration),
            MusicTrackItem(title = "Écho de la Pythie", description = "Le murmure des futurs possibles s'entrelace dans le souffle sacré de l'Oracle", resId = R.raw.bgm_oracle_prompt),

        )
    }
}
