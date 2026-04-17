package com.revizeus.app.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LearningRecommendationDao {

    @Insert
    suspend fun insert(recommendation: LearningRecommendation)

    @Query("SELECT * FROM learning_recommendation WHERE userId = :userId AND isRead = 0 ORDER BY priority ASC, createdAt DESC")
    suspend fun getUnread(userId: Int): List<LearningRecommendation>

    @Query("UPDATE learning_recommendation SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM learning_recommendation WHERE userId = :userId")
    suspend fun deleteAll(userId: Int)
}