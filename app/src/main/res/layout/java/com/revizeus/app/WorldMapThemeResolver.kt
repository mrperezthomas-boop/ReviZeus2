package com.revizeus.app

object WorldMapThemeResolver {

    data class WorldMapTheme(
        val tier: Int,
        val backgroundDrawableName: String,
        val backgroundVideoRawName: String?,
        val lightOverlayName: String,
        val corruptionOverlayName: String,
        val divineParticlesName: String,
        val chaosParticlesName: String
    )

    fun resolve(totalRestorationScore: Int): WorldMapTheme {
        val tier = calculateWorldTier(totalRestorationScore)
        val baseName = "bg_world_map_tier_$tier"
        return WorldMapTheme(
            tier = tier,
            backgroundDrawableName = baseName,
            backgroundVideoRawName = baseName,
            lightOverlayName = "overlay_world_light",
            corruptionOverlayName = "overlay_world_corruption",
            divineParticlesName = "world_particle_divine",
            chaosParticlesName = "world_particle_chaos"
        )
    }

    fun calculateWorldTier(totalRestorationScore: Int): Int {
        val score = totalRestorationScore.coerceIn(0, 200)
        return when {
            score >= 180 -> 10
            else -> (score / 20) + 1
        }.coerceIn(1, 10)
    }
}
