# Implementation Tasks: Catálogo Canónico de Errores Backend y Mapeo MVI
**Change ID**: `KILOMENOS-18`

## Phase 1: Specifications & Architecture (Product Owner & Architect)
- [x] Refine Given/When/Then Acceptance Criteria in delta specification
- [x] Complete Technical Design with MVI state definitions (`design.md`)
- [x] Author ADR-KILOMENOS-18 detailing error catalog mapping matrix

## Phase 2: Implementation (Senior Android Developer)
- [x] Expand `KmError` in `:core:domain` with official catalog codes
- [x] Upgrade `ErrorMapper.kt` in `:core:infrastructure` to support canonical codes and fields
- [x] Add localized string resources in `values/strings.xml` and `values-en/strings.xml` in `:core:ui`
- [x] Update `ErrorExtensions.kt` in `:core:ui` to map all catalog errors to `TextProvider`

## Phase 3: Independent Verification (QA Engineer)
- [x] Implement unit tests in `ErrorMapperTest.kt` verifying all catalog categories
- [x] Implement unit tests in `ErrorExtensionsTest.kt` verifying TextProvider mapping
- [x] Verify clean compilation with `./gradlew :app:assembleDevDebug`
- [x] Execute unit tests in `:core:infrastructure` and `:core:ui`
