package com.example.storyforge.persistence

import androidx.room.*

@Entity(
    tableName = "turn_logs",
    foreignKeys = [ForeignKey(
        entity = GameSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class TurnLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val turnNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val speaker: String,
    val content: String,
    val metadataJson: String = "{}"
)