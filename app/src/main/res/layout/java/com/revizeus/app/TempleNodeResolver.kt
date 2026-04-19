package com.revizeus.app

object TempleNodeResolver {

    data class NodeResolution(
        val title: String,
        val message: String,
        val godId: String,
        val shouldMarkComplete: Boolean
    )

    fun resolveNodeIconCandidates(nodeType: TempleNodeType): List<String> {
        return when (nodeType) {
            TempleNodeType.COMBAT_STANDARD,
            TempleNodeType.COMBAT_ELITE -> listOf("ic_node_combat")
            TempleNodeType.EVENT_DIVINE,
            TempleNodeType.EVENT_RANDOM,
            TempleNodeType.QUEST_SPECIAL -> listOf("ic_node_event")
            TempleNodeType.MINI_BOSS,
            TempleNodeType.BOSS,
            TempleNodeType.CHAOS_INTRUSION -> listOf("ic_node_boss")
            TempleNodeType.TREASURE,
            TempleNodeType.RELIC_FORGE -> listOf("ic_node_treasure")
            TempleNodeType.OFFERING_SITE,
            TempleNodeType.HEALING_SITE -> listOf("ic_node_offering")
            TempleNodeType.LOCKED_GATE -> listOf("ic_node_locked")
        }
    }

    fun resolve(config: TempleMapConfig, node: TempleMapNode): NodeResolution {
        val godId = config.godId
        return when (node.nodeType) {
            TempleNodeType.COMBAT_STANDARD,
            TempleNodeType.COMBAT_ELITE -> NodeResolution(
                title = node.title,
                message = "Le vrai combat pédagogique sera branché au bloc suivant. Pour ce MVP, ce node valide la progression de la route du temple.",
                godId = godId,
                shouldMarkComplete = true
            )
            TempleNodeType.EVENT_DIVINE,
            TempleNodeType.EVENT_RANDOM,
            TempleNodeType.QUEST_SPECIAL -> NodeResolution(
                title = node.title,
                message = node.description,
                godId = godId,
                shouldMarkComplete = true
            )
            TempleNodeType.MINI_BOSS -> NodeResolution(
                title = node.title,
                message = "Le mini-boss complet sera codé ensuite. Ici, tu peux déjà tester la logique de route, d'ouverture et de progression du temple.",
                godId = godId,
                shouldMarkComplete = true
            )
            TempleNodeType.BOSS -> NodeResolution(
                title = node.title,
                message = "Le boss final de la map sera relié au vrai moteur de combat plus tard. Pour l'instant, sa validation sert de borne de complétion du temple local V1.",
                godId = godId,
                shouldMarkComplete = true
            )
            TempleNodeType.TREASURE -> NodeResolution(
                title = node.title,
                message = "Un ancien coffre répond à ton avancée. Le système de récompense détaillé arrivera avec les artefacts et l'inventaire aventure.",
                godId = "prometheus",
                shouldMarkComplete = true
            )
            TempleNodeType.RELIC_FORGE -> NodeResolution(
                title = node.title,
                message = "La forge d'artefacts complète sera branchée plus tard. Ce node sert ici de rencontre validable pour la boucle aventure.",
                godId = "hephaistos",
                shouldMarkComplete = true
            )
            TempleNodeType.HEALING_SITE -> NodeResolution(
                title = node.title,
                message = "Le sanctuaire de soin complet viendra plus tard. Pour ce ticket, ce passage valide la progression locale.",
                godId = godId,
                shouldMarkComplete = true
            )
            TempleNodeType.OFFERING_SITE -> NodeResolution(
                title = node.title,
                message = "Ce site d'offrande servira plus tard aux bénédictions et aux affinités divines. Il peut déjà être validé dans la progression locale.",
                godId = godId,
                shouldMarkComplete = true
            )
            TempleNodeType.CHAOS_INTRUSION -> NodeResolution(
                title = node.title,
                message = "Une intrusion du Chaos est détectée. Le système dynamique complet sera branché plus tard.",
                godId = "zeus",
                shouldMarkComplete = true
            )
            TempleNodeType.LOCKED_GATE -> NodeResolution(
                title = node.title,
                message = "Cette porte reste scellée tant que la condition d'ouverture n'est pas remplie.",
                godId = godId,
                shouldMarkComplete = false
            )
        }
    }
}
