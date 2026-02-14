package com.example.sunseeker_app.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val storageRepository: StorageRepository
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { continuation.resume(AuthResult(isSuccess = true)) }
                .addOnFailureListener { e ->
                    continuation.resume(AuthResult(isSuccess = false, errorMessage = e.message))
                }
        }
    }

    override suspend fun register(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { continuation.resume(AuthResult(isSuccess = true)) }
                .addOnFailureListener { e ->
                    continuation.resume(AuthResult(isSuccess = false, errorMessage = e.message))
                }
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentDisplayName(): String? {
        return firebaseAuth.currentUser?.displayName
    }

    override fun getCurrentPhotoUrl(): String? {
        return firebaseAuth.currentUser?.photoUrl?.toString()
    }

    override fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun updateProfile(displayName: String, photoUri: Uri?) {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("Not logged in")

        val uploadedPhotoUri = if (photoUri != null) {
            val url = storageRepository.uploadProfileImage(user.uid, photoUri)
            Uri.parse(url)
        } else {
            null
        }

        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .apply { if (uploadedPhotoUri != null) setPhotoUri(uploadedPhotoUri) }
            .build()

        user.updateProfile(request).await()
    }
}
