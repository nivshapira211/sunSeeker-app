package com.example.sunseeker_app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.model.Event
import com.example.sunseeker_app.data.remote.AuthRepository
import com.example.sunseeker_app.data.repository.EventsRepository
import com.example.sunseeker_app.ui.view.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val allEvents: LiveData<List<Event>> = eventsRepository.getEvents()

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state

    private val _joinState = MutableLiveData<UiState?>()
    val joinState: LiveData<UiState?> = _joinState

    private val _pastExpanded = MutableLiveData(false)

    private val timeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

    private val _upcomingExpanded = MutableLiveData(true)

    /** Combined feed list: section headers + (optionally) their events. */
    val feedItems: LiveData<List<FeedItem>> = MediatorLiveData<List<FeedItem>>().apply {
        fun rebuild() {
            value = buildFeedItems(
                allEvents.value.orEmpty(),
                _upcomingExpanded.value ?: true,
                _pastExpanded.value ?: false
            )
        }
        addSource(allEvents) { rebuild() }
        addSource(_upcomingExpanded) { rebuild() }
        addSource(_pastExpanded) { rebuild() }
    }

    init {
        refresh()
    }

    fun toggleUpcomingSection() {
        _upcomingExpanded.value = !(_upcomingExpanded.value ?: true)
    }

    fun togglePastSection() {
        _pastExpanded.value = !(_pastExpanded.value ?: false)
    }

    private fun buildFeedItems(
        events: List<Event>,
        upcomingExpanded: Boolean,
        pastExpanded: Boolean
    ): List<FeedItem> {
        val now = Date()
        val upcoming = mutableListOf<Event>()
        val past = mutableListOf<Event>()

        for (event in events) {
            val eventTime = try { timeFormat.parse(event.time) } catch (_: Exception) { null }
            if (eventTime != null && eventTime.before(now)) {
                past.add(event)
            } else {
                upcoming.add(event)
            }
        }

        val items = mutableListOf<FeedItem>()

        if (upcoming.isNotEmpty()) {
            items.add(FeedItem.SectionHeader(
                title = "Upcoming Events",
                count = upcoming.size,
                isExpanded = upcomingExpanded
            ))
            if (upcomingExpanded) {
                items.addAll(upcoming.map { FeedItem.EventItem(it) })
            }
        }

        if (past.isNotEmpty()) {
            items.add(FeedItem.SectionHeader(
                title = "Past Events",
                count = past.size,
                isExpanded = pastExpanded
            ))
            if (pastExpanded) {
                items.addAll(past.map { FeedItem.EventItem(it, isPast = true) })
            }
        }

        return items
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
}

sealed class FeedState {
    data object Loading : FeedState()
    data object Idle : FeedState()
    data class Error(val message: String) : FeedState()
}
