package es.joshluq.kmsafe.feature.fleet.setup

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.components.CanvasKitIcon
import es.joshluq.kmsafe.core.ui.icons.CanvasKitIcons
import es.joshluq.kmsafe.feature.fleet.R
import kotlinx.coroutines.launch

/**
 * Onboarding walkthrough guide for newly registered users.
 * Explains the 3-step core journey: (1) Contract setup, (2) Odometer logging, and (3) Live balance status.
 */
@Composable
fun WelcomeDiscoveryScreen(
    isGuideMode: Boolean = false,
    onNavigateToRentingSetup: () -> Unit,
    onSkip: () -> Unit,
    onFinishGuide: () -> Unit = onSkip
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DiscoverySlide(
                        stepText = stringResource(R.string.welcome_discovery_slide1_step),
                        title = stringResource(R.string.welcome_discovery_slide1_title),
                        description = stringResource(R.string.welcome_discovery_slide1_desc),
                        illustration = { ContractSetupStepIllustration() }
                    )
                    1 -> DiscoverySlide(
                        stepText = stringResource(R.string.welcome_discovery_slide2_step),
                        title = stringResource(R.string.welcome_discovery_slide2_title),
                        description = stringResource(R.string.welcome_discovery_slide2_desc),
                        illustration = { OdometerRecordStepIllustration() }
                    )
                    2 -> DiscoverySlide(
                        stepText = stringResource(R.string.welcome_discovery_slide3_step),
                        title = stringResource(R.string.welcome_discovery_slide3_title),
                        description = stringResource(R.string.welcome_discovery_slide3_desc),
                        illustration = { BalanceTrafficLightStepIllustration() }
                    )
                }
            }

            // Top Skip Action
            CanvasKitButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(CanvasKitTheme.spacing.md),
                variant = CanvasKitButtonVariant.Ghost,
                onClick = onSkip
            ) { contentColor ->
                Text(
                    text = stringResource(R.string.welcome_discovery_action_skip),
                    style = CanvasKitTheme.typography.labelLarge,
                    color = contentColor
                )
            }

            // Bottom Navigation & Actions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(CanvasKitTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dynamic Pill Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.lg)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "pill_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        CanvasKitTheme.colors.brandAccent
                                    } else {
                                        CanvasKitTheme.colors.textSecondary.copy(alpha = 0.25f)
                                    }
                                )
                        )
                    }
                }

                if (pagerState.currentPage < 2) {
                    CanvasKitButton(
                        text = stringResource(R.string.welcome_discovery_action_next),
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CanvasKitButton(
                        text = stringResource(
                            if (isGuideMode) R.string.welcome_discovery_action_understood
                            else R.string.welcome_discovery_action_start
                        ),
                        onClick = if (isGuideMode) onFinishGuide else onNavigateToRentingSetup,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverySlide(
    stepText: String,
    title: String,
    description: String,
    illustration: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CanvasKitTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        illustration()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stepText,
            style = CanvasKitTheme.typography.labelSmall,
            color = CanvasKitTheme.colors.brandAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = CanvasKitTheme.typography.headingMedium,
            color = CanvasKitTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = CanvasKitTheme.typography.bodyMedium,
            color = CanvasKitTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = CanvasKitTheme.spacing.sm)
        )
        Spacer(modifier = Modifier.height(44.dp))
    }
}

/**
 * Step 1: Contract setup illustration showcasing key lease terms and daily base budget.
 */
@Composable
private fun ContractSetupStepIllustration() {
    val brandAccent = CanvasKitTheme.colors.brandAccent
    val borderSubtle = CanvasKitTheme.colors.borderSubtle
    val bgSecondary = CanvasKitTheme.colors.backgroundSecondary
    val bgPrimary = CanvasKitTheme.colors.backgroundPrimary
    val textPrimary = CanvasKitTheme.colors.textPrimary
    val textSecondary = CanvasKitTheme.colors.textSecondary
    val success = CanvasKitTheme.colors.success

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgSecondary)
            .border(1.dp, borderSubtle, RoundedCornerShape(20.dp))
    ) {
        // Decorative ambient glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(brandAccent.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = size.width * 0.45f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Vehicle Blueprint Identity + Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brandAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CanvasKitIcon(
                            imageVector = CanvasKitIcons.Navigation.AuditLog,
                            contentDescription = null,
                            tint = brandAccent,
                            size = 16.dp
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.welcome_discovery_title),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Contrato de Renting",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Active status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(success.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(success)
                        )
                        Text(
                            text = stringResource(R.string.vehicles_selected_tag),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Middle Section: Visual Runway Pacing Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HORIZONTE KM",
                        style = CanvasKitTheme.typography.labelSmall,
                        color = textSecondary,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "60.000 km pactados",
                        style = CanvasKitTheme.typography.labelSmall,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Aero Runway Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(bgPrimary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(brandAccent.copy(alpha = 0.6f), brandAccent)
                                )
                            )
                    )
                }
            }

            // Bottom Section: Two Blueprint Data Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duration Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgPrimary)
                        .border(0.5.dp, borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "PLAZO",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = textSecondary,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stringResource(R.string.welcome_discovery_slide1_badge_duration),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Daily Budget Highlight Card
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brandAccent.copy(alpha = 0.10f))
                        .border(0.5.dp, brandAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "PRESUPUESTO DIARIO",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = brandAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "41,1 km / día",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = brandAccent,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 2: Odometer logging illustration showcasing the easy live odometer entry.
 */
@Composable
private fun OdometerRecordStepIllustration() {
    val brandAccent = CanvasKitTheme.colors.brandAccent
    val borderSubtle = CanvasKitTheme.colors.borderSubtle
    val bgSecondary = CanvasKitTheme.colors.backgroundSecondary
    val bgPrimary = CanvasKitTheme.colors.backgroundPrimary
    val textPrimary = CanvasKitTheme.colors.textPrimary
    val textSecondary = CanvasKitTheme.colors.textSecondary

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgSecondary)
            .border(1.dp, borderSubtle, RoundedCornerShape(20.dp))
    ) {
        // Decorative Dashboard Instrument Cluster Arc drawn behind
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 1.05f)
            val radius = size.width * 0.52f
            drawCircle(
                color = brandAccent.copy(alpha = 0.08f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = brandAccent.copy(alpha = 0.04f),
                radius = radius - 18.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Live Odometer Tag + Telemetry Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brandAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CanvasKitIcon(
                            imageVector = CanvasKitIcons.Mobility.OdometerLive,
                            contentDescription = null,
                            tint = brandAccent,
                            size = 16.dp
                        )
                    }
                    Text(
                        text = stringResource(R.string.welcome_discovery_slide2_odometer_label).uppercase(),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = textSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    )
                }

                // Live Sync Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(brandAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AUDITABLE",
                        style = CanvasKitTheme.typography.labelSmall,
                        color = brandAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            // Middle Section: Segmented Automotive Drum Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val digits = listOf("0", "4", "2", "5", "0", "0")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    digits.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgPrimary)
                                .border(1.dp, borderSubtle, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                style = CanvasKitTheme.typography.headingMedium,
                                color = textPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KM",
                    style = CanvasKitTheme.typography.labelSmall,
                    color = brandAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }

            // Bottom Section: Quick Increment Pill + Dedicated FAB [+]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recent increment delta chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgPrimary)
                        .border(0.5.dp, borderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CanvasKitIcon(
                            imageVector = CanvasKitIcons.Navigation.RiskSentinel,
                            contentDescription = null,
                            tint = brandAccent,
                            size = 14.dp
                        )
                        Text(
                            text = "+45 km último trayecto",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Dedicated Quick Settlement FAB [+]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(brandAccent)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "+",
                            style = CanvasKitTheme.typography.labelLarge,
                            color = bgPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Registrar",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = bgPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 3: Live balance traffic light illustration showcasing the Green Zone status.
 */
@Composable
private fun BalanceTrafficLightStepIllustration() {
    val success = CanvasKitTheme.colors.success
    val bgSecondary = CanvasKitTheme.colors.backgroundSecondary
    val bgPrimary = CanvasKitTheme.colors.backgroundPrimary
    val textSecondary = CanvasKitTheme.colors.textSecondary

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgSecondary)
            .border(1.2.dp, success.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
    ) {
        // Decorative ambient green glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(success.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.width * 0.55f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Status Radar Badge + Safe Mode Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(success.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(success)
                        )
                        Text(
                            text = stringResource(R.string.welcome_discovery_slide3_status_badge),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = success,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = "SALDO ADITIVO",
                    style = CanvasKitTheme.typography.labelSmall,
                    color = textSecondary,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            }

            // Middle Section: Massive Hero Balance Value with Contextual Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "+ 350 km",
                        style = CanvasKitTheme.typography.headingLarge,
                        color = success,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "Colchón a tu favor respecto al plan",
                        style = CanvasKitTheme.typography.labelSmall,
                        color = textSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom Section: Guaranteed Zero-Penalty Shield Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgPrimary)
                    .border(0.5.dp, success.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(success.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                color = success,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "Penalización prevista",
                            style = CanvasKitTheme.typography.labelSmall,
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "0,00 €",
                        style = CanvasKitTheme.typography.labelSmall,
                        color = success,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun WelcomeDiscoveryScreenPreview() {
    CanvasKitTheme {
        WelcomeDiscoveryScreen(onNavigateToRentingSetup = {}, onSkip = {})
    }
}
