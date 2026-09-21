#!/usr/bin/env python3
"""
lint_triage.py - Android Lint & ktlint Static Analysis Triager

Parses Android Lint XML reports and ktlint outputs into categorized issue manifests.
Distinguishes between fatal errors, warnings, and informational notices,
enforcing strict quality gates for the Dev-to-QA transition.

Standard library only.
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


def parse_android_lint_xml(xml_path: Path) -> List[Dict[str, Any]]:
    """Parses Android Lint XML report into a list of structured issue dicts."""
    issues = []
    if not xml_path.is_file():
        return issues

    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except Exception as e:
        sys.stderr.write(f"Error parsing Lint XML: {e}\n")
        return issues

    for issue in root.findall("issue"):
        issue_id = issue.attrib.get("id", "Unknown")
        severity = issue.attrib.get("severity", "Warning").capitalize()
        message = issue.attrib.get("message", "")
        category = issue.attrib.get("category", "Correctness")
        priority = int(issue.attrib.get("priority", "5"))
        explanation = issue.attrib.get("explanation", "")

        locations = []
        for loc in issue.findall("location"):
            locations.append({
                "file": loc.attrib.get("file", ""),
                "line": int(loc.attrib.get("line", "0")),
                "column": int(loc.attrib.get("column", "0")),
            })

        first_loc = locations[0] if locations else {"file": "", "line": 0, "column": 0}

        issues.append({
            "id": issue_id,
            "severity": severity,
            "category": category,
            "priority": priority,
            "message": message,
            "explanation": explanation,
            "file": first_loc["file"],
            "line": first_loc["line"],
            "column": first_loc["column"],
            "tool": "android-lint",
        })

    return issues


def parse_ktlint_report(report_path: Path) -> List[Dict[str, Any]]:
    """Parses ktlint plain-text or checkstyle XML report."""
    issues = []
    if not report_path.is_file():
        return issues

    content = report_path.read_text(encoding="utf-8", errors="replace")

    # Try XML parsing first
    if content.strip().startswith("<"):
        try:
            root = ET.fromstring(content)
            for file_node in root.findall("file"):
                fname = file_node.attrib.get("name", "")
                for err in file_node.findall("error"):
                    issues.append({
                        "id": err.attrib.get("source", "ktlint"),
                        "severity": err.attrib.get("severity", "Error").capitalize(),
                        "category": "Style",
                        "priority": 3,
                        "message": err.attrib.get("message", ""),
                        "file": fname,
                        "line": int(err.attrib.get("line", "0")),
                        "column": int(err.attrib.get("column", "0")),
                        "tool": "ktlint",
                    })
            return issues
        except Exception:
            pass

    # Fallback: Plain text line matching: file:line:col: message (rule)
    pattern = re.compile(r"^(.+?):(\d+):(\d+):\s+(.+?)(?:\s+\((.+?)\))?$")
    for line in content.splitlines():
        line = line.strip()
        match = pattern.match(line)
        if match:
            file_path, line_no, col_no, msg, rule_id = match.groups()
            issues.append({
                "id": rule_id or "ktlint-style",
                "severity": "Error",
                "category": "Style",
                "priority": 3,
                "message": msg.strip(),
                "file": file_path,
                "line": int(line_no),
                "column": int(col_no),
                "tool": "ktlint",
            })

    return issues


def route_issue_to_role(issue: Dict[str, Any]) -> str:
    """Assigns defect to responsible upstream role based on category and severity."""
    cat = issue.get("category", "").lower()
    sev = issue.get("severity", "").lower()

    if "security" in cat:
        return "Software Architect"
    elif "architecture" in cat or sev == "fatal":
        return "Software Architect"
    elif "usability" in cat or "accessibility" in cat:
        return "Senior Android Developer"
    else:
        return "Senior Android Developer"


def triage_issues(issues: List[Dict[str, Any]], max_warnings: int = 0) -> Dict[str, Any]:
    """Aggregates and evaluates lint results."""
    fatal_count = sum(1 for i in issues if i["severity"] == "Fatal")
    error_count = sum(1 for i in issues if i["severity"] == "Error")
    warning_count = sum(1 for i in issues if i["severity"] == "Warning")
    info_count = sum(1 for i in issues if i["severity"] in ("Information", "Info"))

    categories: Dict[str, int] = {}
    for i in issues:
        cat = i.get("category", "General")
        categories[cat] = categories.get(cat, 0) + 1

    for i in issues:
        i["target_role_for_remediation"] = route_issue_to_role(i)

    lint_clean = (fatal_count == 0 and error_count == 0 and warning_count <= max_warnings)

    return {
        "lint_clean": lint_clean,
        "fatal_count": fatal_count,
        "error_count": error_count,
        "warning_count": warning_count,
        "info_count": info_count,
        "total_issues": len(issues),
        "max_warnings_threshold": max_warnings,
        "categories": categories,
        "issues": issues,
    }


def get_mock_lint_summary() -> Dict[str, Any]:
    """Generates synthetic clean lint report for testing."""
    return {
        "lint_clean": True,
        "fatal_count": 0,
        "error_count": 0,
        "warning_count": 0,
        "info_count": 0,
        "total_issues": 0,
        "max_warnings_threshold": 0,
        "categories": {
            "Correctness": 0,
            "Performance": 0,
            "Security": 0,
            "Style": 0,
        },
        "issues": [],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Android Lint & ktlint Triager - Parses static analysis XML/text reports into structured quality manifests."
    )
    parser.add_argument("--lint-xml", help="Path to Android Lint XML report (e.g. lint-results-debug.xml)")
    parser.add_argument("--ktlint-report", help="Path to ktlint report file")
    parser.add_argument("--project-dir", help="Project root containing build reports")
    parser.add_argument("--max-warnings", type=int, default=0, help="Maximum allowed warnings (default: 0)")
    parser.add_argument("--output", "-o", help="Output path for JSON triage report")
    parser.add_argument("--mock-sample", action="store_true", help="Generate synthetic clean report for verification")

    args = parser.parse_args()

    if args.mock_sample:
        summary = get_mock_lint_summary()
        json_str = json.dumps(summary, indent=2)
        if args.output:
            Path(args.output).write_text(json_str, encoding="utf-8")
        print(json_str)
        return 0

    all_issues: List[Dict[str, Any]] = []

    # 1. Direct Lint XML
    if args.lint_xml:
        p = Path(args.lint_xml)
        all_issues.extend(parse_android_lint_xml(p))

    # 2. Direct ktlint report
    if args.ktlint_report:
        p = Path(args.ktlint_report)
        all_issues.extend(parse_ktlint_report(p))

    # 3. Project dir fallback search
    if args.project_dir and not args.lint_xml and not args.ktlint_report:
        pdir = Path(args.project_dir)
        lint_candidates = list(pdir.glob("**/lint-results*.xml"))
        for lc in lint_candidates:
            all_issues.extend(parse_android_lint_xml(lc))

        ktlint_candidates = list(pdir.glob("**/ktlint*.txt")) + list(pdir.glob("**/ktlint*.xml"))
        for kc in ktlint_candidates:
            all_issues.extend(parse_ktlint_report(kc))

    if not args.lint_xml and not args.ktlint_report and not args.project_dir:
        print("No report specified. Use --lint-xml, --ktlint-report, --project-dir, or --mock-sample.", file=sys.stderr)
        return 1

    summary = triage_issues(all_issues, max_warnings=args.max_warnings)
    json_str = json.dumps(summary, indent=2)

    if args.output:
        out_p = Path(args.output)
        out_p.parent.mkdir(parents=True, exist_ok=True)
        out_p.write_text(json_str, encoding="utf-8")
        print(f"Lint triage report saved to {out_p}")

    print(json_str)

    return 0 if summary["lint_clean"] else 1


if __name__ == "__main__":
    sys.exit(main())
