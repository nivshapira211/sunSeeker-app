package com.example.sunseeker_app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.UserRepository
import com.example.sunseeker_app.data.local.SunSeekerDatabase
import com.example.sunseeker_app.data.local.User
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository
    
    init {
        val userDao = SunSeekerDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
    }

    fun getUserById(id: String): LiveData<User?> {
        return repository.getUserById(id)
    }

    fun upsertUser(user: User) {
        viewModelScope.launch {
            repository.upsertUser(user)
        }
    }
}