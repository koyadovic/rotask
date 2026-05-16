package com.rotask.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index("groupId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val description: String = "",
    val weight: Double,
    val enabled: Boolean = true,
)
