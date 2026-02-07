package com.example.sunseeker_app.data.remote

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String): AuthResult
    fun logout()
    fun getCurrentUser(): FirebaseUser?
}

data class AuthResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
