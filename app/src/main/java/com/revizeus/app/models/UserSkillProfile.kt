package com.revizeus.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserSkillProfile - Profil de compétences par matière
 * Correspond EXACTEMENT à ce que AnalyticsManager.kt utilise
 */
@Entity(tableName = "user_skill_profile")
data class UserSkillProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Int,
    val subject: String,
    val topic: String,
    val masteryLevel: Float,
    val confidence: Float,
    val lastPracticed: Long,
    val practiceCount: Int,
    val avgResponseTime: Long,
    val successRate: Float,
    val needsReview: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)