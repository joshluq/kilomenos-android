# Implementation Tasks: Desacoplamiento de la Cancelación de Notificación de Viaje

**Change ID**: `KILOMENOS-6`  

---

## Phase 1: Specifications & Architecture (Product Owner & Architect)
- [x] Refine Given/When/Then Acceptance Criteria in PRD and Jira
- [x] Author ADR-KILOMENOS-6 and COMP-SPEC-KILOMENOS-6
- [x] Author and validate `architect_to_dev_KILOMENOS-6.json` against schema
- [x] Update OpenSpec change artifacts (`proposal.md`, `design.md`, `delta_spec.md`)

## Phase 2: Implementation (Senior Android Developer)
- [x] Add `dismissTripFinishedNotification()` to `TrackingServiceController` in `:core:domain`
- [x] Implement `DismissTripNotificationUseCase` in `:core:domain`
- [x] Update `LocationTrackingService` constant visibility to `internal` in `:core:tracking`
- [x] Implement `dismissTripFinishedNotification()` in `TrackingServiceControllerImpl` in `:core:tracking`
- [x] Register `DismissTripNotificationUseCase` binding in `UseCaseModule` in `:core:infrastructure`
- [x] Remove `Effect.DismissTrackingNotifications` from `Contract.kt` in `:feature:overview`
- [x] Remove `NotificationManager` handling in `OverviewScreen.kt` in `:feature:overview`
- [x] Update `OverviewViewModel` to inject and call `DismissTripNotificationUseCase` in `:feature:overview`
- [x] Author unit tests in `DismissTripNotificationUseCaseTest`
- [x] Update unit tests in `OverviewViewModelTest`
- [x] Validate compilation and developer unit test suite

## Phase 3: Independent Verification (QA Engineer)
- [x] Map 100% of Acceptance Criteria (AC-01 .. AC-04) to automated tests
- [x] Execute full application verification (`./gradlew :feature:overview:testDebugUnitTest :core:domain:test`)
- [x] Author `QA-REPORT-KILOMENOS-6.md`
- [x] Issue formal quality gate verdict in `handoffs/qa_verdict_KILOMENOS-6.json`
