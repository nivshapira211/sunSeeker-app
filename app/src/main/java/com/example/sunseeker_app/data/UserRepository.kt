package com.example.sunseeker_app.data

import androidx.lifecycle.LiveData
import com.example.sunseeker_app.data.local.User
import com.example.sunseeker_app.data.local.UserDao

class UserRepository(private val userDao: UserDao) {

    fun getUserById(id: String): LiveData<User?> {
        // Here, we would eventually add the logic to check the Remote API 
        // via the Connectivity Specialist's implementation.
        // For now, it serves as the single source of truth from Room.
        return userDao.getUserById(id)
    }

    suspend fun upsertUser(user: User) {
        userDao.upsertUser(user)
    }
}