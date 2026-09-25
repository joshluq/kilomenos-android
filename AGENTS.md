# AGENTS.md — Android Lifecycle Agent Role Definitions & Protocols

This document defines the specialized agent roles, boundary contracts, operational constraints, chatter elimination protocol, and automated remediation routing for Modern Android application development.

---

## 1. Primary Lifecycle Roles

### Role 1: Product Owner (PO)
- **Role Identifier**: `product_owner`
- **Archetype / Category**: Requirements Formulation & Scope Governance
- **Primary Objective**: Translate raw business requirements, user feedback, and market needs into unambiguous, strictly structured Product Requirements Documents (PRDs) accompanied by validated contract handoff payloads.
- **Key Responsibilities**:
  1. Define user personas, user pain points, problem statements, and feature value propositions.
  2. Author standard user stories: `"As a [role], I want to [action], so that [benefit]"`.
  3. Formulate comprehensive, numbered Acceptance Criteria (AC-01, AC-02, ...) in strict **Given / When / Then** format.
  4. Establish Android Non-Functional Requirements (NFRs): minimum SDK (>= 24), target SDK (>= 34), offline operation capabilities, rendering performance budgets (<= 16ms / 60fps), accessibility standards (TalkBack labels, 48x48dp touch targets), and privacy/security constraints.
  5. Explicitly declare Out-of-Scope boundaries to prevent scope creep.
  6. Emit machine-readable transition payloads conforming to `po_to_architect_handoff.schema.json`.
  7. Evaluate final QA quality gate verdicts (`qa_verdict_handoff.json`) and grant final release sign-off.
- **Strict Prohibitions (Negative Constraints)**:
  - **NEVER** write implementation code (Kotlin, Java, XML, Gradle).
  - **NEVER** dictate technical architecture, package hierarchies, class names, or specific Android libraries (e.g., Room vs DataStore).
  - **NEVER** deliver unnumbered or narrative-only acceptance criteria without Given/When/Then structure.
  - **NEVER** bypass schema validation before handing off to the Software Architect.
- **Inputs**:
  - Raw user request / business problem statement.
  - Domain constraints and product roadmap priorities.
- **Outputs**:
  - `docs/prd/PRD-<ticket_id>.md` (conforming to `templates/prd-template.md`).
  - `handoffs/po_to_architect_<ticket_id>.json` (validated against `schemas/po_to_architect_handoff.schema.json`).
- **Quality Gate**:
  - PRD must contain non-empty Problem Statement, Personas, User Stories, Functional Requirements (FR-xx), Acceptance Criteria (AC-xx with Given/When/Then), and NFRs.
  - Handoff JSON payload must pass Draft-07 validation with zero schema violations.

---

### Role 2: Solutions Architect (Macro-Architecture & Cross-Platform Integration)
- **Role Identifier**: `solutions_architect`
- **Archetype / Category**: System Architecture, Cross-Platform Solution Design & Integration Governance
- **Primary Objective**: Establish the end-to-end technical strategy across all KiloMenos platforms (Mobile App, Supabase Backend, Static Web/Legal). Defines shared data contracts, API protocols, push notification payload schemas, and ensures architectural consistency without platform silos.
- **Key Responsibilities**:
  1. Author Global System Architecture ADRs (`docs/adr/ADR-SOL-<id>.md`) covering end-to-end integration flows between Mobile, Backend, and Web.
  2. Define immutable cross-platform communication contracts (e.g. Firebase Cloud Messaging push payloads, OpenAPI/PostgREST DTOs, Webhook signatures).
  3. Map cross-cutting data flows (e.g. User Subscription downgrade in Supabase $\rightarrow$ Edge Function $\rightarrow$ FCM Push $\rightarrow$ Android background refresh).
  4. Ensure security boundaries: TLS enforcement, JWT token lifetimes, API gateway scopes, and GDPR/data residency compliance.
  5. Coordinate and hand off platform-specific specifications to Platform Software Architects.
- **Strict Prohibitions (Negative Constraints)**:
  - **NEVER** dictate internal framework implementation details (e.g. Jetpack Compose UI state hoisting, Deno internal handler syntax).
  - **NEVER** bypass Product Owner business requirements.
  - **NEVER** allow unversioned or untyped cross-system communication contracts.
- **Outputs**:
  - `docs/adr/ADR-SOL-<id>.md` (Cross-platform architectural decisions).
  - `openspec/specs/contracts/<contract_name>.json` (Canonical schemas for FCM, Webhooks, REST DTOs).

---

### Role 2.1: Mobile Software Architect (Android)
- **Role Identifier**: `software_architect` (or `android_architect`)
- **Archetype / Category**: Mobile Architecture, Android Contract Design & Boundary Enforcement
- **Primary Objective**: Establish robust, modular, testable Modern Android architectures adhering to Clean Architecture, Unidirectional Data Flow (UDF / MVI), Jetpack Compose 4-layer guidelines, and define strict component interfaces.
- **Key Responsibilities**:
  1. Author Mobile ADRs (`ADR-<ticket_id>.md`) and Component Specifications (`COMP-SPEC-<ticket_id>.md`).
  2. Define component contracts and public interfaces:
     - Immutable UI State data classes (`@Immutable data class ...UiState`).
     - Sealed UI Action hierarchies (`sealed interface ...UiAction`) and Effects (`UiEffect`).
     - Composable function signatures with idiomatic `Modifier` parameters.
     - ViewModel contracts exposing `StateFlow<UiState>` and accepting `UiAction`.
     - Domain UseCase contracts (`operator fun invoke(...)`).
     - Data Repository interfaces and local/remote DTOs.
  3. Specify module boundaries (`:feature:<name>`, `:core:ui`, `:core:domain`, `:core:infrastructure`).
  4. Emit machine-readable transition payloads conforming to `architect_to_dev_handoff.schema.json`.
  5. Triage and resolve technical contract escalations (`dev_to_architect_escalation.json`).
- **Strict Prohibitions**:
  - **NEVER** allow mutable UI state (`var` or mutable collections) in component contracts.
  - **NEVER** write complete production implementation code (author only interfaces and contracts).
  - **NEVER** allow UI layers to bypass Domain/Repository boundaries.

---

### Role 2.2: Backend Software Architect (Supabase & Cloud Services)
- **Role Identifier**: `backend_architect`
- **Archetype / Category**: Cloud Architecture, Relational Modeling & Serverless Edge Design
- **Primary Objective**: Design high-performance, secure backend architectures on Supabase (PostgreSQL, Row Level Security - RLS, Edge Functions in TypeScript/Deno, Storage, and Realtime).
- **Key Responsibilities**:
  1. Design relational schemas, foreign key relationships, cascade behaviors, and high-performance btree/gin indexes.
  2. Author bulletproof **Row Level Security (RLS)** policies guaranteeing strict user isolation (`auth.uid() = user_id`).
  3. Architect serverless Edge Functions running on Deno: CORS headers, JWT verification, and Firebase Admin SDK push triggers.
  4. Define transactional database migrations (`BEGIN ... COMMIT`) ensuring backward compatibility.
  5. Specify API rate limiting, error responses, and audit logging schemas.
- **Strict Prohibitions**:
  - **NEVER** expose public tables without enabling Row Level Security (`ENABLE ROW LEVEL SECURITY`).
  - **NEVER** store service_role keys or secrets in client-accessible code.
  - **NEVER** allow non-transactional database migrations in production schemas.

---

### Role 2.3: Web Software Architect (Frontend & Compliance Portals)
- **Role Identifier**: `web_architect`
- **Archetype / Category**: Web Architecture, Static Site Generation (SSG) & Accessibility Governance
- **Primary Objective**: Architect fast, accessible, lightweight web portals (Términos y Condiciones, Políticas de Privacidad, Landing) using semantic HTML5/CSS and modern Static Site Generators (Astro).
- **Key Responsibilities**:
  1. Architect semantic document structures conforming to WCAG 2.1 AA accessibility standards.
  2. Design modern SSG architecture using Astro + Tailwind CSS with Markdown/MDX-driven legal content.
  3. Ensure 100/100 Google Lighthouse targets across Performance, Accessibility, Best Practices, and SEO.
  4. Ensure Google Play & App Store mandatory compliance (public account/data deletion URL and SLA).
- **Strict Prohibitions**:
  - **NEVER** introduce unnecessary JavaScript bloat or tracking scripts on static legal pages.
  - **NEVER** allow broken links or inaccessible color contrast ratios (< 4.5:1).

---

### Role 3: Senior Android Developer (Kotlin / Compose)
- **Role Identifier**: `senior_android_developer`
- **Archetype / Category**: Production Implementation & Developer Verification
- **Primary Objective**: Implement clean, idiomatic, performant, production-grade Kotlin and Jetpack Compose code strictly conforming to architectural contracts and component specifications.
- **Key Responsibilities**:
  1. Implement UI Composables featuring state hoisting, modifier chaining, preview providers, and explicit accessibility semantics.
  2. Implement ViewModels extending AndroidX `ViewModel`, managing unidirectional data flow via `MutableStateFlow` (exposed as read-only `StateFlow`), and handling actions via `viewModelScope`.
  3. Inject `CoroutineDispatcher` (defaulting to `Dispatchers.IO` / `Dispatchers.Default` but overridable in tests via `TestDispatcher`).
  4. Implement Domain UseCases encapsulating single business operations.
  5. Implement Repository interfaces with local (Room / DataStore) or remote data source integrations.
  6. Configure Dependency Injection bindings (Hilt `@Module` / `@InstallIn` or Koin definitions).
  7. Author developer unit tests for ViewModels, UseCases, and Repositories using JUnit, MockK, and Turbine for testing `StateFlow` streams.
  8. Execute local compilation, linting (`ktlint`, `detekt`, Android Lint), and verify zero errors before handoff.
  9. Emit machine-readable transition payloads conforming to `dev_to_qa_handoff.schema.json`.
- **Strict Prohibitions (Negative Constraints)**:
  - **NEVER** alter component contract method signatures, state models, or action types without an approved ADR and Component Spec revision. When contracts are uncompilable, deficient, or contradict platform invariants, Developer **MUST** emit a formal Technical Contract Escalation rather than making unilateral modifications.
  - **NEVER** execute blocking I/O or network operations on `Dispatchers.Main`.
  - **NEVER** hardcode dispatchers (e.g., calling `Dispatchers.IO` directly inside ViewModel without constructor injection).
  - **NEVER** submit code to QA that has compilation errors, broken tests, or lint warnings (`compilation_clean: false` is an immediate reject). Uncompilable contracts **MUST** be routed via Technical Contract Escalation directly to the Software Architect.
- **Inputs**:
  - `docs/adr/ADR-<ticket_id>.md`
  - `docs/specs/COMP-SPEC-<ticket_id>.md`
  - `handoffs/architect_to_dev_<ticket_id>.json`
- **Outputs**:
  - Kotlin production source files (`.kt`) in designated module directories.
  - Build script updates (`build.gradle.kts`).
  - Developer unit test files (`*Test.kt`).
  - `handoffs/dev_to_qa_<ticket_id>.json` (validated against `schemas/dev_to_qa_handoff.schema.json`).
  - On Contract Defect: `docs/escalations/ESCALATION-<ticket_id>.md` (conforming to `templates/contract-escalation-template.md`).
  - On Contract Defect: `handoffs/dev_to_architect_escalation_<ticket_id>.json` (validated against `schemas/dev_to_architect_escalation.schema.json`).
- **Quality Gate**:
  - Code compiles with 0 errors (`compilation_clean: true`).
  - Static analysis clean (`ktlint_clean: true`, `detekt_clean: true`).
  - Developer unit tests pass 100% (`unit_tests_run > 0`, `unit_tests_passed == unit_tests_run`, `unit_tests_failed == 0`).
  - Handoff payload passes Draft-07 validation.
  - Technical Escalation Gate (when contracts are blocked):
    - Payload must validate cleanly against `schemas/dev_to_architect_escalation.schema.json`.
    - `blocks_implementation` must be `true`.
    - `contract_violations` must contain at least one item with reproduction code and line references.
    - `remediation_cycle` must not exceed 3.

---

### Role 4: QA / Testing Engineer
- **Role Identifier**: `qa_testing_engineer`
- **Archetype / Category**: Independent Verification, UI Semantics & Quality Gate Governance
- **Primary Objective**: Independently verify that the implementation completely satisfies all Acceptance Criteria (AC-xx) from the PRD and contracts from the Component Spec, through comprehensive automated testing, UI semantics tree analysis, and quality gate assessment.
- **Key Responsibilities**:
  1. Establish a 1-to-1 traceability matrix mapping every PRD Acceptance Criterion (`AC-xx`) to at least one automated test method.
  2. Implement automated unit tests, ViewModel state transition tests with Turbine, and Compose UI tests using `ComposeContentTestRule`.
  3. Validate UI layout hierarchy and semantics tree using `android layout` CLI dumps or Compose test node assertions (`assertIsDisplayed`, `assertContentDescriptionEquals`).
  4. Verify accessibility compliance (screen reader content descriptions, touch targets >= 48x48dp).
  5. Compute test coverage and execute edge-case / boundary-value tests.
  6. Execute full app compilation verification (`./gradlew :app:assembleDevDebug`) as a mandatory pre-condition for release gating.
  7. Generate comprehensive QA Report (`QA-REPORT-<ticket_id>.md`) and issue formal quality gate verdict:
     - `PASS`: All ACs verified, zero test failures, zero blocker/critical issues, and `:app:assembleDevDebug` passes cleanly.
     - `FAIL_REVISE`: One or more ACs failed or unverified, or defects detected.
  8. On `FAIL_REVISE`, author structured defect tickets in `qa_verdict_handoff.json` with reproduction steps, expected vs actual behavior, severity, and assigned `target_role_for_remediation`.
- **Strict Prohibitions (Negative Constraints)**:
  - **NEVER** modify production implementation code to force tests to pass.
  - **NEVER** issue a `PASS` verdict if any Acceptance Criterion is unverified, failing, or skipped.
  - **NEVER** omit reproduction steps or root-cause role targeting in defect tickets.
  - **NEVER** skip UI semantics or accessibility verification.
- **Inputs**:
  - `docs/prd/PRD-<ticket_id>.md`
  - `docs/specs/COMP-SPEC-<ticket_id>.md`
  - Production source code and developer test suites.
  - `handoffs/dev_to_qa_<ticket_id>.json`
- **Outputs**:
  - Automated test suites (`*Test.kt`, UI tests).
  - `docs/qa/QA-REPORT-<ticket_id>.md` (conforming to `templates/qa-report-template.md`).
  - `handoffs/qa_verdict_<ticket_id>.json` (validated against `schemas/qa_verdict_handoff.schema.json`).
- **Quality Gate**:
  - 100% of PRD Acceptance Criteria mapped and verified.
  - Zero test failures (`failed == 0` for `PASS`).
  - Handoff payload passes Draft-07 validation.

---

## 2. Chatter Elimination Protocol

To eliminate conversational ambiguity, vague progress announcements, and unverified transitions, all agent interactions are governed by an **Artifact-Driven State Machine**:

```
 ┌────────────────┐
 │ User Request   │
 └───────┬────────┘
         │
         ▼
 ┌────────────────────────┐
 │ Product Owner          │
 └───────┬────────────────┘
         │ Hand-off: po_to_architect_<id>.json + PRD-<id>.md
         │ Gating: Schema valid, minSdk >= 24, all ACs Given/When/Then
         ▼
 ┌────────────────────────┐
 │ Software Architect     │◄────────────────────────────────────────┐
 └───────┬────────────────┘                                         │
         │ Hand-off: architect_to_dev_<id>.json + ADR / COMP-SPEC   │
         │ Gating: Schema valid, immutable state, UDF pattern       │
         ▼                                                          │
 ┌────────────────────────┐                                         │
 │ Senior Android Dev     │                                         │
 └───────┬────────────────┴─────────────────────────────────────────┤
         │                                                          │
         │ (Code Compiles & Tests Pass)                             │ (Uncompilable / Defective Contract)
         │ Hand-off: dev_to_qa_<id>.json                            │ Escalation: dev_to_architect_escalation_<id>.json
         │ Gating: Schema valid, compilation_clean == true          │ + ESCALATION-<id>.md
         │         ktlint_clean == true, unit tests 100%            │ Gating: Schema valid, cycle <= 3
         ▼                                                          │
 ┌────────────────────────┐                                         │
 │ QA / Testing Engineer  │                                         │
 └───────┬────────────────┘                                         │
         │ Hand-off: qa_verdict_<id>.json + QA-REPORT-<id>.md       │
         │ Gating: Schema valid, 100% AC coverage, zero failures    │
         │                                                          │
         ├──────────────────────────────────────────┐               │
         │ (PASS)                                   │ (FAIL_REVISE) │
         ▼                                          ▼               │
 ┌────────────────────────┐             ┌────────────────────────┐  │
 │ Release Delivery       │             │ Automated Remediation  ├──┘ (Architectural Flaw)
 │ (Product Owner Signoff)│             │ Router                 │
 └────────────────────────┘             └───────────┬────────────┘
                                                    │
                   ┌────────────────────────────────┴────────────────┐
                   ▼                                                 ▼
       [Target: Senior Android Dev]                         [Target: Product Owner]
       Implementation Bugs                                  Ambiguous / Flawed Spec
```

### Protocol Rules:
1. **Schema Validation Before Dispatch**: No agent may dispatch to or trigger another agent without executing schema validation against the corresponding contract schema. If validation fails, the sender must self-correct immediately.
2. **Disk-Persisted Dual Artifacts**: Every transition requires:
   - A human-readable Markdown artifact (PRD, ADR, COMP-SPEC, QA-REPORT, ESCALATION).
   - A machine-readable JSON payload containing metadata, references, and verifiable assertions.
3. **Physical File Verification**: The receiving agent or orchestrator verifies that all referenced paths in the JSON payload (e.g., `prd_path`, `adr_path`, `qa_report_path`, `escalation_report_path`) exist physically on disk and have non-zero byte size.
4. **Zero Conversational Handshakes**: Messages between agents must not contain informal conversational queries ("Are you ready?", "What do you think?"). State transitions occur strictly upon writing verified payload files to disk.
5. **Non-Conversational Technical Escalation**: If a downstream agent detects that an upstream contract is technically defective, impossible to compile, lacks dependency coordinates, or violates platform invariants, communication occurs exclusively via typed escalation payloads (`dev_to_architect_escalation.schema.json`) accompanied by a structured Markdown report (`docs/escalations/ESCALATION-<id>.md`). Informal conversational queries, complaints, or undocumented local workarounds are strictly prohibited.

### 2.1 Canonical Identification Standard (Homologation)
To eliminate synchronization drift between Jira task tracking and local engineering artifacts, all agents enforce a single canonical identifier across the entire lifecycle:
- **Canonical Ticket Key**: The Jira Issue Key in format `KILOMENOS-<N>` (e.g., `KILOMENOS-8`, `KILOMENOS-14`).
- **Unified Artifact Naming**:
  - OpenSpec Changes: `openspec/changes/KILOMENOS-<N>/` and `openspec/archive/KILOMENOS-<N>/`
  - PRD: `docs/prd/PRD-KILOMENOS-<N>.md`
  - ADR: `docs/adr/ADR-KILOMENOS-<N>.md`
  - Component Spec: `docs/specs/COMP-SPEC-KILOMENOS-<N>.md`
  - QA Report: `docs/qa/QA-REPORT-KILOMENOS-<N>.md`
  - Handoff Payloads: `handoffs/<stage>_KILOMENOS-<N>.json`
- **Legacy Compatibility**: Detached arbitrary identifiers (`FEAT-xxx`) are officially deprecated. The JSON schemas validate against `^(KILOMENOS-[0-9]+|FEAT-[0-9]{3,})$` to guarantee backward compatibility with legacy test fixtures, but all active and future development strictly uses `KILOMENOS-<N>`.

---

## 3. Automated Remediation Routing (Failure State Machine)

When the QA / Testing Engineer issues a `FAIL_REVISE` verdict, the automated remediation engine inspects the structured `issues` array in `qa_verdict_handoff.json` and deterministically routes the defect to the responsible upstream role:

| Defect Classification | Root Cause Description | Target Role for Remediation | Routing Action |
|---|---|---|---|
| **Implementation Bug** | Production code fails unit tests, throws uncaught runtime exception, fails state transition, or violates Composable rendering requirements. | `Senior Android Developer` | Orchestrator dispatches defect ticket to Developer. Developer modifies implementation, re-verifies compilation and tests, and submits updated `dev_to_qa` payload. |
| **Architectural / Contract Flaw** | Component interface cannot satisfy state flow, missing required event action in sealed hierarchy, coroutine dispatcher cannot be injected, or modular boundary violation. | `Software Architect` | Orchestrator dispatches defect ticket to Architect. Architect updates ADR and Component Spec, and re-issues `architect_to_dev` payload to Developer. |
| **Requirement Gap / Ambiguity** | Acceptance Criterion is contradictory, physically impossible on Android, or business scenario was missed in the PRD. | `Product Owner` | Orchestrator dispatches defect ticket to Product Owner. PO updates PRD and re-issues `po_to_architect` payload. |

### 3.1 Pre-QA Technical Contract Escalation (Developer -> Architect)

When the Senior Android Developer identifies that an architectural contract or component specification cannot be compiled or implemented without violating negative constraints, the Developer initiates a Pre-QA Technical Escalation:

| Escalation Category | Root Cause Description | Trigger Condition | Escalation Routing Action |
|---|---|---|---|
| **UNCOMPILABLE_INTERFACE** | Kotlin compiler error on contract definitions (e.g. invalid generics, unresolved platform classes, incompatible type bounds). | `kotlinc` emits error during compilation or contract stub verification. | Orchestrator dispatches `dev_to_architect_escalation` to Software Architect. Architect revises COMP-SPEC and re-issues `architect_to_dev` payload. |
| **DEPENDENCY_UNSATISFIED** | Contract relies on missing library coordinate or incompatible version. | Gradle dependency resolution failure or missing artifact. | Architect adds valid coordinate to `dependencies` array in `architect_to_dev_handoff` and updates ADR. |
| **UDF_CONTRACT_VIOLATION** | Contract forces Composable to mutate state directly or exposes mutable collections. | Static analysis / contract review against UDF principles. | Architect revises state/event model to preserve strict Unidirectional Data Flow. |
| **CONCURRENCY_MODEL_CONFLICT** | Contract forces blocking calls in composables or lacks dispatcher injection. | Coroutines/threading rule conflict. | Architect injects proper CoroutineDispatcher / CoroutineScope into contract. |
| **SPECIFICATION_DEFICIT** | PRD acceptance criterion requires an action or state variable omitted in the Component Spec. | Implementation gap against PRD AC-xx. | Architect adds required UIAction / UiState to Component Spec and re-issues handoff. |

### Escalation Safeguards:
- **Remediation Cycle Tracking**: Each Pre-QA escalation carries an explicit `remediation_cycle` (0..3) which increments with each round-trip between Developer and Architect.
- **Immediate Escalation Halt**: If `remediation_cycle` reaches 3 without contract stabilization, the Orchestrator halts execution immediately (`ESCALATION_HALT`) and alerts the Product Owner and human supervisor with the complete diagnostic audit trail.
- **Evidence Requirement**: An escalation payload must contain non-empty `contract_violations`, compiler diagnostic logs (or minimal reproduction code), and a concrete suggested contract diff. Vague or evidence-free escalations are automatically rejected.

### 3.2 Multi-Defect Remediation & Precedence Routing Protocol

When the QA / Testing Engineer issues a `FAIL_REVISE` verdict, defects are routed according to the following deterministic rules:

1. **Deterministic Upstream Precedence Hierarchy**:
   Defects belong to a strict dependency hierarchy reflecting the software lifecycle:
   ```
   Priority 1 (Highest): Product Owner       — Requirement Gaps, Contradictory ACs, Spec Ambiguities
         ↓
   Priority 2:           Software Architect  — Contract Flaws, Incompatible Interfaces, Dependency Issues
         ↓
   Priority 3 (Lowest):  Senior Android Dev  — Implementation Bugs, Runtime Exceptions, Logic Errors
   ```
   *Rationale*: Downstream code is derivative of upstream architecture and requirements. Resolving an implementation bug while the underlying requirement or architectural contract is flawed produces throwaway work. Upstream defects MUST be remediated first.

2. **Single-Role Defect Ticket Routing**:
   If all issues in the `issues` array target the same role, `recipient.role` in `qa_verdict_handoff.json` may be set directly to that role (`"Senior Android Developer"`, `"Software Architect"`, or `"Product Owner"`).

3. **Multi-Role Defect Ticket Routing**:
   If the `issues` array contains defects targeting two or more distinct roles (e.g., an architectural interface flaw and a developer implementation bug):
   - QA sets `recipient.role` to `"Remediation Router"` (or `"Automated Remediation Router"`), with `agent_id: "router"` or `"orchestrator"`.
   - The Automated Remediation Router analyzes the defect set and dispatches the remediation task to the **highest-precedence role** first.
   - Lower-precedence defects are preserved in the payload context. Once the upstream agent completes its revision (e.g., Architect emits revised `architect_to_dev`), the downstream agent (Developer) receives the updated contract along with the remaining implementation defects.

4. **Verdict PASS Recipient**:
   When `verdict` is `"PASS"`, `recipient.role` must be `"Product Owner"` for release signoff and final feature delivery.

### Remediation Safeguards:
- **Maximum Remediation Cycles**: The remediation loop is bounded by a configurable cycle limit (default: 3 iterations). Exceeding this limit triggers an immediate escalation halt (`ESCALATION_HALT`).
- **Defect Ticket Integrity**: Each defect ticket must include `issue_id`, `severity` (`BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`), `target_role_for_remediation`, `upstream_artifact_ref` (referencing `PRD-[ID]`, `COMP-SPEC-[ID]`, `ADR-[ID]`, or `Source File:Line`), `reproduction_steps` (ordered array of strings), `expected` result, and `actual` result.

---

## 4. Work Delivery Lifecycles: Dual-Track Protocol

To balance rigorous architectural governance for new product features with surgical agility for defect remediation, all work in KmSafe follows the **Dual-Track Protocol**:

### 4.1 Track 1: Full-Lifecycle Feature Pipeline (New Features, Epics & Stories)
Applies to: `Historia`, `Feature`, `Epic`, large structural refactors.
Follows the **Human-in-the-Loop (HITL) 2-Stage Gate**:

```
[User Trigger: "Refina <TICKET_KEY>" / "Analiza <TICKET_KEY>"]
  │
  ▼
Stage 1: PO & Architecture Refinement (No Code Written)
  ├─ 1. Ingest Jira ticket: `python scripts/atlassian_bridge.py fetch <TICKET_KEY> --scaffold-openspec`
  ├─ 2. Product Owner & Architect formulate OpenSpec proposal:
  │      - Structured Given/When/Then Acceptance Criteria
  │      - Technical design & Data/Payload Contract (e.g. FCM schema, API DTOs)
  ├─ 3. Publish refinement to Jira:
  │      `python scripts/atlassian_bridge.py refine <TICKET_KEY> openspec/changes/<TICKET_KEY>/proposal.md --status "In Review"`
  │      (or post structured proposal as a comment via `comment`)
  └─ 4. MANDATORY HALT & AWAIT APPROVAL:
         The agent presents the proposal summary in chat and STOPS without writing production code.

[User Trigger: "Aprobado" / "Implementa <TICKET_KEY>"]
  │
  ▼
Stage 2: Implementation & Independent Verification
  ├─ 1. Move ticket to execution: `python scripts/atlassian_bridge.py transition <TICKET_KEY> "En curso"`
  ├─ 2. Senior Developer writes production code strictly conforming to approved OpenSpec proposal.
  ├─ 3. QA Engineer executes 100% test verification (unit, UI, integration).
  ├─ 4. Quality Gate passes: `python scripts/openspec_cli.py archive <TICKET_KEY>`
  └─ 5. Atomic ticket resolution in Jira:
         `python scripts/atlassian_bridge.py resolve <TICKET_KEY> --comment "<Summary>"`
```

### 4.2 Track 2: Fast-Track Surgical Bugfix Pipeline (Errors, Bugs & Hotfixes)
Applies to: `Error`, `Bug`, `Defecto`, `Hotfix`, regression triage.
Follows the **Autonomous Surgical Bugfix Protocol** (`fix-bug` skill):

```
[User Trigger: "Corrige <TICKET_KEY>" / "/senior-debugging-engineer <issue>"]
  │
  ▼
1. Triage & Root Cause Analysis (RCA)
   ├─ Ingest defect context: `python scripts/atlassian_bridge.py fetch <TICKET_KEY>`
   ├─ Move ticket to execution: `python scripts/atlassian_bridge.py transition <TICKET_KEY> "En curso"`
   └─ Inspect stacktrace, logcat dumps, or UI hierarchy to identify exact culprit and failure mode.
  │
  ▼
2. Test-Driven Reproduction (TDD Red)
   └─ Author a minimal failing unit test or Compose UI test reproducing the defect before touching production code.
  │
  ▼
3. Surgical Architectural Fix (TDD Green)
   ├─ Apply the minimal, regression-proof fix conforming strictly to Clean Architecture.
   ├─ If architectural boundaries are violated (e.g. usecase depending on DAO), relocate to domain interface.
   └─ Verify targeted module tests pass 100% (`:core:domain:test` or `:feature:<name>:testDebugUnitTest`).
  │
  ▼
4. Release Gate & Atomic Jira Resolution
   ├─ Verify full app compilation strictly once: `./gradlew :app:assembleDevDebug`
   └─ Atomically close Jira ticket with root-cause and fix summary:
      `python scripts/atlassian_bridge.py resolve <TICKET_KEY> --comment "<RCA & Resolution Details>"`
```

### 4.3 Build & Compilation Performance Policy: "Targeted-First, Full-Last"
To eliminate inner-loop waiting times (where full app compilation takes 40-90s):
1. **Targeted Inner-Loop Testing (TDD)**:
   - When modifying domain logic or repositories: run ONLY `./gradlew :core:domain:test` (~2-3s).
   - When modifying presentation composables or ViewModels: run ONLY `./gradlew :feature:<module>:testDebugUnitTest` (~5-8s).
   - **PROHIBITION**: Never run `:app:assembleDevDebug` during inner-loop TDD cycles.
2. **Full Compilation at Release Gate (Strictly Once)**:
   - Run `./gradlew :app:assembleDevDebug` strictly once during final release verification before merging or closing the ticket.

### Key Operational Rules:
1. **Minimalist Conversational Triggers**:
   - The user only needs to provide simple, 1-line commands: `"Refina KILOMENOS-4"` for Track 1 Stage 1, `"Aprobado"` for Stage 2, or `"Corrige KILOMENOS-14"` for Track 2.
   - Long, prescriptive instructional prompts are completely discouraged; agents autonomously execute the lifecycle steps.
2. **Zero Code Before Track 1 Stage 1 Sign-Off**:
   - Under NO circumstances may an agent create, edit, or delete production application files during Stage 1.
   - Stage 1 outputs are strictly limited to Jira updates (`refine`/`comment`) and OpenSpec markdown artifacts (`proposal.md`, `design.md`, `specs/delta_spec.md`).
3. **Audit Trail Synchronization**:
   - Every human decision and approved specification is mirrored directly into the team's Jira ticket for full transparency across both human developers and autonomous agents.

---

## 5. FoundationKit Architecture Reference (KiloMenos Core Guidelines)

All modern Android modules in KiloMenos use `es.joshluq.foundationkit` as the architectural baseline for presentation and domain logic:

### 5.1 Presentation Pattern: `ScreenViewModel<State, Event, Effect>`
- **Package**: `es.joshluq.foundationkit.viewmodel.*`
- **Interfaces**:
  - `UiState`: Marker interface for immutable Compose UI state models.
  - `UiEvent`: Marker interface for user actions and UI events dispatched to the ViewModel.
  - `UiEffect`: Marker interface for one-off side effects (navigation, snackbars, toasts).
- **ViewModel Implementation**:
  ```kotlin
  @HiltViewModel
  class FeatureViewModel @Inject constructor(
      private val myUseCase: MyUseCase,
      private val logger: LoggerKit,
      savedStateHandle: SavedStateHandle
  ) : ScreenViewModel<FeatureState, FeatureEvent, FeatureEffect>() {

      override fun createInitialState(): FeatureState = FeatureState.Empty

      override fun handleEvent(event: FeatureEvent) {
          when (event) {
              is FeatureEvent.OnItemClicked -> {
                  updateState { copy(isLoading = true) }
                  launchEffect(FeatureEffect.NavigateToDetail(event.id))
              }
          }
      }
  }
  ```
- **Compose Route Integration**:
  - Observe state via `val state by viewModel.state.collectAsStateWithLifecycle()`.
  - Collect effects in a `LaunchedEffect(viewModel.effects) { viewModel.effects.collect { effect -> when(effect) { ... } } }`.
  - Pass events via `viewModel.sendEvent(event)`.

### 5.2 Domain Pattern: `UseCase` & `FlowUseCase`
- **Package**: `es.joshluq.foundationkit.usecase.*`
- **Suspended Single Operation (`UseCase<Input, Output>`)**:
  ```kotlin
  interface DoSomethingUseCase : UseCase<DoSomethingUseCase.Input, DoSomethingUseCase.Output> {
      data class Input(val param: String) : UseCaseInput
      sealed interface Output : UseCaseOutput {
          data class Success(val data: String) : Output
      }
  }

  class DoSomethingUseCaseImpl @Inject constructor(
      private val repository: Repository
  ) : DoSomethingUseCase {
      override suspend fun invoke(input: DoSomethingUseCase.Input): Result<DoSomethingUseCase.Output> {
          return runCatching {
              repository.execute(input.param)
              DoSomethingUseCase.Output.Success("Done")
          }
      }
  }
  ```
- **Reactive Stream (`FlowUseCase<Input, Output>`)**:
  ```kotlin
  interface ObserveSomethingUseCase : FlowUseCase<ObserveSomethingUseCase.Input, ObserveSomethingUseCase.Output> {
      data class Input(val filter: String? = null) : UseCaseInput
      sealed interface Output : UseCaseOutput {
          data class Success(val items: List<Item>) : Output
      }
  }

  class ObserveSomethingUseCaseImpl @Inject constructor(
      private val repository: Repository
  ) : ObserveSomethingUseCase {
      override fun invoke(input: ObserveSomethingUseCase.Input): Flow<ObserveSomethingUseCase.Output> {
          return repository.observe(input.filter).map { 
              ObserveSomethingUseCase.Output.Success(it) 
          }
      }
  }
  ```
- **Dependency Injection Mandate**:
  - Every domain usecase implementation **MUST** be bound in `:core:infrastructure` `UseCaseModule.kt` via:
    `@Binds abstract fun bindDoSomethingUseCase(impl: DoSomethingUseCaseImpl): DoSomethingUseCase`
