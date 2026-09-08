package es.joshluq.kmsafe.feature.expenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Coordinator Composable for the Expenses feature screen.
 *
 * Observes state and sideEffects from [ExpensesViewModel] and coordinates navigation back to the shell.
 */
@Composable
fun ExpensesRoute(
    stationId: String? = null,
    autoOpenAdd: Boolean = false,
    priceReportMode: Boolean = false,
    onNavigateToUpgrade: () -> Unit = {},
    onNavigateToStations: () -> Unit = {},
    onNavigateToStationDetail: (String) -> Unit = {},
    viewModel: ExpensesViewModel = hiltViewModel(
        creationCallback = { factory: ExpensesViewModel.Factory ->
            factory.create(
                stationId = stationId,
                autoOpenAdd = autoOpenAdd,
                priceReportMode = priceReportMode
            )
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ExpensesEffect.NavigateToUpgrade -> {
                    onNavigateToUpgrade()
                }
                ExpensesEffect.NavigateToStations -> {
                    onNavigateToStations()
                }
                is ExpensesEffect.NavigateToStationDetail -> {
                    onNavigateToStationDetail(effect.stationId)
                }
            }
        }
    }

    ExpensesScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}
