package com.example.sunseeker_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.data.remote.SolarResult
import com.example.sunseeker_app.data.remote.WeatherRepository
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
    private val application: android.app.Application,
    private val eventsRepository: EventsRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage,
    private val weatherRepository: WeatherRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    // ... existing properties ...

    private val _state = MutableLiveData<CreateEventState>()
    val state: LiveData<CreateEventState> = _state

    private val _solarState = MutableLiveData<SolarState>()
    val solarState: LiveData<SolarState> = _solarState

    fun loadEvent(eventId: String): LiveData<EventEntity?> = eventsRepository.getEventById(eventId)

    fun createEvent(
        title: String,
        location: String,
        time: String,
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
                    time = time,
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
        time: String,
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
                    "time" to time,
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

    fun fetchSunTimes(latitude: Double, longitude: Double) {
        _solarState.value = SolarState.Loading
        viewModelScope.launch {
            when (val result = weatherRepository.fetchSolarData(latitude, longitude)) {
                is SolarResult.Success -> {
                    _solarState.value = SolarState.Success(result.data)
                }
                is SolarResult.Error -> {
                    _solarState.value = SolarState.Error(result.message)
                }
            }
        }
    }

    private suspend fun uploadImage(imageUri: Uri, userId: String): String {
        val ref = firebaseStorage.reference
            .child("events/$userId/${UUID.randomUUID()}.jpg")
        
        try {
            val inputStream = application.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Could not open stream for image")
            
            ref.putStream(inputStream).await()
            // Stream is closed by putStream or GC, but safer to let putStream handle it
        } catch (e: Exception) {
            throw Exception("Image upload failed (Stream): ${e.message}")
        }

        try {
            return ref.downloadUrl.await().toString()
        } catch (e: Exception) {
             throw Exception("Getting URL failed: ${e.message}")
        }
    }
}

sealed class CreateEventState {
    data object Loading : CreateEventState()
    data object Success : CreateEventState()
    data class Error(val message: String) : CreateEventState()
}

sealed class SolarState {
    data object Loading : SolarState()
    data class Success(val data: com.example.sunseeker_app.data.remote.SolarData) : SolarState()
    data class Error(val message: String) : SolarState()
}
