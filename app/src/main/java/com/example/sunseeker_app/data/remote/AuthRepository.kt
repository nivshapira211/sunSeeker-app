package com.example.sunseeker_app.data.remote

import android.net.Uri

/**
 * Repository interface for authentication operations.
 * Abstracts away the Firebase Auth SDK from ViewModels.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String): AuthResult
    fun logout()

    /** Get the current user's ID, or null if not logged in. */
    fun getCurrentUserId(): String?

    /** Get the current user's display name. */
    fun getCurrentDisplayName(): String?

    /** Get the current user's photo URL. */
    fun getCurrentPhotoUrl(): String?

    /** Returns true if a user is currently logged in. */
    fun isLoggedIn(): Boolean

    /** Update the current user's profile (display name and optional photo). */
    suspend fun updateProfile(displayName: String, photoUri: Uri?)
}

data class AuthResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
