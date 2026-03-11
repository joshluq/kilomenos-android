package es.joshluq.kmsafe.ui.launch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.di.CheckSession
import es.joshluq.kmsafe.di.SyncContracts
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
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
                        fetchContracts()
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

    private fun fetchContracts() {
        syncContractsUseCase(SyncContractsUseCase.Input)
            .onEach { syncOutput ->
                when (syncOutput) {
                    SyncContractsUseCase.Output.Progress -> Unit
                    is SyncContractsUseCase.Output.Failure -> {
                        delay(800.milliseconds)
                        logger.d("LaunchViewModel", "Sync finished, navigating to Dashboard")
                        launchEffect(LaunchEffect.NavigateToDashboard)
                    }
                    SyncContractsUseCase.Output.Success -> {
                        logger.d("LaunchViewModel", "Sync finished, navigating to Dashboard")
                        launchEffect(LaunchEffect.NavigateToDashboard)
                    }
                }
            }.launchIn(viewModelScope)
    }
}
