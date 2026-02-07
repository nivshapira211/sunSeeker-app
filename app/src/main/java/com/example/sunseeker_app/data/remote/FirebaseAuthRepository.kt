package com.example.sunseeker_app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
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

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}
