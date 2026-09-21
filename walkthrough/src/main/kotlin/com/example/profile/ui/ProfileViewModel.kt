package com.example.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.profile.data.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }
    fun onAction(action: ProfileUiAction) {
        when (action) {
            is ProfileUiAction.Refresh -> loadProfile()
            is ProfileUiAction.ToggleTheme -> toggleTheme()
            is ProfileUiAction.UpdateName -> updateName(action.newName)
        }
    }
    private fun loadProfile() {
        viewModelScope.launch {
            repository.getProfile()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { profile -> _uiState.update { it.copy(profile = profile, isLoading = false) } }
        }
    }
    private fun toggleTheme() {
        viewModelScope.launch {
            val current = _uiState.value.profile ?: return@launch
            repository.updateTheme(!current.isDarkMode)
        }
    }
    private fun updateName(name: String) {
        if (name.length < 2 || name.length > 50) {
            _uiState.update { it.copy(errorMessage = "Invalid name length") }
            return
        }
        viewModelScope.launch { repository.updateName(name) }
    }
}
