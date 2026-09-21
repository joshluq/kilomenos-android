package com.example.profile.domain

import com.example.profile.data.ProfileRepository
import com.example.profile.model.UserProfile
import kotlinx.coroutines.flow.Flow

class GetProfileUseCase(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<UserProfile> = repository.getProfile()
}
