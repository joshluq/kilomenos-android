# Component Interface Specification: Optimización de Arranque y Feedback Reactivo de Downgrade

**Feature ID**: KILOMENOS-15  
**Component Identifiers**: `LaunchScreenFlow`, `StartupSessionController`, `SubscriptionDowngradeHud`, `DashboardOverlayBridge`  
**Package**: `es.joshluq.kmsafe.feature.auth.launch`, `es.joshluq.kmsafe.domain`, `es.joshluq.kmsafe.feature.dashboard`, `es.joshluq.kmsafe.core.ui`  
**Target Modules**: `:feature:auth`, `:core:domain`, `:feature:dashboard`, `:core:ui`  
**Architecture Pattern**: Clean Architecture + MVI + Single-Window Overlay  
**Status**: APPROVED  

---

## 1. Component Overview & Module Responsibilities

| Module | Component | Responsibility |
| :--- | :--- | :--- |
| `:feature:auth` | `LaunchViewModel` | Evalúa de forma no bloqueante la sesión local con `CheckSessionUseCase`. Despacha inmediatamente `LaunchEffect.NavigateToDashboard` ante `ActiveSession`. Se eliminan dependencias de red (`SyncContractsUseCase`, `GetEntitlementsUseCase`, `FingerprintProvider`). |
| `:core:domain` | `AppOverlayState` | Modela los estados de HUD global de la aplicación. Se añade la variante inmutable `SubscriptionDowngraded` para comunicar la transición exclusiva `PREMIUM -> FREE`. |
| `:core:domain` | `SetAppOverlayUseCase` | Caso de uso para establecer o limpiar el estado del HUD global en `AppOverlayRepository`. |
| `:feature:dashboard` | `DashboardContract` | Declara los eventos `OnDismissSubscriptionOverlay` y `OnUpgradeFromSubscriptionOverlay` para gestionar la interacción del usuario con el HUD de downgrade. |
| `:feature:dashboard` | `DashboardViewModel` | Observa los cambios reactivos de suscripción en `UserSessionModel`. Si detecta la transición `PREMIUM -> FREE` (por sincronización de fondo o por evento FCM Push), activa el overlay invocando `SetAppOverlayUseCase`. |
| `:core:ui` | `AppExecutiveHudOverlay` | Componente Composable raíz que renderiza la tarjeta ejecutiva de downgrade con botones para navegar a planes o cerrar el diálogo. |

---

## 2. Public Interfaces & Contract Specifications

### 2.1 Domain Layer (`:core:domain`)

#### `es.joshluq.kmsafe.domain.model.AppOverlayState.kt`
```kotlin
package es.joshluq.kmsafe.domain.model

import es.joshluq.foundationkit.text.TextProvider

/**
 * Universal state representing full-screen, blocking HUD overlays across the application.
 */
sealed interface AppOverlayState {

    /**
     * No blocking HUD overlay is displayed; standard user interaction enabled.
     */
    data object None : AppOverlayState

    /**
     * Displayed when the active renting contract / vehicle is being switched and synced.
     */
    data class VehicleSwitching(
        val vehicleName: String?
    ) : AppOverlayState

    /**
     * Displayed during session termination and secure token revocation.
     */
    data object LoggingOut : AppOverlayState

    /**
     * Displayed during GDPR account deletion and local database purging.
     */
    data class AccountDeletion(
        val stepMessage: TextProvider? = null,
        val progress: Float? = null
    ) : AppOverlayState

    /**
     * Displayed during OCR optical receipt scanning and AI entity extraction.
     */
    data class AiReceiptScanning(
        val stepMessage: TextProvider? = null
    ) : AppOverlayState

    /**
     * Displayed when a subscription downgrade from PREMIUM to FREE is detected in background or via Push FCM.
     *
     * @param title Contextual title informing about the subscription transition.
     * @param message Detailed message explaining the updated feature access.
     */
    data class SubscriptionDowngraded(
        val title: TextProvider? = null,
        val message: TextProvider? = null
    ) : AppOverlayState
}
```

---

### 2.2 Presentation Layer: Launch Feature (`:feature:auth`)

#### `es.joshluq.kmsafe.feature.auth.launch.LaunchViewModel.kt`
```kotlin
package es.joshluq.kmsafe.feature.auth.launch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Optimized ViewModel for LaunchScreen adhering to Zero-Network Cold Start guidelines.
 *
 * Responsibilities:
 * - Validate local session integrity via [CheckSessionUseCase].
 * - Immediately dispatch [LaunchEffect.NavigateToDashboard] on [CheckSessionUseCase.Output.ActiveSession].
 * - Zero blocking remote HTTP/sync calls in the startup critical path.
 */
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
            signOutUseCase(SignOutUseCase.Input(clearLocalData = true, minHoldDurationMs = 0L))
            launchEffect(LaunchEffect.NavigateToLogin)
        }
    }
}
```

---

### 2.3 Presentation Layer: Dashboard (`:feature:dashboard`)

#### `es.joshluq.kmsafe.feature.dashboard.Contract.kt`
```kotlin
package es.joshluq.kmsafe.feature.dashboard

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.AppOverlayState

@Immutable
data class State(
    val selectedTab: DashboardTab = DashboardTab.OVERVIEW,
    val hasRentingContract: Boolean = true,
    val isSwitchingVehicle: Boolean = false,
    val switchingVehicleName: String? = null,
    val hudOverlayState: AppOverlayState = AppOverlayState.None,
    val showUpdateDialog: Boolean = false,
    val currentMileageInput: String = ""
) : UiState {
    val isNavigationBlocked: Boolean
        get() = (hudOverlayState !is AppOverlayState.None && hudOverlayState !is AppOverlayState.SubscriptionDowngraded) || isSwitchingVehicle

    companion object {
        val Empty = State()
    }
}

sealed interface Event : UiEvent {
    data class OnTabSelected(val tab: DashboardTab) : Event
    data class OnTabSynced(val tab: DashboardTab) : Event
    data object ResetToOverview : Event
    data object OnOdometerClicked : Event
    data object OnDismissOdometerDialog : Event
    data class OnOdometerChanged(val mileage: String) : Event
    
    // Feature KILOMENOS-15: Subscription Downgrade HUD interactions
    data object OnDismissSubscriptionOverlay : Event
    data object OnUpgradeFromSubscriptionOverlay : Event
}

sealed interface Effect : UiEffect {
    data class NavigateToTab(val tab: DashboardTab) : Effect
    data object NavigateToPaywall : Effect
}
```

#### `es.joshluq.kmsafe.feature.dashboard.DashboardViewModel.kt`
```kotlin
package es.joshluq.kmsafe.feature.dashboard

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveAppOverlayUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveFleetSwitchingUseCase
import es.joshluq.kmsafe.domain.usecase.SetAppOverlayUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getRentingContractUseCase: GetRentingContractUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val observeFleetSwitchingUseCase: ObserveFleetSwitchingUseCase,
    private val observeAppOverlayUseCase: ObserveAppOverlayUseCase,
    private val setAppOverlayUseCase: SetAppOverlayUseCase,
    private val analytics: AnalyticsTracker,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var lastUserId: String? = null
    private var lastSubscriptionLevel: SubscriptionLevel? = null

    override fun createInitialState(): State = State.Empty

    init {
        observeRentingContract()
        observeCurrentUser()
        observeFleetSwitching()
        observeAppOverlay()
        observeSubscriptionDowngrades()
    }

    override fun handleEvent(event: Event) {
        when (event) {
            is Event.OnTabSelected -> handleTabSelected(event.tab)
            is Event.OnTabSynced -> handleTabSynced(event.tab)
            Event.ResetToOverview -> updateState { copy(selectedTab = DashboardTab.OVERVIEW) }
            Event.OnOdometerClicked -> updateState { copy(showUpdateDialog = true, currentMileageInput = "") }
            Event.OnDismissOdometerDialog -> updateState { copy(showUpdateDialog = false) }
            is Event.OnOdometerChanged -> updateState { copy(currentMileageInput = event.mileage) }
            Event.OnDismissSubscriptionOverlay -> dismissSubscriptionOverlay()
            Event.OnUpgradeFromSubscriptionOverlay -> {
                dismissSubscriptionOverlay()
                launchEffect(Effect.NavigateToPaywall)
            }
        }
    }

    private fun observeSubscriptionDowngrades() {
        getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .distinctUntilChanged()
            .onEach { output ->
                if (output is GetEntitlementsUseCase.Output.Success) {
                    val currentLevel = output.entitlements.subscriptionLevel
                    val previousLevel = lastSubscriptionLevel
                    lastSubscriptionLevel = currentLevel

                    if (previousLevel == SubscriptionLevel.PREMIUM && currentLevel == SubscriptionLevel.FREE) {
                        logger.w("DashboardViewModel", "Subscription downgrade detected: PREMIUM -> FREE. Triggering HUD Overlay.")
                        setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.SubscriptionDowngraded()))
                            .launchIn(viewModelScope)
                    }
                }
            }
            .catch { logger.e("DashboardViewModel", "Error observing entitlements for downgrade", it) }
            .launchIn(viewModelScope)
    }

    private fun dismissSubscriptionOverlay() {
        setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.None))
            .launchIn(viewModelScope)
    }
}
```

---

### 2.4 UI Component Layer (`:core:ui`)

#### `es.joshluq.kmsafe.core.ui.components.AppExecutiveHudOverlay.kt`
Adición del nodo de renderizado en el `when (state)` del Composable:
```kotlin
is AppOverlayState.SubscriptionDowngraded -> {
    SubscriptionDowngradedContent(
        state = state,
        onDismiss = onDismiss,
        onUpgrade = onUpgrade
    )
}
```
Con touch targets >= 48dp y texto accesible para TalkBack.

---

## 3. Data Flow Diagram (UDF)

```
[Start App]
     │
     ▼
[CheckSessionUseCase] (reads Tink/TokenRepository)
     │
     ├─ ActiveSession ──► [LaunchEffect.NavigateToDashboard] (Immediate < 800ms)
     ├─ IdleSession   ──► [LaunchEffect.NavigateToLogin]
     └─ Inconsistent  ──► [SignOutUseCase] ──► [NavigateToLogin]

[DashboardScreen Active]
     │
     ▼
[WorkManager: SyncWorker] / [FCM Push Event] (in background)
     │
     ▼ (Updates UserSessionDataSource)
     │
[DashboardViewModel: observeSubscriptionDowngrades]
     │
     ▼ (previous == PREMIUM && current == FREE)
     │
[SetAppOverlayUseCase(SubscriptionDowngraded)]
     │
     ▼
[AppExecutiveHudOverlay]
     ├── Tap [Entendido]   ──► [SetAppOverlayUseCase(None)]
     └── Tap [Ver Planes]  ──► [SetAppOverlayUseCase(None)] + [NavigateToPaywall]
```
