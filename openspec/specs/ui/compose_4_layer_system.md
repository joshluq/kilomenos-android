# UI Specification: 4-Layer Jetpack Compose System

## 1. Purpose & Scope
Governs the presentation layer across all Android features, enforcing the 4-layer UI pattern, CanvasKit design tokens, and recomposition optimization.

## 2. The 4-Layer UI Architecture

```
Layer 1: Destination / Screen (Route Entrypoint)
   ↓  Owns ViewModel, Scoped Lifecycle Key, and Window Insets
Layer 2: Coordinator / Route Layout
   ↓  Collects State, Channels Navigation Events, Maps User Actions
Layer 3: Screen Content Layout
   ↓  Stateless Scaffold, Emits UI Actions, Renders Main Layout
Layer 4: Atomic Components & Cards
      Pure Stateless CanvasKit Widgets, Reusable Design Tokens
```

### Layer Rules:
- **Layer 1 (Screen)**: Connects to Navigation 3 / NavDisplay. Must pass deterministic keys to `hiltViewModel(key = "...")` for entity-scoped or ephemeral flows.
- **Layer 2 (Coordinator)**: Pure Compose bridge. Observes `viewModel.uiState` and handles one-off side-effects (`Channel<Effect>`).
- **Layer 3 (Content)**: Stateless composable accepting `(state: UiState, onAction: (UiAction) -> Unit)`. Preview-friendly.
- **Layer 4 (Components)**: Reusable atomic widgets built using CanvasKit tokens. Zero direct ViewModel references.

## 3. Stability & Performance Invariants
- Composable parameters must be `@Immutable` or `@Stable`.
- Never allocate new lambdas or unstable collections inside recomposition loops without `remember`.
- Every actionable component must have an explicit `Modifier.testTag()` for automated UI semantics testing.
