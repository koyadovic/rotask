package com.rotask.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_sessions",
    indices = [Index("taskId", "date")]
)
data class WorkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val date: String,
    val durationSeconds: Long
)
