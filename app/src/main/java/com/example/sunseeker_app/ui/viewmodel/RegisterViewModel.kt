package com.example.sunseeker_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunseeker_app.data.remote.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData<RegisterState>()
    val state: LiveData<RegisterState> = _state

    fun register(email: String, password: String, displayName: String, photoUri: Uri?) {
        _state.value = RegisterState.Loading
        viewModelScope.launch {
            val result = authRepository.register(email.trim(), password)
            if (result.isSuccess) {
                try {
                    authRepository.updateProfile(displayName.trim(), photoUri)
                    _state.value = RegisterState.Success
                } catch (e: Exception) {
                    // Registration succeeded but profile update failed — still navigate
                    _state.value = RegisterState.Success
                }
            } else {
                _state.value = RegisterState.Error(result.errorMessage ?: "Registration failed")
            }
        }
    }
}

sealed class RegisterState {
    data object Loading : RegisterState()
    data object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}
