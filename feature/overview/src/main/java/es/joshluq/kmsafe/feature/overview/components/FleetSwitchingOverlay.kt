package es.joshluq.kmsafe.feature.overview.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.feature.overview.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fullscreen HUD overlay displayed during active vehicle switching.
 *
 * Blocks all underlying user interactions and navigation tabs while local and remote data
 * synchronizes, preventing incomplete data states, room emission storms, and cross-tab race conditions.
 *
 * @param isVisible Flag indicating if the switching overlay is active.
 * @param vehicleName The display name of the target vehicle being switched to.
 * @param modifier Modifier applied to the outer container.
 */
@Composable
fun FleetSwitchingOverlay(
    isVisible: Boolean,
    vehicleName: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        // Intercept hardware and gesture back button to prevent navigation mid-switch
        BackHandler(enabled = true) {}

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasKitTheme.colors.backgroundPrimary.copy(alpha = 0.75f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {} // Consume all touches to protect screen underneath
                .testTag("fleet_switching_overlay"),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
            val shimmerTranslate by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_progress"
            )

            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(initialScale = 0.9f, animationSpec = tween(250)),
                exit = scaleOut(targetScale = 0.95f, animationSpec = tween(200))
            ) {
                CanvasKitCard(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth(),
                    variant = CanvasKitCardVariant.Elevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Vehicle HUD animation with radar waves, speed lines and orbiting satellite
                        VehicleSwitchingAnimation(size = 84.dp)

                        Spacer(modifier = Modifier.height(4.dp))

                        // Context Subtitle
                        Text(
                            text = stringResource(R.string.overview_switching_vehicle_subtitle),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Target Vehicle Name
                        Text(
                            text = vehicleName ?: stringResource(R.string.overview_switching_vehicle_default),
                            style = CanvasKitTheme.typography.headingMedium,
                            color = CanvasKitTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        // Description
                        Text(
                            text = stringResource(R.string.overview_switching_vehicle_description),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Animated Shimmer Progress Bar
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                CanvasKitTheme.colors.brandAccent.copy(alpha = 0.3f),
                                CanvasKitTheme.colors.brandAccent,
                                CanvasKitTheme.colors.brandAccent.copy(alpha = 0.3f)
                            ),
                            startX = shimmerTranslate * 300f,
                            endX = (shimmerTranslate * 300f) + 300f
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Executive automotive switching animation featuring expanding telemetry radar waves,
 * horizontal velocity lines, a rotating satellite dot, and subtle vehicle suspension motion.
 */
@Composable
private fun VehicleSwitchingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    accentColor: Color = CanvasKitTheme.colors.brandAccent,
    surfaceColor: Color = CanvasKitTheme.colors.backgroundSecondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vehicle_switching_anim")

    // Orbiting satellite dot angle (360° continuous loop)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    // Expanding telemetry radar waves
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    // Horizontal speed lines progress
    val speedLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speed_lines"
    )

    // Suspension bounce
    val carYOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "suspension_bounce"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.width / 2f

            // 1. Telemetry Radar Waves (Expanding concentric rings)
            val ring1Radius = maxRadius * pulseProgress
            val ring1Alpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.45f
            drawCircle(
                color = accentColor.copy(alpha = ring1Alpha),
                radius = ring1Radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            val phase2Progress = (pulseProgress + 0.5f) % 1f
            val ring2Radius = maxRadius * phase2Progress
            val ring2Alpha = (1f - phase2Progress).coerceIn(0f, 1f) * 0.45f
            drawCircle(
                color = accentColor.copy(alpha = ring2Alpha),
                radius = ring2Radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Horizontal Velocity Lines
            val lineYPositions = listOf(center.y - 14.dp.toPx(), center.y + 14.dp.toPx())
            lineYPositions.forEachIndexed { index, yPos ->
                val directionMultiplier = if (index % 2 == 0) -1f else 1f
                val startX = (center.x - maxRadius * 0.65f) + (speedLineProgress * maxRadius * 1.3f * directionMultiplier)
                val lineLength = 16.dp.toPx()
                drawLine(
                    color = accentColor.copy(alpha = 0.25f),
                    start = Offset(startX, yPos),
                    end = Offset(startX + lineLength, yPos),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Orbiting Telemetry Dot
            val orbitRadius = maxRadius * 0.72f
            val angleRad = Math.toRadians(rotation.toDouble())
            val dotX = center.x + orbitRadius * cos(angleRad).toFloat()
            val dotY = center.y + orbitRadius * sin(angleRad).toFloat()

            drawCircle(
                color = accentColor,
                radius = 3.5.dp.toPx(),
                center = Offset(dotX, dotY)
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = 7.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }

        // Central Vehicle Badge
        Box(
            modifier = Modifier
                .size(size * 0.55f)
                .offset { IntOffset(0, carYOffset.dp.roundToPx()) }
                .clip(CircleShape)
                .background(surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(size * 0.32f)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FleetSwitchingOverlayPreview() {
    CanvasKitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasKitTheme.colors.backgroundPrimary)
        ) {
            FleetSwitchingOverlay(
                isVisible = true,
                vehicleName = "Peugeot 3008 GT"
            )
        }
    }
}
