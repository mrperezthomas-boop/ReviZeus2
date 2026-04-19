package com.revizeus.app.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TempleAdventureDao {

    @Query(
        """
        SELECT * FROM temple_adventure_map
        WHERE subject = :subject
          AND god_id = :godId
          AND temple_level = :templeLevel
          AND map_index = :mapIndex
        LIMIT 1
        """
    )
    suspend fun getMapState(
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int
    ): TempleAdventureMapEntity?

    @Query(
        """
        SELECT * FROM temple_adventure_node_progress
        WHERE subject = :subject
          AND god_id = :godId
          AND temple_level = :templeLevel
          AND map_index = :mapIndex
        ORDER BY id ASC
        """
    )
    suspend fun getNodeStates(
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int
    ): List<TempleAdventureNodeProgressEntity>

    @Query(
        """
        SELECT * FROM temple_adventure_node_progress
        WHERE subject = :subject
          AND god_id = :godId
          AND temple_level = :templeLevel
          AND map_index = :mapIndex
          AND node_id = :nodeId
        LIMIT 1
        """
    )
    suspend fun getNodeState(
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int,
        nodeId: String
    ): TempleAdventureNodeProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMapState(entity: TempleAdventureMapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNodeState(entity: TempleAdventureNodeProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNodeStates(entities: List<TempleAdventureNodeProgressEntity>)

    @Query(
        """
        UPDATE temple_adventure_node_progress
        SET is_completed = 1,
            is_unlocked = 1,
            completion_count = completion_count + 1,
            last_played_at = :completedAt,
            best_result = CASE WHEN best_result < :result THEN :result ELSE best_result END
        WHERE subject = :subject
          AND god_id = :godId
          AND temple_level = :templeLevel
          AND map_index = :mapIndex
          AND node_id = :nodeId
        """
    )
    suspend fun markNodeCompleted(
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int,
        nodeId: String,
        completedAt: Long,
        result: Int
    ): Int

    @Query(
        """
        UPDATE temple_adventure_map
        SET is_completed = 1,
            completion_percent = 100,
            last_played_at = :completedAt
        WHERE subject = :subject
          AND god_id = :godId
          AND temple_level = :templeLevel
          AND map_index = :mapIndex
        """
    )
    suspend fun markMapCompleted(
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int,
        completedAt: Long
    ): Int

    @Query(
        """
        SELECT * FROM temple_adventure_map
        WHERE subject = :subject
          AND god_id = :godId
          AND is_unlocked = 1
        ORDER BY temple_level DESC, map_index DESC, last_played_at DESC
        LIMIT 1
        """
    )
    suspend fun getLastUnlockedMap(
        subject: String,
        godId: String
    ): TempleAdventureMapEntity?

    @Query(
        """
        SELECT * FROM temple_adventure_node_progress
        WHERE subject = :subject
          AND god_id = :godId
          AND temple_level = :templeLevel
          AND map_index = :mapIndex
          AND is_completed = 1
        ORDER BY last_played_at DESC, id DESC
        """
    )
    suspend fun getReplayableNodes(
        subject: String,
        godId: String,
        templeLevel: Int,
        mapIndex: Int
    ): List<TempleAdventureNodeProgressEntity>
}
