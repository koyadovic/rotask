package com.rotask.domain

import com.rotask.data.AppDatabase
import com.rotask.data.Task
import com.rotask.data.WorkSession
import java.time.LocalDate

data class TaskStatus(
    val task: Task,
    val targetSecondsToday: Long,
    val workedSecondsToday: Long,
    val remainingSecondsToday: Long
)

class TaskScheduler(private val db: AppDatabase) {

    suspend fun computeStatus(today: LocalDate): List<TaskStatus> {
        val settings = db.settingsDao().get() ?: return emptyList()
        val tasks = db.taskDao().getAll()
        if (tasks.isEmpty()) return emptyList()
        val enabledTasks = tasks.filter { it.enabled }
        val sumWeights = enabledTasks.sumOf { it.weight }
        val totalSecs = settings.dailyMinutes * 60L
        return tasks.map { t ->
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
                    remainingSecondsToday = 0L
                )
            }
        }
    }

    suspend fun pickNext(today: LocalDate): TaskStatus? {
        return computeStatus(today)
            .filter { it.task.enabled && it.remainingSecondsToday > 0 }
            .maxByOrNull { it.remainingSecondsToday }
    }

    suspend fun recordWork(taskId: Long, seconds: Long, today: LocalDate) {
        if (seconds <= 0) return
        db.workSessionDao().insert(
            WorkSession(taskId = taskId, date = today.toString(), durationSeconds = seconds)
        )
    }
}
