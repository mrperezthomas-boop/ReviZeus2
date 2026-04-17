package com.revizeus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "temple_adventure_node_progress",
    indices = [
        Index(value = ["subject", "god_id", "temple_level", "map_index", "node_id"], unique = true)
    ]
)
data class TempleAdventureNodeProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "subject")
    val subject: String,
    @ColumnInfo(name = "god_id")
    val godId: String,
    @ColumnInfo(name = "temple_level")
    val templeLevel: Int,
    @ColumnInfo(name = "map_index")
    val mapIndex: Int,
    @ColumnInfo(name = "node_id")
    val nodeId: String,
    @ColumnInfo(name = "node_type")
    val nodeType: String,
    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "completion_count")
    val completionCount: Int,
    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long,
    @ColumnInfo(name = "best_result")
    val bestResult: Int,
    @ColumnInfo(name = "reward_claimed_state_json")
    val rewardClaimedStateJson: String,
    @ColumnInfo(name = "rare_state_locked")
    val rareStateLocked: Boolean,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String
)
