package es.joshluq.kmsafe.feature.expenses.stations.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Coordinator Composable for the Station Detail screen.
 */
@Composable
fun StationDetailRoute(
    stationId: String,
    onNavigateBack: () -> Unit,
    viewModel: StationDetailViewModel = hiltViewModel(
        creationCallback = { factory: StationDetailViewModel.Factory ->
            factory.create(stationId)
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StationDetailScreen(
        state = state,
        onEvent = viewModel::sendEvent,
        onBack = onNavigateBack
    )
}
