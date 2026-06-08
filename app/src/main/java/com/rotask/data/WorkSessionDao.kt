package com.rotask.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Insert
    suspend fun insert(session: WorkSession): Long

    @Query("SELECT IFNULL(SUM(durationSeconds), 0) FROM work_sessions WHERE taskId = :taskId AND date = :date")
    suspend fun totalForDate(taskId: Long, date: String): Long

    @Query("SELECT COUNT(*) FROM work_sessions WHERE taskId = :taskId AND date = :date")
    suspend fun countForDate(taskId: Long, date: String): Int

    @Query("SELECT * FROM work_sessions ORDER BY date, id")
    suspend fun getAll(): List<WorkSession>

    @Query("SELECT COUNT(*) FROM work_sessions")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM work_sessions WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()
}
