package com.revizeus.app

data class WorldMapState(
    val totalRestorationScore: Int,
    val worldTier: Int,
    val slots: List<WorldMapTempleSlot>
)
