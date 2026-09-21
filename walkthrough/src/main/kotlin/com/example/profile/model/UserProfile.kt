package com.example.profile.model

data class UserProfile(
    val id: String,
    val name: String,
    val isDarkMode: Boolean = false
)
