package com.rotask.domain

import androidx.room.withTransaction
import com.rotask.data.AppDatabase
import com.rotask.data.Group
import com.rotask.data.Task
import com.rotask.data.WorkSession
import org.json.JSONArray
import org.json.JSONObject
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

    suspend fun statusForTask(taskId: Long): TaskStatus? = statusForTask(taskId, clock())

    private suspend fun statusForTask(taskId: Long, today: LocalDate): TaskStatus? {
        val task = db.taskDao().get(taskId) ?: return null
        return scheduler.computeGroupStatuses(today)
            .firstOrNull { it.group.id == task.groupId }
            ?.statuses
            ?.firstOrNull { it.task.id == taskId }
    }

    suspend fun pickNextInGroup(groupId: Long): TaskStatus? =
        scheduler.pickNextInGroup(groupId, clock())

    suspend fun pickNextInGroupExcluding(groupId: Long, excludeTaskId: Long): TaskStatus? =
        scheduler.pickNextInGroup(groupId, clock(), excludeTaskId)

    suspend fun addGroup(name: String, dailyMinutes: Int, timed: Boolean) {
        db.groupDao().insert(
            Group(
                name = name.trim(),
                dailyMinutes = dailyMinutes.coerceAtLeast(1),
                timed = timed,
            )
        )
    }

    suspend fun updateGroup(group: Group) {
        db.groupDao().update(
            group.copy(
                name = group.name.trim(),
                dailyMinutes = group.dailyMinutes.coerceAtLeast(1),
            )
        )
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
        scheduledDays: Int,
    ) {
        db.taskDao().insert(
            Task(
                groupId = groupId,
                name = name,
                description = description,
                weight = weight,
                enabled = enabled,
                scheduledDays = Task.sanitizedScheduledDays(scheduledDays),
            )
        )
    }

    suspend fun updateTask(task: Task) {
        db.taskDao().update(task.copy(scheduledDays = Task.sanitizedScheduledDays(task.scheduledDays)))
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

    suspend fun markTaskDone(taskId: Long) {
        val today = clock()
        val status = statusForTask(taskId, today) ?: return
        if (!status.task.enabled || !status.scheduledToday) return
        if (status.timed) {
            if (status.remainingSecondsToday > 0L) {
                scheduler.recordWork(taskId, status.remainingSecondsToday, today)
            }
        } else if (!status.completedToday) {
            scheduler.recordCompletion(taskId, today)
        }
    }

    suspend fun exportBackupJson(): String {
        val groups = db.groupDao().getAll()
        val tasks = db.taskDao().getAll()
        val workSessions = db.workSessionDao().getAll()
        return JSONObject()
            .put("version", BACKUP_VERSION)
            .put("groups", groups.toJsonArray { group ->
                JSONObject()
                    .put("id", group.id)
                    .put("name", group.name)
                    .put("dailyMinutes", group.dailyMinutes)
                    .put("timed", group.timed)
            })
            .put("tasks", tasks.toJsonArray { task ->
                JSONObject()
                    .put("id", task.id)
                    .put("groupId", task.groupId)
                    .put("name", task.name)
                    .put("description", task.description)
                    .put("weight", task.weight)
                    .put("enabled", task.enabled)
                    .put("scheduledDays", Task.sanitizedScheduledDays(task.scheduledDays))
            })
            .put("workSessions", workSessions.toJsonArray { session ->
                JSONObject()
                    .put("id", session.id)
                    .put("taskId", session.taskId)
                    .put("date", session.date)
                    .put("durationSeconds", session.durationSeconds)
            })
            .toString(2)
    }

    suspend fun importBackupJson(rawJson: String) {
        val backup = parseBackup(rawJson)
        db.withTransaction {
            db.workSessionDao().deleteAll()
            db.taskDao().deleteAll()
            db.groupDao().deleteAll()
            backup.groups.forEach { db.groupDao().insert(it) }
            backup.tasks.forEach { db.taskDao().insert(it) }
            backup.workSessions.forEach { db.workSessionDao().insert(it) }
        }
    }

    private fun parseBackup(rawJson: String): BackupData {
        val root = JSONObject(rawJson)
        val version = root.getInt("version")
        require(version == BACKUP_VERSION) { "Unsupported backup version: $version" }

        val groups = root.getJSONArray("groups").mapObjects { obj ->
            Group(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                dailyMinutes = obj.getInt("dailyMinutes").coerceAtLeast(1),
                timed = obj.optBoolean("timed", true),
            )
        }
        val groupIds = groups.map { it.id }.toSet()
        val tasks = root.getJSONArray("tasks").mapObjects { obj ->
            val groupId = obj.getLong("groupId")
            require(groupId in groupIds) { "Task references missing group: $groupId" }
            Task(
                id = obj.getLong("id"),
                groupId = groupId,
                name = obj.getString("name"),
                description = obj.optString("description", ""),
                weight = obj.getDouble("weight").takeIf { it > 0.0 } ?: 1.0,
                enabled = obj.optBoolean("enabled", true),
                scheduledDays = Task.sanitizedScheduledDays(obj.optInt("scheduledDays", Task.ALL_DAYS_MASK)),
            )
        }
        val taskIds = tasks.map { it.id }.toSet()
        val workSessions = root.optJSONArray("workSessions").orEmpty().mapObjects { obj ->
            val taskId = obj.getLong("taskId")
            require(taskId in taskIds) { "Work session references missing task: $taskId" }
            WorkSession(
                id = obj.getLong("id"),
                taskId = taskId,
                date = obj.getString("date"),
                durationSeconds = obj.getLong("durationSeconds").coerceAtLeast(0L),
            )
        }

        return BackupData(groups = groups, tasks = tasks, workSessions = workSessions)
    }

    private data class BackupData(
        val groups: List<Group>,
        val tasks: List<Task>,
        val workSessions: List<WorkSession>,
    )

    private inline fun <T> List<T>.toJsonArray(toJson: (T) -> JSONObject): JSONArray =
        JSONArray().also { array ->
            forEach { array.put(toJson(it)) }
        }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    companion object {
        private const val BACKUP_VERSION = 1
    }
}
