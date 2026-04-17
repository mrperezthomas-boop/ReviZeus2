package com.revizeus.app

enum class TempleMapEdgeType {
    STANDARD_ROUTE,
    OPTIONAL_BRANCH,
    SHORTCUT,
    LOCKED_PATH,
    CHAOS_RIFT
}

data class TempleMapEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val edgeType: TempleMapEdgeType = TempleMapEdgeType.STANDARD_ROUTE,
    val isLocked: Boolean = false,
    val unlockConditionJson: String = ""
) {
    // Compatibilité rétroactive avec le socle actuel.
    val isLockedByDefault: Boolean get() = isLocked
}
