package es.joshluq.kmsafe.feature.expenses.stations

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.di.CheckFeatureAccess
import es.joshluq.kmsafe.domain.di.DeleteServiceStation
import es.joshluq.kmsafe.domain.di.GetAllServiceStations
import es.joshluq.kmsafe.domain.di.SaveServiceStation
import es.joshluq.kmsafe.domain.di.SetFavoriteStation
import es.joshluq.kmsafe.domain.di.SyncStations
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteServiceStationUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.SaveServiceStationUseCase
import es.joshluq.kmsafe.domain.usecase.SetFavoriteStationUseCase
import es.joshluq.kmsafe.domain.usecase.SyncStationsUseCase
import es.joshluq.kmsafe.feature.expenses.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for managing the user's service stations.
 */
@HiltViewModel
class StationManagementViewModel @Inject constructor(
    @param:GetAllServiceStations private val getAllServiceStationsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetAllServiceStationsUseCase.Input, GetAllServiceStationsUseCase.Output>,
    @param:SaveServiceStation private val saveServiceStationUseCase:
    @JvmSuppressWildcards FlowUseCase<SaveServiceStationUseCase.Input, SaveServiceStationUseCase.Output>,
    @param:DeleteServiceStation private val deleteServiceStationUseCase:
    @JvmSuppressWildcards FlowUseCase<DeleteServiceStationUseCase.Input, DeleteServiceStationUseCase.Output>,
    @param:SetFavoriteStation private val setFavoriteStationUseCase:
    @JvmSuppressWildcards FlowUseCase<SetFavoriteStationUseCase.Input, SetFavoriteStationUseCase.Output>,
    @param:CheckFeatureAccess private val checkFeatureAccessUseCase:
    @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>,
    @param:SyncStations private val syncStationsUseCase:
    @JvmSuppressWildcards FlowUseCase<SyncStationsUseCase.Input, SyncStationsUseCase.Output>,
    private val logger: LoggerKit
) : ScreenViewModel<StationManagementState, StationManagementEvent, StationManagementEffect>() {

    init {
        checkSubscription()
        loadStations()
    }

    override fun createInitialState(): StationManagementState = StationManagementState.Empty

    override fun handleEvent(event: StationManagementEvent) {
        logger.d("StationManagementViewModel", "Handling event: $event")
        when (event) {
            StationManagementEvent.OnRefresh -> handleRefresh()
            is StationManagementEvent.OnSearchQueryChanged -> handleSearch(event.query)
            is StationManagementEvent.OnToggleFavorite -> handleToggleFavorite(event.stationId, event.isFavorite)
            is StationManagementEvent.OnDeleteStation -> updateState { copy(showDeleteConfirmation = true, deleteTargetId = event.stationId) }
            StationManagementEvent.OnConfirmDeleteStation -> {
                val id = state.value.deleteTargetId
                if (id != null) handleDeleteStation(id)
                updateState { copy(showDeleteConfirmation = false, deleteTargetId = null) }
            }
            StationManagementEvent.OnCancelDeleteStation -> updateState { copy(showDeleteConfirmation = false, deleteTargetId = null) }
            is StationManagementEvent.OnLocationCaptured -> updateState { copy(currentLat = event.latitude, currentLng = event.longitude) }
            is StationManagementEvent.OnEditStation -> updateState { copy(selectedStation = event.station, isEditSheetOpen = true) }
            StationManagementEvent.OnAddStationClicked -> updateState { copy(selectedStation = null, isEditSheetOpen = true) }
            is StationManagementEvent.OnViewDetail -> launchEffect(StationManagementEffect.NavigateToDetail(event.stationId))
            StationManagementEvent.OnDismissEdit -> updateState { copy(selectedStation = null, isEditSheetOpen = false) }
            is StationManagementEvent.OnSaveStation -> handleSaveStation(event)
            StationManagementEvent.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun checkSubscription() {
        logger.d("StationManagementViewModel", "Checking subscription level")
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    logger.i("StationManagementViewModel", "Subscription check success: isPremium=${output.isGranted}")
                    updateState { copy(isPremium = output.isGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadStations() {
        logger.d("StationManagementViewModel", "Loading all stations")
        getAllServiceStationsUseCase(GetAllServiceStationsUseCase.Input)
            .onEach { output ->
                when (output) {
                    GetAllServiceStationsUseCase.Output.Progress -> {
                        logger.d("StationManagementViewModel", "Loading stations in progress...")
                        updateState { copy(isLoading = true) }
                    }
                    is GetAllServiceStationsUseCase.Output.Success -> {
                        logger.i("StationManagementViewModel", "Stations loaded: ${output.stations.size} found")
                        updateState {
                            copy(
                                isLoading = false,
                                stations = output.stations,
                                filteredStations = filterStations(output.stations, searchQuery)
                            )
                        }
                    }
                    GetAllServiceStationsUseCase.Output.Empty -> {
                        logger.i("StationManagementViewModel", "No stations found")
                        updateState { copy(isLoading = false, stations = emptyList(), filteredStations = emptyList()) }
                    }
                    GetAllServiceStationsUseCase.Output.Failure -> {
                        logger.e("StationManagementViewModel", "Failed to load stations")
                        updateState { copy(isLoading = false, error = TextProvider.Resource(R.string.expenses_error_load)) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleSearch(query: String) {
        updateState {
            copy(
                searchQuery = query,
                filteredStations = filterStations(stations, query)
            )
        }
    }

    private fun handleRefresh() {
        if (state.value.isPremium) {
            logger.i("StationManagementViewModel", "Premium user: Triggering remote sync on refresh")
            syncStationsUseCase(SyncStationsUseCase.Input)
                .onEach { output ->
                    if (output is SyncStationsUseCase.Output.Success || output is SyncStationsUseCase.Output.Failure) {
                        loadStations()
                    }
                }
                .launchIn(viewModelScope)
        } else {
            logger.d("StationManagementViewModel", "Free user: Local load on refresh")
            loadStations()
        }
    }

    private fun filterStations(stations: List<ServiceStation>, query: String): List<ServiceStation> {
        if (query.isBlank()) return stations
        return stations.filter { 
            it.name.contains(query, ignoreCase = true) || it.brand.contains(query, ignoreCase = true) 
        }
    }

    private fun handleToggleFavorite(stationId: String, isFavorite: Boolean) {
        logger.d("StationManagementViewModel", "Toggling favorite for $stationId to $isFavorite")
        setFavoriteStationUseCase(SetFavoriteStationUseCase.Input(stationId, isFavorite))
            .onEach { output ->
                if (output is SetFavoriteStationUseCase.Output.Success) {
                    logger.i("StationManagementViewModel", "Favorite toggled successfully for $stationId")
                    // Local state update for immediate feedback
                    updateState {
                        val updated = stations.map {
                            if (it.id == stationId) it.copy(isFavorite = isFavorite) else it
                        }
                        copy(stations = updated, filteredStations = filterStations(updated, searchQuery))
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleDeleteStation(stationId: String) {
        logger.w("StationManagementViewModel", "Requesting deletion of station: $stationId")
        deleteServiceStationUseCase(DeleteServiceStationUseCase.Input(stationId))
            .onEach { output ->
                if (output is DeleteServiceStationUseCase.Output.Success) {
                    logger.i("StationManagementViewModel", "Station $stationId deleted successfully")
                    loadStations()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleSaveStation(event: StationManagementEvent.OnSaveStation) {
        if (state.value.isSaving) {
            logger.w("StationManagementViewModel", "Save station ignored: already in progress")
            return
        }
        logger.d("StationManagementViewModel", "Saving station: ${event.name} (id: ${event.id ?: "new"})")
        updateState { copy(isSaving = true) }

        saveServiceStationUseCase(
            SaveServiceStationUseCase.Input(
                id = event.id,
                name = event.name,
                brand = event.brand,
                latitude = state.value.currentLat ?: state.value.selectedStation?.latitude ?: 0.0,
                longitude = state.value.currentLng ?: state.value.selectedStation?.longitude ?: 0.0,
                address = "",
                isFavorite = state.value.selectedStation?.isFavorite ?: false
            )
        ).onEach { output ->
            when (output) {
                is SaveServiceStationUseCase.Output.Success -> {
                    logger.i("StationManagementViewModel", "Station saved successfully: ${output.stationId}")
                    updateState {
                        copy(
                            isEditSheetOpen = false,
                            selectedStation = null,
                            isSaving = false,
                            currentLat = null,
                            currentLng = null,
                        )
                    }
                    loadStations()
                }
                SaveServiceStationUseCase.Output.Failure -> {
                    logger.e("StationManagementViewModel", "Failed to save station")
                    updateState { copy(isSaving = false, error = TextProvider.Resource(R.string.expenses_error_save)) }
                }
                SaveServiceStationUseCase.Output.Progress -> {
                    logger.d("StationManagementViewModel", "Save station in progress...")
                }
            }
        }.launchIn(viewModelScope)
    }
}
