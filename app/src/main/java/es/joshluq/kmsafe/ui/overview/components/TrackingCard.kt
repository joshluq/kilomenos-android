package es.joshluq.kmsafe.ui.overview.components

import android.content.res.Configuration
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.util.safeClick

@Composable
fun TrackingCard(
    isTracking: Boolean,
    distanceMeters: Double,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val dotColor by infiniteTransition.animateColor(
        initialValue = CanvasKitTheme.colors.error,
        targetValue = CanvasKitTheme.colors.error.copy(alpha = 0.2f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotColor"
    )

    CanvasKitCard(
        modifier = modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated,
        header = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = if (isTracking) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTracking) {
                            stringResource(R.string.tracking_card_active_mode)
                        } else {
                            stringResource(R.string.tracking_card_idle_mode)
                        },
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                }
                
                if (isTracking) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            val kms = distanceMeters / 1000.0
            Column(
                modifier = Modifier.weight(.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "%.2f".format(kms),
                    style = CanvasKitTheme.typography.displayMedium.copy(fontSize = 48.sp),
                    color = CanvasKitTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.tracking_card_kms_label),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
            Column (
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3-State Action Logic
                when {
                    // State 2: Tracking Active
                    isTracking -> {
                        CanvasKitButton(
                            variant = CanvasKitButtonVariant.Secondary,
                            onClick = safeClick { onStop() },
                            modifier = Modifier.fillMaxWidth()
                        ) { contentColor ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = contentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.tracking_card_stop_action), color = contentColor)
                            }
                        }
                    }
                    // State 3: Trip Completed (Waiting for confirmation or cancellation)
                    distanceMeters > 0 -> {

                            CanvasKitButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = safeClick { onConfirm() },
                            ) { contentColor ->
                                Text(stringResource(R.string.tracking_card_save_action), color = contentColor)
                            }
                            CanvasKitButton(
                                modifier = Modifier.fillMaxWidth(),
                                variant = CanvasKitButtonVariant.Ghost,
                                onClick = safeClick { onCancel() },
                            ) { contentColor ->
                                Text(stringResource(R.string.tracking_card_cancel_action), color = contentColor)
                            }

                    }
                    // State 1: Initial (Ready to start)
                    else -> {
                        CanvasKitButton(
                            onClick = safeClick { onStart() },
                            modifier = Modifier.fillMaxWidth()
                        ) { contentColor ->
                            Text(stringResource(R.string.tracking_card_start_action), color = contentColor)
                        }
                    }
                }
            }

        }
    }
}

@Preview(name = "State 1: Initial")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Initial - Dark")
@Composable
fun PreviewTrackingCardInitial() {
    CanvasKitTheme {
        TrackingCard(
            isTracking = false,
            distanceMeters = 0.0,
            onStart = {},
            onStop = {},
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(name = "State 2: Tracking")
@Composable
fun PreviewTrackingCardActive() {
    CanvasKitTheme {
        TrackingCard(
            isTracking = true,
            distanceMeters = 1250.0,
            onStart = {},
            onStop = {},
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(name = "State 3: Confirmation")
@Composable
fun PreviewTrackingCardConfirmation() {
    CanvasKitTheme {
        TrackingCard(
            isTracking = false,
            distanceMeters = 900080.0,
            onStart = {},
            onStop = {},
            onConfirm = {},
            onCancel = {}
        )
    }
}
