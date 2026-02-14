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
class EventViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _joinState = MutableLiveData<ActionState>()
    val joinState: LiveData<ActionState> = _joinState

    fun getEvent(eventId: String): LiveData<EventEntity?> {
        return eventsRepository.getEventById(eventId)
    }

    fun isOwner(event: EventEntity): Boolean {
        return firebaseAuth.currentUser?.uid == event.creatorId
    }

    fun joinEvent(eventId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _joinState.value = ActionState.Error("You must be logged in to join")
            return
        }

        _joinState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.joinEvent(eventId, userId)
                _joinState.value = ActionState.Success("Joined event")
            } catch (e: Exception) {
                _joinState.value = ActionState.Error(e.message ?: "Join failed")
            }
        }
    }

    fun leaveEvent(eventId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
             _joinState.value = ActionState.Error("You must be logged in")
            return
        }

        _joinState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.leaveEvent(eventId, userId)
                _joinState.value = ActionState.Success("Left event")
            } catch (e: Exception) {
                _joinState.value = ActionState.Error(e.message ?: "Leave failed")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        _joinState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                eventsRepository.deleteEvent(eventId)
                _joinState.value = ActionState.Success("Event deleted")
            } catch (e: Exception) {
                _joinState.value = ActionState.Error(e.message ?: "Delete failed")
            }
        }
    }
}

sealed class ActionState {
    data object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}
