package com.example.sunseeker_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.model.Event
import com.example.sunseeker_app.data.remote.AuthRepository
import com.example.sunseeker_app.data.remote.SolarResult
import com.example.sunseeker_app.data.remote.StorageRepository
import com.example.sunseeker_app.data.remote.WeatherRepository
import com.example.sunseeker_app.data.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val application: android.app.Application,
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository,
    private val weatherRepository: WeatherRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    private val _state = MutableLiveData<CreateEventState>()
    val state: LiveData<CreateEventState> = _state

    private val _solarState = MutableLiveData<SolarState>()
    val solarState: LiveData<SolarState> = _solarState

    fun loadEvent(eventId: String): LiveData<Event?> = eventsRepository.getEventById(eventId)

    fun createEvent(
        title: String,
        location: String,
        time: String,
        description: String,
        imageUri: Uri?,
        sunType: String
    ) {
        val userId = authRepository.getCurrentUserId()
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
                val imageUrl = withContext(Dispatchers.IO) {
                    storageRepository.uploadEventImage(userId, imageUri)
                }
                val eventId = UUID.randomUUID().toString()
                val event = Event(
                    id = eventId,
                    title = title.trim(),
                    location = location.trim(),
                    time = time,
                    description = description.trim(),
                    imageUrl = imageUrl,
                    participantsCount = 0,
                    attendeeIds = emptyList(),
                    creatorId = userId,
                    sunType = sunType
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
        existingImageUrl: String,
        sunType: String
    ) {
        val userId = authRepository.getCurrentUserId()
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
                        storageRepository.uploadEventImage(userId, imageUri)
                    } else {
                        existingImageUrl
                    }
                }

                val updates = mapOf(
                    "title" to title.trim(),
                    "location" to location.trim(),
                    "time" to time,
                    "description" to description.trim(),
                    "imageUrl" to imageUrl,
                    "sunType" to sunType
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
