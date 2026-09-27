package es.joshluq.kmsafe.feature.notifications.ui.list

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.kmsafe.core.ui.components.NotificationPermissionRationaleDialog
import java.util.UUID

/**
 * Navigation coordinator for NotificationsListScreen.
 * Uses session-scoped key to ensure fresh state upon entry, prompts for runtime notification
 * permissions on Android 13+ if ungranted, and collects side effects.
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showNotificationRationale by rememberSaveable { mutableStateOf(false) }
    var hasEvaluatedPermissionOnEntry by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled; notifications stream updates reactively
    }

    LaunchedEffect(Unit) {
        if (!hasEvaluatedPermissionOnEntry) {
            hasEvaluatedPermissionOnEntry = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!isGranted) {
                    showNotificationRationale = true
                }
            }
        }
    }

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

    if (showNotificationRationale) {
        NotificationPermissionRationaleDialog(
            onConfirm = {
                showNotificationRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showNotificationRationale = false
            }
        )
    }
}
