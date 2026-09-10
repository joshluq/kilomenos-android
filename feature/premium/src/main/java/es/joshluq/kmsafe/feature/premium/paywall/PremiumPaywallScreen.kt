package es.joshluq.kmsafe.feature.premium.paywall

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.premium.R
import java.util.UUID
import es.joshluq.kmsafe.core.ui.R as CoreR

/**
 * Navigation coordinator for the Premium Paywall screen.
 */
@Composable
fun PremiumPaywallRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
    onLaunchBilling: () -> Unit,
    sessionId: String = rememberSaveable { UUID.randomUUID().toString() },
    viewModel: PremiumPaywallViewModel = hiltViewModel(key = sessionId)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateToDashboard -> onNavigateToDashboard()
                Effect.NavigateBack -> onNavigateBack()
                Effect.LaunchBillingFlow -> onLaunchBilling()
            }
        }
    }

    PremiumPaywallScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

/**
 * High-conversion Premium Paywall screen focused on financial loss aversion,
 * hands-free Bluetooth tracking, real-time excess sentinel, station price savings,
 * and transparent plan selection with a 7-day free trial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    CanvasKitLoadingScaffold(
        isLoading = false,
        containerColor = CanvasKitTheme.colors.backgroundPrimary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background Gradient with subtle Brand Accent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CanvasKitTheme.colors.brandAccent.copy(alpha = 0.08f),
                                CanvasKitTheme.colors.brandPrimary.copy(alpha = 0.04f),
                                CanvasKitTheme.colors.backgroundPrimary
                            )
                        )
                    )
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CanvasKitTheme.spacing.md)
            ) {
                // Top Action Bar with Dismiss button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = CanvasKitTheme.spacing.sm),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = safeClick { onEvent(Event.OnDismissClicked) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.premium_upgrade_cancel),
                            tint = CanvasKitTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Layer 1: Hero Section with Protection Badge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CanvasKitTheme.spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

                    // Small pill badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.premium_paywall_badge),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

                    Text(
                        text = stringResource(R.string.premium_paywall_title),
                        style = CanvasKitTheme.typography.displayMedium,
                        color = CanvasKitTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = stringResource(R.string.premium_paywall_subtitle),
                        style = CanvasKitTheme.typography.bodyLarge,
                        color = CanvasKitTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = CanvasKitTheme.spacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.lg))

                // Layer 2: 3 High-Conversion Feature Cards
                Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)) {
                    BenefitCard(
                        icon = Icons.Default.AutoMode,
                        title = stringResource(R.string.premium_benefit_autotracking_title),
                        description = stringResource(R.string.premium_benefit_autotracking_desc)
                    )
                    BenefitCard(
                        icon = Icons.Default.Speed,
                        title = stringResource(R.string.premium_benefit_sentinel_title),
                        description = stringResource(R.string.premium_benefit_sentinel_desc)
                    )
                    BenefitCard(
                        icon = Icons.Default.LocalGasStation,
                        title = stringResource(R.string.premium_benefit_fuel_radar_title),
                        description = stringResource(R.string.premium_benefit_fuel_radar_desc)
                    )
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                // Layer 3: Loss Aversion / ROI Box (The App Pays for Itself)
                LossAversionRoiBox()

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.lg))

                // Layer 4: Interactive Plan Selector
                PlanSelectorSection(
                    selectedPlan = state.selectedPlan,
                    onPlanSelected = { onEvent(Event.OnPlanSelected(it)) }
                )

                // Large bottom spacer for sticky CTA
                Spacer(modifier = Modifier.height(220.dp))
            }

            // Sticky Footer with CTA Buttons & Trust Guarantee
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                CanvasKitTheme.colors.backgroundPrimary.copy(alpha = 0.95f),
                                CanvasKitTheme.colors.backgroundPrimary
                            )
                        )
                    )
                    .padding(horizontal = CanvasKitTheme.spacing.md, vertical = CanvasKitTheme.spacing.sm)
                    .navigationBarsPadding()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
                ) {
                    if (state.isMigrating) {
                        Text(
                            text = stringResource(R.string.premium_paywall_migrating_msg),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Main Primary CTA Button
                    CanvasKitButton(
                        onClick = safeClick { onEvent(Event.OnUpgradeClicked) },
                        loading = state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("premium_buy_now_button")
                    ) { contentColor ->
                        val ctaText = if (state.isLoading) {
                            stringResource(CoreR.string.common_processing)
                        } else if (state.selectedPlan == PremiumBillingPlan.ANNUAL) {
                            stringResource(R.string.premium_cta_start_trial)
                        } else {
                            stringResource(R.string.premium_cta_monthly)
                        }
                        Text(
                            text = ctaText,
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }

                    // Legal / Trust Microcopy
                    Text(
                        text = stringResource(R.string.premium_guarantee_notice),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Secondary Ghost Button (Continue Free)
                    CanvasKitButton(
                        variant = CanvasKitButtonVariant.Ghost,
                        onClick = safeClick { onEvent(Event.OnDismissClicked) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.premium_continue_free),
                            style = CanvasKitTheme.typography.labelLarge,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Error Banner
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(CanvasKitTheme.spacing.sm)
                    .navigationBarsPadding(),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(Event.OnDismissError) }
            )
        }
    }
}

/**
 * Feature card displaying a high-conversion capability.
 */
@Composable
private fun BenefitCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Loss Aversion ROI Box highlighting the financial penalty avoided vs the subscription cost.
 */
@Composable
private fun LossAversionRoiBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(CanvasKitTheme.spacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.premium_roi_box_title),
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.brandAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.premium_roi_box_desc),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Interactive Plan Selector showing Annual (7-day free trial) and Monthly plans.
 */
@Composable
private fun PlanSelectorSection(
    selectedPlan: PremiumBillingPlan,
    onPlanSelected: (PremiumBillingPlan) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
    ) {
        // Option 1: Annual Plan (with 7-Day Free Trial)
        PlanOptionCard(
            title = stringResource(R.string.premium_plan_annual_title),
            price = stringResource(R.string.premium_plan_annual_price),
            period = stringResource(R.string.premium_plan_annual_period),
            badge = stringResource(R.string.premium_plan_annual_badge),
            isBadgeHighlighted = true,
            isSelected = selectedPlan == PremiumBillingPlan.ANNUAL,
            onClick = { onPlanSelected(PremiumBillingPlan.ANNUAL) },
            testTag = "premium_plan_annual_card"
        )

        // Option 2: Monthly Plan
        PlanOptionCard(
            title = stringResource(R.string.premium_plan_monthly_title),
            price = stringResource(R.string.premium_plan_monthly_price),
            period = stringResource(R.string.premium_plan_monthly_period),
            badge = stringResource(R.string.premium_plan_monthly_badge),
            isBadgeHighlighted = false,
            isSelected = selectedPlan == PremiumBillingPlan.MONTHLY,
            onClick = { onPlanSelected(PremiumBillingPlan.MONTHLY) },
            testTag = "premium_plan_monthly_card"
        )
    }
}

/**
 * Individual plan option card with selection state and pricing breakdown.
 */
@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    period: String,
    badge: String,
    isBadgeHighlighted: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) {
        CanvasKitTheme.colors.brandAccent
    } else {
        CanvasKitTheme.colors.borderSubtle
    }

    val borderWidth = if (isSelected) 2.dp else 1.dp

    val backgroundColor = if (isSelected) {
        CanvasKitTheme.colors.brandAccent.copy(alpha = 0.05f)
    } else {
        CanvasKitTheme.colors.backgroundSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(CanvasKitTheme.spacing.md)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
                    ) {
                        Text(
                            text = title,
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CanvasKitTheme.colors.textPrimary
                        )

                        // Highlight Badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isBadgeHighlighted) {
                                        CanvasKitTheme.colors.brandAccent.copy(alpha = 0.15f)
                                    } else {
                                        CanvasKitTheme.colors.backgroundSecondary
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                style = CanvasKitTheme.typography.labelSmall,
                                color = if (isBadgeHighlighted) {
                                    CanvasKitTheme.colors.brandAccent
                                } else {
                                    CanvasKitTheme.colors.textSecondary
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = period,
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            }

            // Price column
            Text(
                text = price,
                style = CanvasKitTheme.typography.headingMedium,
                fontWeight = FontWeight.Black,
                color = if (isSelected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textPrimary
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun PremiumPaywallScreenPreview() {
    CanvasKitTheme {
        PremiumPaywallScreen(
            state = State(selectedPlan = PremiumBillingPlan.ANNUAL),
            onEvent = {}
        )
    }
}
