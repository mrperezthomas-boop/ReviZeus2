package com.revizeus.app.utils

/**
 * 2026-04-26 — PATCH RÉSUMÉS TEMPLE
 *
 * Centralise le parsing d'un résumé Oracle sauvegardé afin de reconstruire
 * un rendu premium (titre/sous-titre/paragraphes/listes) lors de la relecture.
 *
 * Objectif: aligner GodMatiereActivity sur l'expérience visuelle de ResultActivity
 * sans modifier la persistance Room ni compacter le texte sauvegardé.
 */
object SummaryRenderFormatter {

    enum class BlockType {
        CHAPTER,
        SUBTITLE,
        TEXT,
        LIST_ITEM
    }

    data class Block(
        val type: BlockType,
        val content: String
    )

    data class RenderModel(
        val title: String,
        val level: String,
        val blocks: List<Block>,
        val hasStructuredFormat: Boolean
    )

    fun parse(raw: String): RenderModel {
        val cleanedRaw = raw
            .replace("\r", "")
            .replace(Regex("\\u0000"), "")
            .replace(Regex("^---START_RESUME---\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*---END_RESUME---$", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanedRaw.isBlank()) {
            return RenderModel(
                title = "Notions principales",
                level = "",
                blocks = listOf(Block(BlockType.TEXT, "Information non lisible dans le document.")),
                hasStructuredFormat = false
            )
        }

        val lines = cleanedRaw.lines()
        val hasStructuredFormat = lines.any { line ->
            val t = line.trimStart()
            t.startsWith("TITLE:", ignoreCase = true) ||
                t.startsWith("LEVEL:", ignoreCase = true) ||
                t.startsWith("CHAPTER:", ignoreCase = true) ||
                t.startsWith("SUBTITLE:", ignoreCase = true) ||
                t.startsWith("TEXT:", ignoreCase = true)
        }

        return if (hasStructuredFormat) {
            parseStructured(lines)
        } else {
            parseNatural(cleanedRaw)
        }
    }

    private fun parseStructured(lines: List<String>): RenderModel {
        var title = ""
        var level = ""
        val blocks = mutableListOf<Block>()

        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEach

            when {
                trimmed.startsWith("TITLE:", ignoreCase = true) -> {
                    title = trimmed.substringAfter(":").trim()
                }

                trimmed.startsWith("LEVEL:", ignoreCase = true) -> {
                    level = trimmed.substringAfter(":").trim()
                }

                trimmed.startsWith("CHAPTER:", ignoreCase = true) -> {
                    val value = trimmed.substringAfter(":").trim()
                    if (value.isNotBlank()) blocks.add(Block(BlockType.CHAPTER, value))
                }

                trimmed.startsWith("SUBTITLE:", ignoreCase = true) -> {
                    val value = trimmed.substringAfter(":").trim()
                    if (value.isNotBlank()) blocks.add(Block(BlockType.SUBTITLE, value))
                }

                trimmed.startsWith("TEXT:", ignoreCase = true) -> {
                    val value = trimmed.substringAfter(":").trim()
                    if (value.isNotBlank()) blocks.add(Block(BlockType.TEXT, value))
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val value = trimmed.removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()
                    if (value.isNotBlank()) blocks.add(Block(BlockType.LIST_ITEM, value))
                }

                else -> {
                    blocks.add(Block(BlockType.TEXT, trimmed))
                }
            }
        }

        return RenderModel(
            title = title.ifBlank { "Notions principales" },
            level = level,
            blocks = if (blocks.isEmpty()) listOf(Block(BlockType.TEXT, "Information non lisible dans le document.")) else blocks,
            hasStructuredFormat = true
        )
    }

    private fun parseNatural(cleanedRaw: String): RenderModel {
        val blocks = mutableListOf<Block>()
        val lines = cleanedRaw.lines()

        // 2026-04-26 — Préserve la respiration d'origine : paragraphes séparés,
        // listes lisibles, headings markdown reconnus sans aplatir en pavé unique.
        var paragraphBuffer = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                val paragraph = paragraphBuffer.joinToString("\n").trim()
                if (paragraph.isNotBlank()) blocks.add(Block(BlockType.TEXT, paragraph))
                paragraphBuffer = mutableListOf()
            }
        }

        lines.forEachIndexed { _, lineRaw ->
            val line = lineRaw.trimEnd()
            val trimmed = line.trim()

            if (trimmed.isBlank()) {
                flushParagraph()
                return@forEachIndexed
            }

            when {
                trimmed.startsWith("##") || trimmed.startsWith("#") -> {
                    flushParagraph()
                    val title = trimmed.replace(Regex("^#+\\s*"), "").trim()
                    if (title.isNotBlank()) blocks.add(Block(BlockType.CHAPTER, title))
                }

                trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 -> {
                    flushParagraph()
                    blocks.add(Block(BlockType.SUBTITLE, trimmed.removePrefix("**").removeSuffix("**").trim()))
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    flushParagraph()
                    val item = trimmed.removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()
                    if (item.isNotBlank()) blocks.add(Block(BlockType.LIST_ITEM, item))
                }

                else -> {
                    paragraphBuffer.add(line)
                }
            }
        }

        flushParagraph()

        val firstHeading = blocks.firstOrNull { it.type == BlockType.CHAPTER }?.content.orEmpty()

        return RenderModel(
            title = if (firstHeading.isNotBlank()) firstHeading else "Notions principales",
            level = "",
            blocks = if (blocks.isEmpty()) listOf(Block(BlockType.TEXT, cleanedRaw)) else blocks,
            hasStructuredFormat = false
        )
    }
}
