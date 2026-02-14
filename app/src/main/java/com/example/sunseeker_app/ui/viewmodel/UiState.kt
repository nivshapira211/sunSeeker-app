package com.example.sunseeker_app.ui.viewmodel

/**
 * Shared sealed class for common UI action states (join, leave, delete, etc.).
 * Replaces the duplicated FeedJoinState, ProfileJoinState, and ActionState classes.
 */
sealed class UiState {
    data object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
