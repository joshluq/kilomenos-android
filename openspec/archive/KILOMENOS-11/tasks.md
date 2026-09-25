# Implementation Tasks: Formato Canónico UUID v4 y Deduplicación de Notificaciones
**Change ID**: `KILOMENOS-11`

## Phase 1: Specifications & Architecture (Product Owner & Architect)
- [x] Refine Given/When/Then Acceptance Criteria in delta specification
- [x] Author PRD and handoff payload (`handoffs/po_to_architect_FEAT-004.json`)
- [x] Complete Technical Design with MVI state definitions and deduplication architecture (`design.md`)
- [x] Validate architectural handoff payload against schema

## Phase 2: Implementation (Senior Android Developer)
- [x] Update notification generation in `OverviewViewModel` to use `UUID.randomUUID().toString()` and store semantic keys in `data`
- [x] Add semantic key deduplication in Room DAO / Entity for local alerts
- [x] Ensure `NotificationSyncItemDto` and repository sync pass the `data` map
- [x] Implement defensive handling for legacy non-UUID IDs in `NotificationRepositoryImpl`
- [x] Author developer unit tests for UUID conformance, data serialization, and deduplication
- [x] Run compilation and linting verification (`./gradlew :app:assembleDevDebug`)

## Phase 3: Independent Verification (QA Engineer)
- [x] Map 100% of Acceptance Criteria (AC-01 to AC-06) to automated test methods
- [x] Verify PATCH read and POST sync with valid UUID against mocked and integration flows
- [x] Verify Quality Gate verdict PASS with 0 failures
