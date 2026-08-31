package es.joshluq.kmsafe.ui.common.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.core.ui.R as CoreR
import es.joshluq.kmsafe.core.ui.util.safeClick

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoTrackingPermissionsScreen(
    onAllPermissionsGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val activityRecognitionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACTIVITY_RECOGNITION)
    } else {
        null
    }

    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val backgroundLocationState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    val notificationsPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        null
    }

    val isActivityGranted = activityRecognitionState?.status?.isGranted ?: true
    val isBackgroundLocationGranted = backgroundLocationState?.status?.isGranted ?: true
    val isNotificationsGranted = notificationsPermissionState?.status?.isGranted ?: true
    val isBluetoothGranted = bluetoothPermissionState?.status?.isGranted ?: true
    val isFineLocationGranted = fineLocationState.status.isGranted

    val isAllGranted = isActivityGranted && isBackgroundLocationGranted && isNotificationsGranted && isFineLocationGranted && isBluetoothGranted

    LaunchedEffect(isAllGranted) {
        if (isAllGranted) {
            onAllPermissionsGranted()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CanvasKitTheme.colors.backgroundSecondary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CanvasKitTheme.colors.brandAccent
            )

            Text(
                text = stringResource(R.string.overview_permissions_required_title),
                style = CanvasKitTheme.typography.headingLarge,
                color = CanvasKitTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.overview_permissions_required_desc),
                style = CanvasKitTheme.typography.bodyLarge,
                color = CanvasKitTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionStepItem(
                title = stringResource(R.string.overview_permissions_step_activity_title),
                description = stringResource(R.string.overview_permissions_step_activity_desc),
                isGranted = isActivityGranted,
                icon = Icons.Default.DirectionsCar
            )

            PermissionStepItem(
                title = stringResource(R.string.overview_permissions_step_location_title),
                description = stringResource(R.string.overview_permissions_step_location_desc),
                isGranted = isBackgroundLocationGranted,
                icon = Icons.Default.LocationOn
            )

            PermissionStepItem(
                title = stringResource(R.string.overview_permissions_step_notifications_title),
                description = stringResource(R.string.overview_permissions_step_notifications_desc),
                isGranted = isNotificationsGranted,
                icon = Icons.Default.Notifications
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionStepItem(
                    title = stringResource(R.string.overview_permissions_step_bluetooth_title),
                    description = stringResource(R.string.overview_permissions_step_bluetooth_desc),
                    isGranted = isBluetoothGranted,
                    icon = Icons.Default.Bluetooth
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            CanvasKitButton(
                onClick = {
                    when {
                        !isActivityGranted -> activityRecognitionState.launchPermissionRequest()
                        !isFineLocationGranted -> fineLocationState.launchPermissionRequest()
                        backgroundLocationState != null && !isBackgroundLocationGranted -> {
                            backgroundLocationState.launchPermissionRequest()
                        }
                        notificationsPermissionState != null && !isNotificationsGranted -> {
                            notificationsPermissionState.launchPermissionRequest()
                        }
                        bluetoothPermissionState != null && !isBluetoothGranted -> {
                            bluetoothPermissionState.launchPermissionRequest()
                        }
                        else -> onAllPermissionsGranted()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { contentColor ->
                Text(
                    text = if (!isAllGranted) {
                        stringResource(R.string.overview_permissions_button_continue)
                    } else {
                        stringResource(CoreR.string.history_close_button)
                    },
                    color = contentColor
                )
            }

            CanvasKitButton(
                onClick = safeClick { onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                variant = CanvasKitButtonVariant.Ghost
            ) {
                Text(
                    text = stringResource(CoreR.string.history_close_button),
                    color = CanvasKitTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionStepItem(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) CanvasKitTheme.colors.success else CanvasKitTheme.colors.brandAccent,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                Text(
                    text = description,
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.success
                )
            }
        }
    }
}
