package com.qiaomu.prompter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updated_at DESC")
    fun observeScripts(): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getScript(id: String): Script?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(script: Script)

    @Delete
    suspend fun delete(script: Script)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun count(): Int
}
