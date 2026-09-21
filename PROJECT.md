# Project: Android Agentic Ecosystem

## Architecture
The Android Agentic Ecosystem is a modular, role-based multi-agent framework and starter template for Modern Android application development (Kotlin, Jetpack Compose, Coroutines, StateFlow, Clean Architecture).

### Core Components
1. **Agent Roles and Contract Handoffs (`AGENTS.md`, `templates/`, `schemas/`)**:
   - Four primary lifecycle roles: Product Owner, Software Architect, Senior Android Developer, QA/Testing Engineer.
   - Standardized document templates (PRD, ADR, Component Spec, QA Report).
   - Strict Draft-07 JSON Schemas governing machine-readable transition payloads between roles, eliminating informal unstructured chatter.
2. **Specialized Android Development Skills (`skills/`)**:
   - `skills/android-device/`: Android CLI & emulator interaction, ADB bridge, layout tree inspection, UI coordinate resolution, journey tests.
   - `skills/android-testing/`: JUnit 4/5, MockK, Turbine for StateFlow, ComposeTestRule UI testing, Coroutine test dispatchers.
   - `skills/android-quality/`: Android Lint XML triage, ktlint/detekt, pre-handoff sanity verification, Logcat crash triage, defect routing.
3. **Runnable Helper Scripts (`skills/*/scripts/`)**:
   - Cross-platform CLI utilities for device management, test scaffolding, lint triage, and sanity gating.
4. **Concrete End-to-End Walkthrough Demonstration (`walkthrough/`, `scripts/run_walkthrough.py`)**:
   - Real-world scenario (`FEAT-001: User Profile & Preferences`) demonstrating the complete handoff pipeline (PO -> Architect -> Android Dev -> QA), generating valid Kotlin/Compose code and tests.
5. **Self-Contained Verification Suite (`scripts/verify_ecosystem.py`, `tests/`)**:
   - Zero-external-dependency Draft-07 validator and test suite verifying all skills, roles, schemas, and walkthrough artifacts.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Role Definitions | Comprehensive AGENTS.md for PO, Architect, Android Dev, QA | M1 | spec_miner_survey_1 |
| 2 | PRD Template | Standardized PRD markdown template with ACs and NFRs | M1 | spec_miner_survey_1 |
| 3 | ADR Template | Architecture Decision Record template with Android specifics | M1 | spec_miner_survey_1 |
| 4 | Component Spec Template | Immutable UI state, actions, Composable, ViewModel contracts | M1 | spec_miner_survey_1 |
| 5 | QA Report Template | Execution metrics, 1-to-1 AC verification matrix, defect tickets | M1 | spec_miner_survey_1 |
| 6 | PO->Architect Schema | JSON Schema enforcing strict typing for PO handoffs | M1 | spec_miner_survey_1 |
| 7 | Architect->Dev Schema | JSON Schema enforcing architectural and component contracts | M1 | spec_miner_survey_1 |
| 8 | Dev->QA Schema | JSON Schema enforcing sanity flags, test status, manifests | M1 | spec_miner_survey_1 |
| 9 | QA Verdict Schema | JSON Schema enforcing quality gate verdicts and defect routing | M1 | spec_miner_survey_1 |
| 10 | Chatter Elimination Protocol | Formal artifact-driven state machine protocol | M1 | spec_miner_survey_1 |
| 11 | Automated Defect Routing | Feedback loop routing defects to responsible role on rejection | M1 | spec_miner_survey_1 |
| 12 | Android Device Skill | `skills/android-device/SKILL.md` (CLI, emulator, layout, screen) | M2 | spec_miner_survey_2 |
| 13 | Android Testing Skill | `skills/android-testing/SKILL.md` (JUnit, MockK, Turbine, Compose) | M2 | spec_miner_survey_2 |
| 14 | Android Quality Skill | `skills/android-quality/SKILL.md` (Lint, ktlint, logcat, sanity) | M2 | spec_miner_survey_2 |
| 15 | Device Runner Script | `skills/android-device/scripts/device_runner.py` | M2 | spec_miner_survey_2 |
| 16 | Layout Inspector Script | `skills/android-device/scripts/inspect_layout.py` | M2 | spec_miner_survey_2 |
| 17 | Journey Runner Script | `skills/android-device/scripts/run_journey.py` | M2 | spec_miner_survey_2 |
| 18 | Test Runner Script | `skills/android-testing/scripts/test_runner.py` | M2 | spec_miner_survey_2 |
| 19 | Test Scaffold Generator | `skills/android-testing/scripts/generate_test_scaffold.py` | M2 | spec_miner_survey_2 |
| 20 | Lint Triage Script | `skills/android-quality/scripts/lint_triage.py` | M2 | spec_miner_survey_2 |
| 21 | Logcat Triage Script | `skills/android-quality/scripts/logcat_triage.py` | M2 | spec_miner_survey_2 |
| 22 | Sanity Check Script | `skills/android-quality/scripts/sanity_check.py` | M2 | spec_miner_survey_2 |
| 23 | Schema Validator | `scripts/schema_validator.py` zero-dependency Draft-07 validator | M3 | explorer_survey_3 |
| 24 | Walkthrough Feature Artifacts | Complete FEAT-001 PRD, ADR, Spec, Kotlin code, tests, QA report | M3 | explorer_survey_3 |
| 25 | Walkthrough Runner | `scripts/run_walkthrough.py` end-to-end simulation script | M3 | explorer_survey_3 |
| 26 | Walkthrough Handoff JSONs | Real validated handoff JSONs for each role transition | M3 | explorer_survey_3 |
| 27 | Ecosystem Verifier | `scripts/verify_ecosystem.py` single automated verification script | M4 | explorer_survey_3 |
| 28 | One-Click Execution Wrappers | `run_verification.ps1` and `run_verification.sh` | M4 | explorer_survey_3 |
| 29 | Comprehensive Test Suite | Automated test modules in `tests/` | M4 | explorer_survey_3 |
| 30 | E2E Acceptance & Quality Gate | 100% verification suite pass and adversarial hardening | M4 | explorer_survey_3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Agent Roles & Contract Schemas | AGENTS.md, templates/ (PRD, ADR, COMP-SPEC, QA-REPORT), schemas/ (4 transition schemas) | Survey | IN_PROGRESS |
| M2 | Specialized Android Skills & Tools | skills/ (android-device, android-testing, android-quality) + 8 runnable helper scripts | M1 | PLANNED |
| M3 | Walkthrough Pipeline & Artifacts | scripts/schema_validator.py, walkthrough/ (FEAT-001 artifacts, code, tests), scripts/run_walkthrough.py | M1, M2 | PLANNED |
| M4 | Verification Suite & Final Quality Gate | scripts/verify_ecosystem.py, tests/, run_verification.ps1/sh, 100% E2E verification pass | M1, M2, M3 | PLANNED |

## Interface Contracts
### Product Owner ↔ Software Architect
- Input: Feature request, domain goals
- Output: `docs/prd/PRD-<id>.md` (PRD template), `handoffs/po_to_architect_<id>.json`
- Schema: `schemas/po_to_architect_handoff.schema.json`

### Software Architect ↔ Senior Android Developer
- Input: `docs/prd/PRD-<id>.md`, `handoffs/po_to_architect_<id>.json`
- Output: `docs/adr/ADR-<id>.md`, `docs/specs/COMP-SPEC-<id>.md`, `handoffs/architect_to_dev_<id>.json`
- Schema: `schemas/architect_to_dev_handoff.schema.json`

### Senior Android Developer ↔ QA / Testing Engineer
- Input: `docs/adr/ADR-<id>.md`, `docs/specs/COMP-SPEC-<id>.md`, `handoffs/architect_to_dev_<id>.json`
- Output: Kotlin production source, build files, `handoffs/dev_to_qa_<id>.json`
- Schema: `schemas/dev_to_qa_handoff.schema.json`

### QA / Testing Engineer ↔ PO / Feedback Loop
- Input: Kotlin code, test suite, `handoffs/dev_to_qa_<id>.json`
- Output: Automated test files, `docs/qa/QA-REPORT-<id>.md`, `handoffs/qa_verdict_<id>.json`
- Schema: `schemas/qa_verdict_handoff.schema.json`

## Code Layout
```
android_agentic_ecosystem/
├── AGENTS.md                                 # Primary lifecycle role definitions
├── PROJECT.md                                # Master project architecture & status
├── README.md                                 # Ecosystem overview, architecture & usage
├── run_verification.ps1                      # Windows PowerShell one-click verification
├── run_verification.sh                       # POSIX Bash one-click verification
├── templates/                                # Standardized markdown document templates
│   ├── prd-template.md                       # Product Requirements Document template
│   ├── adr-template.md                       # Architecture Decision Record template
│   ├── component-spec-template.md            # Component Interface Specification template
│   └── qa-report-template.md                 # QA Quality Gate Report template
├── schemas/                                  # Machine-readable JSON Schema contracts
│   ├── po_to_architect_handoff.schema.json   # PO -> Architect transition contract
│   ├── architect_to_dev_handoff.schema.json  # Architect -> Android Dev transition contract
│   ├── dev_to_qa_handoff.schema.json         # Android Dev -> QA transition contract
│   └── qa_verdict_handoff.schema.json        # QA -> PO/Remediation verdict contract
├── skills/                                   # Specialized Android Antigravity skills
│   ├── android-device/
│   │   ├── SKILL.md                          # Device & CLI interaction skill definition
│   │   └── scripts/
│   │       ├── device_runner.py              # Emulator lifecycle & APK installation
│   │       ├── inspect_layout.py             # UI layout hierarchy inspection
│   │       └── run_journey.py                # Journey XML test execution
│   ├── android-testing/
│   │   ├── SKILL.md                          # Testing skill definition (MockK/Turbine/Compose)
│   │   └── scripts/
│   │       ├── test_runner.py                # Gradle test runner & JUnit XML parser
│   │       └── generate_test_scaffold.py     # Component spec test scaffold generator
│   └── android-quality/
│       ├── SKILL.md                          # Quality & linting skill definition
│       └── scripts/
│           ├── lint_triage.py                # Android Lint & ktlint triage
│           ├── logcat_triage.py              # Crash & ANR triage from logcat
│           └── sanity_check.py               # Pre-handoff sanity verification gate
├── walkthrough/                              # Concrete end-to-end demonstration
│   ├── docs/
│   │   ├── PRD-FEAT-001.md                   # Concrete PRD for User Profile & Preferences
│   │   ├── ADR-FEAT-001.md                   # Concrete Architecture Decision Record
│   │   ├── COMP-SPEC-FEAT-001.md             # Concrete Component Interface Specification
│   │   └── QA-REPORT-FEAT-001.md             # Concrete QA Quality Gate Report
│   ├── handoffs/
│   │   ├── po_to_architect_FEAT-001.json     # Validated PO -> Architect handoff payload
│   │   ├── architect_to_dev_FEAT-001.json    # Validated Architect -> Dev handoff payload
│   │   ├── dev_to_qa_FEAT-001.json           # Validated Dev -> QA handoff payload
│   │   └── qa_verdict_FEAT-001.json          # Validated QA verdict payload
│   ├── src/                                  # Generated Kotlin/Compose production code
│   │   └── main/kotlin/com/example/profile/
│   │       ├── model/UserProfile.kt
│   │       ├── ui/ProfileUiState.kt
│   │       ├── ui/ProfileUiAction.kt
│   │       ├── ui/ProfileScreen.kt
│   │       ├── ui/ProfileViewModel.kt
│   │       ├── domain/GetProfileUseCase.kt
│   │       ├── domain/UpdatePreferencesUseCase.kt
│   │       └── data/ProfileRepository.kt
│   └── src/                                  # Automated unit and UI tests
│       └── test/kotlin/com/example/profile/
│           ├── ProfileViewModelTest.kt       # MockK + Turbine StateFlow test
│           └── ProfileScreenTest.kt          # Compose UI semantics test
├── scripts/                                  # Shared ecosystem scripts & runners
│   ├── schema_validator.py                   # Self-contained zero-dependency JSON Schema validator
│   ├── run_walkthrough.py                    # End-to-end walkthrough simulation runner
│   └── verify_ecosystem.py                   # Comprehensive verification runner
└── tests/                                    # Ecosystem test suite
    ├── __init__.py
    ├── test_skills.py                        # SKILL.md frontmatter & structure tests
    ├── test_contracts.py                     # JSON Schema & contract validation tests
    ├── test_walkthrough.py                   # Walkthrough pipeline & artifact tests
    └── test_scripts.py                       # Helper scripts unit tests
```
