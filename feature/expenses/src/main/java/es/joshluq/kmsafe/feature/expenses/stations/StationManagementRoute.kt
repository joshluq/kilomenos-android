package es.joshluq.kmsafe.feature.expenses.stations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Coordinator Composable for the Station Management screen.
 */
@Composable
fun StationManagementRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: StationManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StationManagementEffect.NavigateBack -> onNavigateBack()
                is StationManagementEffect.NavigateToDetail -> onNavigateToDetail(effect.stationId)
            }
        }
    }

    StationManagementScreen(
        state = state,
        onEvent = viewModel::sendEvent,
        onBack = onNavigateBack
    )
}
