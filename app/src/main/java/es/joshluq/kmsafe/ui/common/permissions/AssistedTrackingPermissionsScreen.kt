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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
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
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.R as CoreR

/**
 * Permissions onboarding screen for Assisted Copilot (Free tier manual GPS tracking).
 * Educates the user and requests exclusively the 2 essential permissions: GPS Location and Notifications.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AssistedTrackingPermissionsScreen(
    onAllPermissionsGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val notificationsPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val isFineLocationGranted = fineLocationState.status.isGranted
    val isNotificationsGranted = notificationsPermissionState?.status?.isGranted ?: true

    val isAllGranted = isFineLocationGranted && isNotificationsGranted

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
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CanvasKitTheme.colors.brandAccent
            )

            Text(
                text = stringResource(R.string.assisted_permissions_title),
                style = CanvasKitTheme.typography.headingLarge,
                color = CanvasKitTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.assisted_permissions_desc),
                style = CanvasKitTheme.typography.bodyLarge,
                color = CanvasKitTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionStepItem(
                title = stringResource(R.string.assisted_permissions_step_location_title),
                description = stringResource(R.string.assisted_permissions_step_location_desc),
                isGranted = isFineLocationGranted,
                icon = Icons.Default.LocationOn
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionStepItem(
                    title = stringResource(R.string.assisted_permissions_step_notifications_title),
                    description = stringResource(R.string.assisted_permissions_step_notifications_desc),
                    isGranted = isNotificationsGranted,
                    icon = Icons.Default.Notifications
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            CanvasKitButton(
                onClick = {
                    when {
                        !isFineLocationGranted -> fineLocationState.launchPermissionRequest()
                        notificationsPermissionState != null && !isNotificationsGranted -> {
                            notificationsPermissionState.launchPermissionRequest()
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
