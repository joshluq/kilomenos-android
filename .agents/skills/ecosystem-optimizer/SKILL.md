---
name: ecosystem-optimizer
description: Audits, optimizes, and prevents regressions in the agentic ecosystem. Analyzes prompt context budgets, detects cross-platform domain leaks, enforces schema integrity, and validates Hub-Spoke synchronization.
---

# Ecosystem Optimizer — AgentOps, Prompt Compression & System Health

The **Ecosystem Optimizer** is a specialized AgentOps and Prompt Engineering capability designed to maintain, compress, and continuously improve the multi-agent development ecosystem across all platforms (Android, Backend, and Web).

As an agentic architecture grows, system prompts, living specs, and handoff contracts accumulate drift, redundant verbiage, and accidental cross-platform coupling. This skill provides automated audits, actionable compression heuristics, and regression guardrails.

---

## 1. Core Responsibilities & Heuristics

### 1.1 Context Budgeting & Anti-Bloat
- **Budget Thresholds**:
  - `copilot_instructions.md`: $\le$ 800 words (~1,000 tokens). Focus strictly on top-level golden rules and platform-specific invariants.
  - `AGENTS.md`: $\le$ 3,500 words (~4,500 tokens). Defines roles, schemas, negative constraints, and handoff state machine.
  - `SKILL.md` (per skill): $\le$ 1,200 words (~1,500 tokens). Highly focused, runnable instructions.
- **Compression Rule**: Eliminate verbose conversational prose, decorative emojis, and duplicate boilerplate across role instructions. Prefer numbered directives and table structures.

### 1.2 Platform Domain Isolation (Zero-Leakage Invariant)
Each platform profile must maintain strict technical boundaries without contamination:
- **Android Spoke (`KmSafe`)**:
  - Allowed: Kotlin, Jetpack Compose, FoundationKit, Coroutines, Flow, Hilt, Turbine, MockK.
  - Forbidden: Supabase direct Deno imports, Node.js server syntax, Hono routers, raw PostgreSQL migrations.
- **Backend Spoke (`kilomenos`)**:
  - Allowed: Hono v4, Supabase Edge Functions, PostgreSQL, RLS policies, FCM HTTP v1, Deno, TypeScript.
  - Forbidden: Compose ViewModels, Android UI state hoisting, Gradle dependencies, `:app:assembleDevDebug`.
- **Web Spoke (`kilomenos-web`)**:
  - Allowed: Astro SSG, HTML/CSS, Tailwind, SEO, Lighthouse audit, legal compliance.
  - Forbidden: Android Compose code, server-side database connection pools.

### 1.3 Schema & Contract Determinism
- Validates all handoff payloads against JSON Schema Draft-07.
- Asserts that every change in `openspec/changes/` preserves bidirectional links between PO requirements (`FR-xx`, `AC-xx`), Architectural specifications (`ADR-xx`, `COMP-SPEC-xx`), and QA test assertions.

### 1.4 Hub-to-Spoke Synchronization
- Audits that satellite repositories (`KmSafe`, `kilomenos`) are in sync with the canonical schemas, shared skills, and workflow orchestrators in the base hub repository (`android_agentic_ecosystem`).

---

## 2. Available Automated Tooling

The skill is backed by automated CLI tools located in `skills/ecosystem-optimizer/scripts/`:

### 2.1 Unified Ecosystem Health Audit
Run a comprehensive diagnostic of context budgets, platform isolation, and schema integrity:
```bash
python skills/ecosystem-optimizer/scripts/ecosystem_audit.py
```
Or via the unified workflow CLI:
```bash
python scripts/workflow.py audit
```

Options:
- `--target <path>`: Directory of the project or spoke to audit (default: current workspace).
- `--profile <android|backend|web|all|auto>`: Explicit profile to evaluate.
- `--json`: Output results in structured machine-readable JSON.
- `--report <path>`: Generate a Markdown health report.

### 2.2 Context Budget Auditor
Audit token sizes and word counts of all instructions, prompts, and skills:
```bash
python skills/ecosystem-optimizer/scripts/audit_context_budget.py --target .
```

### 2.3 Platform Leakage Linter
Detect cross-domain leaks between Mobile, Backend, and Web instructions:
```bash
python skills/ecosystem-optimizer/scripts/lint_platform_leakage.py --target . --profile auto
```

### 2.4 Hub & Spoke Sync Validator
Verify that a spoke repository is updated with canonical schemas and shared tools:
```bash
python skills/ecosystem-optimizer/scripts/validate_spokes_sync.py --hub . --spoke ../KmSafe
```

---

## 3. Workflow Integration

When should you activate the **Ecosystem Optimizer**?
1. **Post-Feature Retrospective**: After finishing an OpenSpec feature (`workflow.py verify`), run `workflow.py audit` to ensure no prompt bloat was introduced.
2. **Platform Installer Verification**: Whenever running `install_to_project.py`, execute the leakage linter to ensure target spokes received clean platform-specific rules.
3. **Continuous Integration (CI)**: Embed `ecosystem_audit.py` into automated validation suites to fail builds if context budgets are exceeded or domain boundaries are violated.
