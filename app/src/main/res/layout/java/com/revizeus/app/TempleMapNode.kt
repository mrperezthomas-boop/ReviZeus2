package com.revizeus.app

data class TempleMapNode(
    val nodeId: String,
    val nodeType: TempleNodeType,
    val title: String,
    val description: String,
    val difficultyTier: Int,
    val xRatio: Float,
    val yRatio: Float,
    val isOptional: Boolean = false,
    val branchIndex: Int = 0,
    val isReplayable: Boolean = true,
    // Prépare la future couche gameplay/récompense sans hardcode UI.
    val rewardProfileId: String = "",
    val enemyFamilyId: String = "",
    val eventTemplateId: String = "",
    val specialFlagsJson: String = ""
) {
    // Alias explicites demandés métier : les écrans peuvent migrer vers x/y plus tard.
    val x: Float get() = xRatio
    val y: Float get() = yRatio
}
