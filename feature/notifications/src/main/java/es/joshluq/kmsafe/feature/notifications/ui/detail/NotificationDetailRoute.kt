package es.joshluq.kmsafe.feature.notifications.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Navigation coordinator for [NotificationDetailScreen].
 * Applies deterministic entity scoping key: notification_detail_$notificationId.
 */
@Composable
fun NotificationDetailRoute(
    notificationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDeepLink: (String) -> Unit,
    viewModel: NotificationDetailViewModel = hiltViewModel(
        key = "notification_detail_$notificationId"
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(notificationId) {
        viewModel.loadNotification(notificationId)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationDetailEffect.NavigateBack -> onNavigateBack()
                is NotificationDetailEffect.NavigateToDeepLink -> onNavigateToDeepLink(effect.deepLinkUri)
            }
        }
    }

    NotificationDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}
