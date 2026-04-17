package com.revizeus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Représente un parchemin/cours enregistré par l'Oracle.
 *
 * ÉVOLUTION CONSERVATIVE :
 * - title reste conservé pour toute compatibilité ancienne
 * - customTitle permet le renommage manuel sans casser les anciens flux
 * - folderName permet la création de sous-dossiers à l'intérieur d'un temple
 */
@Entity(tableName = "course_entry")
data class CourseEntry(
    @PrimaryKey val id: String,
    val subject: String,
    val dateAdded: Long,
    val extractedText: String,
    val keyConceptsString: String,
    val difficultyLevel: Int,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "lastReviewedAt") var lastReviewedAt: Long = 0L,
    @ColumnInfo(name = "folder_name") val folderName: String = "",
    @ColumnInfo(name = "custom_title") val customTitle: String = ""
) {

    // ═══════════════════════════════════════════════════════════════
    // EXTENSIONS POUR COMPATIBILITÉ AVEC LES BUILDERS
    // ═══════════════════════════════════════════════════════════════

    /** Alias pour les builders qui utilisent "matiere" */
    val matiere: String get() = subject

    /** Alias pour les builders qui utilisent "topic" */
    val topic: String get() = displayTitle()

    /** Alias pour les builders qui utilisent "content" */
    val content: String get() = extractedText

    // ═══════════════════════════════════════════════════════════════

    fun getKeyConceptsList(): List<String> {
        return if (keyConceptsString.isEmpty()) emptyList() else keyConceptsString.split(",")
    }

    fun displayTitle(): String {
        return when {
            customTitle.isNotBlank() -> customTitle.trim()
            title.isNotBlank() -> title.trim()
            else -> "Parchemin sans nom"
        }
    }

    fun displayFolder(): String {
        return folderName.trim()
    }
}
