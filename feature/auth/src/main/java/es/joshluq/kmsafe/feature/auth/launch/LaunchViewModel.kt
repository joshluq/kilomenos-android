package es.joshluq.kmsafe.feature.auth.launch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val syncContractsUseCase: SyncContractsUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val fingerprintProvider: FingerprintProvider,
    private val logger: LoggerKit
) : ScreenViewModel<LaunchState, LaunchEvent, LaunchEffect>() {

    override fun createInitialState(): LaunchState = LaunchState()

    private var isNavigating = false

    init {
        validateSession()
    }

    override fun handleEvent(event: LaunchEvent) {}

    private fun validateSession() {
        checkSessionUseCase(CheckSessionUseCase.Input)
            .onEach { output ->
                logger.d("LaunchViewModel", "checkSessionUseCase emitted: $output | isNavigating=$isNavigating")
                if (isNavigating && output != CheckSessionUseCase.Output.Progress) return@onEach

                when (output) {
                    CheckSessionUseCase.Output.ActiveSession -> {
                        isNavigating = true
                        logger.d("LaunchViewModel", "Session active and consistent, starting mandatory sync")
                        fetchInitialData()
                    }
                    CheckSessionUseCase.Output.InconsistentSession -> {
                        isNavigating = true
                        logger.w("LaunchViewModel", "Inconsistent session detected. Forcing logout.")
                        handleInconsistentSession()
                    }
                    CheckSessionUseCase.Output.IdleSession -> {
                        isNavigating = true
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

    private fun handleInconsistentSession() {
        viewModelScope.launch {
            // Force sign out to clear tokens and any orphan data
            signOutUseCase(SignOutUseCase.Input(clearLocalData = true)).collect()
            delay(500.milliseconds)
            launchEffect(LaunchEffect.NavigateToLogin)
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            val fingerprint = fingerprintProvider.getFingerprint()

            // 1. Fetch Entitlements FIRST (Linearized)
            try {
                getEntitlementsUseCase(GetEntitlementsUseCase.Input(fingerprint, forceRefresh = true))
                    .first() // We take the first terminal emission
                logger.d("LaunchViewModel", "Entitlements sync finished.")
            } catch (e: Exception) {
                logger.e("LaunchViewModel", "Entitlements sync failed: ${e.message}")
            }

            // 2. Fetch Contracts SECOND (Linearized)
            try {
                syncContractsUseCase(SyncContractsUseCase.Input)
                    .first { it !is SyncContractsUseCase.Output.Progress }
                logger.d("LaunchViewModel", "Initial data sync finished.")
            } catch (e: Exception) {
                logger.e("LaunchViewModel", "Contract sync failed: ${e.message}")
            }

            // 3. Final Navigation (Guaranteed once)
            logger.d("LaunchViewModel", "Linear startup finished, navigating to Dashboard")
            delay(500.milliseconds)
            launchEffect(LaunchEffect.NavigateToDashboard)
        }
    }
}
