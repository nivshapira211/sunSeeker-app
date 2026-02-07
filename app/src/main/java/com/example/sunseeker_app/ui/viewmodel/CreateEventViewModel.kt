package com.example.sunseeker_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.data.repository.EventsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : ViewModel() {

    private val _state = MutableLiveData<CreateEventState>()
    val state: LiveData<CreateEventState> = _state

    fun loadEvent(eventId: String): LiveData<EventEntity?> = eventsRepository.getEventById(eventId)

    fun createEvent(
        title: String,
        location: String,
        description: String,
        imageUri: Uri?
    ) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _state.value = CreateEventState.Error("You must be logged in to create events")
            return
        }
        if (title.isBlank() || location.isBlank() || description.isBlank() || imageUri == null) {
            _state.value = CreateEventState.Error("All fields and image are required")
            return
        }

        _state.value = CreateEventState.Loading
        viewModelScope.launch {
            try {
                val imageUrl = withContext(Dispatchers.IO) { uploadImage(imageUri, userId) }
                val eventId = UUID.randomUUID().toString()
                val event = EventEntity(
                    id = eventId,
                    title = title.trim(),
                    location = location.trim(),
                    time = "TBD",
                    description = description.trim(),
                    imageUrl = imageUrl,
                    participantsCount = 0,
                    attendeeIds = emptyList(),
                    creatorId = userId
                )
                withContext(Dispatchers.IO) { eventsRepository.createEvent(event) }
                _state.value = CreateEventState.Success
            } catch (e: Exception) {
                _state.value = CreateEventState.Error(e.message ?: "Create failed")
            }
        }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        location: String,
        description: String,
        imageUri: Uri?,
        existingImageUrl: String
    ) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _state.value = CreateEventState.Error("You must be logged in to edit events")
            return
        }
        if (title.isBlank() || location.isBlank() || description.isBlank()) {
            _state.value = CreateEventState.Error("All fields are required")
            return
        }

        _state.value = CreateEventState.Loading
        viewModelScope.launch {
            try {
                val imageUrl = withContext(Dispatchers.IO) {
                    if (imageUri != null) {
                        uploadImage(imageUri, userId)
                    } else {
                        existingImageUrl
                    }
                }

                val updates = mapOf(
                    "title" to title.trim(),
                    "location" to location.trim(),
                    "description" to description.trim(),
                    "imageUrl" to imageUrl
                )
                withContext(Dispatchers.IO) { eventsRepository.updateEvent(eventId, updates) }
                _state.value = CreateEventState.Success
            } catch (e: Exception) {
                _state.value = CreateEventState.Error(e.message ?: "Update failed")
            }
        }
    }

    private suspend fun uploadImage(imageUri: Uri, userId: String): String {
        val ref = firebaseStorage.reference
            .child("events/$userId/${UUID.randomUUID()}.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }
}

sealed class CreateEventState {
    data object Loading : CreateEventState()
    data object Success : CreateEventState()
    data class Error(val message: String) : CreateEventState()
}
