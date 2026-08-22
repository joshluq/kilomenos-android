package es.joshluq.kmsafe.ui.projection

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.ui.projection.components.ProjectionGauge
import kotlin.math.absoluteValue
import es.joshluq.kmsafe.core.ui.util.NumberFormatter

@Composable
fun ProjectionAnalysisRoute() {
    val viewModel: ProjectionAnalysisViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()

    ProjectionAnalysisScreen(
        state = state.value,
        onEvent = viewModel::sendEvent
    )
}

@Composable
fun ProjectionAnalysisScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_item_projection),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                HeaderSection(state)

                SimulationSection(
                    currentRealKm = state.currentRealDailyAverage,
                    currentSimulatedKm = state.simulatedDailyKm,
                    onKmChanged = { onEvent(Event.OnSimulatedKmChanged(it)) },
                    plannedKm = state.plannedTripKms,
                    onPlannedKmChanged = { onEvent(Event.OnPlannedTripChanged(it.toInt())) },
                    isOverLimit = state.simulatedFinalBalance < 0
                )
                Spacer(modifier = Modifier.height(24.dp))
                FinancialImpactCard(
                    finalBalance = state.simulatedFinalBalance,
                    penaltyPrice = state.penaltyPricePerKm,
                    estimatedPenalty = state.estimatedPenalty,
                    recommendedKm = state.recommendedDailyKm
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Toast-style Banner (Error)
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(Event.OnDismissError) }
            )
        }
    }
}

@Composable
private fun HeaderSection(state: State) {
    val projectedTotal = state.totalContractKms - state.simulatedFinalBalance

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ProjectionGauge(
            projectedKms = projectedTotal,
            limitKms = state.totalContractKms,
            balance = state.simulatedFinalBalance
        )
    }
}

@Composable
private fun SimulationSection(
    currentRealKm: Float,
    currentSimulatedKm: Float,
    onKmChanged: (Float) -> Unit,
    plannedKm: Int,
    onPlannedKmChanged: (Float) -> Unit,
    isOverLimit: Boolean
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        header = {
            Text(
                text = stringResource(R.string.projection_analysis_simulator_title),
                style = CanvasKitTheme.typography.labelLarge,
                color = CanvasKitTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(CanvasKitTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            // Daily KM Simulator
            Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.projection_analysis_simulator_desc),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.projection_analysis_actual_average, NumberFormatter.formatRate(currentRealKm.toDouble())),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.projection_analysis_daily_km_slider, NumberFormatter.formatRate(currentSimulatedKm.toDouble())),
                        style = CanvasKitTheme.typography.bodyLarge,
                        color = CanvasKitTheme.colors.brandAccent,
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = currentSimulatedKm,
                    onValueChange = onKmChanged,
                    valueRange = 0f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent,
                        activeTrackColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent,
                        inactiveTrackColor = CanvasKitTheme.colors.borderSubtle,
                    )
                )
            }

            // Planned Trip Simulator
            Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.projection_planner_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        text = stringResource(R.string.projection_analysis_planned_trip_increment, plannedKm),
                        style = CanvasKitTheme.typography.bodyLarge,
                        color = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = plannedKm.toFloat(),
                    onValueChange = onPlannedKmChanged,
                    valueRange = 0f..5000f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent,
                        activeTrackColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent,
                        inactiveTrackColor = CanvasKitTheme.colors.borderSubtle
                    )
                )
            }
        }
    }
}

@Composable
private fun FinancialImpactCard(
    finalBalance: Double,
    penaltyPrice: Float,
    estimatedPenalty: Double,
    recommendedKm: Double?
) {
    val isPositive = finalBalance >= 0
    val cardColor = if (isPositive) {
        CanvasKitTheme.colors.success.copy(
            alpha = 0.05f
        )
    } else {
        CanvasKitTheme.colors.error.copy(alpha = 0.05f)
    }
    val accentColor = if (isPositive) CanvasKitTheme.colors.success else CanvasKitTheme.colors.error

    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated,
        header = {
            Row(
                modifier = Modifier
                    .padding(horizontal = CanvasKitTheme.spacing.md, vertical = CanvasKitTheme.spacing.sm)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.projection_analysis_financial_impact_title),
                    style = CanvasKitTheme.typography.labelLarge,
                    color = CanvasKitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) {
        Box(modifier = Modifier.background(cardColor)) {
            Column(
                modifier = Modifier.padding(CanvasKitTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.projection_analysis_simulated_balance),
                            style = CanvasKitTheme.typography.bodyLarge,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                        if (isPositive) {
                            Text(
                                text = stringResource(R.string.projection_analysis_budget_protected),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(
                            if (isPositive) R.string.common_km_positive_suffix else R.string.common_km_negative_suffix,
                            NumberFormatter.formatDistance(finalBalance.absoluteValue)
                        ),
                        style = CanvasKitTheme.typography.headingLarge,
                        color = accentColor,
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isPositive) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.projection_analysis_estimated_penalty),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                            Text(
                                text = stringResource(R.string.common_currency_format, estimatedPenalty),
                                style = CanvasKitTheme.typography.displayMedium,
                                color = CanvasKitTheme.colors.error,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = stringResource(R.string.projection_analysis_penalty_price_label) + ": " + stringResource(R.string.common_currency_format, penaltyPrice),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Integrated Advisory
                HorizontalDivider(color = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.3f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (recommendedKm != null) {
                            stringResource(R.string.projection_advisory_action, NumberFormatter.formatRate(recommendedKm))
                        } else {
                            stringResource(R.string.projection_advisory_safe)
                        },
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun ProjectionAnalysisScreenPreview() {
    CanvasKitTheme {
        ProjectionAnalysisScreen(
            state = State(
                isLoading = false,
                baselineProjection = TripProjection(
                    projectedTotalKms = 46200.0,
                    expectedFinalBalance = -1200.0,
                    isOverLimit = true,
                    dailyAverage = 42.5,
                    hasEnoughData = true
                ),
                simulatedDailyKm = 48f,
                simulatedFinalBalance = -1200.0,
                estimatedPenalty = 60.0,
                daysRemaining = 450,
                totalContractKms = 45000.0
            ),
            onEvent = {}
        )
    }
}
