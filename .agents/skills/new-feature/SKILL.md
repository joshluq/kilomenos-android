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

## Step-by-Step Execution Protocol (Unified 2-Phase Orchestrator)

The lifecycle is driven by the unified orchestrator `scripts/workflow.py`, which enforces OpenSpec as the Single Source of Truth (SSOT) and eliminates documentation duplication:

### Phase 1: Planning, Scaffolding & Jira Refinement (PO & Architecture)
Initiated via user command (e.g. `"Refina <ticket>"` or `python scripts/workflow.py plan <TICKET_KEY>`):
1. **Execute Plan Orchestrator**:
   ```bash
   python scripts/workflow.py plan <TICKET_KEY> [--title "Feature Title"]
   ```
2. **What `workflow.py plan` accomplishes automatically**:
   - Ingests Jira requirements (or user request).
   - Scaffolds `openspec/changes/<CHANGE_ID>/` (`proposal.md`, `design.md`, `tasks.md`, `specs/delta_spec.md`).
   - Automatically derives and validates dual machine handoffs (`po_to_architect_<ID>.json`, `architect_to_dev_<ID>.json`) directly from OpenSpec.
   - Posts technical proposal as a structured comment to Jira (`[REFINAMIENTO TÉCNICO - PO & ARQUITECTURA]`) and transitions status to `"In Review"`.
3. **Mandatory Human-in-the-Loop (HITL) Review Gate (HALT)**:
   - **HALT EXECUTION IMMEDIATELY**.
   - Present proposal summary in chat.
   - **DO NOT create, modify, or delete any production code files** during this phase.
   - Await the user's explicit confirmation (`"Aprobado"` or `"Construye <TICKET_KEY>"`).

---

### Phase 2: Implementation & Developer Verification (Senior Developer)
Upon receiving user approval, resume development:
1. **Execute Build Orchestrator**:
   ```bash
   python scripts/workflow.py build <TICKET_KEY>
   ```
2. **What `workflow.py build` accomplishes automatically**:
   - **Halt Gate Enforcement**: Scans `proposal.md` for unresolved blocking questions (`Dudas Abiertas Bloqueantes`). Refuses to proceed if any question is `BLOQUEANTE` or pending.
   - Transitions Jira ticket to `"En curso"` (`In Progress`).
   - Marks Phase 1 specification tasks as completed in `tasks.md`.
   - Emits and validates `handoffs/dev_to_qa_<ID>.json`.
3. **Apply Platform-Specific Implementation Rules**:
   - **Android**: Consult `android-staff-engineer-compose`. Implement domain UseCases in `:core:domain`, ViewModel extending `ScreenViewModel`, stateless `Screen` with `CanvasKitTheme`, and update Navigation 3 triad.
   - **Backend**: Consult `supabase-db-triage` and `supabase-edge-functions`. Author atomic SQL migrations with mandatory RLS policies and serverless handlers with JWT & entitlement guards.
   - **Web**: Consult `web-lighthouse-seo` and `legal-compliance-audit`. Author semantic Astro SSG components with zero client JS bloat.
4. **Developer Verification & Unit Testing**:
   - **Android**: Run tests with Turbine/MockK (`[Feature]ViewModelTest.kt`, `[UseCase]Test.kt`). Run sanity check.
   - **Backend**: Run TypeScript compilation check (`tsc --noEmit`) and integration tests.
   - **Web**: Run build check and link integrity.

---

### Phase 3: QA Verification, Living Spec Archival & Final Delivery (QA Engineer)
1. **Execute Verify Orchestrator**:
   ```bash
   python scripts/workflow.py verify <TICKET_KEY> --verdict PASS
   ```
2. **What `workflow.py verify` accomplishes automatically**:
   - Verifies 100% Acceptance Criteria traceability (`AC-xx`) and Pre-Verdict Audit Checklist (Zero Ghost Code, failure branch coverage for `BR-xx`).
   - Generates `docs/qa/QA-REPORT-<ID>.md` and validated `handoffs/qa_verdict_<ID>.json`.
   - **Consolidates Living Specs & Archives OpenSpec**: Delta specs merged into `openspec/specs/`, change directory moved to `openspec/archive/<ID>/`.
   - Posts completion comment to Jira and transitions ticket to `"Listo"` (`Done`).

