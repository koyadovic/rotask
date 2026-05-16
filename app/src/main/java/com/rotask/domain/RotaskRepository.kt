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
    fun observeWorkSessionsTick() = db.workSessionDao().observeCount()

    suspend fun bootstrap() {
        if (db.settingsDao().get() == null) {
            db.settingsDao().upsert(SettingsEntity(dailyMinutes = 60))
        }
    }

    suspend fun status(): List<TaskStatus> = scheduler.computeStatus(clock())

    suspend fun statusForTask(taskId: Long): TaskStatus? =
        status().firstOrNull { it.task.id == taskId }

    suspend fun pickNext(): TaskStatus? = scheduler.pickNext(clock())

    suspend fun pickNextExcluding(excludeTaskId: Long): TaskStatus? {
        return scheduler.computeStatus(clock())
            .filter { it.task.id != excludeTaskId && it.task.enabled && it.remainingSecondsToday > 0 }
            .maxByOrNull { it.remainingSecondsToday }
    }

    suspend fun setDailyMinutes(minutes: Int) {
        val s = db.settingsDao().get() ?: return
        db.settingsDao().upsert(s.copy(dailyMinutes = minutes))
    }

    suspend fun addTask(name: String, description: String, weight: Double, enabled: Boolean) {
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
        db.taskDao().update(task)
    }

    suspend fun setEnabled(task: Task, enabled: Boolean) {
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
