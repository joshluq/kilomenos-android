package es.joshluq.kmsafe.feature.overview.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.util.safeClickable
import es.joshluq.kmsafe.feature.overview.model.CapsulePriority
import es.joshluq.kmsafe.feature.overview.model.CapsuleVariant
import es.joshluq.kmsafe.feature.overview.model.StatusCapsuleUiModel

/**
 * Unified status capsule component (Cápsula Unificada Monocanal).
 * Displays prioritized notifications, risks, and insights in a single 40dp interactive container
 * preventing multi-banner stacking and layout shifts.
 */
@Composable
fun StatusCapsule(
    item: StatusCapsuleUiModel?,
    onClick: (StatusCapsuleUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    val (backgroundColor, borderColor, contentColor, icon) = when (item.variant) {
        CapsuleVariant.WARNING -> CapsuleColors(
            bg = CanvasKitTheme.colors.error.copy(alpha = 0.08f),
            border = CanvasKitTheme.colors.error.copy(alpha = 0.35f),
            content = CanvasKitTheme.colors.error,
            icon = Icons.Default.Warning
        )
        CapsuleVariant.INFO -> CapsuleColors(
            bg = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.08f),
            border = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.35f),
            content = CanvasKitTheme.colors.brandAccent,
            icon = if (item.priority == CapsulePriority.BLUETOOTH_SETUP) Icons.Default.Bluetooth else Icons.Default.Info
        )
        CapsuleVariant.SUCCESS -> CapsuleColors(
            bg = CanvasKitTheme.colors.success.copy(alpha = 0.08f),
            border = CanvasKitTheme.colors.success.copy(alpha = 0.35f),
            content = CanvasKitTheme.colors.success,
            icon = Icons.Default.Info
        )
        CapsuleVariant.NEUTRAL -> CapsuleColors(
            bg = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.3f),
            border = CanvasKitTheme.colors.borderSubtle,
            content = CanvasKitTheme.colors.textPrimary,
            icon = Icons.Default.Lightbulb
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(width = 1.dp, color = borderColor, shape = CircleShape)
                .safeClickable { onClick(item) }
                .padding(horizontal = 14.dp)
                .testTag("status_capsule"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedContent(
                    targetState = item.message.asString(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "CapsuleMessageAnimation"
                ) { messageText ->
                    Text(
                        text = messageText,
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private data class CapsuleColors(
    val bg: Color,
    val border: Color,
    val content: Color,
    val icon: ImageVector
)

@Preview(name = "StatusCapsule - Warning Risk", showBackground = true)
@Preview(name = "StatusCapsule - Warning Risk Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewStatusCapsuleWarning() {
    CanvasKitTheme {
        StatusCapsule(
            item = StatusCapsuleUiModel.CriticalRisk(
                message = TextProvider.Dynamic("⚠️ Risk Sentinel: +1.200 km proyectados al vencimiento"),
                isOverLimit = true
            ),
            onClick = {}
        )
    }
}

@Preview(name = "StatusCapsule - Bluetooth Missing", showBackground = true)
@Composable
fun PreviewStatusCapsuleBluetooth() {
    CanvasKitTheme {
        StatusCapsule(
            item = StatusCapsuleUiModel.BluetoothMissing(
                message = TextProvider.Dynamic("Enlaza tu coche para activar SmartCopilot automático")
            ),
            onClick = {}
        )
    }
}
