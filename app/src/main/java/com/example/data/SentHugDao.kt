package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SentHugDao {
    @Query("SELECT * FROM sent_hugs ORDER BY timestamp DESC")
    fun getAllSentHugs(): Flow<List<SentHug>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentHug(hug: SentHug)

    @Query("DELETE FROM sent_hugs WHERE id = :id")
    suspend fun deleteSentHugById(id: Int)

    @Query("DELETE FROM sent_hugs")
    suspend fun clearAll()
}
