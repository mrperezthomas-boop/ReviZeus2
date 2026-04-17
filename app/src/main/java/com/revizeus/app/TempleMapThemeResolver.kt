package com.revizeus.app

object TempleMapThemeResolver {

    data class TempleMapTheme(
        val resolvedVisualLevel: Int,
        val backgroundDrawableName: String,
        val backgroundVideoRawName: String?,
        val accentColorHex: String,
        val fallbackDrawableResId: Int
    )

    fun resolve(godId: String, templeLevel: Int): TempleMapTheme {
        val safeGodId = godId.lowercase()
        val visualLevel = resolveVisualLevel(templeLevel)
        val backgroundName = "bg_map_temple_${safeGodId}_lvl_$visualLevel"

        val accent = when (safeGodId) {
            "zeus" -> "#1E90FF"
            "athena" -> "#FFD700"
            "ares" -> "#DAA520"
            else -> "#FFBF00"
        }

        return TempleMapTheme(
            resolvedVisualLevel = visualLevel,
            backgroundDrawableName = backgroundName,
            backgroundVideoRawName = backgroundName,
            accentColorHex = accent,
            fallbackDrawableResId = R.drawable.bg_olympus_dark
        )
    }

    fun resolveVisualLevel(templeLevel: Int): Int {
        val clamped = templeLevel.coerceIn(0, 20)
        return when {
            clamped >= 20 -> 20
            else -> (clamped / 2) * 2
        }
    }
}
