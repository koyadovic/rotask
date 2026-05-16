package com.rotask.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val weight: Int,
    val enabled: Boolean = true,
    val debtSeconds: Long = 0L
)
