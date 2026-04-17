package com.revizeus.app

data class WorldMapTempleSlot(
    val godId: String,
    val subject: String,
    val displayName: String,
    val templeLevel: Int,
    val isUnlocked: Boolean,
    val angleDegrees: Float,
    val iconPrefix: String
)
