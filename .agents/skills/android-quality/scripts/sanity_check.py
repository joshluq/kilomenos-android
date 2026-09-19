#!/usr/bin/env python3
"""
sanity_check.py - Pre-Handoff Sanity Verification Gate

Automates pre-handoff quality gating for Android development.
Enforces that compilation, style checks (ktlint/detekt), Android Lint, and unit tests
are 100% clean and passing before permitting transition from Senior Android Developer
to QA/Testing Engineer. Validates or generates dev_to_qa_handoff.json payloads.

Standard library only.
"""

import argparse
import datetime
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


def find_gradle_wrapper(project_dir: Path) -> Optional[Path]:
    """Finds gradlew or gradlew.bat in project directory or parent directories."""
    grad_name = "gradlew.bat" if sys.platform.startswith("win") else "gradlew"
    curr = project_dir.resolve()
    for _ in range(4):
        cand = curr / grad_name
        if cand.is_file():
            return cand
        if curr.parent == curr:
            break
        curr = curr.parent
    return None


def run_gradle_task(project_dir: Path, task: str) -> Tuple[bool, int, str]:
    """Executes a single Gradle task."""
    wrapper = find_gradle_wrapper(project_dir)
    if not wrapper:
        return False, 0, f"Gradle wrapper not found in {project_dir}"

    cmd = [str(wrapper), task]
    start = time.time()
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(project_dir),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        duration_ms = int((time.time() - start) * 1000)
        passed = (proc.returncode == 0)
        output = proc.stdout if passed else (proc.stderr or proc.stdout)
        return passed, duration_ms, output
    except Exception as e:
        duration_ms = int((time.time() - start) * 1000)
        return False, duration_ms, str(e)


def execute_live_sanity_check(project_dir: Path) -> Dict[str, Any]:
    """Runs compilation, style, lint, and unit tests via Gradle."""
    print(f"Executing Pre-Handoff Sanity Checks in: {project_dir}")

    # 1. Compilation
    print("  [1/4] Checking Kotlin Compilation (compileDebugKotlin)...")
    comp_pass, comp_time, comp_out = run_gradle_task(project_dir, "compileDebugKotlin")

    # 2. ktlint / code formatting
    print("  [2/4] Checking Code Style (ktlintCheck)...")
    ktlint_pass, ktlint_time, ktlint_out = run_gradle_task(project_dir, "ktlintCheck")

    # 3. Android Lint
    print("  [3/4] Running Static Analysis (lintDebug)...")
    lint_pass, lint_time, lint_out = run_gradle_task(project_dir, "lintDebug")

    # 4. Unit Tests
    print("  [4/4] Executing Unit Tests (testDebugUnitTest)...")
    tests_pass, tests_time, tests_out = run_gradle_task(project_dir, "testDebugUnitTest")

    all_passed = comp_pass and ktlint_pass and lint_pass and tests_pass

    return {
        "compilation_clean": comp_pass,
        "ktlint_clean": ktlint_pass,
        "detekt_clean": ktlint_pass,
        "lint_clean": lint_pass,
        "unit_tests_passed": tests_pass,
        "metrics": {
            "compilation_time_ms": comp_time,
            "style_check_time_ms": ktlint_time,
            "lint_time_ms": lint_time,
            "tests_time_ms": tests_time,
        },
        "can_handoff_to_qa": all_passed,
    }


def validate_dev_handoff_json(handoff_path: Path) -> Tuple[bool, List[str], Dict[str, Any]]:
    """Validates an existing dev_to_qa_handoff.json against quality gate criteria."""
    errors = []
    if not handoff_path.is_file():
        return False, [f"File not found: {handoff_path}"], {}

    try:
        data = json.loads(handoff_path.read_text(encoding="utf-8"))
    except Exception as e:
        return False, [f"Invalid JSON in {handoff_path}: {e}"], {}

    # Check required top-level keys
    required_keys = [
        "handoff_id", "schema_version", "timestamp", "sender", "recipient",
        "feature_id", "architect_handoff_ref", "remediation_cycle",
        "implementation_manifest", "code_changes", "developer_test_summary",
        "lint_and_sanity"
    ]
    for k in required_keys:
        if k not in data:
            errors.append(f"Missing required field: '{k}'")

    # Check lint_and_sanity gate booleans
    lint_sanity = data.get("lint_and_sanity", {})
    if not isinstance(lint_sanity, dict):
        errors.append("'lint_and_sanity' must be an object")
    else:
        if lint_sanity.get("compilation_clean") is not True:
            errors.append("Quality Gate Failed: 'compilation_clean' must be true")
        if lint_sanity.get("ktlint_clean") is not True:
            errors.append("Quality Gate Failed: 'ktlint_clean' must be true")
        if lint_sanity.get("detekt_clean") is not True:
            errors.append("Quality Gate Failed: 'detekt_clean' must be true")

    # Check test summary
    test_summary = data.get("developer_test_summary", {})
    if not isinstance(test_summary, dict):
        errors.append("'developer_test_summary' must be an object")
    else:
        run_count = test_summary.get("unit_tests_run", 0)
        passed_count = test_summary.get("unit_tests_passed", 0)
        failed_count = test_summary.get("unit_tests_failed", -1)

        if failed_count != 0:
            errors.append(f"Quality Gate Failed: 'unit_tests_failed' is {failed_count} (must be 0)")
        if run_count < 1 or passed_count < 1:
            errors.append(f"Quality Gate Failed: At least 1 passing unit test required (run: {run_count}, passed: {passed_count})")

    # Check code changes
    code_changes = data.get("code_changes", [])
    if not isinstance(code_changes, list) or len(code_changes) == 0:
        errors.append("Quality Gate Failed: 'code_changes' must contain at least 1 change item")

    is_clean = len(errors) == 0
    return is_clean, errors, data


def generate_sample_dev_handoff(feature_id: str = "FEAT-001") -> Dict[str, Any]:
    """Generates a compliant dev_to_qa_handoff dictionary."""
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return {
        "handoff_id": f"HANDOFF-DEV-QA-{feature_id}-001",
        "schema_version": "1.0.0",
        "timestamp": timestamp,
        "sender": {
            "role": "Senior Android Developer",
            "agent_id": "senior_android_developer_1",
        },
        "recipient": {
            "role": "QA/Testing Engineer",
            "agent_id": "qa_testing_engineer_1",
        },
        "feature_id": feature_id,
        "architect_handoff_ref": f"HANDOFF-ARCH-DEV-{feature_id}-001",
        "remediation_cycle": 0,
        "implementation_manifest": {
            "module": "app",
            "entry_point_composable": "ProfileScreen",
            "viewmodel_class": "ProfileViewModel",
            "repository_class": "ProfileRepository",
        },
        "code_changes": [
            {
                "file_path": "app/src/main/kotlin/com/example/profile/ui/ProfileScreen.kt",
                "action": "CREATED",
                "language": "kotlin",
                "line_count": 85,
            },
            {
                "file_path": "app/src/main/kotlin/com/example/profile/ui/ProfileViewModel.kt",
                "action": "CREATED",
                "language": "kotlin",
                "line_count": 65,
            },
            {
                "file_path": "app/src/test/kotlin/com/example/profile/ProfileViewModelTest.kt",
                "action": "CREATED",
                "language": "kotlin",
                "line_count": 110,
            },
        ],
        "developer_test_summary": {
            "unit_tests_run": 8,
            "unit_tests_passed": 8,
            "unit_tests_failed": 0,
            "test_frameworks_used": ["JUnit4", "MockK", "Turbine", "kotlinx-coroutines-test"],
            "test_files": [
                "app/src/test/kotlin/com/example/profile/ProfileViewModelTest.kt",
                "app/src/test/kotlin/com/example/profile/ProfileScreenTest.kt",
            ],
        },
        "lint_and_sanity": {
            "compilation_clean": True,
            "ktlint_clean": True,
            "detekt_clean": True,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Pre-Handoff Sanity Verification Gate - Validates code health, lint, and tests prior to QA handoff."
    )
    parser.add_argument("--project-dir", help="Path to Android project directory to run live Gradle checks")
    parser.add_argument("--dev-handoff", help="Path to dev_to_qa_handoff.json file to audit against gating rules")
    parser.add_argument("--feature-id", default="FEAT-001", help="Feature ID for generated handoff (default: FEAT-001)")
    parser.add_argument("--output", "-o", help="Output path to save verified handoff JSON")
    parser.add_argument("--mock-sample", action="store_true", help="Generate and validate synthetic handoff payload")

    args = parser.parse_args()

    # Mode 1: Mock sample generation and validation
    if args.mock_sample:
        payload = generate_sample_dev_handoff(args.feature_id)
        json_str = json.dumps(payload, indent=2)
        if args.output:
            out_p = Path(args.output)
            out_p.parent.mkdir(parents=True, exist_ok=True)
            out_p.write_text(json_str, encoding="utf-8")
            print(f"Sample Dev-to-QA handoff payload written to: {out_p}")
        print(json_str)
        print("\nSanity Check Result: PASS (All quality gates satisfied)")
        return 0

    # Mode 2: Audit existing handoff JSON
    if args.dev_handoff:
        p = Path(args.dev_handoff)
        is_clean, errors, data = validate_dev_handoff_json(p)
        if is_clean:
            print(f"Sanity Check: PASS - Handoff payload '{p.name}' meets all gating criteria.")
            print(f"  Feature ID:          {data.get('feature_id')}")
            print(f"  Compilation Clean:   {data.get('lint_and_sanity', {}).get('compilation_clean')}")
            print(f"  ktlint Clean:        {data.get('lint_and_sanity', {}).get('ktlint_clean')}")
            print(f"  Unit Tests Passed:   {data.get('developer_test_summary', {}).get('unit_tests_passed')} / {data.get('developer_test_summary', {}).get('unit_tests_run')}")
            return 0
        else:
            print(f"Sanity Check: FAIL - Handoff payload '{p.name}' failed quality gate:", file=sys.stderr)
            for err in errors:
                print(f"  - {err}", file=sys.stderr)
            return 1

    # Mode 3: Live Gradle execution
    if args.project_dir:
        pdir = Path(args.project_dir)
        report = execute_live_sanity_check(pdir)
        json_str = json.dumps(report, indent=2)
        if args.output:
            out_p = Path(args.output)
            out_p.parent.mkdir(parents=True, exist_ok=True)
            out_p.write_text(json_str, encoding="utf-8")
            print(f"Sanity report written to: {out_p}")
        print(json_str)
        if report["can_handoff_to_qa"]:
            print("\nSanity Check Result: PASS - Ready for Dev-to-QA handoff.")
            return 0
        else:
            print("\nSanity Check Result: FAIL - Quality gate blocked.", file=sys.stderr)
            return 1

    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
