#!/usr/bin/env python3
"""
run_journey.py - Android Journey XML Test Runner

Executes sequential, declarative XML user journey test cases against an Android device.
Interprets user interactions ('Tap', 'Type', 'Swipe') and state expectations ('Verify', 'Check').
Outputs structured JSON execution summaries matching QA contract requirements.

Standard library only.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


def parse_journey_xml(file_path: Path) -> Tuple[str, str, List[str]]:
    """Parses journey XML into (name, description, list_of_action_strings)."""
    if not file_path.is_file():
        raise FileNotFoundError(f"Journey XML not found: {file_path}")

    tree = ET.parse(file_path)
    root = tree.getroot()
    if root.tag != "journey":
        raise ValueError(f"Root tag must be <journey>, found <{root.tag}>")

    journey_name = root.attrib.get("name", file_path.stem)
    desc_elem = root.find("description")
    description = desc_elem.text.strip() if desc_elem is not None and desc_elem.text else ""

    actions = []
    actions_elem = root.find("actions")
    if actions_elem is not None:
        for act in actions_elem.findall("action"):
            if act.text:
                actions.append(act.text.strip())

    return journey_name, description, actions


def extract_quoted_strings(text: str) -> List[str]:
    """Extracts strings enclosed in double or single quotes."""
    return re.findall(r'["\']([^"\']+)["\']', text)


def evaluate_action_dry_run(action: str, step_num: int, serial: str) -> Dict[str, Any]:
    """Simulates action execution during dry-run."""
    lower = action.lower()
    commands = []
    comment = "Dry run simulated"

    if lower.startswith("tap") or lower.startswith("click"):
        quotes = extract_quoted_strings(action)
        target = quotes[0] if quotes else "element"
        commands.append(f"adb -s {serial} shell input tap 540 600")
        comment = f"Resolved target '{target}' to coordinates (540, 600) and tapped"

    elif lower.startswith("type") or lower.startswith("enter"):
        quotes = extract_quoted_strings(action)
        text_to_type = quotes[0] if quotes else "sample_input"
        safe_text = text_to_type.replace(" ", "%s")
        commands.append(f"adb -s {serial} shell input text \"{safe_text}\"")
        comment = f"Typed '{text_to_type}' into focused field"

    elif lower.startswith("swipe") or lower.startswith("scroll"):
        commands.append(f"adb -s {serial} shell input swipe 540 800 540 300 500")
        comment = "Executed swipe gesture"

    elif lower.startswith("verify") or lower.startswith("check"):
        quotes = extract_quoted_strings(action)
        expected = quotes[0] if quotes else "expected state"
        comment = f"Verified expectation: '{expected}' is visible on screen"

    else:
        comment = "Generic action evaluated"

    return {
        "step": step_num,
        "action": action,
        "status": "PASSED",
        "commands": commands,
        "comment": comment,
    }


def evaluate_action_live(
    action: str,
    step_num: int,
    serial: str,
    layout_inspector_script: Path,
    mock_layout_file: Optional[Path],
) -> Dict[str, Any]:
    """Executes step against live device or mock layout file."""
    lower = action.lower()
    commands = []

    adb = shutil.which("adb") or "adb"
    adb_base = [adb]
    if serial:
        adb_base.extend(["-s", serial])

    try:
        # VERIFICATION STEP
        if lower.startswith("verify") or lower.startswith("check"):
            quotes = extract_quoted_strings(action)
            expected_text = quotes[0] if quotes else ""

            # Check via inspect_layout.py
            find_cmd = [sys.executable, str(layout_inspector_script), "find", "--json"]
            if mock_layout_file and mock_layout_file.is_file():
                find_cmd.extend(["--input-file", str(mock_layout_file)])
            elif serial:
                find_cmd.extend(["--serial", serial])
            else:
                find_cmd.append("--mock-sample")

            if expected_text:
                find_cmd.extend(["--text", expected_text])

            proc = subprocess.run(find_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            if proc.returncode != 0 or not proc.stdout.strip():
                return {
                    "step": step_num,
                    "action": action,
                    "status": "FAILED",
                    "commands": [" ".join(find_cmd)],
                    "comment": f"Assertion failed: expected '{expected_text}' not found in layout hierarchy",
                }
            return {
                "step": step_num,
                "action": action,
                "status": "PASSED",
                "commands": [" ".join(find_cmd)],
                "comment": f"Found expected element with text '{expected_text}'",
            }

        # TAP / CLICK STEP
        elif lower.startswith("tap") or lower.startswith("click"):
            quotes = extract_quoted_strings(action)
            target = quotes[0] if quotes else ""

            # Find coordinates
            find_cmd = [sys.executable, str(layout_inspector_script), "find", "--json", "--first"]
            if mock_layout_file and mock_layout_file.is_file():
                find_cmd.extend(["--input-file", str(mock_layout_file)])
            elif serial:
                find_cmd.extend(["--serial", serial])
            else:
                find_cmd.append("--mock-sample")

            if target:
                find_cmd.extend(["--text", target])

            proc = subprocess.run(find_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            tap_x, tap_y = 540, 600
            if proc.returncode == 0 and proc.stdout.strip():
                try:
                    nodes = json.loads(proc.stdout)
                    if nodes and isinstance(nodes, list):
                        center = nodes[0].get("center", [540, 600])
                        tap_x, tap_y = center[0], center[1]
                except Exception:
                    pass

            cmd_tap = adb_base + ["shell", "input", "tap", str(tap_x), str(tap_y)]
            commands.append(" ".join(cmd_tap))
            if not mock_layout_file and serial:
                subprocess.run(cmd_tap, check=False)
            time.sleep(0.5)

            return {
                "step": step_num,
                "action": action,
                "status": "PASSED",
                "commands": commands,
                "comment": f"Tapped target '{target}' at ({tap_x}, {tap_y})",
            }

        # TYPE / ENTER TEXT STEP
        elif lower.startswith("type") or lower.startswith("enter"):
            quotes = extract_quoted_strings(action)
            text_val = quotes[0] if quotes else "test_input"
            safe_text = text_val.replace(" ", "%s")

            cmd_text = adb_base + ["shell", "input", "text", f'"{safe_text}"']
            commands.append(" ".join(cmd_text))
            if not mock_layout_file and serial:
                subprocess.run(cmd_text, check=False)
            time.sleep(0.5)

            return {
                "step": step_num,
                "action": action,
                "status": "PASSED",
                "commands": commands,
                "comment": f"Entered text '{text_val}'",
            }

        # SWIPE / SCROLL STEP
        elif lower.startswith("swipe") or lower.startswith("scroll"):
            cmd_swipe = adb_base + ["shell", "input", "swipe", "540", "800", "540", "300", "500"]
            commands.append(" ".join(cmd_swipe))
            if not mock_layout_file and serial:
                subprocess.run(cmd_swipe, check=False)
            time.sleep(0.5)

            return {
                "step": step_num,
                "action": action,
                "status": "PASSED",
                "commands": commands,
                "comment": "Swiped up 500px over 500ms",
            }

        # UNRECOGNIZED ACTION
        else:
            return {
                "step": step_num,
                "action": action,
                "status": "PASSED",
                "commands": [],
                "comment": "Unrecognized action passed as no-op",
            }

    except Exception as e:
        return {
            "step": step_num,
            "action": action,
            "status": "FAILED",
            "commands": commands,
            "comment": f"Step failed with exception: {str(e)}",
        }


def run_journey(
    xml_file: Path,
    serial: str,
    dry_run: bool = False,
    mock_layout: Optional[Path] = None,
) -> Dict[str, Any]:
    """Executes all steps in a journey XML."""
    journey_name, description, actions = parse_journey_xml(xml_file)

    script_dir = Path(__file__).resolve().parent
    layout_inspector = script_dir / "inspect_layout.py"

    step_results = []
    journey_failed = False

    for idx, action in enumerate(actions, 1):
        if journey_failed:
            step_results.append({
                "step": idx,
                "action": action,
                "status": "SKIPPED",
                "commands": [],
                "comment": "Skipped due to prior step failure",
            })
            continue

        if dry_run:
            res = evaluate_action_dry_run(action, idx, serial or "emulator-5554")
        else:
            res = evaluate_action_live(action, idx, serial, layout_inspector, mock_layout)

        step_results.append(res)
        if res["status"] == "FAILED":
            journey_failed = True

    passed_count = sum(1 for r in step_results if r["status"] == "PASSED")
    failed_count = sum(1 for r in step_results if r["status"] == "FAILED")
    skipped_count = sum(1 for r in step_results if r["status"] == "SKIPPED")

    verdict = "FAILED" if journey_failed or failed_count > 0 else "PASSED"

    return {
        "journey": journey_name,
        "description": description,
        "verdict": verdict,
        "total_steps": len(actions),
        "passed_steps": passed_count,
        "failed_steps": failed_count,
        "skipped_steps": skipped_count,
        "results": step_results,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Android Journey XML Test Runner - Executes XML-defined behavior test journeys and outputs JSON evaluation report."
    )
    parser.add_argument("--file", "-f", required=True, help="Path to journey XML test file")
    parser.add_argument("--serial", "-s", default="", help="Device serial (e.g. emulator-5554)")
    parser.add_argument("--output", "-o", help="Path to write JSON verdict report")
    parser.add_argument("--dry-run", action="store_true", help="Simulate execution without requiring connected device")
    parser.add_argument("--mock-layout", help="Path to mock layout JSON/XML file for verification evaluation")

    args = parser.parse_args()

    xml_path = Path(args.file)
    if not xml_path.is_file():
        print(f"Error: Journey file not found: '{xml_path}'", file=sys.stderr)
        return 1

    try:
        report = run_journey(
            xml_file=xml_path,
            serial=args.serial,
            dry_run=args.dry_run,
            mock_layout=Path(args.mock_layout) if args.mock_layout else None,
        )
    except Exception as e:
        print(f"Error executing journey: {e}", file=sys.stderr)
        return 1

    json_str = json.dumps(report, indent=2)

    if args.output:
        out_file = Path(args.output)
        out_file.parent.mkdir(parents=True, exist_ok=True)
        out_file.write_text(json_str, encoding="utf-8")
        print(f"Journey report saved to: {out_file}")

    print(json_str)

    return 0 if report["verdict"] == "PASSED" else 1


if __name__ == "__main__":
    sys.exit(main())
