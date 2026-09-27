package es.joshluq.kmsafe.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.R

/**
 * Educational rationale dialog displayed before requesting the runtime POST_NOTIFICATIONS permission
 * on Android 13+ (API 33+).
 *
 * Implements Zero-Friction and Loss-Aversion principles:
 * Explains how notifications safeguard the driver against mileage penalties and preserve
 * synchronized contract entitlements.
 *
 * @param onConfirm Invoked when the driver proceeds to grant the system permission.
 * @param onDismiss Invoked when the driver postpones or cancels the dialog.
 * @param modifier Composable modifier for layout adjustments.
 */
@Composable
fun NotificationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accDialogDesc = stringResource(R.string.notification_permission_dialog_acc_description)
    val accIconDesc = stringResource(R.string.notification_permission_dialog_icon_description)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CanvasKitCard(
                variant = CanvasKitCardVariant.Elevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = accDialogDesc
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = accIconDesc,
                            tint = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = stringResource(R.string.notification_permission_dialog_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtitle / Psychological Rationale
                    Text(
                        text = stringResource(R.string.notification_permission_dialog_message),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feature highlights
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CanvasKitTheme.colors.backgroundSecondary)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.notification_permission_dialog_feature),
                            style = CanvasKitTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = CanvasKitTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons (ensuring >= 48dp touch targets)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasKitButton(
                            text = stringResource(R.string.notification_permission_dialog_confirm),
                            onClick = onConfirm,
                            variant = CanvasKitButtonVariant.Primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )

                        CanvasKitButton(
                            text = stringResource(R.string.notification_permission_dialog_dismiss),
                            onClick = onDismiss,
                            variant = CanvasKitButtonVariant.Ghost,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionRationaleDialogPreview() {
    CanvasKitTheme {
        NotificationPermissionRationaleDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
