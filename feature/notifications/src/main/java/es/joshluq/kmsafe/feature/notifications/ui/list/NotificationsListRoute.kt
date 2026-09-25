package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/**
 * Navigation coordinator for NotificationsListScreen.
 * Uses session-scoped key to ensure fresh state upon entry and collects side effects.
 */
@Composable
fun NotificationsListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToDeepLink: (String) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToProjection: () -> Unit = {},
    onNavigateToEditContract: (String) -> Unit = {},
    sessionId: String = rememberSaveable { UUID.randomUUID().toString() },
    viewModel: NotificationsListViewModel = hiltViewModel(key = sessionId)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationsListEffect.NavigateBack -> onNavigateBack()
                is NotificationsListEffect.NavigateToDetail -> onNavigateToDetail(effect.notificationId)
                is NotificationsListEffect.NavigateToDeepLink -> onNavigateToDeepLink(effect.deepLinkUri)
                NotificationsListEffect.NavigateToPaywall -> onNavigateToPaywall()
                NotificationsListEffect.NavigateToProjection -> onNavigateToProjection()
                is NotificationsListEffect.NavigateToEditContract -> onNavigateToEditContract(effect.vehicleId)
            }
        }
    }

    NotificationsListScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}
