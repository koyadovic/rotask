package com.rotask.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM `groups` ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Group>>

    @Query("SELECT * FROM `groups` ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Group>

    @Query("SELECT * FROM `groups` WHERE id = :id")
    suspend fun get(id: Long): Group?

    @Insert
    suspend fun insert(group: Group): Long

    @Update
    suspend fun update(group: Group)

    @Delete
    suspend fun delete(group: Group)

    @Query("DELETE FROM `groups`")
    suspend fun deleteAll()
}
