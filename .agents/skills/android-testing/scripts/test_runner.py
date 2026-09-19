#!/usr/bin/env python3
"""
test_runner.py - Android Gradle / JUnit Test Runner & Traceability Mapper

Executes Gradle test tasks, parses JUnit XML test reports, extracts execution metrics,
and maps test cases 1-to-1 with PRD Acceptance Criteria (AC-xx).

Standard library only.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple


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


def run_gradle_tests(project_dir: Path, variant: str = "debug") -> Tuple[int, str, str]:
    """Executes Gradle unit test task."""
    wrapper = find_gradle_wrapper(project_dir)
    if not wrapper:
        return 1, "", f"Gradle wrapper not found in {project_dir}"

    task_name = f"test{variant.capitalize()}UnitTest"
    cmd = [str(wrapper), task_name]

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
        return proc.returncode, proc.stdout, proc.stderr
    except Exception as e:
        return 1, "", str(e)


def extract_ac_id(test_name: str) -> Optional[str]:
    """Extracts AC identifier (e.g. 'AC-01') from test method name like 'ac01_testName'."""
    # Match patterns like: ac01_, ac_01_, AC-01, test_ac01_
    match = re.search(r"(?i)(?:\b|_)ac[-_]?(\d{1,3})(?:_|-|$)", test_name)
    if match:
        num = int(match.group(1))
        return f"AC-{num:02d}"
    return None


def parse_junit_xml_file(xml_path: Path) -> Dict[str, Any]:
    """Parses a single JUnit XML file into structured test metrics."""
    results = {
        "tests": 0,
        "passed": 0,
        "failed": 0,
        "skipped": 0,
        "duration_ms": 0.0,
        "failures": [],
        "cases": [],
    }

    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except Exception as e:
        sys.stderr.write(f"Failed to parse XML {xml_path}: {e}\n")
        return results

    # Root can be <testsuite> or <testsuites>
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")

    for suite in suites:
        time_sec = float(suite.attrib.get("time", 0.0))
        results["duration_ms"] += time_sec * 1000.0

        for case in suite.findall("testcase"):
            results["tests"] += 1
            case_name = case.attrib.get("name", "unknown")
            classname = case.attrib.get("classname", "unknown")
            case_time = float(case.attrib.get("time", 0.0)) * 1000.0
            ac_id = extract_ac_id(case_name)

            failure_elem = case.find("failure")
            error_elem = case.find("error")
            skipped_elem = case.find("skipped")

            if failure_elem is not None or error_elem is not None:
                err_elem = failure_elem if failure_elem is not None else error_elem
                msg = err_elem.attrib.get("message", "")
                stack = err_elem.text.strip() if err_elem.text else ""
                results["failed"] += 1
                results["failures"].append({
                    "classname": classname,
                    "test_name": case_name,
                    "ac_id": ac_id,
                    "message": msg,
                    "stacktrace": stack,
                })
                results["cases"].append({
                    "name": case_name,
                    "classname": classname,
                    "status": "FAILED",
                    "ac_id": ac_id,
                    "duration_ms": case_time,
                })
            elif skipped_elem is not None:
                results["skipped"] += 1
                results["cases"].append({
                    "name": case_name,
                    "classname": classname,
                    "status": "SKIPPED",
                    "ac_id": ac_id,
                    "duration_ms": case_time,
                })
            else:
                results["passed"] += 1
                results["cases"].append({
                    "name": case_name,
                    "classname": classname,
                    "status": "PASSED",
                    "ac_id": ac_id,
                    "duration_ms": case_time,
                })

    return results


def parse_junit_results(xml_files: List[Path]) -> Dict[str, Any]:
    """Aggregates metrics across multiple JUnit XML test reports."""
    total_tests = 0
    total_passed = 0
    total_failed = 0
    total_skipped = 0
    total_duration = 0.0
    all_failures = []
    all_cases = []
    ac_status_map: Dict[str, str] = {}

    for x_file in xml_files:
        res = parse_junit_xml_file(x_file)
        total_tests += res["tests"]
        total_passed += res["passed"]
        total_failed += res["failed"]
        total_skipped += res["skipped"]
        total_duration += res["duration_ms"]
        all_failures.extend(res["failures"])
        all_cases.extend(res["cases"])

    # Map AC coverage
    for case in all_cases:
        ac = case.get("ac_id")
        if ac:
            current_status = ac_status_map.get(ac, "PASSED")
            if case["status"] == "FAILED":
                ac_status_map[ac] = "FAILED"
            elif case["status"] == "SKIPPED" and current_status != "FAILED":
                ac_status_map[ac] = "SKIPPED"
            elif current_status not in ("FAILED", "SKIPPED"):
                ac_status_map[ac] = "PASSED"

    return {
        "total_tests": total_tests,
        "passed": total_passed,
        "failed": total_failed,
        "skipped": total_skipped,
        "duration_ms": round(total_duration, 2),
        "all_tests_passed": (total_failed == 0 and total_tests > 0),
        "failures": all_failures,
        "ac_coverage": dict(sorted(ac_status_map.items())),
        "cases": all_cases,
    }


def parse_prd_acceptance_criteria(prd_path: Path) -> Set[str]:
    """Extracts all AC-xx identifiers declared in PRD markdown."""
    if not prd_path.is_file():
        return set()
    text = prd_path.read_text(encoding="utf-8", errors="replace")
    matches = re.findall(r"\bAC-(\d{1,3})\b", text)
    return {f"AC-{int(m):02d}" for m in matches}


def get_mock_test_summary() -> Dict[str, Any]:
    """Returns sample summary for offline testing."""
    return {
        "total_tests": 8,
        "passed": 8,
        "failed": 0,
        "skipped": 0,
        "duration_ms": 1850.0,
        "all_tests_passed": True,
        "failures": [],
        "ac_coverage": {
            "AC-01": "PASSED",
            "AC-02": "PASSED",
            "AC-03": "PASSED",
        },
        "ac_traceability_complete": True,
        "missing_ac": [],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Android Test Runner - Executes Gradle tests, parses JUnit XML results, and maps AC coverage."
    )
    parser.add_argument("--project-dir", help="Project root containing gradlew")
    parser.add_argument("--variant", default="debug", help="Build variant (default: debug)")
    parser.add_argument("--xml-dir", help="Directory containing JUnit XML result files")
    parser.add_argument("--xml-file", help="Path to single JUnit XML result file")
    parser.add_argument("--prd", help="Path to PRD markdown file to verify AC traceability")
    parser.add_argument("--output", "-o", help="Path to output summary JSON")
    parser.add_argument("--mock-sample", action="store_true", help="Generate synthetic test summary for verification")

    args = parser.parse_args()

    if args.mock_sample:
        summary = get_mock_test_summary()
        json_out = json.dumps(summary, indent=2)
        if args.output:
            Path(args.output).write_text(json_out, encoding="utf-8")
        print(json_out)
        return 0

    xml_files: List[Path] = []

    # 1. Single XML file provided
    if args.xml_file:
        p = Path(args.xml_file)
        if p.is_file():
            xml_files.append(p)
        else:
            print(f"Error: XML file '{p}' not found.", file=sys.stderr)
            return 1

    # 2. XML directory provided
    elif args.xml_dir:
        dir_p = Path(args.xml_dir)
        if dir_p.is_dir():
            xml_files.extend(list(dir_p.glob("**/*.xml")))
        else:
            print(f"Error: XML directory '{dir_p}' not found.", file=sys.stderr)
            return 1

    # 3. Project directory provided -> optionally run gradle
    elif args.project_dir:
        pdir = Path(args.project_dir)
        # Check standard output directories first
        test_res_dir = pdir / "app" / "build" / "test-results" / f"test{args.variant.capitalize()}UnitTest"
        if not test_res_dir.is_dir():
            # Run gradle tests
            print(f"Running Gradle tests in {pdir}...")
            code, stdout, stderr = run_gradle_tests(pdir, args.variant)
            if code != 0:
                print(f"Gradle test run failed:\n{stderr or stdout}", file=sys.stderr)

        if test_res_dir.is_dir():
            xml_files.extend(list(test_res_dir.glob("*.xml")))

    if not xml_files:
        print("No JUnit XML files found. Provide --xml-dir, --xml-file, or --mock-sample.", file=sys.stderr)
        return 1

    summary = parse_junit_results(xml_files)

    # Cross-reference with PRD ACs if requested
    if args.prd:
        prd_acs = parse_prd_acceptance_criteria(Path(args.prd))
        covered_acs = set(summary["ac_coverage"].keys())
        missing_acs = sorted(list(prd_acs - covered_acs))
        summary["required_acs"] = sorted(list(prd_acs))
        summary["missing_ac"] = missing_acs
        summary["ac_traceability_complete"] = len(missing_acs) == 0
    else:
        summary["ac_traceability_complete"] = len(summary["ac_coverage"]) > 0
        summary["missing_ac"] = []

    json_str = json.dumps(summary, indent=2)

    if args.output:
        out_p = Path(args.output)
        out_p.parent.mkdir(parents=True, exist_ok=True)
        out_p.write_text(json_str, encoding="utf-8")
        print(f"Test summary written to {out_p}")

    print(json_str)

    if summary["failed"] > 0:
        return 1
    if args.prd and not summary["ac_traceability_complete"]:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
