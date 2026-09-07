package es.joshluq.kmsafe.feature.projection

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.domain.model.PlannedTrip
import es.joshluq.kmsafe.feature.projection.components.PaceSimulatorCard
import es.joshluq.kmsafe.feature.projection.components.ProjectionSentinelCard
import es.joshluq.kmsafe.feature.projection.components.RemedialAdvisoryCard
import es.joshluq.kmsafe.feature.projection.components.RunwayTimelineCard
import es.joshluq.kmsafe.feature.projection.components.TripPlannerCard
import es.joshluq.kmsafe.core.ui.R as CoreR

@Composable
fun ProjectionAnalysisRoute(
    onNavigateToUpgrade: () -> Unit = {}
) {
    val viewModel: ProjectionAnalysisViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateToPremiumPaywall -> onNavigateToUpgrade()
                is Effect.ShowToast -> {
                    // Handled via state.error or feedback
                }
            }
        }
    }

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
                        text = stringResource(CoreR.string.dashboard_item_projection),
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
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Layer 1: Financial Sentinel (Hero Glanceable)
                ProjectionSentinelCard(
                    endDateMillis = state.contractEndDateMillis,
                    totalContractKms = state.totalContractKms,
                    startOdometer = state.startOdometer,
                    projectedTotalKms = state.simulatedProjectedTotalKms,
                    finalBalance = state.simulatedFinalBalance,
                    estimatedPenalty = state.estimatedPenalty
                )

                // Layer 2: Runway Timeline (Radar de Agotamiento)
                RunwayTimelineCard(
                    isPremium = state.isPremium,
                    isOverLimit = state.isOverLimit,
                    exhaustionDateMillis = state.exhaustionDateMillis,
                    monthsAheadOrBehind = state.monthsAheadOrBehind,
                    onUpgradeClick = { onEvent(Event.OnUpgradeToPremiumClicked) }
                )

                // Layer 3A: Pace Simulator (Ritmo Diario)
                PaceSimulatorCard(
                    realDailyAverage = state.realDailyAverage,
                    simulatedDailyKm = state.simulatedDailyKm,
                    isOverLimit = state.isOverLimit,
                    onKmChanged = { onEvent(Event.OnSimulatedKmChanged(it)) },
                    onPresetSelected = { onEvent(Event.OnPacePresetSelected(it)) }
                )

                // Layer 3B: Trip Planner (Planificador de Escapadas)
                TripPlannerCard(
                    isPremium = state.isPremium,
                    plannedTrips = state.plannedTrips,
                    isOverLimit = state.isOverLimit,
                    onAddTrip = { title, km -> onEvent(Event.OnAddPresetTrip(title, km)) },
                    onRemoveTrip = { onEvent(Event.OnRemoveTrip(it)) },
                    onUpgradeClick = { onEvent(Event.OnUpgradeToPremiumClicked) }
                )

                // Layer 4: Remedial Advisory (Prescripción Copilot para 0,00 €)
                RemedialAdvisoryCard(
                    isPremium = state.isPremium,
                    isOverLimit = state.isOverLimit,
                    remedialDailyKm = state.remedialDailyKm,
                    onUpgradeClick = { onEvent(Event.OnUpgradeToPremiumClicked) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Error Banner
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PreviewProjectionAnalysisScreenPremiumOverLimit() {
    CanvasKitTheme {
        ProjectionAnalysisScreen(
            state = State(
                isLoading = false,
                isPremium = true,
                totalContractKms = 45000.0,
                startOdometer = 10000.0,
                contractEndDateMillis = System.currentTimeMillis() + (300L * 24 * 3600 * 1000),
                daysRemaining = 300,
                realDailyAverage = 45.0f,
                simulatedDailyKm = 52.0f,
                simulatedProjectedTotalKms = 57450.0,
                simulatedFinalBalance = -2450.0,
                estimatedPenalty = 196.0,
                exhaustionDateMillis = System.currentTimeMillis() + (180L * 24 * 3600 * 1000),
                monthsAheadOrBehind = 8,
                remedialDailyKm = 36.8,
                plannedTrips = listOf(
                    PlannedTrip(title = "Escapada Montaña", distanceKms = 350)
                )
            ),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun PreviewProjectionAnalysisScreenFreeOverLimit() {
    CanvasKitTheme {
        ProjectionAnalysisScreen(
            state = State(
                isLoading = false,
                isPremium = false,
                totalContractKms = 45000.0,
                startOdometer = 10000.0,
                contractEndDateMillis = System.currentTimeMillis() + (300L * 24 * 3600 * 1000),
                daysRemaining = 300,
                realDailyAverage = 45.0f,
                simulatedDailyKm = 52.0f,
                simulatedProjectedTotalKms = 57450.0,
                simulatedFinalBalance = -2450.0,
                estimatedPenalty = 196.0,
                plannedTrips = listOf(
                    PlannedTrip(title = "Escapada", distanceKms = 350)
                )
            ),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun PreviewProjectionAnalysisScreenSafeZone() {
    CanvasKitTheme {
        ProjectionAnalysisScreen(
            state = State(
                isLoading = false,
                isPremium = true,
                totalContractKms = 45000.0,
                startOdometer = 10000.0,
                contractEndDateMillis = System.currentTimeMillis() + (300L * 24 * 3600 * 1000),
                daysRemaining = 300,
                realDailyAverage = 30.0f,
                simulatedDailyKm = 30.0f,
                simulatedProjectedTotalKms = 53500.0,
                simulatedFinalBalance = 1500.0,
                estimatedPenalty = 0.0,
                remedialDailyKm = 35.0
            ),
            onEvent = {}
        )
    }
}
