package com.example.sunseeker_app.data.remote

import android.net.Uri

/**
 * Repository interface for file storage operations.
 * Abstracts away the Firebase Storage SDK from ViewModels.
 */
interface StorageRepository {
    /** Upload an image and return the download URL. */
    suspend fun uploadImage(path: String, imageUri: Uri): String

    /** Upload a profile image and return the download URL. */
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String

    /** Upload an event image and return the download URL. */
    suspend fun uploadEventImage(userId: String, imageUri: Uri): String
}
