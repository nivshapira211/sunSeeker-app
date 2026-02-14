package com.example.sunseeker_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.model.Event
import com.example.sunseeker_app.data.remote.AuthRepository
import com.example.sunseeker_app.data.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _profileState = MutableLiveData<UiState>()
    val profileState: LiveData<UiState> = _profileState

    private val _joinState = MutableLiveData<UiState?>()
    val joinState: LiveData<UiState?> = _joinState

    val myEvents: LiveData<List<Event>> = eventsRepository.getEvents().map { events ->
        val uid = authRepository.getCurrentUserId()
        if (uid == null) emptyList() else events.filter { it.creatorId == uid }
    }

    private val _profileUi = MutableLiveData<ProfileUi>()
    val profileUi: LiveData<ProfileUi> = _profileUi

    fun refreshProfile() {
        _profileUi.value = ProfileUi(
            name = authRepository.getCurrentDisplayName() ?: "SunSeeker User",
            photoUrl = authRepository.getCurrentPhotoUrl()
        )
    }

    fun logout() {
        authRepository.logout()
    }

    fun joinEvent(eventId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
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
        val userId = authRepository.getCurrentUserId() ?: return
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

    fun updateProfile(displayName: String, imageUri: Uri?) {
        if (!authRepository.isLoggedIn()) {
            _profileState.value = UiState.Error("Not logged in")
            return
        }

        _profileState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.updateProfile(displayName, imageUri)
                refreshProfile()
                _profileState.value = UiState.Success("Profile updated")
            } catch (e: Exception) {
                _profileState.value = UiState.Error(e.message ?: "Update failed")
            }
        }
    }
}

data class ProfileUi(
    val name: String,
    val photoUrl: String?
)
