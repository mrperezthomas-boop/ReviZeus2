package com.revizeus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "temple_adventure_map",
    indices = [
        Index(value = ["subject", "god_id", "temple_level", "map_index"], unique = true)
    ]
)
data class TempleAdventureMapEntity(
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
    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "completion_percent")
    val completionPercent: Int,
    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long,
    @ColumnInfo(name = "visual_state_json")
    val visualStateJson: String,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String
)
