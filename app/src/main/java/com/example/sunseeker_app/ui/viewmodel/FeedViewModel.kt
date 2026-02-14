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
class FeedViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val events: LiveData<List<Event>> = eventsRepository.getEvents()

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state

    private val _joinState = MutableLiveData<UiState?>()
    val joinState: LiveData<UiState?> = _joinState

    init {
        refresh()
    }

    fun refresh() {
        _state.value = FeedState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.refreshEvents()
                _state.value = FeedState.Idle
            } catch (e: Exception) {
                _state.value = FeedState.Error(e.message ?: "Failed to load feed")
            }
        }
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
                eventsRepository.joinEvent(eventId, userId)
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
}

sealed class FeedState {
    data object Loading : FeedState()
    data object Idle : FeedState()
    data class Error(val message: String) : FeedState()
}
