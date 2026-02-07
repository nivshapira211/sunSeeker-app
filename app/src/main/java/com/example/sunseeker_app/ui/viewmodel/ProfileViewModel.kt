package com.example.sunseeker_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.data.remote.AuthRepository
import com.example.sunseeker_app.data.repository.EventsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : ViewModel() {

    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState

    val myEvents: LiveData<List<EventEntity>> = Transformations.map(
        eventsRepository.getEvents()
    ) { events ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) emptyList() else events.filter { it.creatorId == uid }
    }

    private val _profileUi = MutableLiveData<ProfileUi>()
    val profileUi: LiveData<ProfileUi> = _profileUi

    fun refreshProfile() {
        val user = firebaseAuth.currentUser
        _profileUi.value = ProfileUi(
            name = user?.displayName ?: "SunSeeker User",
            photoUrl = user?.photoUrl?.toString()
        )
    }

    fun logout() {
        authRepository.logout()
    }

    fun updateProfile(displayName: String, imageUri: Uri?) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            _profileState.value = ProfileState.Error("Not logged in")
            return
        }

        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val photoUrl = withContext(Dispatchers.IO) {
                    if (imageUri != null) uploadProfileImage(user.uid, imageUri) else null
                }
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .apply { if (photoUrl != null) setPhotoUri(photoUrl) }
                    .build()

                withContext(Dispatchers.IO) { user.updateProfile(request).await() }
                refreshProfile()
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Update failed")
            }
        }
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: Uri): Uri {
        val ref = firebaseStorage.reference
            .child("profiles/$userId/${UUID.randomUUID()}.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await()
    }
}

sealed class ProfileState {
    data object Loading : ProfileState()
    data object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
}

data class ProfileUi(
    val name: String,
    val photoUrl: String?
)
