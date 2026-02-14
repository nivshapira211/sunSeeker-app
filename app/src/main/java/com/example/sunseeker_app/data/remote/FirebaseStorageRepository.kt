package com.example.sunseeker_app.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

/**
 * Firebase implementation of [StorageRepository].
 * All Firebase Storage SDK calls are encapsulated here.
 */
class FirebaseStorageRepository @Inject constructor(
    private val firebaseStorage: FirebaseStorage
) : StorageRepository {

    override suspend fun uploadImage(path: String, imageUri: Uri): String {
        val ref = firebaseStorage.reference.child(path)
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        return uploadImage("profiles/$userId/${UUID.randomUUID()}.jpg", imageUri)
    }

    override suspend fun uploadEventImage(userId: String, imageUri: Uri): String {
        return uploadImage("events/$userId/${UUID.randomUUID()}.jpg", imageUri)
    }
}
