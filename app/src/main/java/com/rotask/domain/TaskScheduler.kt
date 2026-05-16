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

    suspend fun ensureSettled(today: LocalDate) {
        val settings = db.settingsDao().get() ?: return
        val last = LocalDate.parse(settings.lastSettleDate)
        if (!last.isBefore(today)) return

        val tasks = db.taskDao().getAll().toMutableList()
        if (tasks.isEmpty()) {
            db.settingsDao().upsert(settings.copy(lastSettleDate = today.toString()))
            return
        }

        val totalSecs = settings.dailyMinutes * 60L
        var cur = last
        while (cur.isBefore(today)) {
            val sumWeights = tasks.sumOf { it.weight }
            if (sumWeights > 0) {
                tasks.forEachIndexed { i, t ->
                    val base = totalSecs * t.weight / sumWeights
                    val worked = db.workSessionDao().totalForDate(t.id, cur.toString())
                    val newDebt = (t.debtSeconds + base - worked).coerceAtLeast(0)
                    tasks[i] = t.copy(debtSeconds = newDebt)
                }
            }
            cur = cur.plusDays(1)
        }

        tasks.forEach { db.taskDao().update(it) }
        db.settingsDao().upsert(settings.copy(lastSettleDate = today.toString()))
    }

    suspend fun computeStatus(today: LocalDate): List<TaskStatus> {
        ensureSettled(today)
        val settings = db.settingsDao().get() ?: return emptyList()
        val tasks = db.taskDao().getAll()
        if (tasks.isEmpty()) return emptyList()
        val sumWeights = tasks.sumOf { it.weight }
        if (sumWeights == 0) return emptyList()
        val totalSecs = settings.dailyMinutes * 60L
        return tasks.map { t ->
            val base = totalSecs * t.weight / sumWeights
            val target = base + t.debtSeconds
            val worked = db.workSessionDao().totalForDate(t.id, today.toString())
            TaskStatus(
                task = t,
                targetSecondsToday = target,
                workedSecondsToday = worked,
                remainingSecondsToday = (target - worked).coerceAtLeast(0)
            )
        }
    }

    suspend fun pickNext(today: LocalDate): TaskStatus? {
        return computeStatus(today)
            .filter { it.remainingSecondsToday > 0 }
            .maxByOrNull { it.remainingSecondsToday }
    }

    suspend fun recordWork(taskId: Long, seconds: Long, today: LocalDate) {
        if (seconds <= 0) return
        db.workSessionDao().insert(
            WorkSession(taskId = taskId, date = today.toString(), durationSeconds = seconds)
        )
    }
}
