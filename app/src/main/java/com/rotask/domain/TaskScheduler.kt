package com.rotask.domain

import com.rotask.data.AppDatabase
import com.rotask.data.Group
import com.rotask.data.Task
import com.rotask.data.WorkSession
import java.time.LocalDate

data class TaskStatus(
    val task: Task,
    val timed: Boolean,
    val scheduledToday: Boolean,
    val completedToday: Boolean,
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
    val scheduledTaskCountToday: Int
        get() = statuses.count { it.task.enabled && it.scheduledToday }
    val completedTaskCountToday: Int
        get() = statuses.count { it.task.enabled && it.scheduledToday && it.completedToday }
    val hasWorkRemaining: Boolean
        get() = if (group.timed) {
            statuses.any { it.task.enabled && it.scheduledToday && it.remainingSecondsToday > 0 }
        } else {
            statuses.any { it.task.enabled && it.scheduledToday && !it.completedToday }
        }
}

class TaskScheduler(private val db: AppDatabase) {

    suspend fun computeGroupStatuses(today: LocalDate): List<GroupStatus> {
        val groups = db.groupDao().getAll()
        return groups.map { group ->
            val tasks = db.taskDao().getAllInGroup(group.id)
            val enabledTasks = tasks.filter { it.enabled && it.isScheduledOn(today.dayOfWeek) }
            val sumWeights = enabledTasks.sumOf { it.weight }
            val totalSecs = group.dailyMinutes * 60L
            val targetSecondsByTaskId = if (group.timed && sumWeights > 0.0) {
                allocateTargetSeconds(enabledTasks, totalSecs, sumWeights)
            } else {
                emptyMap()
            }
            val statuses = tasks.map { t ->
                val scheduledToday = t.isScheduledOn(today.dayOfWeek)
                if (!group.timed) {
                    val completed = db.workSessionDao().countForDate(t.id, today.toString()) > 0
                    TaskStatus(
                        task = t,
                        timed = false,
                        scheduledToday = scheduledToday,
                        completedToday = completed,
                        targetSecondsToday = 0L,
                        workedSecondsToday = 0L,
                        remainingSecondsToday = 0L,
                    )
                } else if (t.enabled && scheduledToday && sumWeights > 0.0) {
                    val worked = db.workSessionDao().totalForDate(t.id, today.toString())
                    val target = targetSecondsByTaskId[t.id] ?: 0L
                    val remaining = (target - worked).coerceAtLeast(0)
                    TaskStatus(
                        task = t,
                        timed = true,
                        scheduledToday = scheduledToday,
                        completedToday = target > 0L && remaining == 0L,
                        targetSecondsToday = target,
                        workedSecondsToday = worked,
                        remainingSecondsToday = remaining
                    )
                } else {
                    val worked = db.workSessionDao().totalForDate(t.id, today.toString())
                    TaskStatus(
                        task = t,
                        timed = true,
                        scheduledToday = scheduledToday,
                        completedToday = false,
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
    ): TaskStatus? = pickNextInGroup(
        groupId = groupId,
        today = today,
        excludeTaskIds = setOfNotNull(excludeTaskId),
    )

    suspend fun pickNextInGroup(
        groupId: Long,
        today: LocalDate,
        excludeTaskIds: Set<Long>,
    ): TaskStatus? {
        val groupStatus = computeGroupStatuses(today)
            .firstOrNull { it.group.id == groupId }
            ?: return null
        val candidates = groupStatus.statuses
            .filter {
                it.task.enabled &&
                    it.scheduledToday &&
                    it.task.id !in excludeTaskIds &&
                    if (groupStatus.group.timed) {
                        it.remainingSecondsToday > 0
                    } else {
                        !it.completedToday
                    }
            }
        if (candidates.isEmpty()) return null
        if (!groupStatus.group.timed) {
            return candidates.minWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.task.name })
        }
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

    suspend fun recordCompletion(taskId: Long, today: LocalDate) {
        val date = today.toString()
        if (db.workSessionDao().countForDate(taskId, date) > 0) return
        db.workSessionDao().insert(
            WorkSession(taskId = taskId, date = date, durationSeconds = 0L)
        )
    }

    private fun allocateTargetSeconds(
        tasks: List<Task>,
        totalSeconds: Long,
        sumWeights: Double,
    ): Map<Long, Long> {
        if (tasks.isEmpty() || totalSeconds <= 0L || sumWeights <= 0.0) return emptyMap()

        data class TargetPart(
            val task: Task,
            val baseSeconds: Long,
            val fractionalRemainder: Double,
        )

        val parts = tasks.map { task ->
            val rawSeconds = totalSeconds * task.weight / sumWeights
            val baseSeconds = rawSeconds.toLong()
            TargetPart(
                task = task,
                baseSeconds = baseSeconds,
                fractionalRemainder = rawSeconds - baseSeconds,
            )
        }
        val remainingSeconds = (totalSeconds - parts.sumOf { it.baseSeconds }).coerceAtLeast(0L)
        val extraSecondTaskIds = parts
            .sortedWith(
                compareByDescending<TargetPart> { it.fractionalRemainder }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.task.name }
                    .thenBy { it.task.id }
            )
            .take(remainingSeconds.toInt())
            .map { it.task.id }
            .toSet()

        return parts.associate { part ->
            part.task.id to part.baseSeconds + if (part.task.id in extraSecondTaskIds) 1L else 0L
        }
    }

    companion object {
        /** Tolerance for considering two percent-incomplete values equal (treated as a tie). */
        private const val PCT_EPSILON = 1e-9
    }
}
