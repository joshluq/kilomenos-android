---
name: new-feature
description: Platform-agnostic, spec-driven feature construction pipeline. Coordinates ingestion, OpenSpec proposals, HITL approval gates, and delegates implementation to the installed platform skills.
---
# New Feature -- Spec-Driven Multi-Stage Construction Pipeline

This skill executes the **rigorous specification, design, and implementation pipeline for new features** across KiloMenos platforms (Android, Supabase Backend, Web Portals). It enforces strict adherence to `AGENTS.md` and prevents architectural drift by separating concerns across specialized lifecycle roles.

---

## Architecture & Platform Delegation Matrix

`new-feature` acts as the **universal workflow orchestrator**. It standardizes requirements, proposals, and quality gates, while **delegating platform-specific architectural patterns and rules to the installed platform skills**:

| Platform Target | Active Profile | Platform Architecture Skills | Platform Quality & Testing Skills |
|---|---|---|---|
| **Android (Mobile)** | `android` | `android-staff-engineer-compose` (FoundationKit, MVI, Route/Screen, Navigation 3, CanvasKit) | `android-testing` (Turbine, MockK, Compose rules), `android-quality` (`:app:assembleDevDebug`, lint, logcat) |
| **Backend (Cloud / DB)** | `backend` | `supabase-db-triage` (PostgreSQL schemas, RLS, transactional migrations), `supabase-edge-functions` (Deno, JWT, FCM push) | `senior-debugging-engineer` (Service logs, Postgres error triage) |
| **Web (SSG / Portals)** | `web` | `web-lighthouse-seo` (Astro SSG, Tailwind, WCAG 2.1 AA accessibility), `legal-compliance-audit` (GDPR, Play Store deletion) | `web-lighthouse-seo` (Lighthouse performance/SEO >= 90) |

---

## Step-by-Step Execution Protocol

When a new feature request is initiated via chat command (e.g., `/new-feature [name]` or `"Refina <ticket>"`):

### Phase 1: Requirements Ingestion & OpenSpec Proposal Formulation
1. **Ticket Ingestion**: If linked to a Jira issue, retrieve requirements via `atlassian-bridge`:
   ```bash
   python scripts/atlassian_bridge.py fetch <TICKET_KEY> --scaffold-openspec
   ```
2. **OpenSpec Proposal Creation**:
   - Initialize proposal directory: `openspec/changes/<CHANGE_ID>/` via `python scripts/openspec_cli.py propose <CHANGE_ID> "<Title>"`.
   - **Acceptance Criteria (AC-xx)**: Formulate testable criteria strictly using **Given / When / Then** syntax.
   - **Technical Design & Contracts**: Define public interfaces and cross-platform data payloads (e.g., JSON schemas for FCM push, REST DTOs, or database schema deltas in `specs/delta_spec.md`).

### Phase 2: Human-in-the-Loop (HITL) Review Gate (MANDATORY HALT)
1. **Publish Refinement**: Post the technical proposal and acceptance criteria to Jira:
   ```bash
   python scripts/atlassian_bridge.py refine <TICKET_KEY> openspec/changes/<CHANGE_ID>/proposal.md --status "In Review"
   ```
2. **Strict Negative Constraint (Zero Code Before Approval)**:
   - **HALT EXECUTION IMMEDIATELY**.
   - Present a concise proposal summary to the user in chat.
   - **DO NOT create, modify, or delete any production code files** during this phase.
   - Await the user's explicit confirmation (`"Aprobado"` or `"Implementa <TICKET_KEY>"`).

### Phase 3: Domain & Business Logic Implementation
Upon receiving user approval, transition the ticket to in-progress (`python scripts/atlassian_bridge.py transition <TICKET_KEY> "En curso"`):
1. **Apply Platform-Specific Domain Rules**:
   - **Android**: Consult `android-staff-engineer-compose`. Implement domain UseCases in `:core:domain` (`UseCase<Input, Output>` or `FlowUseCase<Input, Output>`). Ensure domain purity (zero Android framework dependencies). Bind implementations in `:core:infrastructure` `UseCaseModule.kt`.
   - **Backend**: Consult `supabase-db-triage`. Write transactional SQL migrations (`BEGIN ... COMMIT`) with mandatory Row Level Security (RLS) policies. Implement Edge Function business logic under Deno/TypeScript.
   - **Web**: Consult `web-lighthouse-seo` and `legal-compliance-audit`. Define typed content collections (Markdown/MDX) and legal data policies.

### Phase 4: Presentation / Component / API Implementation
1. **Apply Platform-Specific Presentation Rules**:
   - **Android**: Consult `android-staff-engineer-compose`.
     * Create MVI contracts (`FeatureState : UiState`, `FeatureEvent : UiEvent`, `FeatureEffect : UiEffect`).
     * ViewModel extends `ScreenViewModel<State, Event, Effect>`.
     * Implement stateless `Screen` composable with `CanvasKitTheme` tokens and 4-Layer Visual Architecture.
     * Enforce the **Navigation 3 Triad**: update `:core:navigation`, feature route, and `:app:AppNavigation.kt`.
   - **Backend**: Consult `supabase-edge-functions`. Expose secure serverless HTTP handlers, enforce CORS headers, verify Supabase JWT auth, and integrate Firebase Admin SDK for push notifications.
   - **Web**: Consult `web-lighthouse-seo`. Build semantic Astro components with Tailwind CSS, ensuring responsive layouts and zero unnecessary client JavaScript.

### Phase 5: Automated Testing
1. **Apply Platform-Specific Test Suites**:
   - **Android**: Consult `android-testing`. Author unit tests with **Turbine** and **MockK** (`[Feature]ViewModelTest.kt`, `[UseCase]Test.kt`). Test UI composables with `ComposeContentTestRule`.
   - **Backend**: Run Deno unit tests for Edge Functions and execute pgTAP / PostgREST integration queries verifying RLS permissions.
   - **Web**: Run static linting, broken link checks, and accessibility assertions.

### Phase 6: Quality Gate Audit & Archival
1. **Execute Full Build / Compilation Verification**:
   - **Android**: Consult `android-quality`. Run `./gradlew :app:assembleDevDebug` to guarantee that all modules, Hilt bindings, and routes compile cleanly into the APK with zero errors. Run `sanity_check.py`.
   - **Backend**: Execute `deno check` and dry-run database migrations.
   - **Web**: Execute `npm run build` and run Lighthouse CI audits (guaranteeing >= 90 across Performance, Accessibility, Best Practices, and SEO).
2. **Archive OpenSpec & Finalize Ticket**:
   ```bash
   python scripts/openspec_cli.py archive <CHANGE_ID>
   python scripts/atlassian_bridge.py comment <TICKET_KEY> "Feature implementation completed and 100% verified."
   python scripts/atlassian_bridge.py transition <TICKET_KEY> "Listo"
   ```
