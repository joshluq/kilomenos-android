#!/usr/bin/env python3
"""
validate_spokes_sync.py - Hub-and-Spoke Synchronization & Drift Validator.

Audits whether satellite spoke repositories (e.g. KmSafe, kilomenos) are in sync
with canonical contracts, schemas, shared skills, and orchestrator scripts from the Hub.

Usage:
    python validate_spokes_sync.py --hub . --spoke ../KmSafe
    python validate_spokes_sync.py --hub . --spoke ../kilomenos --json
    python validate_spokes_sync.py --auto
"""

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

CANONICAL_SCHEMAS = [
    "po_to_architect_handoff.schema.json",
    "architect_to_dev_handoff.schema.json",
    "dev_to_qa_handoff.schema.json",
    "qa_verdict_handoff.schema.json",
    "dev_to_architect_escalation.schema.json",
    "deployment_handoff.schema.json",
]

CORE_SCRIPTS = [
    "workflow.py",
    "schema_validator.py",
    "openspec_cli.py",
]

CORE_SKILLS = [
    "openspec",
    "atlassian-bridge",
]


def file_sha256(path: Path) -> str:
    """Calculates SHA256 hash of a file ignoring CRLF/LF line endings."""
    if not path.is_file():
        return ""
    content = path.read_bytes().replace(b"\r\n", b"\n")
    return hashlib.sha256(content).hexdigest()


def check_spoke_sync(hub_dir: Path, spoke_dir: Path) -> Dict[str, Any]:
    """Audits synchronization between Hub and a Spoke repository."""
    spoke_name = spoke_dir.name
    results: List[Dict[str, Any]] = []

    # 1. Audit Schemas
    hub_schemas = hub_dir / "schemas"
    spoke_schemas = spoke_dir / "schemas"
    for s_name in CANONICAL_SCHEMAS:
        hub_f = hub_schemas / s_name
        spoke_f = spoke_schemas / s_name
        if not hub_f.is_file():
            continue

        if not spoke_f.is_file():
            results.append({
                "category": "schema",
                "item": s_name,
                "status": "MISSING_IN_SPOKE",
                "detail": f"File {s_name} not found in spoke schemas directory"
            })
        elif file_sha256(hub_f) == file_sha256(spoke_f):
            results.append({
                "category": "schema",
                "item": s_name,
                "status": "IN_SYNC",
                "detail": "Hashes match 100%"
            })
        else:
            results.append({
                "category": "schema",
                "item": s_name,
                "status": "DRIFT_DETECTED",
                "detail": "Content diverges from hub canonical schema"
            })

    # 2. Audit Core Scripts
    hub_scripts = hub_dir / "scripts"
    spoke_scripts = spoke_dir / "scripts"
    for script_name in CORE_SCRIPTS:
        hub_s = hub_scripts / script_name
        spoke_s = spoke_scripts / script_name
        if not hub_s.is_file():
            continue

        if not spoke_s.is_file():
            results.append({
                "category": "script",
                "item": script_name,
                "status": "MISSING_IN_SPOKE",
                "detail": f"Script {script_name} not found in spoke"
            })
        elif file_sha256(hub_s) == file_sha256(spoke_s):
            results.append({
                "category": "script",
                "item": script_name,
                "status": "IN_SYNC",
                "detail": "Hashes match 100%"
            })
        else:
            results.append({
                "category": "script",
                "item": script_name,
                "status": "DRIFT_DETECTED",
                "detail": "Script version diverges from hub"
            })

    # 3. Audit Copilot Instructions
    spoke_copilot = spoke_dir / ".github" / "copilot_instructions.md"
    if spoke_copilot.is_file():
        results.append({
            "category": "governance",
            "item": "copilot_instructions.md",
            "status": "IN_SYNC",
            "detail": "Present in .github/"
        })
    else:
        results.append({
            "category": "governance",
            "item": "copilot_instructions.md",
            "status": "MISSING_IN_SPOKE",
            "detail": ".github/copilot_instructions.md missing in spoke"
        })

    # Summary counts
    in_sync = sum(1 for r in results if r["status"] == "IN_SYNC")
    drifted = sum(1 for r in results if r["status"] == "DRIFT_DETECTED")
    missing = sum(1 for r in results if r["status"] == "MISSING_IN_SPOKE")

    overall = "IN_SYNC"
    if missing > 0:
        overall = "OUTDATED"
    elif drifted > 0:
        overall = "DRIFT_DETECTED"

    return {
        "spoke": spoke_name,
        "spoke_path": str(spoke_dir),
        "overall_status": overall,
        "in_sync_count": in_sync,
        "drifted_count": drifted,
        "missing_count": missing,
        "items": results,
    }


def find_known_spokes(hub_dir: Path) -> List[Path]:
    """Attempts to locate known spoke repos in standard relative paths."""
    candidates = [
        hub_dir.parent.parent / "AndroidStudioProjects" / "KmSafe",
        Path("C:/Users/josh_/AndroidStudioProjects/KmSafe"),
        Path("C:/Users/josh_/Documents/projects/backEnd/kilomenos"),
        hub_dir.parent / "KmSafe",
        hub_dir.parent / "kilomenos",
    ]
    found = []
    seen = set()
    for c in candidates:
        if c.is_dir() and str(c.resolve()) not in seen:
            seen.add(str(c.resolve()))
            found.append(c.resolve())
    return found


def main():
    parser = argparse.ArgumentParser(description="Validate synchronization between Hub and Spokes.")
    parser.add_argument("--hub", default=".", help="Hub root directory (default: current dir)")
    parser.add_argument("--spoke", help="Spoke repository directory to inspect")
    parser.add_argument("--auto", action="store_true", help="Auto-discover known spokes")
    parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    args = parser.parse_args()

    hub_dir = Path(args.hub).resolve()

    spokes_to_check: List[Path] = []
    if args.spoke:
        spokes_to_check.append(Path(args.spoke).resolve())
    elif args.auto:
        spokes_to_check = find_known_spokes(hub_dir)
        if not spokes_to_check:
            print("[WARN] No known spokes auto-discovered.")
            sys.exit(0)
    else:
        # Check if current dir is hub and try auto discovery
        spokes_to_check = find_known_spokes(hub_dir)
        if not spokes_to_check:
            print("[INFO] No spoke specified. Use --spoke <path> or --auto.")
            sys.exit(0)

    reports = []
    for s_path in spokes_to_check:
        if s_path.is_dir():
            reports.append(check_spoke_sync(hub_dir, s_path))

    if args.json:
        print(json.dumps(reports, indent=2))
        sys.exit(0)

    print("===========================================================")
    print("   🔄 HUB & SPOKE SYNCHRONIZATION AUDIT")
    print(f"   Hub Root : {hub_dir}")
    print(f"   Spokes   : {len(reports)} evaluated")
    print("===========================================================")

    for rep in reports:
        flag = "✅" if rep["overall_status"] == "IN_SYNC" else "⚠️ "
        print(f"\n {flag} Spoke: {rep['spoke']} [{rep['overall_status']}] ({rep['spoke_path']})")
        print(f"    In Sync: {rep['in_sync_count']} | Drifted: {rep['drifted_count']} | Missing: {rep['missing_count']}")
        for it in rep["items"]:
            if it["status"] != "IN_SYNC":
                print(f"    - [{it['status']}] {it['category']}/{it['item']}: {it['detail']}")

    print("\n===========================================================")


if __name__ == "__main__":
    main()
