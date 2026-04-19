package com.revizeus.app

import android.content.Context
import android.content.Intent

object AdventureManager {

    private val allTempleDefinitions = listOf(
        WorldMapTempleSlot("zeus", "Mathématiques", "Temple de Zeus", 1, true, -90f, "ic_temple_world_zeus_lvl_"),
        WorldMapTempleSlot("athena", "Français", "Temple d'Athéna", 1, true, -54f, "ic_temple_world_athena_lvl_"),
        WorldMapTempleSlot("poseidon", "SVT", "Temple de Poséidon", 0, false, -18f, "ic_temple_world_poseidon_lvl_"),
        WorldMapTempleSlot("ares", "Histoire", "Temple d'Arès", 1, true, 18f, "ic_temple_world_ares_lvl_"),
        WorldMapTempleSlot("aphrodite", "Art / Musique", "Temple d'Aphrodite", 0, false, 54f, "ic_temple_world_aphrodite_lvl_"),
        WorldMapTempleSlot("hermes", "Langues", "Temple d'Hermès", 0, false, 90f, "ic_temple_world_hermes_lvl_"),
        WorldMapTempleSlot("demeter", "Géographie", "Temple de Déméter", 0, false, 126f, "ic_temple_world_demeter_lvl_"),
        WorldMapTempleSlot("hephaistos", "Physique-Chimie", "Temple d'Héphaïstos", 0, false, 162f, "ic_temple_world_hephaistos_lvl_"),
        WorldMapTempleSlot("apollon", "Philo / SES", "Temple d'Apollon", 0, false, 198f, "ic_temple_world_apollon_lvl_"),
        WorldMapTempleSlot("promethee", "Vie & Projets", "Temple de Prométhée", 0, false, 234f, "ic_temple_world_promethee_lvl_")
    )

    fun loadWorldState(context: Context): WorldMapState {
        val slots = allTempleDefinitions.map { base ->
            base.copy(templeLevel = resolveTempleLevel(context, base.godId))
        }
        val totalScore = slots.sumOf { it.templeLevel }
        return WorldMapState(
            totalRestorationScore = totalScore,
            worldTier = WorldMapThemeResolver.calculateWorldTier(totalScore),
            slots = slots
        )
    }

    private fun resolveTempleLevel(context: Context, godId: String): Int {
        val key = "adventure_temple_level_$godId"
        val prefs = context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
        val stored = prefs.getInt(key, Int.MIN_VALUE)
        if (stored != Int.MIN_VALUE) {
            return stored.coerceIn(0, 20)
        }
        return when (godId.lowercase()) {
            "zeus", "athena", "ares" -> 1
            else -> 0
        }
    }

    fun persistTempleLevel(context: Context, godId: String, level: Int) {
        val key = "adventure_temple_level_$godId"
        context.getSharedPreferences("ReviZeusPrefs", Context.MODE_PRIVATE)
            .edit()
            .putInt(key, level.coerceIn(0, 20))
            .apply()
    }

    fun buildTempleIntent(context: Context, slot: WorldMapTempleSlot): Intent {
        return Intent(context, MapTempleActivity::class.java).apply {
            putExtra(MapTempleActivity.EXTRA_GOD_ID, slot.godId)
            putExtra(MapTempleActivity.EXTRA_SUBJECT, slot.subject)
            putExtra(MapTempleActivity.EXTRA_TEMPLE_LEVEL, slot.templeLevel)
            putExtra(MapTempleActivity.EXTRA_DISPLAY_NAME, slot.displayName)
            putExtra(MapTempleActivity.EXTRA_MAP_INDEX, 1)
        }
    }
}
