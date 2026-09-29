package es.joshluq.kmsafe

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.navigation.NavigationResultStore
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.RegisterDeviceTokenUseCase
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import es.joshluq.kmsafe.ui.navigation.DeepLinkParser
import es.joshluq.kmsafe.ui.util.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Root ViewModel for [MainActivity], responsible for lifecycle-scoped application events
 * including proactive device push token registration, network synchronization, and deep link resolution.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val syncManager: SyncManager,
    private val resultStore: NavigationResultStore,
    private val logger: LoggerKit
) : ViewModel() {

    init {
        observeActiveSessionAndRegisterToken()
        observeNetworkConnectivity()
    }

    private fun observeActiveSessionAndRegisterToken() {
        checkSessionUseCase(CheckSessionUseCase.Input)
            .filter { it == CheckSessionUseCase.Output.ActiveSession }
            .onEach {
                logger.d("MainViewModel", "Active session confirmed. Triggering device push token registration.")
                registerDeviceTokenUseCase(RegisterDeviceTokenUseCase.Input())
            }
            .launchIn(viewModelScope)
    }

    private fun observeNetworkConnectivity() {
        connectivityObserver.observe()
            .onEach { status ->
                logger.d("MainViewModel", "Network status changed: $status")
                if (status == NetworkConnectivityObserver.Status.Available) {
                    delay(1000.milliseconds)
                    logger.d("MainViewModel", "Triggering background sync")
                    syncManager.scheduleSync()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Resolves incoming intents, parsing deep link destinations and action parameters,
     * publishing results to [resultStore] for consumption by Navigation 3 coordinators.
     */
    fun handleIntent(intent: Intent?) {
        val destination = DeepLinkParser.parse(intent)
        if (destination != null) {
            logger.d("MainViewModel", "Deep link destination resolved: $destination")
            resultStore.setResult("deep_link_destination", destination)
        }
        val isQuickAdd = intent?.data?.getQueryParameter("action") == "quick_add"
        if (isQuickAdd) {
            logger.d("MainViewModel", "Quick add odometer action detected")
            resultStore.setResult("quick_add_odometer", true)
        }
    }

    /**
     * Triggered on application resume to execute the cyclic entitlements check (Capa 2).
     * If an active user session exists, queries GET /v1/user/entitlements in the background.
     */
    fun onAppResumed() {
        checkSessionUseCase(CheckSessionUseCase.Input)
            .filter { it == CheckSessionUseCase.Output.ActiveSession }
            .take(1)
            .onEach {
                logger.d("MainViewModel", "App resumed with active session. Executing cyclic entitlements refresh.")
                getEntitlementsUseCase(GetEntitlementsUseCase.Input(deviceFingerprint = "", forceRefresh = true))
                    .catch { e -> logger.e("MainViewModel", "Cyclic entitlements refresh failed: ${e.message}", e) }
                    .collect()
            }
            .launchIn(viewModelScope)
    }
}
