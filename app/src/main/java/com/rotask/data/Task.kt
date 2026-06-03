package com.rotask.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(
    tableName = "tasks",
    indices = [Index("groupId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val description: String = "",
    val weight: Double,
    val enabled: Boolean = true,
    val scheduledDays: Int = ALL_DAYS_MASK,
) {
    fun isScheduledOn(dayOfWeek: DayOfWeek): Boolean =
        (scheduledDays and dayMask(dayOfWeek)) != 0

    companion object {
        const val MONDAY_MASK = 1
        const val TUESDAY_MASK = 2
        const val WEDNESDAY_MASK = 4
        const val THURSDAY_MASK = 8
        const val FRIDAY_MASK = 16
        const val SATURDAY_MASK = 32
        const val SUNDAY_MASK = 64
        const val ALL_DAYS_MASK = 127

        fun dayMask(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
            DayOfWeek.MONDAY -> MONDAY_MASK
            DayOfWeek.TUESDAY -> TUESDAY_MASK
            DayOfWeek.WEDNESDAY -> WEDNESDAY_MASK
            DayOfWeek.THURSDAY -> THURSDAY_MASK
            DayOfWeek.FRIDAY -> FRIDAY_MASK
            DayOfWeek.SATURDAY -> SATURDAY_MASK
            DayOfWeek.SUNDAY -> SUNDAY_MASK
        }

        fun sanitizedScheduledDays(mask: Int): Int {
            val scheduledDays = mask and ALL_DAYS_MASK
            return if (scheduledDays == 0) ALL_DAYS_MASK else scheduledDays
        }
    }
}
