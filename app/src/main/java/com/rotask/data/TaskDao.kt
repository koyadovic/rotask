package com.rotask.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE groupId = :groupId ORDER BY name COLLATE NOCASE")
    suspend fun getAllInGroup(groupId: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
