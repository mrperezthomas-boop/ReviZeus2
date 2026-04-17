package com.revizeus.app.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserAnalyticsDao {

    @Insert
    suspend fun insert(analytics: UserAnalytics)

    @Query("SELECT * FROM user_analytics WHERE userId = :userId AND subject = :subject ORDER BY timestamp DESC")
    suspend fun getBySubject(userId: Int, subject: String): List<UserAnalytics>

    @Query("SELECT * FROM user_analytics WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(userId: Int, limit: Int): List<UserAnalytics>

    @Query("DELETE FROM user_analytics WHERE userId = :userId")
    suspend fun deleteAll(userId: Int)
}