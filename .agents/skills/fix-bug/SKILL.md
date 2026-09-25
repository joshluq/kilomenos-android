---
name: fix-bug
description: Surgical root-cause investigation and regression-proof bugfix protocol across platforms. Diagnoses errors, authors a failing reproduction test, applies minimal resilient fixes, and verifies system integrity.
---
# Fix Bug -- Surgical Triage & Regression Defense Pipeline

This skill executes a **systematic root-cause investigation** to diagnose and eradicate software defects across KiloMenos platforms (Android, Supabase Backend, Web). It strictly rejects cosmetic band-aids, premature workarounds, or silent exception suppression in favor of definitive architectural resolution.

---

## Diagnostic Tooling Delegation Matrix

`fix-bug` orchestrates the universal triage and fix lifecycle, while delegating diagnostic tooling to the active platform skills:

| Platform Target | Active Profile | Platform Diagnostic Skills | Diagnostic Utilities & Strategies |
|---|---|---|---|
| **Android (Mobile)** | `android` | `android-quality`, `senior-debugging-engineer` | `logcat_triage.py`, `crash_listener.py` (live/dump logcat), Compose recomposition triage |
| **Backend (Cloud / DB)** | `backend` | `supabase-db-triage`, `senior-debugging-engineer` | PostgreSQL error codes, RLS policy audit, Edge Function Deno logs, route parity audit |
| **Web (SSG / Portals)** | `web` | `web-lighthouse-seo` | Lighthouse CI audit, WCAG contrast analyzer, responsive viewport inspection |

---

## Step-by-Step Execution Protocol

When the user invokes `/fix-bug [description, log dump, or stacktrace]`:

### Phase 1: Root-Cause Triage (Symptom vs. Defect)
1. **Identify the Violated Invariant**: Determine which architectural contract, domain invariant, or security boundary from `AGENTS.md` was violated.
2. **Platform-Specific Execution Tracing**:
   - **Android**: Was an I/O operation executed on the Main dispatcher? Was a Navigation 3 scoping key omitted? Is a Composable state mutation unhoisted?
   - **Backend**: Was Row Level Security bypassed? Is a database migration non-transactional? Did an Edge Function fail CORS headers or JWT validation? Is an FCM token dead or unpruned?
   - **Web**: Is an interactive element inaccessible via keyboard/screen reader? Is a client-side script bloating the initial paint?
3. **Isolate the Trigger from the Root Cause**: Separate the immediate symptom (e.g. `NullPointerException`, `401 Unauthorized`, `LCP > 2.5s`) from the underlying defect in state, contract, or architecture.

### Phase 2: Author the Reproduction Test (Red Phase)
1. **Pre-Fix Imperative**: Before modifying production implementation code, author an automated test that deterministically reproduces the defect.
   - **Android**: Author a failing unit test in Kotlin (`[Feature]ViewModelTest`, `[UseCase]Test`) asserting the expected contract with Turbine/MockK.
   - **Backend**: Author an integration test in TypeScript (`tests/test_[feature].ts`) asserting the expected HTTP status code, database row mutation, or guard behavior.
   - **Web**: Author an automated test or assertion verifying the broken element, link, or Lighthouse audit rule.
2. **Verify Failure**: Run the test to ensure it fails cleanly (RED), proving the bug is captured.

### Phase 3: Surgical Correction (Green Phase)
1. Implement the **minimal, resilient, and backward-compatible** fix addressing the verified root cause.
2. **Negative Constraints Checklist**:
   - **NEVER** suppress errors with empty `try/catch` blocks or silent catch-all handlers.
   - **NEVER** alter public API contracts or domain interfaces without an approved ADR/Component Spec revision.
   - **NEVER** bypass Row Level Security or write raw privileged queries to circumvent permissions.
   - **NEVER** put business calculation math inside ViewModels, Composables, or HTTP handlers.

### Phase 4: Verification & Regression Immunity
1. **Execute Reproduction Test**: Verify that the reproduction test now passes cleanly (GREEN).
2. **Run Full Test Suite**: Execute the module/project test suite to ensure zero secondary regressions:
   - **Android**: `./gradlew :app:assembleDevDebug` + unit tests.
   - **Backend**: `npx tsc --noEmit` + test suite.
   - **Web**: `npm run build` + Lighthouse audit.
3. **Report Deliverable**:
   - **Root Cause Identified**: Clear technical explanation of the failure mechanism.
   - **Fix Applied**: Summary of files and logic modified.
   - **Regression Defense**: Automated test path safeguarding against future recurrence.
