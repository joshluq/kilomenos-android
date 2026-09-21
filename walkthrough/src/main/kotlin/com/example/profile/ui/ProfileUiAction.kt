package com.example.profile.ui

sealed interface ProfileUiAction {
    data class UpdateName(val newName: String) : ProfileUiAction
    object ToggleTheme : ProfileUiAction
    object Refresh : ProfileUiAction
}
