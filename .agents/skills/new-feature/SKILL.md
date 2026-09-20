---
name: new-feature
description: >-
  Modular design and implementation pipeline for new features in KmSafe. Guides feature creation step-by-step strictly following AGENTS.md: 4-Layer visual architecture, Route/Screen separation, UseCase First mandate in :core:domain, CanvasKit tokens, and unit tests with Turbine.
---

# New Feature -- Modular Construction Pipeline (KmSafe)

This command executes the **rigorous specification, design, and implementation pipeline for new features** in KmSafe, ensuring strict adherence to AGENTS.md and preventing architectural drift.

It coordinates ndroid-staff-engineer-compose, android-testing, and .agents/templates/component-spec-template.md.

---

## When to Use This Command
- To create a new screen, tab, modal, or user flow inside a :feature:* module.
- To implement a new business process with its corresponding domain UseCase, MVI presentation layer, and unit tests.

---

## Step-by-Step Execution Protocol

When the user invokes /new-feature [feature name or brief description]:

### Phase 1: Component Specification (Component Spec)
Generate the technical specification following .agents/templates/component-spec-template.md:
1. **4-Layer Visual Mapping**:
   - *Layer 1 (The Pulse)*: Glanceable hero metric (< 2s scan).
   - *Layer 2 (Decision Radar)*: Actionable insight cards and comparison deltas.
   - *Layer 3 (Zero-Friction Action)*: 1-Tap preset chips, auditable FAB, quick forms.
   - *Layer 4 (Diagnostic Feed)*: Contextual operational cycle history.
2. **MVI Contracts**:
   - UiState: Immutable Kotlin data class marked with @Immutable.
   - UiAction: Sealed interface capturing all unidirectional user intents.
   - UiEffect: (Optional) Single-shot side effect channel (navigation, snackbars).

### Phase 2: "UseCase First" Mandate (Domain Layer)
1. Define or identify the dedicated UseCase in :core:domain (pluginkit.jvm.library).
2. **Golden Rule**: ViewModels **MUST NEVER** inject *Repository interfaces directly. All data access and business processes flow through domain UseCases (FlowUseCase or UseCase from FoundationKit).
3. Ensure domain purity: strictly zero Android framework imports (ndroid.*) in :core:domain.

### Phase 3: Presentation Implementation (Coordinator / Route)
1. **[Feature]Route.kt**:
   - Injects hiltViewModel(key = scopingKey) with deterministic entity or session keys for Navigation 3 scoping.
   - Collects UI state using collectAsStateWithLifecycle().
   - Coordinates navigation callbacks and savedStateHandle result handling.
2. **[Feature]Screen.kt**:
   - Pure, stateless Composable function accepting UiState and onAction lambda.
   - Built exclusively with CanvasKitTheme tokens (typography, colors, spacing).
   - Minimum 48dp touch targets on interactive elements and mandatory contentDescription.
   - Mandatory @Preview composables for Loading, Content, and Error states.
3. **[Feature]ViewModel.kt**:
   - MVI state reducer exposing StateFlow<UiState>.
   - Enforces main-safety using injected DispatcherProvider.
   - **Zero business calculations**: all metrics are pre-computed by domain entities or UseCases.

### Phase 4: Automated Testing
1. Generate [Feature]ViewModelTest.kt:
   - State flow emissions asserted with **Turbine** (iewModel.uiState.test { ... }).
   - Mock only the injected UseCases with **MockK**.
   - Use StandardTestDispatcher for deterministic coroutine execution.
