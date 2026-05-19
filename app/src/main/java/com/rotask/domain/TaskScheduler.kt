package com.rotask.domain

import com.rotask.data.AppDatabase
import com.rotask.data.Group
import com.rotask.data.Task
import com.rotask.data.WorkSession
import java.time.LocalDate

data class TaskStatus(
    val task: Task,
    val targetSecondsToday: Long,
    val workedSecondsToday: Long,
    val remainingSecondsToday: Long,
) {
    /** Fraction of today's target still to do. 0 when complete, 1 when nothing done. */
    val percentIncomplete: Double
        get() = if (targetSecondsToday > 0) {
            remainingSecondsToday.toDouble() / targetSecondsToday
        } else 0.0
}

data class GroupStatus(
    val group: Group,
    val statuses: List<TaskStatus>,
) {
    val totalTargetSeconds: Long get() = statuses.sumOf { it.targetSecondsToday }
    val totalWorkedSeconds: Long get() = statuses.sumOf { it.workedSecondsToday }
    val hasWorkRemaining: Boolean
        get() = statuses.any { it.task.enabled && it.remainingSecondsToday > 0 }
}

class TaskScheduler(private val db: AppDatabase) {

    suspend fun computeGroupStatuses(today: LocalDate): List<GroupStatus> {
        val groups = db.groupDao().getAll()
        return groups.map { group ->
            val tasks = db.taskDao().getAllInGroup(group.id)
            val enabledTasks = tasks.filter { it.enabled }
            val sumWeights = enabledTasks.sumOf { it.weight }
            val totalSecs = group.dailyMinutes * 60L
            val statuses = tasks.map { t ->
                val worked = db.workSessionDao().totalForDate(t.id, today.toString())
                if (t.enabled && sumWeights > 0.0) {
                    val target = (totalSecs * t.weight / sumWeights).toLong()
                    TaskStatus(
                        task = t,
                        targetSecondsToday = target,
                        workedSecondsToday = worked,
                        remainingSecondsToday = (target - worked).coerceAtLeast(0)
                    )
                } else {
                    TaskStatus(
                        task = t,
                        targetSecondsToday = 0L,
                        workedSecondsToday = worked,
                        remainingSecondsToday = 0L,
                    )
                }
            }
            GroupStatus(group = group, statuses = statuses)
        }
    }

    suspend fun pickNextInGroup(
        groupId: Long,
        today: LocalDate,
        excludeTaskId: Long? = null,
    ): TaskStatus? {
        val candidates = computeGroupStatuses(today)
            .firstOrNull { it.group.id == groupId }
            ?.statuses
            ?.filter { it.task.enabled && it.remainingSecondsToday > 0 && it.task.id != excludeTaskId }
            ?: return null
        if (candidates.isEmpty()) return null
        val maxPct = candidates.maxOf { it.percentIncomplete }
        val topTier = candidates.filter { it.percentIncomplete >= maxPct - PCT_EPSILON }
        return topTier.random()
    }

    suspend fun recordWork(taskId: Long, seconds: Long, today: LocalDate) {
        if (seconds <= 0) return
        db.workSessionDao().insert(
            WorkSession(taskId = taskId, date = today.toString(), durationSeconds = seconds)
        )
    }

    companion object {
        /** Tolerance for considering two percent-incomplete values equal (treated as a tie). */
        private const val PCT_EPSILON = 1e-9
    }
}
