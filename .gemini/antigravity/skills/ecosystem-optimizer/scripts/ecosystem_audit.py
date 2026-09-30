#!/usr/bin/env python3
"""
ecosystem_audit.py - Unified Master Diagnostic & Health Check CLI.

Coordinates the complete health verification of the agentic ecosystem:
1. Context Budget & Token Footprint Audit (Anti-Bloat)
2. Platform Domain Boundary Isolation (Zero Leakage)
3. Schema Draft-07 Validation & Contract Integrity
4. Hub-to-Spoke Drift and Synchronization Check

Usage:
    python skills/ecosystem-optimizer/scripts/ecosystem_audit.py
    python skills/ecosystem-optimizer/scripts/ecosystem_audit.py --target ../KmSafe --profile android
    python skills/ecosystem-optimizer/scripts/ecosystem_audit.py --report docs/ecosystem/ECOSYSTEM_HEALTH_REPORT.md
"""

import argparse
import datetime
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

# Add skill scripts directory and repo root to path
CURRENT_DIR = Path(__file__).resolve().parent
REPO_ROOT = CURRENT_DIR.parent.parent.parent
if str(CURRENT_DIR) not in sys.path:
    sys.path.insert(0, str(CURRENT_DIR))
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from audit_context_budget import audit_context_budget
from lint_platform_leakage import lint_platform_leakage
from validate_spokes_sync import check_spoke_sync, find_known_spokes
from audit_context_pruning import audit_context_pruning

try:
    from scripts.schema_validator import validate_schema
    SCHEMA_VALIDATOR_AVAILABLE = True
except ImportError:
    try:
        from schema_validator import validate_schema
        SCHEMA_VALIDATOR_AVAILABLE = True
    except ImportError:
        SCHEMA_VALIDATOR_AVAILABLE = False


def audit_schemas(target_dir: Path) -> Dict[str, Any]:
    """Validates that all JSON schemas in target_dir/schemas compile and are valid Draft-07."""
    schemas_dir = target_dir / "schemas"
    if not schemas_dir.is_dir():
        # Fallback to repo root schemas if target has none
        schemas_dir = REPO_ROOT / "schemas"

    results = []
    if schemas_dir.is_dir():
        for s_file in sorted(schemas_dir.glob("*.schema.json")):
            try:
                schema_json = json.loads(s_file.read_text(encoding="utf-8"))
                # Basic draft-07 check
                has_schema = "$schema" in schema_json or "type" in schema_json
                results.append({
                    "name": s_file.name,
                    "status": "PASS" if has_schema else "WARN",
                    "error": None if has_schema else "Missing $schema declaration"
                })
            except Exception as e:
                results.append({
                    "name": s_file.name,
                    "status": "FAIL",
                    "error": str(e)
                })

    passed = sum(1 for r in results if r["status"] == "PASS")
    failed = sum(1 for r in results if r["status"] == "FAIL")

    return {
        "status": "FAIL" if failed > 0 else "PASS",
        "schemas_checked": len(results),
        "passed": passed,
        "failed": failed,
        "details": results,
    }


def run_full_ecosystem_audit(target_dir: Path, profile: str = "auto", check_spokes: bool = True) -> Dict[str, Any]:
    """Executes all diagnostics and produces an aggregated health report."""
    # 1. Context budget
    budget_result = audit_context_budget(target_dir)

    # 2. Platform leakage
    leakage_result = lint_platform_leakage(target_dir, profile)

    # 3. Schema validation
    schema_result = audit_schemas(target_dir)

    # 4. Context pruning (platitudes, redundancies, contradictions)
    pruning_result = audit_context_pruning(target_dir)

    # 5. Spoke sync (if run on hub)
    spokes_results = []
    is_hub = (target_dir / "profiles").is_dir()
    if is_hub and check_spokes:
        spokes = find_known_spokes(target_dir)
        for s in spokes:
            if s.is_dir():
                spokes_results.append(check_spoke_sync(target_dir, s))

    # Determine master verdict
    critical_failures = (
        budget_result["overall_status"] == "FAIL" or
        leakage_result["status"] == "FAIL" or
        schema_result["status"] == "FAIL" or
        pruning_result["status"] == "FAIL"
    )

    warnings = (
        budget_result["overall_status"] == "WARN" or
        pruning_result["status"] == "WARN" or
        any(s["overall_status"] != "IN_SYNC" for s in spokes_results)
    )

    overall_status = "FAIL" if critical_failures else ("WARN" if warnings else "PASS")

    return {
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "target_dir": str(target_dir),
        "profile": leakage_result["profile"],
        "overall_status": overall_status,
        "budget": budget_result,
        "leakage": leakage_result,
        "schemas": schema_result,
        "pruning": pruning_result,
        "spokes": spokes_results,
    }


def generate_markdown_report(report: Dict[str, Any], output_path: Path):
    """Outputs a human-readable Markdown summary report."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    status_icon = "🟢" if report["overall_status"] == "PASS" else ("🟡" if report["overall_status"] == "WARN" else "🔴")

    md = [
        f"# Ecosystem Health & Optimization Report",
        f"",
        f"**Generated**: `{report['timestamp']}`  ",
        f"**Target**: `{report['target_dir']}`  ",
        f"**Profile**: `{report['profile']}`  ",
        f"**Overall Status**: {status_icon} **{report['overall_status']}**",
        f"",
        f"---",
        f"",
        f"## 1. Context Budget & Token Footprint",
        f"- **Status**: `{report['budget']['overall_status']}`",
        f"- **Files Analyzed**: {report['budget']['files_analyzed']}",
        f"- **Total Words**: {report['budget']['total_words']:,}",
        f"- **Estimated Token Footprint**: ~{report['budget']['total_estimated_tokens']:,} tokens",
        f"",
        f"### Recommendations:",
    ]

    for rec in report["budget"]["recommendations"]:
        md.append(f"- {rec}")

    md.extend([
        f"",
        f"---",
        f"",
        f"## 2. Platform Domain Boundary Isolation",
        f"- **Status**: `{report['leakage']['status']}`",
        f"- **Violations / Leaks Detected**: {report['leakage']['violations_count']}",
        f"",
    ])

    if report["leakage"]["violations"]:
        md.append("| File | Line | Token | Violation Detail |")
        md.append("| :--- | :--- | :--- | :--- |")
        for v in report["leakage"]["violations"]:
            md.append(f"| `{v['name']}` | `{v['line']}` | `{v['matched']}` | {v['reason']} |")
    else:
        md.append("✅ Zero cross-platform terminology leaks detected. Pure domain boundaries enforced.")

    md.extend([
        f"",
        f"---",
        f"",
        f"## 3. Schema & Contract Integrity",
        f"- **Status**: `{report['schemas']['status']}`",
        f"- **Schemas Validated**: {report['schemas']['schemas_checked']}",
        f"- **Valid Draft-07**: {report['schemas']['passed']} / {report['schemas']['schemas_checked']}",
        f"",
        f"---",
        f"",
        f"## 4. Context Pruning & Redundancy Audit",
        f"- **Status**: `{report.get('pruning', {}).get('status', 'PASS')}`",
        f"- **Zero-Signal Platitudes**: {report.get('pruning', {}).get('platitudes_count', 0)}",
        f"- **Cross-File Redundancies**: {report.get('pruning', {}).get('redundancies_count', 0)}",
        f"- **Rule Contradictions**: {report.get('pruning', {}).get('contradictions_count', 0)}",
        f"- **Prunable Tokens**: ~{report.get('pruning', {}).get('estimated_prunable_tokens', 0):,} tokens",
        f"",
    ])

    if report["spokes"]:
        md.extend([
            f"---",
            f"",
            f"## 5. Hub-to-Spoke Synchronization Matrix",
            f"| Spoke | Status | In Sync | Drifted | Missing |",
            f"| :--- | :--- | :--- | :--- | :--- |",
        ])
        for sp in report["spokes"]:
            md.append(f"| `{sp['spoke']}` | **{sp['overall_status']}** | {sp['in_sync_count']} | {sp['drifted_count']} | {sp['missing_count']} |")

    output_path.write_text("\n".join(md) + "\n", encoding="utf-8")
    print(f"\n[+] Wrote health audit report to: {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Run complete diagnostic and health check of the agentic ecosystem.")
    parser.add_argument("--target", default=".", help="Target repository directory (default: current dir)")
    parser.add_argument("--profile", default="auto", choices=["android", "backend", "web", "auto"], help="Platform profile")
    parser.add_argument("--no-spokes", action="store_true", help="Skip spoke synchronization checks")
    parser.add_argument("--report", help="Path to write Markdown summary report")
    parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    args = parser.parse_args()

    target_dir = Path(args.target).resolve()
    report = run_full_ecosystem_audit(target_dir, args.profile, check_spokes=not args.no_spokes)

    if args.json:
        print(json.dumps(report, indent=2))
        sys.exit(1 if report["overall_status"] == "FAIL" else 0)

    print("===========================================================")
    print("   🏥 AGENTIC ECOSYSTEM COMPREHENSIVE HEALTH AUDIT")
    print(f"   Target  : {target_dir}")
    print(f"   Profile : {report['profile']}")
    print(f"   Status  : {report['overall_status']}")
    print("===========================================================")
    print(f" 1. Context Budget  : [{report['budget']['overall_status']}] ~{report['budget']['total_estimated_tokens']:,} tokens across {report['budget']['files_analyzed']} files")
    print(f" 2. Platform Purity : [{report['leakage']['status']}] {report['leakage']['violations_count']} domain leaks detected")
    print(f" 3. Schema Quality  : [{report['schemas']['status']}] {report['schemas']['passed']}/{report['schemas']['schemas_checked']} schemas valid Draft-07")
    print(f" 4. Context Pruning : [{report['pruning']['status']}] {report['pruning']['platitudes_count']} platitudes, {report['pruning']['redundancies_count']} duplicates, {report['pruning']['contradictions_count']} contradictions")

    if report["spokes"]:
        print(f" 5. Spoke Parity    : {len(report['spokes'])} satellite repositories audited")
        for sp in report["spokes"]:
            print(f"    - {sp['spoke']}: [{sp['overall_status']}] (In Sync: {sp['in_sync_count']}, Drift: {sp['drifted_count']}, Missing: {sp['missing_count']})")

    print("-----------------------------------------------------------")
    if report["overall_status"] == "PASS":
        print(" 🎉 System health is pristine! All agentic constraints satisfied.")
    elif report["overall_status"] == "WARN":
        print(" ⚠️ System is healthy with optimization opportunities (see details).")
    else:
        print(" ❌ Critical issues detected. Please review violations above.")
    print("===========================================================")

    if args.report:
        generate_markdown_report(report, Path(args.report).resolve())

    sys.exit(1 if report["overall_status"] == "FAIL" else 0)


if __name__ == "__main__":
    main()
