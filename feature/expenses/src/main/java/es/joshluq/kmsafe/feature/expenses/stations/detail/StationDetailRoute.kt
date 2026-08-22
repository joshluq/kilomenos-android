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
    onNavigateBack: () -> Unit,
    viewModel: StationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StationDetailScreen(
        state = state,
        onEvent = viewModel::sendEvent,
        onBack = onNavigateBack
    )
}
