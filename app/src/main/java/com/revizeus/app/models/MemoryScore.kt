package com.revizeus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ═══════════════════════════════════════════════════════════════
 * MODÈLE : MemoryScore
 * ═══════════════════════════════════════════════════════════════
 * CHANGEMENTS EFFECTUÉS :
 * - Ajout de la colonne "subject" (matière) réclamée par l'erreur ligne 170.
 * - Maintien des autres colonnes vitales (concept, correctCount).
 *
 * IDÉES FUTURES :
 * - Ajouter un "streak" local par concept : si l'élève répond juste
 * 3 fois de suite, le concept devient "Maîtrisé" (bouclier en or).
 */
@Entity(tableName = "memory_score")
data class MemoryScore(
    @PrimaryKey val conceptId: String,

    @ColumnInfo(name = "concept")
    var concept: String = "",

    @ColumnInfo(name = "subject") // <-- LA COLONNE QUI MANQUAIT AU DAO
    var subject: String = "",

    @ColumnInfo(name = "correctCount")
    var correctCount: Int = 0,

    var stabilityScore: Float = 0.0f,
    var lastReviewed: Long = System.currentTimeMillis(),
    var errorCount: Int = 0,
    var isFragile: Boolean = false
) {
    fun updateScore(isCorrect: Boolean) {
        if (isCorrect) {
            stabilityScore += 1.5f
            correctCount += 1
            isFragile = false
        } else {
            stabilityScore -= 2.0f
            errorCount += 1
            isFragile = true
        }
        lastReviewed = System.currentTimeMillis()
    }
}