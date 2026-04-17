package com.revizeus.app.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserSkillProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserSkillProfile)

    @Update
    suspend fun update(profile: UserSkillProfile)

    @Query("SELECT * FROM user_skill_profile WHERE userId = :userId AND subject = :subject AND topic = :topic LIMIT 1")
    suspend fun get(userId: Int, subject: String, topic: String): UserSkillProfile?

    @Query("SELECT * FROM user_skill_profile WHERE userId = :userId ORDER BY masteryLevel DESC")
    suspend fun getAllByUser(userId: Int): List<UserSkillProfile>

    @Query("DELETE FROM user_skill_profile WHERE userId = :userId")
    suspend fun deleteAll(userId: Int)
}