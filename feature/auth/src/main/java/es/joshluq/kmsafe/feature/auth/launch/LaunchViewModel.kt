package es.joshluq.kmsafe.feature.auth.launch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val signOutUseCase: SignOutUseCase,
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
                        logger.i("LaunchViewModel", "Active session confirmed. Instant navigation to Dashboard.")
                        launchEffect(LaunchEffect.NavigateToDashboard)
                    }
                    CheckSessionUseCase.Output.InconsistentSession -> {
                        isNavigating = true
                        logger.w("LaunchViewModel", "Inconsistent session detected. Forcing logout.")
                        handleInconsistentSession()
                    }
                    CheckSessionUseCase.Output.IdleSession -> {
                        isNavigating = true
                        logger.d("LaunchViewModel", "No active session, navigating to Login")
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
            signOutUseCase(SignOutUseCase.Input(clearLocalData = true, minHoldDurationMs = 0L)).collect()
            launchEffect(LaunchEffect.NavigateToLogin)
        }
    }
}
