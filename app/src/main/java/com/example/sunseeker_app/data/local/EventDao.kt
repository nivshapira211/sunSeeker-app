package com.example.sunseeker_app.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("SELECT * FROM events")
    fun getAllEvents(): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    fun getEventById(id: String): LiveData<EventEntity?>

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        deleteAll()
        insertAll(events)
    }
}
