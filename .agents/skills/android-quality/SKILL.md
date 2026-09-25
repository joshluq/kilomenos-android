---
name: android-quality
description: Automated static analysis (Android Lint, Detekt, ktlint), pre-handoff sanity verification, logcat crash diagnosis, ANR triage, and role-based defect routing.
---
# Android Quality, Linting & Triage Skill

## 1. Overview & Purpose
The `android-quality` skill governs code health, static analysis, pre-handoff gating, and runtime crash triage across the Android development lifecycle. It guarantees that code passing from development to QA meets strict quality thresholds and provides automated root-cause diagnostics when defects occur.

Key capabilities:
1. **Automated Lint Triage**: Parses Gradle Android Lint XML reports and ktlint outputs, filtering benign warnings from gating errors and categorizing issues by architectural domain.
2. **Pre-Handoff Sanity Gating**: A single automated gate script (`sanity_check.py`) that executes compilation, code styling, linting, and unit tests, producing verified machine-readable payloads for `dev_to_qa_handoff.json`.
3. **Logcat Crash Triage**: Captures and demangles runtime Android exceptions (`FATAL EXCEPTION`), extracting culprit source files, line numbers, and formatted stacktraces.
4. **ANR & Deadlock Diagnosis**: Detects Application Not Responding (ANR) events and inspects thread traces for main thread blocking.
5. **Role-Based Defect Routing**: Automatically classifies defects and dispatches remediation tickets to the responsible lifecycle role defined in `AGENTS.md`.

---

## 2. Integration with Agent Roles

### Senior Android Developer
- **Pre-Commit Verification**: Runs `sanity_check.py` locally before committing code or generating handoff contracts.
- **Full App Build Verification**: Executes `./gradlew :app:assembleDevDebug` to guarantee all module dependencies, Hilt bindings, and route navigations compile cleanly into the debug APK.
- **Architectural Conformance**: Ensures ViewModels inherit `es.joshluq.foundationkit.viewmodel.ScreenViewModel<State, Event, Effect>` and Domain UseCases implement `UseCase<Input, Output>` / `FlowUseCase<Input, Output>` bound in `UseCaseModule.kt`.
- **Lint Remediation**: Consumes `lint_triage.py` JSON reports to pinpoint exact line numbers and rules causing build violations.

### QA / Testing Engineer
- **Quality Gate Audit**: Runs `sanity_check.py` and verifies that `./gradlew :app:assembleDevDebug` passes cleanly (exit code 0) before initiating full test suites.
- **Crash Triage**: Runs `logcat_triage.py` when automated tests or manual exploration trigger runtime failures.
- **Defect Ticket Generation**: Uses the output of triage scripts to populate `qa_verdict_handoff.json` defect tickets with precise culprit locations and remediation routing.

---

## 3. Quality Standards & Categorization

### 3.1 Severity Levels
| Severity | Gate Impact | Action Required |
|---|---|---|
| `Fatal` | **BLOCKS HANDOFF** | Immediate remediation. Build cannot proceed. |
| `Error` | **BLOCKS HANDOFF** | Must be fixed by Developer prior to QA handoff. |
| `Warning` | Allowed with threshold | Logged in triage summary. New code must have 0 warnings. |
| `Information` | Non-blocking | Informational only. |

### 3.2 Role-Based Defect Routing Matrix
When defects are detected by tests, lint, or runtime triage, they are mapped to the upstream role responsible for remediation:

```
+------------------------------------+-----------------------------+---------------------------+
| Defect Classification              | Examples                    | Target Remediation Role   |
+------------------------------------+-----------------------------+---------------------------+
| IMPLEMENTATION_BUG                 | NullPointerException,       | Senior Android Developer  |
|                                    | assertion error, logic bug, |                           |
|                                    | Kotlin syntax error         |                           |
+------------------------------------+-----------------------------+---------------------------+
| DESIGN_FLAW                        | Architecture violation,     | Software Architect        |
|                                    | circular dependency,        |                           |
|                                    | missing contract field      |                           |
+------------------------------------+-----------------------------+---------------------------+
| REQUIREMENT_AMBIGUITY              | Contradictory ACs,          | Product Owner             |
|                                    | unspecified edge cases      |                           |
+------------------------------------+-----------------------------+---------------------------+
```

---

## 4. Helper Scripts

The skill provides three runnable Python 3 CLI utilities in `skills/android-quality/scripts/`:

```
skills/android-quality/
├── SKILL.md
└── scripts/
    ├── lint_triage.py
    ├── logcat_triage.py
    └── sanity_check.py
```

### Script 1: `lint_triage.py`
Parses Android Lint XML reports and ktlint outputs into categorized issue manifests.

#### Usage:
```bash
# Triage Android Lint XML report
python skills/android-quality/scripts/lint_triage.py --lint-xml app/build/reports/lint-results-debug.xml --output lint_summary.json

# Triage with warning tolerance
python skills/android-quality/scripts/lint_triage.py --lint-xml app/build/reports/lint-results-debug.xml --max-warnings 0

# Triage both Android Lint and ktlint
python skills/android-quality/scripts/lint_triage.py --lint-xml app/build/reports/lint-results.xml --ktlint-report build/reports/ktlint.txt
```

#### Output Schema:
```json
{
  "lint_clean": true,
  "fatal_count": 0,
  "error_count": 0,
  "warning_count": 0,
  "total_issues": 0,
  "categories": {
    "Correctness": 0,
    "Performance": 0,
    "Security": 0,
    "Usability": 0
  },
  "issues": []
}
```

---

### Script 2: `logcat_triage.py`
Monitors ADB logcat or saved logs, detects unhandled crashes (`FATAL EXCEPTION`) and ANRs, parses the culprit stacktrace, and formats a structured defect ticket.

#### Usage:
```bash
# Capture and analyze crash from live connected emulator
python skills/android-quality/scripts/logcat_triage.py analyze --serial emulator-5554 --package com.example.profile

# Analyze crash from saved logcat file
python skills/android-quality/scripts/logcat_triage.py analyze --log-file tests/fixtures/crash_logcat.txt --output crash_ticket.json
```

#### Output Schema:
```json
{
  "crash_detected": true,
  "exception_class": "java.lang.NullPointerException",
  "message": "Attempt to invoke virtual method on a null object reference",
  "crashing_thread": "main",
  "culprit_file": "com/example/profile/ui/ProfileViewModel.kt",
  "culprit_line": 42,
  "stacktrace": [
    "com.example.profile.ui.ProfileViewModel.savePreferences(ProfileViewModel.kt:42)",
    "com.example.profile.ui.ProfileViewModel$savePreferences$1.invokeSuspend(ProfileViewModel.kt:38)"
  ],
  "target_role_for_remediation": "Senior Android Developer",
  "defect_type": "IMPLEMENTATION_BUG"
}
```

---

### Script 3: `sanity_check.py`
The primary automated verification gate for the Senior Android Developer before completing the Dev-to-QA handoff. Validates compilation, code style (ktlint), Android Lint, and unit tests, writing verified metrics to `dev_to_qa_handoff.json`.

#### Usage:
```bash
# Execute live pre-handoff sanity check against Android project
python skills/android-quality/scripts/sanity_check.py --project-dir ./app --output handoffs/dev_to_qa_FEAT-001.json

# Validate an existing Dev-to-QA handoff JSON payload against quality gates
python skills/android-quality/scripts/sanity_check.py --dev-handoff handoffs/dev_to_qa_FEAT-001.json

# Offline verification / scriptable mode with synthetic verification
python skills/android-quality/scripts/sanity_check.py --mock-sample --output sanity_report.json
```

#### Output Schema (`dev_to_qa_handoff.json`):
```json
{
  "feature_id": "FEAT-001",
  "compilation_clean": true,
  "ktlint_clean": true,
  "lint_clean": true,
  "unit_tests_passed": true,
  "metrics": {
    "compilation_time_ms": 3200,
    "lint_issues_count": 0,
    "tests_total": 8,
    "tests_failed": 0
  },
  "can_handoff_to_qa": true
}
```
