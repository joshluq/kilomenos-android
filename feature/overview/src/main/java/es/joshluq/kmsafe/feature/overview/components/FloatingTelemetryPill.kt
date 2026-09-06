package es.joshluq.kmsafe.feature.overview.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.kmsafe.feature.overview.R
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.safeClickable

/**
 * Floating Telemetry Pill (Dynamic Island pattern).
 * Displays a live non-intrusive floating indicator during trip recording,
 * allowing instant stop and confirmation without blocking the rest of the screen.
 */
@Composable
fun FloatingTelemetryPill(
    isVisible: Boolean,
    distanceMeters: Double,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val locale = LocalLocale.current.platformLocale

    val infiniteTransition = rememberInfiniteTransition(label = "RecPulseTransition")
    val dotColor by infiniteTransition.animateColor(
        initialValue = CanvasKitTheme.colors.error,
        targetValue = CanvasKitTheme.colors.error.copy(alpha = 0.2f),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RecDotColor"
    )

    val distanceKm = distanceMeters / 1000.0

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xFF1E1E24),
            contentColor = Color.White,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .border(1.dp, CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.4f), CircleShape)
                .testTag("floating_telemetry_pill")
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pulsing recording dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    Column {
                        Text(
                            text = stringResource(R.string.overview_pill_recording_trip),
                            style = CanvasKitTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stringResource(
                                R.string.overview_pill_distance_in_progress,
                                String.format(locale, "%.2f", distanceKm)
                            ),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stop action button inside the pill
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(CanvasKitTheme.colors.error)
                        .safeClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopClick()
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("pill_stop_trip_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.overview_pill_finish_desc),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.overview_pill_finish_button),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "FloatingTelemetryPill - Recording")
@Preview(name = "FloatingTelemetryPill - Recording Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFloatingTelemetryPill() {
    CanvasKitTheme {
        FloatingTelemetryPill(
            isVisible = true,
            distanceMeters = 8450.0,
            onStopClick = {}
        )
    }
}
