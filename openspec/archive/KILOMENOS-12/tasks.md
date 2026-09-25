# Implementation Tasks: Mejoras en el Comportamiento y Lectura de Notificaciones
**Change ID**: `KILOMENOS-12`

## Phase 1: Specifications & Architecture (Product Owner & Architect)
- [x] Refine Given/When/Then Acceptance Criteria in delta specification
- [x] Author PRD and handoff payload (`handoffs/po_to_architect_FEAT-005.json`)
- [x] Complete Technical Design with MVI state definitions and UseCase architecture (`design.md`)
- [x] Validate architectural handoff payload against schema

## Phase 2: Implementation (Senior Android Developer)
- [x] Remove "Ver más" button from `NotificationPill.kt` in `:feature:notifications`
- [x] Implement direct navigation in `OverviewViewModel` and `OverviewScreen`: `PROJECTION` $\rightarrow$ `NavigateToProjection`, `SYSTEM` (Bluetooth) $\rightarrow$ `NavigateToOnboarding(isEdit = true)`, and mark as read
- [x] Define and implement `PublishNotificationIfUnreadUseCase` in `:core:domain` and `:core:infrastructure`
- [x] Bind `PublishNotificationIfUnreadUseCase` in `UseCaseModule.kt`
- [x] Update `OverviewViewModel` to delegate notification publication to `PublishNotificationIfUnreadUseCase`
- [x] Eliminate `setShowProjectionBanner` and `showProjectionBanner` across all modules
- [x] Author developer unit tests for `PublishNotificationIfUnreadUseCase`, `NotificationPill`, and `OverviewViewModel`
- [x] Run compilation and linting verification (`./gradlew :app:assembleDevDebug`)

## Phase 3: Independent Verification (QA Engineer)
- [x] Map 100% of Acceptance Criteria (AC-01 to AC-07) to automated test methods
- [x] Execute Compose UI semantics verification on `NotificationPill`
- [x] Verify Quality Gate verdict PASS with 0 failures
