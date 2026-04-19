package com.revizeus.app

/**
 * Fournit les cartes locales de temple pour le MVP V1.
 * Le layout XML reste universel, mais la config node/texte varie selon le dieu.
 */
object TempleMapManager {

    fun getTempleMapConfig(godId: String, subject: String, templeLevel: Int, mapIndex: Int): TempleMapConfig {
        return when (godId.lowercase()) {
            "athena" -> buildAthenaMap(subject, templeLevel, mapIndex)
            "ares" -> buildAresMap(subject, templeLevel, mapIndex)
            else -> buildZeusMap(subject, templeLevel, mapIndex)
        }
    }

    private fun buildZeusMap(subject: String, templeLevel: Int, mapIndex: Int): TempleMapConfig {
        val nodes = listOf(
            TempleMapNode("zeus_start", TempleNodeType.EVENT_DIVINE, "Ruines du Calcul Brisé", "Zeus observe les premières fissures des lois sacrées.", 1, 0.10f, 0.52f),
            TempleMapNode("zeus_combat_1", TempleNodeType.COMBAT_STANDARD, "Harpie des Fractions", "Premier affrontement contre l'erreur simple.", 1, 0.28f, 0.34f),
            TempleMapNode("zeus_event_1", TempleNodeType.EVENT_DIVINE, "Éclair de Méthode", "Zeus rappelle la rigueur du raisonnement.", 1, 0.46f, 0.52f),
            TempleMapNode("zeus_combat_2", TempleNodeType.COMBAT_STANDARD, "Loup des Équations", "Le doute attaque les fondations du calcul.", 2, 0.62f, 0.34f),
            TempleMapNode("zeus_treasure", TempleNodeType.TREASURE, "Coffre des Théorèmes", "Une récompense mineure protège la route.", 2, 0.62f, 0.70f, isOptional = true, branchIndex = 1),
            TempleMapNode("zeus_boss", TempleNodeType.BOSS, "Faille du Chaos Numérique", "Le premier cœur du désordre attend au bout du chemin.", 3, 0.84f, 0.52f)
        )

        val edges = listOf(
            TempleMapEdge("zeus_start", "zeus_combat_1"),
            TempleMapEdge("zeus_combat_1", "zeus_event_1"),
            TempleMapEdge("zeus_event_1", "zeus_combat_2"),
            TempleMapEdge("zeus_event_1", "zeus_treasure"),
            TempleMapEdge("zeus_combat_2", "zeus_boss"),
            TempleMapEdge("zeus_treasure", "zeus_boss")
        )

        return TempleMapConfig(
            subject = subject,
            godId = "zeus",
            templeLevel = templeLevel,
            mapIndex = mapIndex,
            title = "Carte du Temple de Zeus",
            description = "Première route mathématique vers la restauration du temple.",
            nodeList = nodes,
            edgeList = edges,
            bossNodeId = "zeus_boss",
            introDialogue = "Chaque réponse juste dressera une pierre de plus dans mon temple. Commence, et frappe l'erreur avec rigueur."
        )
    }

    private fun buildAthenaMap(subject: String, templeLevel: Int, mapIndex: Int): TempleMapConfig {
        val nodes = listOf(
            TempleMapNode("athena_start", TempleNodeType.EVENT_DIVINE, "Vestibule des Manuscrits", "Athéna rouvre le sanctuaire du verbe.", 1, 0.10f, 0.52f),
            TempleMapNode("athena_combat_1", TempleNodeType.COMBAT_STANDARD, "Corbeau des Homophones", "Le premier trouble s'abat sur la langue.", 1, 0.26f, 0.32f),
            TempleMapNode("athena_event_1", TempleNodeType.EVENT_DIVINE, "Tablette de Structure", "Athéna impose l'ordre dans le texte.", 1, 0.44f, 0.52f),
            TempleMapNode("athena_combat_2", TempleNodeType.COMBAT_STANDARD, "Spectre des Accords", "La précision grammaticale vacille.", 2, 0.60f, 0.32f),
            TempleMapNode("athena_offering", TempleNodeType.OFFERING_SITE, "Autel de la Reformulation", "Offrande symbolique à la sagesse du langage.", 2, 0.60f, 0.72f, isOptional = true, branchIndex = 1),
            TempleMapNode("athena_boss", TempleNodeType.BOSS, "Nœud du Chaos Linguistique", "Un foyer de confusion altère la clarté du temple.", 3, 0.82f, 0.52f)
        )

        val edges = listOf(
            TempleMapEdge("athena_start", "athena_combat_1"),
            TempleMapEdge("athena_combat_1", "athena_event_1"),
            TempleMapEdge("athena_event_1", "athena_combat_2"),
            TempleMapEdge("athena_event_1", "athena_offering"),
            TempleMapEdge("athena_combat_2", "athena_boss"),
            TempleMapEdge("athena_offering", "athena_boss")
        )

        return TempleMapConfig(
            subject = subject,
            godId = "athena",
            templeLevel = templeLevel,
            mapIndex = mapIndex,
            title = "Carte du Temple d'Athéna",
            description = "Première route de compréhension, de syntaxe et de clarté.",
            nodeList = nodes,
            edgeList = edges,
            bossNodeId = "athena_boss",
            introDialogue = "Ici, chaque pas doit avoir une structure. Avance avec méthode, et la langue te rendra sa lumière."
        )
    }

    private fun buildAresMap(subject: String, templeLevel: Int, mapIndex: Int): TempleMapConfig {
        val nodes = listOf(
            TempleMapNode("ares_start", TempleNodeType.EVENT_DIVINE, "Champ des Vestiges", "Arès rouvre la route des conflits du passé.", 1, 0.10f, 0.52f),
            TempleMapNode("ares_combat_1", TempleNodeType.COMBAT_STANDARD, "Soldat de la Confusion", "La chronologie commence à saigner.", 1, 0.28f, 0.32f),
            TempleMapNode("ares_event_1", TempleNodeType.EVENT_DIVINE, "Bannière du Courage", "Arès exige que l'erreur soit affrontée, pas contournée.", 1, 0.44f, 0.52f),
            TempleMapNode("ares_combat_2", TempleNodeType.COMBAT_STANDARD, "Spectre des Dates", "Les repères historiques s'effondrent.", 2, 0.60f, 0.32f),
            TempleMapNode("ares_mini", TempleNodeType.MINI_BOSS, "Gardien des Chronologies", "Une première résistance majeure bloque la route.", 2, 0.60f, 0.72f, isOptional = true, branchIndex = 1),
            TempleMapNode("ares_boss", TempleNodeType.BOSS, "Foyer du Chaos Historique", "Le désordre des époques forge un noyau hostile.", 3, 0.82f, 0.52f)
        )

        val edges = listOf(
            TempleMapEdge("ares_start", "ares_combat_1"),
            TempleMapEdge("ares_combat_1", "ares_event_1"),
            TempleMapEdge("ares_event_1", "ares_combat_2"),
            TempleMapEdge("ares_event_1", "ares_mini"),
            TempleMapEdge("ares_combat_2", "ares_boss"),
            TempleMapEdge("ares_mini", "ares_boss")
        )

        return TempleMapConfig(
            subject = subject,
            godId = "ares",
            templeLevel = templeLevel,
            mapIndex = mapIndex,
            title = "Carte du Temple d'Arès",
            description = "Première route martiale contre l'oubli historique.",
            nodeList = nodes,
            edgeList = edges,
            bossNodeId = "ares_boss",
            introDialogue = "L'histoire ne pardonne pas la fuite. Avance, prends les coups du doute, puis rends-les avec courage."
        )
    }
}
