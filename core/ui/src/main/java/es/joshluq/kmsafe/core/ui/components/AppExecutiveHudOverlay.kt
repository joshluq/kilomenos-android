package es.joshluq.kmsafe.core.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.R
import es.joshluq.kmsafe.domain.model.AppOverlayState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal executive HUD overlay displayed during high-stakes or long-running operations.
 *
 * Implements the Single-Window Architecture: renders at root level covering the Scaffold
 * and Navigation Bar, avoiding Dialog window creation (zero status/navigation bar flicker).
 *
 * @param state The current [AppOverlayState] to render.
 * @param modifier Modifier applied to the outer container.
 */
@Composable
fun AppExecutiveHudOverlay(
    state: AppOverlayState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state !is AppOverlayState.None,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250))
    ) {
        BackHandler(enabled = true) {
            // Prevent back gestures during critical operations
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CanvasKitTheme.colors.backgroundPrimary.copy(alpha = 0.82f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {} // Consume all touches to lock navigation and interaction
                .testTag("app_executive_hud_overlay"),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "hud_shimmer")
            val shimmerTranslate by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_progress"
            )

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
                    when (state) {
                        is AppOverlayState.VehicleSwitching -> {
                            VehicleSwitchingAnimation(size = 84.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.fleet_switching_vehicle_subtitle,
                                    state.vehicleName ?: stringResource(R.string.fleet_switching_vehicle_default)
                                ),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.brandAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = state.vehicleName ?: stringResource(R.string.fleet_switching_vehicle_default),
                                style = CanvasKitTheme.typography.headingMedium,
                                color = CanvasKitTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.fleet_switching_vehicle_description),
                                style = CanvasKitTheme.typography.bodyMedium,
                                color = CanvasKitTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        AppOverlayState.LoggingOut -> {
                            SecurityLockAnimation(size = 84.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.hud_logout_title),
                                style = CanvasKitTheme.typography.headingMedium,
                                color = CanvasKitTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.hud_logout_subtitle),
                                style = CanvasKitTheme.typography.bodyMedium,
                                color = CanvasKitTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        is AppOverlayState.AccountDeletion -> {
                            AccountDeletionAnimation(size = 84.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.hud_account_deletion_title),
                                style = CanvasKitTheme.typography.headingMedium,
                                color = CanvasKitTheme.colors.error,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            AnimatedContent(
                                targetState = state.stepMessage,
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                label = "gdpr_message_transition"
                            ) { message ->
                                Text(
                                    text = message?.asString() ?: "",
                                    style = CanvasKitTheme.typography.bodyMedium,
                                    color = CanvasKitTheme.colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is AppOverlayState.AiReceiptScanning -> {
                            ReceiptScannerAnimation(size = 84.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.hud_ocr_scanning_title),
                                style = CanvasKitTheme.typography.headingMedium,
                                color = CanvasKitTheme.colors.brandAccent,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = state.stepMessage?.asString() ?: stringResource(R.string.hud_ocr_scanning_subtitle),
                                style = CanvasKitTheme.typography.bodyMedium,
                                color = CanvasKitTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        AppOverlayState.None -> Unit
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar or shimmer
                    val deletionProgress = (state as? AppOverlayState.AccountDeletion)?.progress
                    if (deletionProgress != null) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = deletionProgress,
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            label = "gdpr_progress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CanvasKitTheme.colors.error,
                            trackColor = CanvasKitTheme.colors.error.copy(alpha = 0.2f)
                        )
                    } else {
                        val accentColor = if (state is AppOverlayState.AccountDeletion) {
                            CanvasKitTheme.colors.error
                        } else {
                            CanvasKitTheme.colors.brandAccent
                        }
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.3f),
                                accentColor,
                                accentColor.copy(alpha = 0.3f)
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
 * Executive automotive switching animation featuring telemetry radar rings,
 * velocity lines, orbiting satellite dot, and subtle vehicle suspension motion.
 */
@Composable
private fun VehicleSwitchingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    accentColor: Color = CanvasKitTheme.colors.brandAccent,
    surfaceColor: Color = CanvasKitTheme.colors.backgroundSecondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vehicle_switching_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    val speedLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speed_lines"
    )

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

            val lineYPositions = listOf(center.y - 14.dp.toPx(), center.y + 14.dp.toPx())
            lineYPositions.forEachIndexed { index, yPos ->
                val directionMultiplier = if (index % 2 == 0) -1f else 1f
                val startX =
                    (center.x - maxRadius * 0.65f) + (speedLineProgress * maxRadius * 1.3f * directionMultiplier)
                val lineLength = 16.dp.toPx()
                drawLine(
                    color = accentColor.copy(alpha = 0.25f),
                    start = Offset(startX, yPos),
                    end = Offset(startX + lineLength, yPos),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val orbitRadius = maxRadius * 0.72f
            val angleRad = Math.toRadians(rotation.toDouble())
            val dotX = center.x + orbitRadius * cos(angleRad).toFloat()
            val dotY = center.y + orbitRadius * sin(angleRad).toFloat()

            drawCircle(
                color = accentColor,
                radius = 3.5.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }

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

/**
 * Security lock animation with radiating protection shields for logout.
 */
@Composable
private fun SecurityLockAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    accentColor: Color = CanvasKitTheme.colors.brandAccent,
    surfaceColor: Color = CanvasKitTheme.colors.backgroundSecondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "security_lock_anim")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.width / 2f

            val ringRadius = maxRadius * pulseProgress
            val ringAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.40f
            drawCircle(
                color = accentColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.55f)
                .clip(CircleShape)
                .background(surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(size * 0.32f)
            )
        }
    }
}

/**
 * GDPR Account deletion animation with pulsating destructive shield.
 */
@Composable
private fun AccountDeletionAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    errorColor: Color = CanvasKitTheme.colors.error,
    surfaceColor: Color = CanvasKitTheme.colors.backgroundSecondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "deletion_anim")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "deletion_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.width / 2f

            val ringRadius = maxRadius * pulseProgress
            val ringAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.50f
            drawCircle(
                color = errorColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.55f)
                .clip(CircleShape)
                .background(surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = errorColor,
                modifier = Modifier.size(size * 0.32f)
            )
        }
    }
}

/**
 * Holographic AI Receipt Scanner animation with a vertical laser scanning beam
 * moving continuously over a stylized receipt card.
 */
@Composable
private fun ReceiptScannerAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    accentColor: Color = CanvasKitTheme.colors.brandAccent,
    surfaceColor: Color = CanvasKitTheme.colors.backgroundSecondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scanner_anim")

    val scanYProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_beam_y"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cardWidth = this.size.width * 0.62f
            val cardHeight = this.size.height * 0.78f
            val cardLeft = (this.size.width - cardWidth) / 2f
            val cardTop = (this.size.height - cardHeight) / 2f

            // 1. Stylized receipt card backdrop
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
                color = accentColor.copy(alpha = 0.35f),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Receipt lines
            val lineMargin = 8.dp.toPx()
            val startX = cardLeft + lineMargin
            val endX = cardLeft + cardWidth - lineMargin
            val lineY1 = cardTop + 12.dp.toPx()
            val lineY2 = cardTop + 20.dp.toPx()
            val lineY3 = cardTop + 28.dp.toPx()
            val lineY4 = cardTop + 36.dp.toPx()

            listOf(lineY1, lineY2, lineY3, lineY4).forEachIndexed { i, y ->
                val length = if (i == 0) (endX - startX) * 0.5f else (endX - startX) * 0.8f
                drawLine(
                    color = accentColor.copy(alpha = 0.20f),
                    start = Offset(startX, y),
                    end = Offset(startX + length, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Laser scanning beam
            val laserY = cardTop + cardHeight * scanYProgress
            val laserStartX = cardLeft - 4.dp.toPx()
            val laserEndX = cardLeft + cardWidth + 4.dp.toPx()

            // Laser glow
            drawLine(
                color = accentColor.copy(alpha = 0.35f),
                start = Offset(laserStartX, laserY),
                end = Offset(laserEndX, laserY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Laser core
            drawLine(
                color = accentColor,
                start = Offset(laserStartX, laserY),
                end = Offset(laserEndX, laserY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.85f),
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
        )
    }
}

@Preview(name = "Vehicle Switching", showBackground = true)
@Composable
private fun PreviewVehicleSwitching() {
    CanvasKitTheme {
        AppExecutiveHudOverlay(
            state = AppOverlayState.VehicleSwitching("Tesla Model 3")
        )
    }
}

@Preview(name = "AI OCR Scanning", showBackground = true)
@Composable
private fun PreviewAiScanning() {
    CanvasKitTheme {
        AppExecutiveHudOverlay(
            state = AppOverlayState.AiReceiptScanning()
        )
    }
}

@Preview(name = "GDPR Deletion", showBackground = true)
@Composable
private fun PreviewAccountDeletion() {
    CanvasKitTheme {
        AppExecutiveHudOverlay(
            state = AppOverlayState.AccountDeletion(
                stepMessage = TextProvider.Dynamic("Revocando credenciales y eliminando copia en la nube…"),
                progress = 0.5f
            )
        )
    }
}
