package com.example.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier

@Composable
fun ProfileScreen(state: ProfileUiState, onAction: (ProfileUiAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(text = state.profile?.name ?: "Unknown")
            Button(onClick = { onAction(ProfileUiAction.ToggleTheme) }) {
                Text("Toggle Theme")
            }
        }
    }
}
