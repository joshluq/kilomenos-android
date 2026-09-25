---
name: openspec
description: Manages living specifications, delta changes, and SDD lifecycles according to the OpenSpec standard. Automates propose, apply, and archive workflows across Android multi-agent architectures.
---

# OpenSpec - Living Specifications & Delta Lifecycle

This skill integrates the **OpenSpec** (`openspec.dev`) standard into the **Android Agentic Ecosystem**. It bridges the gap between high-level human requirements and deterministic multi-agent execution, organizing all specifications into **Living Specs** (`openspec/specs/`) and manageable **Delta Changes** (`openspec/changes/<change_id>/`).

---

## 1. The OpenSpec 3-Phase Lifecycle

```
    1. PROPOSE (/opsx propose <change_id>)
       └─ Scaffolds openspec/changes/<change_id>/
          ├─ proposal.md       (Business intent, user problem, scope)
          ├─ design.md         (4-Layer Compose, MVI state, ADR)
          ├─ tasks.md          (Actionable checklist for PO, Arch, Dev, QA)
          └─ specs/            (Delta specifications: + Added, ~ Modified)
               ↓
    2. APPLY (/opsx apply <change_id> or /new-feature)
       └─ Multi-agent execution: PO -> Architect -> Staff Dev -> QA Engineer.
       └─ Governed by SDD JSON Schemas and guarded by scripts/verify_ecosystem.py.
               ↓
    3. ARCHIVE (/opsx archive <change_id>)
       └─ Quality Gate asserts 100% tests passed and all tasks checked.
       └─ Delta specs merge into permanent living specs in openspec/specs/.
       └─ Directory moves to openspec/archive/<change_id>/ for historical audit.
```

---

## 2. Available Commands & Tooling

The skill is backed by the CLI tool [`scripts/openspec_cli.py`](file:///C:/Users/josh_/.gemini/antigravity/scratch/android_agentic_ecosystem/scripts/openspec_cli.py):

### 2.1 Propose a New Change:
```bash
python scripts/openspec_cli.py propose FEAT-002 --title "Export Mileage Report to PDF"
```

### 2.2 Validate Change Artifacts & Checklist:
```bash
python scripts/openspec_cli.py validate FEAT-002
```

### 2.3 Archive Completed & Verified Feature:
```bash
python scripts/openspec_cli.py archive FEAT-002
```

### 2.4 List Active Changes & Living Specs:
```bash
python scripts/openspec_cli.py list
```

---

## 3. How OpenSpec Cooperates with Our Specialized Roles

| OpenSpec Phase | Responsible Role | Key Artifacts & Actions |
| :--- | :--- | :--- |
| **Phase 1: Propose** | **Product Owner** (`/po-digital-experience-fintech`) | Creates `proposal.md` and delta specs with Given/When/Then acceptance criteria. |
| **Phase 1: Architecture** | **Software Architect** (`AGENTS.md`) | Authors `design.md` detailing MVI State, UI Actions, and UseCases. |
| **Phase 2: Apply** | **Staff Android Dev** (`/android-staff-engineer-compose`) | Implements UseCases, ViewModels, and 4-Layer Compose Screens. Ticks items in `tasks.md`. |
| **Phase 2: Quality Gate** | **QA Engineer** (`/android-testing` & `/android-quality`) | Runs Compose UI tests, Turbine state checks, and asserts 0 defects. |
| **Phase 3: Archive** | **Test Harness** (`verify_ecosystem.py`) | Verifies that all gates pass, merges delta specs into living specs, and archives the change. |
