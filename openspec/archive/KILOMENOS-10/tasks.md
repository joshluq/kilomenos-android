# Implementation Tasks: Integración Local-First de Notificaciones y Sincronización Remota
**Change ID**: `KILOMENOS-10`

## Phase 1: Specifications & Architecture (Product Owner & Architect)
- [x] Refine Given/When/Then Acceptance Criteria in delta specification
- [x] Author PRD and handoff payload (`handoffs/po_to_architect_FEAT-003.json`)
- [x] Complete Technical Design with MVI state definitions (`design.md`)
- [x] Validate architectural handoff payload against schema

## Phase 2: Implementation (Senior Android Developer)
- [x] Extend Room `NotificationEntity` and `NotificationDao` to support remote sync fields
- [x] Implement Supabase Edge Functions REST client in `:core:network`
- [x] Implement `SyncNotificationsWorker` with AndroidX WorkManager and connectivity constraints
- [x] Implement Optimistic UI updates in `NotificationRepositoryImpl`
- [x] Implement Pro Paywall banner handling on 403 `PREMIUM_REQUIRED` in `NotificationsListViewModel`
- [x] Implement FCM push handler for `REFRESH_ENTITLEMENTS` and remote notification ingest
- [x] Run linting and static analysis (`ktlint`, `detekt`) with 0 warnings

## Phase 3: Independent Verification (QA Engineer)
- [x] Map 100% of Acceptance Criteria (AC-01 to AC-08) to automated test methods
- [x] Execute Turbine Coroutine tests on StateFlow transitions
- [x] Execute Compose UI tests (`ComposeTestRule`, `assertIsDisplayed`)
- [x] Verify Quality Gate verdict PASS with 0 failures
