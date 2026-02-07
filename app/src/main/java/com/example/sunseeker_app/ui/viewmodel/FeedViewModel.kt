package com.example.sunseeker_app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.data.repository.EventsRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    val events: LiveData<List<EventEntity>> = eventsRepository.getEvents()

    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state

    private val _joinState = MutableLiveData<FeedJoinState?>()
    val joinState: LiveData<FeedJoinState?> = _joinState

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
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _joinState.value = FeedJoinState.Error("You must be logged in to join")
            return
        }

        _joinState.value = FeedJoinState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.joinEvent(eventId, userId)
                _joinState.value = FeedJoinState.Success("Joined event")
            } catch (e: Exception) {
                _joinState.value = FeedJoinState.Error(e.message ?: "Join failed")
            }
        }
    }
}

sealed class FeedState {
    data object Loading : FeedState()
    data object Idle : FeedState()
    data class Error(val message: String) : FeedState()
}

sealed class FeedJoinState {
    data object Loading : FeedJoinState()
    data class Success(val message: String) : FeedJoinState()
    data class Error(val message: String) : FeedJoinState()
}
