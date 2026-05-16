package com.rotask.domain

import com.rotask.data.AppDatabase
import com.rotask.data.SettingsEntity
import com.rotask.data.Task
import java.time.LocalDate

class RotaskRepository(
    private val db: AppDatabase,
    private val clock: () -> LocalDate = { LocalDate.now() }
) {
    private val scheduler = TaskScheduler(db)

    fun observeTasks() = db.taskDao().observeAll()
    fun observeSettings() = db.settingsDao().observe()

    suspend fun bootstrap() {
        if (db.settingsDao().get() == null) {
            db.settingsDao().upsert(
                SettingsEntity(dailyMinutes = 60, lastSettleDate = clock().toString())
            )
        }
        scheduler.ensureSettled(clock())
    }

    suspend fun status(): List<TaskStatus> = scheduler.computeStatus(clock())

    suspend fun statusForTask(taskId: Long): TaskStatus? =
        status().firstOrNull { it.task.id == taskId }

    suspend fun pickNext(): TaskStatus? = scheduler.pickNext(clock())

    suspend fun setDailyMinutes(minutes: Int) {
        scheduler.ensureSettled(clock())
        val s = db.settingsDao().get() ?: return
        db.settingsDao().upsert(s.copy(dailyMinutes = minutes))
    }

    suspend fun addTask(name: String, description: String, weight: Int, enabled: Boolean) {
        scheduler.ensureSettled(clock())
        db.taskDao().insert(
            Task(
                name = name,
                description = description,
                weight = weight,
                enabled = enabled
            )
        )
    }

    suspend fun updateTask(task: Task) {
        scheduler.ensureSettled(clock())
        db.taskDao().update(task)
    }

    suspend fun setEnabled(task: Task, enabled: Boolean) {
        scheduler.ensureSettled(clock())
        db.taskDao().update(task.copy(enabled = enabled))
    }

    suspend fun deleteTask(task: Task) {
        db.workSessionDao().deleteForTask(task.id)
        db.taskDao().delete(task)
    }

    suspend fun recordWork(taskId: Long, seconds: Long) {
        scheduler.recordWork(taskId, seconds, clock())
    }
}
