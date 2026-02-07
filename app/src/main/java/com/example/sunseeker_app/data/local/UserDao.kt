package com.example.sunseeker_app.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :id")
    fun getUserById(id: String): LiveData<User?>

    @Upsert
    suspend fun upsertUser(user: User)
}