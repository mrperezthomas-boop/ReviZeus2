package com.revizeus.app

/**
 * ═══════════════════════════════════════════════════════════════
 * PANTHÉON CONFIG — Source de vérité unique pour les 10 dieux
 * ═══════════════════════════════════════════════════════════════
 * CONNEXIONS :
 * → DashboardActivity    (cercle des dieux, stats)
 * → OracleActivity       (spinner sélection matière)
 * → ResultActivity       (ethosNom dans le prompt Gemini)
 * → GodMatiereActivity   (écran personnalisé du dieu)
 * → TrainingSelectActivity (choix matière entraînement)
 * → GeminiManager        (personnalité dans le prompt)
 * * 🛠️ CHANGEMENTS : Les 'iconResName' pointent désormais vers 'ic_..._mini'
 */
object PantheonConfig {

    data class GodInfo(
        val matiere: String,
        val divinite: String,
        val couleur: Int,
        val iconResName: String,
        val ethos: String,
        val personnalite: String,
        val emptyMessage: String,
        val spinnerLabel: String
    )

    val GODS: List<GodInfo> = listOf(
        GodInfo("Mathématiques", "Zeus", 0xFF1E90FF.toInt(), "ic_zeus_chibi",
            "Souverain Logic",
            "Autoritaire et exigeant mais protecteur. Ne jure que par la preuve et la rigueur.",
            "Par ma foudre ! Aucune leçon de Mathématiques n'a été gravée ici !",
            "Mathématiques (Zeus)"
        ),
        GodInfo("Français", "Athéna", 0xFFFFD700.toInt(), "ic_athena_mini",
            "Stratège du Verbe",
            "Calme, analytique, très portée sur l'étymologie et la syntaxe.",
            "Ma chouette n'a rien à lire... La bibliothèque de Français est vide.",
            "Français (Athéna)"
        ),
        GodInfo("SVT", "Poséidon", 0xFF40E0D0.toInt(), "ic_poseidon_mini",
            "Explorateur Organique",
            "Fasciné par les courants de la vie, les cellules et l'écologie.",
            "L'océan de la SVT est à sec. Trouve-moi des cours !",
            "SVT (Poséidon)"
        ),
        GodInfo("Histoire", "Arès", 0xFFDAA520.toInt(), "ic_ares_mini",
            "Gardien du Passé",
            "Solennel, passionné par les causes et conséquences des grands conflits.",
            "Pas de guerres, pas de dates ? Remplis cette forge d'Histoire !",
            "Histoire (Arès)"
        ),
        GodInfo("Art/Musique", "Aphrodite", 0xFFFF69B4.toInt(), "ic_aphrodite_mini",
            "Inspiratrice",
            "Chaleureuse, émotive, voit la beauté dans chaque coup de pinceau.",
            "Où est la beauté ? Je ne vois aucune œuvre dans tes cours d'Art !",
            "Art/Musique (Aphrodite)"
        ),
        GodInfo("Anglais", "Hermès", 0xFF87CEEB.toInt(), "ic_hermes_mini",
            "Messager Agile",
            "Vif, malicieux, adepte des jeux de mots et de la communication rapide.",
            "I'm waiting! Mes sandales s'impatientent sans cours d'Anglais !",
            "Anglais (Hermès)"
        ),
        GodInfo("Géographie", "Déméter", 0xFF228B22.toInt(), "ic_demeter_mini",
            "Gardienne des Sols",
            "Maternelle, ancrée, spécialiste des climats et de l'aménagement.",
            "Les terres de Géographie sont arides. Plante de nouveaux cours !",
            "Géographie (Déméter)"
        ),
        GodInfo("Physique-Chimie", "Héphaïstos", 0xFFFF8C00.toInt(), "ic_hephaistos_mini",
            "Forgeron Pragmatique",
            "Technique et un peu bourru, focalisé sur comment les choses marchent.",
            "Ma forge est éteinte. Pas de Physique-Chimie à forger !",
            "Physique-Chimie (Héphaïstos)"
        ),
        GodInfo("Philo/SES", "Apollon", 0xFFDDA0DD.toInt(), "ic_apollon_mini",
            "Méditatif Radieux",
            "Parle par énigmes, cherche la vérité derrière les apparences.",
            "La lumière de la vérité ne brille pas encore ici...",
            "Philo/SES (Apollon)"
        ),
        GodInfo("Vie & Projets", "Prométhée", 0xFFFFBF00.toInt(), "ic_prometheus_mini",
            "Titan Visionnaire",
            "Rebelle bienveillant et pionnier, encourage à sortir des sentiers battus.",
            "Le feu que j'ai volé brûle en toi ! Mais rien n'est déposé ici !",
            "Vie & Projets (Prométhée)"
        )
    )

    fun findByMatiere(matiere: String): GodInfo? =
        GODS.find { it.matiere.equals(matiere, ignoreCase = true) }

    fun findByDivinite(divinite: String): GodInfo? =
        GODS.find { it.divinite.equals(divinite, ignoreCase = true) }

    fun getSpinnerLabels(): Array<String> = GODS.map { it.spinnerLabel }.toTypedArray()
}