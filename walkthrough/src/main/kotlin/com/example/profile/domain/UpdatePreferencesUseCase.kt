package com.example.profile.domain

import com.example.profile.data.ProfileRepository

class UpdatePreferencesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(isDarkMode: Boolean) = repository.updateTheme(isDarkMode)
}
