package com.rotask.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkSessionDao {
    @Insert
    suspend fun insert(session: WorkSession): Long

    @Query("SELECT IFNULL(SUM(durationSeconds), 0) FROM work_sessions WHERE taskId = :taskId AND date = :date")
    suspend fun totalForDate(taskId: Long, date: String): Long

    @Query("DELETE FROM work_sessions WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)
}
