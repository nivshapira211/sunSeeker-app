package com.example.sunseeker_app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.model.Event
import com.example.sunseeker_app.data.remote.AuthRepository
import com.example.sunseeker_app.data.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _joinState = MutableLiveData<UiState?>()
    val joinState: LiveData<UiState?> = _joinState

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    fun getEvent(eventId: String): LiveData<Event?> {
        return eventsRepository.getEventById(eventId)
    }

    fun isOwner(event: Event): Boolean {
        return authRepository.getCurrentUserId() == event.creatorId
    }

    fun joinEvent(eventId: String) {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _joinState.value = UiState.Error("You must be logged in to join")
            return
        }

        _joinState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val displayName = authRepository.getCurrentDisplayName()
                eventsRepository.joinEvent(eventId, userId, displayName)
                _joinState.value = UiState.Success("Joined event")
            } catch (e: Exception) {
                _joinState.value = UiState.Error(e.message ?: "Join failed")
            }
        }
    }

    fun leaveEvent(eventId: String) {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
             _joinState.value = UiState.Error("You must be logged in")
            return
        }

        _joinState.value = UiState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.leaveEvent(eventId, userId)
                _joinState.value = UiState.Success("Left event")
            } catch (e: Exception) {
                _joinState.value = UiState.Error(e.message ?: "Leave failed")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        _joinState.value = UiState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.deleteEvent(eventId)
                _joinState.value = UiState.Success("Event deleted")
            } catch (e: Exception) {
                _joinState.value = UiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    fun clearJoinState() {
        _joinState.value = null
    }
}
