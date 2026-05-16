package com.rotask.domain

import com.rotask.data.AppDatabase
import com.rotask.data.Group
import com.rotask.data.Task
import java.time.LocalDate

class RotaskRepository(
    private val db: AppDatabase,
    private val clock: () -> LocalDate = { LocalDate.now() },
) {
    private val scheduler = TaskScheduler(db)

    fun observeGroups() = db.groupDao().observeAll()
    fun observeTasks() = db.taskDao().observeAll()
    fun observeWorkSessionsTick() = db.workSessionDao().observeCount()

    suspend fun bootstrap() {
        // Nothing to bootstrap: groups and tasks are created by the user.
    }

    suspend fun groupStatuses(): List<GroupStatus> = scheduler.computeGroupStatuses(clock())

    suspend fun statusForTask(taskId: Long): TaskStatus? {
        val task = db.taskDao().get(taskId) ?: return null
        return groupStatuses()
            .firstOrNull { it.group.id == task.groupId }
            ?.statuses
            ?.firstOrNull { it.task.id == taskId }
    }

    suspend fun pickNextInGroup(groupId: Long): TaskStatus? =
        scheduler.pickNextInGroup(groupId, clock())

    suspend fun pickNextInGroupExcluding(groupId: Long, excludeTaskId: Long): TaskStatus? =
        scheduler.pickNextInGroup(groupId, clock(), excludeTaskId)

    suspend fun addGroup(name: String, dailyMinutes: Int) {
        db.groupDao().insert(Group(name = name.trim(), dailyMinutes = dailyMinutes.coerceAtLeast(1)))
    }

    suspend fun updateGroup(group: Group) {
        db.groupDao().update(group)
    }

    suspend fun deleteGroup(group: Group) {
        val tasks = db.taskDao().getAllInGroup(group.id)
        tasks.forEach {
            db.workSessionDao().deleteForTask(it.id)
            db.taskDao().delete(it)
        }
        db.groupDao().delete(group)
    }

    suspend fun addTask(
        groupId: Long,
        name: String,
        description: String,
        weight: Double,
        enabled: Boolean,
    ) {
        db.taskDao().insert(
            Task(
                groupId = groupId,
                name = name,
                description = description,
                weight = weight,
                enabled = enabled,
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
