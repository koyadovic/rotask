package com.rotask.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dailyMinutes: Int,
    val timed: Boolean = true,
    val taskDurationMode: Boolean = false,
) {
    val timingMode: GroupTimingMode
        get() = when {
            !timed -> GroupTimingMode.UNTIMED
            taskDurationMode -> GroupTimingMode.PER_TASK
            else -> GroupTimingMode.WEIGHTED
        }

    fun withTimingMode(mode: GroupTimingMode): Group = copy(
        timed = mode != GroupTimingMode.UNTIMED,
        taskDurationMode = when (mode) {
            GroupTimingMode.WEIGHTED -> false
            GroupTimingMode.PER_TASK -> true
            GroupTimingMode.UNTIMED -> taskDurationMode
        },
    )
}

enum class GroupTimingMode {
    WEIGHTED,
    PER_TASK,
    UNTIMED,
}
