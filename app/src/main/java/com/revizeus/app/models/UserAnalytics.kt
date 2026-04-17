package com.revizeus.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserAnalytics - Tracking ML des réponses
 * Correspond EXACTEMENT à ce que AnalyticsManager.kt utilise
 */
@Entity(tableName = "user_analytics")
data class UserAnalytics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Int,
    val subject: String,
    val topic: String?,
    val questionText: String?,
    val questionId: String?,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val responseTime: Long,
    val difficulty: Int,
    val sessionId: String,
    val timestamp: Long
)