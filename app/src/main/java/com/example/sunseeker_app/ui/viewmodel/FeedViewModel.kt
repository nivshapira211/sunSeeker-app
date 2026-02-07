package com.example.sunseeker_app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.data.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val eventsRepository: EventsRepository
) : ViewModel() {
    val events: LiveData<List<EventEntity>> = eventsRepository.getEvents()

    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state

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
}

sealed class FeedState {
    data object Loading : FeedState()
    data object Idle : FeedState()
    data class Error(val message: String) : FeedState()
}
