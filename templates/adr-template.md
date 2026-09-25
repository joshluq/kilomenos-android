# ADR-[ID]: [Short Title of Architectural Decision]

**Feature ID**: [KILOMENOS-XXX]  
**Status**: PROPOSED | ACCEPTED | REJECTED | DEPRECATED | SUPERSEDED  
**Deciders**: Software Architect  
**Date**: YYYY-MM-DD  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer  

---

## 1. Context and Problem Statement
[Describe the technical context, business drivers, and the specific architectural challenge being solved. Reference PRD-[ID] and relevant functional requirements.]

## 2. Decision Drivers
- [Driver 1: e.g., Unidirectional Data Flow (UDF) predictability]
- [Driver 2: e.g., Testability without Android framework mocks or emulator dependencies]
- [Driver 3: e.g., Seamless integration with Jetpack Compose lifecycle and state hoisting]
- [Driver 4: e.g., Modular boundary isolation and fast incremental build times]

## 3. Considered Architectural Options
1. **Option 1**: [Description — e.g., Classic MVVM with LiveData / MutableLiveData]
   - *Pros*: Simple, familiar to legacy teams.
   - *Cons*: Poor Compose interoperability, lifecycle coupling, mutable state leak risks.
2. **Option 2**: [Description — e.g., Clean Architecture + MVI (Model-View-Intent) with Compose & StateFlow]
   - *Pros*: Strict unidirectional data flow, immutable UI state models, seamless Compose reactivity, zero Android mocks in unit tests.
   - *Cons*: Additional boilerplate for sealed action hierarchies and reducer handling.
3. **Option 3**: [Description — e.g., Pure Pragmatic MVVM with StateFlow (Single Component)]
   - *Pros*: Less file overhead than multi-layered Clean Architecture.
   - *Cons*: Business logic entangles ViewModel, lower reusability across multi-module setups.

## 4. Decision Outcome
- **Chosen Option**: Option 2 — Clean Architecture + MVI with Jetpack Compose & StateFlow
- **Architecture Pattern**: `MVI` | `MVVM_COMPOSE` | `CLEAN_ARCHITECTURE`
- **Justification**: [Explain why the chosen option best balances testability, compile safety, predictability, and long-term maintainability for Android.]

## 5. System Topology & Module Structure
- **Target Modules & Packaging**:
  - `:feature:[feature_name]` (`com.example.app.feature.[name].ui`) — UI Composables, ViewModel, UiState, UiAction.
  - `:core:domain` (`com.example.app.feature.[name].domain`) — Domain Entities, UseCase interactors.
  - `:core:data` (`com.example.app.feature.[name].data`) — Repository interfaces, local/remote DataSources, DTOs.
  - `:core:model` (`com.example.app.core.model`) — Shared domain models and value classes.
- **State Management**:
  - Unidirectional Data Flow via Kotlin `StateFlow<UiState>`.
  - Immutable `@Immutable data class [Feature]UiState`.
  - User and system interactions encapsulated via `sealed interface [Feature]UiAction`.
  - Transient one-off side effects (navigation, snackbar) via `SharedFlow<UiEffect>` or Channel.
- **Concurrency & Threading**:
  - Kotlin Coroutines scoped to AndroidX `viewModelScope`.
  - Strict constructor injection of `CoroutineDispatcher` (defaults to `Dispatchers.IO` / `Dispatchers.Default`, swappable with `StandardTestDispatcher` in tests).
- **Dependency Injection**:
  - Hilt / Koin module bindings separating ViewModel, UseCases, and Repository implementations.

## 6. Consequences & Tradeoffs
- **Positive Consequences**:
  - 100% deterministic state reproduction in tests via Turbine and MockK.
  - Complete decoupling of UI rendering from data loading logic.
  - Zero compilation dependency on Android framework classes in domain and repository contracts.
- **Negative Consequences**:
  - Requires authoring sealed interfaces for every user action.
  - Requires maintaining distinct domain vs presentation models when transformations are needed.
- **Neutral Consequences**:
  - Requires team convention compliance regarding state hoisting and modifier chaining.

## 7. Guardrails & Compliance Rules
1. **No Direct Repository Calls in Composables**: Composable functions must never instantiate or directly collect from Repositories; all access must flow through ViewModel and UseCases.
2. **Inject All Dispatchers**: Hardcoded calls to `Dispatchers.IO` or `Dispatchers.Default` inside ViewModels or UseCases are strictly forbidden.
3. **Immutable State Only**: UI State must be represented by an immutable Kotlin `data class` with `val` properties only. Mutable collections (`ArrayList`, `MutableMap`) are prohibited in UiState.
4. **Modifier Hoisting**: All public Composable screens must accept a root `modifier: Modifier = Modifier` parameter and chain it to the top-level layout node.
