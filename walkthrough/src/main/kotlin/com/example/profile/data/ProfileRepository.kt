package com.example.profile.data

import com.example.profile.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(): Flow<UserProfile>
    suspend fun updateTheme(isDarkMode: Boolean)
    suspend fun updateName(name: String)
}
