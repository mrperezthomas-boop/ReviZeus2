package com.revizeus.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ═══════════════════════════════════════════════════════════════
 * LEARNING RECOMMENDATION — Recommandations personnalisées
 * ═══════════════════════════════════════════════════════════════
 * Suggestions de révision générées par le ML
 */
@Entity(tableName = "learning_recommendation")
data class LearningRecommendation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Int = 1,
    val subject: String,
    val recommendationType: String,
    val priority: Int = 3,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)