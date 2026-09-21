# Architecture Specification: MVI & Clean Architecture

## 1. Purpose & Scope
This specification defines the architectural invariants, layer boundaries, and state management rules for all feature modules and core libraries.

## 2. Core Principles: "The Being" vs. "The Doing"

### 2.1 The "Being" (El Ser) - Immutable Models & State
- **Domain Models**: Rich domain entities (`RentingContract`, `ContractMetrics`) encapsulate domain logic and mathematical invariants.
- **UI State**: Every screen owns a single immutable data class (e.g., `OverviewUiState`) representing the entire visual state at any point in time.
- **Strict Prohibition**: UI state classes must NEVER expose mutable properties (`var`) or mutable collections (`MutableList`, `MutableMap`).

### 2.2 The "Doing" (El Hacer) - Behavior & Orchestration
- **UseCase First**: All business actions, queries, and background operations MUST be implemented as independent UseCases (`FlowUseCase` / `UseCase`).
- **Zero Business Logic in UI/ViewModels**: ViewModels and Compose functions are strictly forbidden from performing arithmetic shortcuts, metric aggregation, or balance calculations. ViewModels only trigger UseCases and reduce results into UI State.
- **Repository Pattern**: Repositories are accessed exclusively through UseCases. ViewModels must never inject repositories directly.

## 3. Asynchronous Execution & Coroutines
- All disk and network I/O must execute on `Dispatchers.IO`.
- UI state reduction must execute on `Dispatchers.Main.immediate`.
- Dispatchers must be injected via constructor to enable 100% deterministic unit testing.
