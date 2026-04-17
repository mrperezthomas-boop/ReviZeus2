package com.revizeus.app

data class TempleMapConfig(
    val subject: String,
    val godId: String,
    val templeLevel: Int,
    val mapIndex: Int,
    val title: String,
    val description: String,
    val nodeList: List<TempleMapNode>,
    val edgeList: List<TempleMapEdge>,
    val bossNodeId: String,
    val introDialogue: String,
    // Nouveau champ métier : prêt pour narration/contextualisation avancée.
    val introDialogueContext: String = "",
    val themeId: String = "",
    val recommendedPower: Int = 0,
    val metadataJson: String = ""
) {
    companion object ResourceNaming {
        // Convention confirmée pour les icônes de temples sur la world map.
        fun worldTempleIconName(godId: String, level: Int): String {
            val safeLevel = level.coerceIn(0, 20)
            return "ic_temple_world_${godId.lowercase()}_lvl_$safeLevel"
        }

        // Convention confirmée pour les backgrounds de map temple.
        fun templeMapBackgroundName(godId: String, palier: Int): String {
            val safePalier = normalizeTempleMapPalier(palier)
            return "bg_map_temple_${godId.lowercase()}_lvl_$safePalier"
        }

        // Paliers visuels confirmés métier.
        fun normalizeTempleMapPalier(value: Int): Int {
            val clamped = value.coerceIn(0, 20)
            val allowed = intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
            return allowed.minByOrNull { kotlin.math.abs(it - clamped) } ?: 0
        }

        // Socle monde confirmé : noms centralisés pour éviter le hardcode futur en Activity.
        const val RES_IC_RETURN_AVENTURE = "ic_return_aventure"
        const val RES_BRIDGE_RAINBOW_CORRUPTED = "bridge_rainbow_corrupted"
        const val RES_BRIDGE_RAINBOW_DIVINE = "bridge_rainbow_divine"
        const val RES_IC_WORLD_CHAOS_CORE = "ic_world_chaos_core"
        const val RES_ISLAND_BASE_CHAOS = "island_base_chaos"
        const val RES_ISLAND_BASE_DIVINE = "island_base_divine"
        const val RES_OVERLAY_WORLD_CORRUPTION = "overlay_world_corruption"
        const val RES_OVERLAY_WORLD_LIGHT = "overlay_world_light"
        const val RES_WORLD_PARTICLE_CHAOS = "world_particle_chaos"
        const val RES_WORLD_PARTICLE_DIVINE = "world_particle_divine"
    }
}
