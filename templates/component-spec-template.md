# Component Interface Specification: [COMPONENT_NAME]

**Feature ID**: [KILOMENOS-XXX]  
**Component Identifier**: [e.g., OverviewRoute / ExpensesRoute / ProfileRoute]  
**Package**: es.joshluq.kmsafe.feature.[feature_name]  
**Target Modules**: :feature:[feature_name], :core:domain, :core:ui (CanvasKit)  
**Architecture Pattern**: MVI + Coordinator/Route Pattern + 4-Layer Clean UI  
**Status**: APPROVED  

---

## 1. Presentation Architecture: Route vs. Screen Separation

KmSafe strictly enforces the **Coordinator / Route Pattern** to decouple navigation infrastructure from pure UI rendering:

### 1.1 The Route Composable (Navigation Coordinator)
Responsible for resolving the ViewModel, observing lifecycle state, and handling navigation callbacks.

`kotlin
package es.joshluq.kmsafe.feature.[feature_name].ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Navigation coordinator for [ComponentName].
 * Manages ViewModel scoping key and handles one-off side effects.
 */
@Composable
fun [ComponentName]Route(
    onNavigateBack: () -> Unit,
    onNavigateToTarget: (String) -> Unit,
    // Provide deterministic key for Navigation 3 scoping (entity-scoped or session-scoped)
    scopingKey: String? = null,
    viewModel: [ComponentName]ViewModel = hiltViewModel(key = scopingKey)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    [ComponentName]Screen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}
`

### 1.2 The Screen Composable (Pure & Stateless UI)
Stateless Composable adhering exclusively to **CanvasKitTheme** tokens, with zero business logic and zero ViewModel references.

`kotlin
package es.joshluq.kmsafe.feature.[feature_name].ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun [ComponentName]Screen(
    uiState: [ComponentName]UiState,
    onAction: ([ComponentName]UiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
`

---

## 2. The 4-Layer Hierarchical Visual Architecture Specification

Every primary feature screen must map its components into the **4 Universal Visual Layers**:

| Layer | Name | Mental Question & Design Rule | Concrete Feature Elements |
| :--- | :--- | :--- | :--- |
| **Layer 1** | **The Pulse / Hero Glanceable** | *How am I doing right now?* (< 2s). High-contrast typography (CanvasKitTheme.typography.headingLarge), semantic health colors. | [e.g., Pacing Runway Bar, Hero Balance, Cost/100km] |
| **Layer 2** | **Contextual Decision Radar** | *What should I do today?*. Horizontal glanceable cards, comparison delta badges (+/- variance). | [e.g., Daily Allowance remaining, Price volatility radar cards] |
| **Layer 3** | **Zero-Friction Action** | Quick execution (< 5s). 1-Tap preset chips ([30€], [50€]), auditable FAB, pre-filled inputs. | [e.g., Auditable FAB [+], 1-Tap refuel bottom sheet] |
| **Layer 4** | **Intelligent Diagnostic Feed** | Contextual historical stories grouped by meaningful operational cycles. | [e.g., Refuel efficiency cycle feed, classified trips list] |

---

## 3. UI State Contract (Immutable Data Models)

`kotlin
package es.joshluq.kmsafe.feature.[feature_name].ui

import androidx.compose.runtime.Immutable

@Immutable
data class [ComponentName]UiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Layer 1: Pulse State
    val heroMetric: [MetricModel]? = null,
    // Layer 2: Radar Cards
    val decisionItems: List<[DecisionModel]> = emptyList(),
    // Layer 3: Action Presets
    val quickPresets: List<[PresetModel]> = emptyList(),
    // Layer 4: Diagnostic Feed
    val feedItems: List<[FeedItemModel]> = emptyList()
)
`

---

## 4. UI Action / Intent Contract (Unidirectional Events)

`kotlin
package es.joshluq.kmsafe.feature.[feature_name].ui

sealed interface [ComponentName]UiAction {
    data object Refresh : [ComponentName]UiAction
    data class PresetSelected(val presetId: String) : [ComponentName]UiAction
    data class EntityUpdated(val payload: [PayloadModel]) : [ComponentName]UiAction
    data object DismissError : [ComponentName]UiAction
}
`

---

## 5. UseCase First Mandate & ViewModel Contract

### Strict Dependency Invariants:
1. **Zero Direct Repository Injection**: ViewModels **MUST NEVER** inject *Repository interfaces. All operations must consume domain UseCases from :core:domain.
2. **Zero Business Calculations in ViewModel**: All metrics, formulas, and balance computations are pre-computed in rich domain entities or dedicated domain UseCases.
3. **Dispatchers**: Enforce main-safety using DispatcherProvider (dispatchers.io / dispatchers.default).

`kotlin
package es.joshluq.kmsafe.feature.[feature_name].ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// Injected domain UseCases exclusively from :core:domain
@HiltViewModel
class [ComponentName]ViewModel @Inject constructor(
    private val observe[DomainEntity]UseCase: Observe[DomainEntity]UseCase,
    private val calculate[Metrics]UseCase: Calculate[Metrics]UseCase,
    private val mutate[Action]UseCase: Mutate[Action]UseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    val uiState: StateFlow<[ComponentName]UiState>
    fun onAction(action: [ComponentName]UiAction)
}
`

---

## 6. Navigation 3 & Scoping Invariants

- **Entity-Scoped Screens**: If this component displays or edits a specific entity (ehicleId, 
ecordId, stationId), specify the scoping key format: [feature]_.
- **Ephemeral Flows**: If this component is a wizard or paywall, specify the session key pattern: UUID.randomUUID().toString().
- **Persistent Dashboard Tabs**: Top-level tabs use singleton scoping preserved across tab switches.
