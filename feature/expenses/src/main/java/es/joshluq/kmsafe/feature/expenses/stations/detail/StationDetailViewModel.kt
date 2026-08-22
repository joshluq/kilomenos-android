package es.joshluq.kmsafe.feature.expenses.stations.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.di.CheckFeatureAccess
import es.joshluq.kmsafe.domain.di.GetServiceStationDetail
import es.joshluq.kmsafe.domain.di.GetStationVolatility
import es.joshluq.kmsafe.domain.di.SetFavoriteStation
import es.joshluq.kmsafe.domain.di.SyncStations
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetServiceStationDetailUseCase
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCase
import es.joshluq.kmsafe.domain.usecase.SetFavoriteStationUseCase
import es.joshluq.kmsafe.domain.usecase.SyncStationsUseCase
import es.joshluq.kmsafe.feature.expenses.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for viewing service station statistics and history.
 */
@HiltViewModel
class StationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:GetServiceStationDetail private val getServiceStationDetailUseCase:
    @JvmSuppressWildcards FlowUseCase<GetServiceStationDetailUseCase.Input, GetServiceStationDetailUseCase.Output>,
    @param:GetStationVolatility private val getStationVolatilityUseCase:
    @JvmSuppressWildcards FlowUseCase<GetStationVolatilityUseCase.Input, GetStationVolatilityUseCase.Output>,
    @param:SetFavoriteStation private val setFavoriteStationUseCase:
    @JvmSuppressWildcards FlowUseCase<SetFavoriteStationUseCase.Input, SetFavoriteStationUseCase.Output>,
    @param:CheckFeatureAccess private val checkFeatureAccessUseCase:
    @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>,
    @param:SyncStations private val syncStationsUseCase:
    @JvmSuppressWildcards FlowUseCase<SyncStationsUseCase.Input, SyncStationsUseCase.Output>,
    private val logger: LoggerKit
) : ScreenViewModel<StationDetailState, StationDetailEvent, StationDetailEffect>() {

    private val stationId: String = checkNotNull(savedStateHandle["stationId"])

    init {
        updateState { copy(stationId = stationId) }
        checkSubscription()
        loadData()
    }

    override fun createInitialState(): StationDetailState = StationDetailState.Empty

    override fun handleEvent(event: StationDetailEvent) {
        logger.d("StationDetailViewModel", "Handling event: $event")
        when (event) {
            StationDetailEvent.OnRefresh -> handleRefresh()
            is StationDetailEvent.OnToggleFavorite -> handleToggleFavorite(event.isFavorite)
            StationDetailEvent.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun checkSubscription() {
        logger.d("StationDetailViewModel", "Checking subscription level")
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    logger.i("StationDetailViewModel", "Subscription check success: isPremium=${output.isGranted}")
                    updateState { copy(isPremium = output.isGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        logger.d("StationDetailViewModel", "Loading data for station: $stationId")
        getServiceStationDetailUseCase(GetServiceStationDetailUseCase.Input(stationId))
            .onEach { output ->
                when (output) {
                    GetServiceStationDetailUseCase.Output.Progress -> {
                        logger.d("StationDetailViewModel", "Loading station detail in progress...")
                        updateState { copy(isLoading = true) }
                    }
                    is GetServiceStationDetailUseCase.Output.Success -> {
                        logger.i("StationDetailViewModel", "Station detail loaded successfully")
                        updateState { copy(isLoading = false, detail = output.detail) }
                        // Once we have the detail, we can compute volatility for the main fuel type
                        val mainFuelType = output.detail.expenseHistory.firstOrNull()?.fuelType ?: FuelType.GASOLINE_95
                        loadVolatility(mainFuelType)
                    }
                    is GetServiceStationDetailUseCase.Output.Failure -> {
                        logger.e("StationDetailViewModel", "Failed to load station detail")
                        updateState { copy(isLoading = false, error = TextProvider.Resource(R.string.expenses_error_load)) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadVolatility(fuelType: FuelType) {
        logger.d("StationDetailViewModel", "Loading volatility for fuel: $fuelType")
        getStationVolatilityUseCase(GetStationVolatilityUseCase.Input(stationId, fuelType))
            .onEach { output ->
                when (output) {
                    is GetStationVolatilityUseCase.Output.Success -> {
                        logger.i("StationDetailViewModel", "Volatility calculated: ${output.volatility.priceTrend}")
                        updateState { copy(volatility = output.volatility) }
                    }
                    GetStationVolatilityUseCase.Output.Empty -> {
                        logger.i("StationDetailViewModel", "No volatility data available for this station/fuel")
                    }
                    GetStationVolatilityUseCase.Output.Failure -> {
                        logger.e("StationDetailViewModel", "Error calculating station volatility")
                    }
                    GetStationVolatilityUseCase.Output.Progress -> {
                        logger.d("StationDetailViewModel", "Calculating volatility...")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleRefresh() {
        if (state.value.isPremium) {
            logger.i("StationDetailViewModel", "Premium user: Triggering remote sync on refresh")
            syncStationsUseCase(SyncStationsUseCase.Input)
                .onEach { output ->
                    if (output is SyncStationsUseCase.Output.Success || output is SyncStationsUseCase.Output.Failure) {
                        loadData()
                    }
                }
                .launchIn(viewModelScope)
        } else {
            logger.d("StationDetailViewModel", "Free user: Local load on refresh")
            loadData()
        }
    }

    private fun handleToggleFavorite(isFavorite: Boolean) {
        logger.d("StationDetailViewModel", "Setting favorite for $stationId to $isFavorite")
        setFavoriteStationUseCase(SetFavoriteStationUseCase.Input(stationId, isFavorite))
            .onEach { output ->
                if (output is SetFavoriteStationUseCase.Output.Success) {
                    logger.i("StationDetailViewModel", "Favorite state updated successfully")
                    updateState {
                        copy(detail = detail?.copy(station = detail.station.copy(isFavorite = isFavorite)))
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
