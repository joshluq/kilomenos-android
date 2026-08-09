package es.joshluq.kmsafe.ui.launch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.data.util.DeviceFingerprintProvider
import es.joshluq.kmsafe.di.CheckSession
import es.joshluq.kmsafe.di.GetEntitlements
import es.joshluq.kmsafe.di.SyncContracts
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class LaunchViewModel @Inject constructor(
    @param:CheckSession private val checkSessionUseCase:
    @JvmSuppressWildcards FlowUseCase<CheckSessionUseCase.Input, CheckSessionUseCase.Output>,
    @param:SyncContracts private val syncContractsUseCase:
    @JvmSuppressWildcards FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output>,
    @param:GetEntitlements private val getEntitlementsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>,
    private val fingerprintProvider: DeviceFingerprintProvider,
    private val logger: LoggerKit
) : ScreenViewModel<LaunchState, LaunchEvent, LaunchEffect>() {

    override fun createInitialState(): LaunchState = LaunchState()

    init {
        validateSession()
    }

    override fun handleEvent(event: LaunchEvent) {}

    private fun validateSession() {
        checkSessionUseCase(CheckSessionUseCase.Input)
            .onEach { output ->
                when (output) {
                    CheckSessionUseCase.Output.ActiveSession -> {
                        logger.d("LaunchViewModel", "Session active, starting mandatory sync")
                        fetchInitialData()
                    }
                    CheckSessionUseCase.Output.IdleSession -> {
                        logger.d("LaunchViewModel", "No active session, navigating to Login")
                        delay(800.milliseconds)
                        launchEffect(LaunchEffect.NavigateToLogin)
                    }
                    CheckSessionUseCase.Output.Progress -> {
                        updateState { copy(isLoading = true) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun fetchInitialData() {
        val fingerprint = fingerprintProvider.getFingerprint()
        
        // 1. Fetch Entitlements FIRST
        getEntitlementsUseCase(GetEntitlementsUseCase.Input(fingerprint, forceRefresh = true))
            .onEach { output ->
                when (output) {
                    is GetEntitlementsUseCase.Output.Success -> {
                        logger.d("LaunchViewModel", "Entitlements sync finished. Starting contract sync.")
                        // 2. Fetch Contracts SECOND
                        fetchContracts()
                    }
                    is GetEntitlementsUseCase.Output.Failure -> {
                        logger.e("LaunchViewModel", "Entitlements sync failed: ${output.message}")
                        fetchContracts() // Proceed anyway to allow local-first access
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun fetchContracts() {
        syncContractsUseCase(SyncContractsUseCase.Input)
            .onEach { syncOutput ->
                when (syncOutput) {
                    SyncContractsUseCase.Output.Progress -> Unit
                    is SyncContractsUseCase.Output.Failure,
                    SyncContractsUseCase.Output.Success -> {
                        logger.d("LaunchViewModel", "Initial data sync finished, navigating to Dashboard")
                        delay(500.milliseconds)
                        launchEffect(LaunchEffect.NavigateToDashboard)
                    }
                }
            }.launchIn(viewModelScope)
    }
}
