#!/usr/bin/env python3
"""
inspect_layout.py - Android UI Hierarchy Inspector & Coordinate Resolver

Dumps on-screen UI hierarchy to a flat JSON list, calculates element centers,
performs layout diffing, searches for elements by text or ID, and captures screenshots.

Standard library only. Works with 'android layout', live ADB, or offline XML/JSON files.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

CACHE_FILE = Path.home() / ".android_layout_cache.json"


def run_cmd(cmd: List[str], timeout: int = 30) -> Tuple[int, str, str]:
    """Runs a subprocess command returning (exit_code, stdout, stderr)."""
    try:
        proc = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout,
            encoding="utf-8",
            errors="replace",
        )
        return proc.returncode, proc.stdout.strip(), proc.stderr.strip()
    except Exception as e:
        return 1, "", str(e)


def parse_bounds(bounds_str: str) -> Tuple[int, int, int, int]:
    """Parses bounds string '[x1,y1][x2,y2]' into (x1, y1, x2, y2)."""
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds_str.strip())
    if match:
        return (
            int(match.group(1)),
            int(match.group(2)),
            int(match.group(3)),
            int(match.group(4)),
        )
    return (0, 0, 0, 0)


def calculate_center(x1: int, y1: int, x2: int, y2: int) -> List[int]:
    """Calculates [center_x, center_y]."""
    return [(x1 + x2) // 2, (y1 + y2) // 2]


def node_to_dict(elem: ET.Element, key_counter: List[int]) -> Dict[str, Any]:
    """Converts a uiautomator XML node into the standard element dictionary."""
    key_counter[0] += 1
    attrib = elem.attrib

    bounds_str = attrib.get("bounds", "[0,0][0,0]")
    x1, y1, x2, y2 = parse_bounds(bounds_str)
    center = calculate_center(x1, y1, x2, y2)

    interactions = []
    for action in ("clickable", "focusable", "scrollable", "checkable", "long-clickable", "password"):
        if attrib.get(action) == "true":
            interactions.append(action)

    state = []
    for s in ("checked", "focused", "selected"):
        if attrib.get(s) == "true":
            state.append(s)

    # Off-screen if zero area or outside typical screen
    off_screen = (x1 >= x2 or y1 >= y2) or attrib.get("visible-to-user") == "false"

    return {
        "key": key_counter[0],
        "class": attrib.get("class", ""),
        "resourceId": attrib.get("resource-id", ""),
        "text": attrib.get("text", ""),
        "contentDesc": attrib.get("content-desc", ""),
        "interactions": interactions,
        "state": state,
        "bounds": bounds_str,
        "center": center,
        "off-screen": off_screen,
    }


def parse_hierarchy_xml(xml_content: str) -> List[Dict[str, Any]]:
    """Recursively parses uiautomator XML into a flat list of UI elements."""
    elements: List[Dict[str, Any]] = []
    key_counter = [1000]

    try:
        root = ET.fromstring(xml_content)
    except Exception as e:
        sys.stderr.write(f"XML parse error: {e}\n")
        return []

    def traverse(node: ET.Element):
        # We record any node that has class, text, resource-id, or interactive flags
        if node.tag == "node":
            item = node_to_dict(node, key_counter)
            elements.append(item)
        for child in node:
            traverse(child)

    traverse(root)
    return elements


def dump_from_device(serial: Optional[str]) -> Tuple[bool, str, List[Dict[str, Any]]]:
    """Dumps layout using android layout or direct adb uiautomator."""
    # 1. Try 'android layout' CLI if in path
    android_cli = shutil.which("android")
    if android_cli:
        cmd = [android_cli, "layout"]
        if serial:
            cmd.extend([f"--device={serial}"])
        code, stdout, stderr = run_cmd(cmd, timeout=30)
        if code == 0 and stdout:
            try:
                data = json.loads(stdout)
                if isinstance(data, list):
                    return True, "Success via android-cli", data
            except json.JSONDecodeError:
                pass

    # 2. Fallback to ADB uiautomator dump
    adb = shutil.which("adb")
    if not adb:
        # Check standard local paths
        if sys.platform.startswith("win"):
            local_app = os.environ.get("LOCALAPPDATA", "")
            adb_cand = Path(local_app) / "Android" / "Sdk" / "platform-tools" / "adb.exe"
            if adb_cand.is_file():
                adb = str(adb_cand)

    if not adb:
        return False, "Neither android-cli nor adb available in environment", []

    adb_base = [adb]
    if serial:
        adb_base.extend(["-s", serial])

    # Trigger dump on device
    dump_cmd = adb_base + ["shell", "uiautomator", "dump", "/sdcard/window_dump.xml"]
    code, stdout, stderr = run_cmd(dump_cmd, timeout=25)
    if code != 0:
        return False, f"uiautomator dump failed: {stderr or stdout}", []

    # Read dump file
    cat_cmd = adb_base + ["shell", "cat", "/sdcard/window_dump.xml"]
    code, xml_out, stderr = run_cmd(cat_cmd, timeout=15)
    if code != 0 or not xml_out:
        return False, f"Failed to retrieve window_dump.xml: {stderr}", []

    elements = parse_hierarchy_xml(xml_out)
    return True, "Success via adb uiautomator", elements


def diff_elements(current: List[Dict[str, Any]], previous: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Returns elements that are new or have changed state, text, or bounds."""
    prev_map = {}
    for p in previous:
        ident = (p.get("resourceId"), p.get("class"), p.get("text"))
        prev_map[ident] = p

    changed = []
    for c in current:
        ident = (c.get("resourceId"), c.get("class"), c.get("text"))
        if ident not in prev_map:
            changed.append(c)
        else:
            prev = prev_map[ident]
            if (
                prev.get("state") != c.get("state")
                or prev.get("bounds") != c.get("bounds")
                or prev.get("interactions") != c.get("interactions")
            ):
                changed.append(c)
    return changed


def cmd_dump(args: argparse.Namespace) -> int:
    """Handles 'dump' subcommand."""
    elements: List[Dict[str, Any]] = []

    # If an offline XML or JSON source is provided
    if args.xml_source:
        xml_path = Path(args.xml_source)
        if not xml_path.is_file():
            print(f"Error: File not found '{xml_path}'", file=sys.stderr)
            return 1
        content = xml_path.read_text(encoding="utf-8", errors="replace")
        if xml_path.suffix.lower() == ".json":
            elements = json.loads(content)
        else:
            elements = parse_hierarchy_xml(content)
    else:
        success, msg, elements = dump_from_device(args.serial)
        if not success:
            if args.mock_sample:
                # Provide synthetic elements for testing / validation when device is absent
                elements = get_sample_elements()
            else:
                print(f"Error: {msg}", file=sys.stderr)
                return 1

    # Diffing logic
    if args.diff:
        prev_elements = []
        cache_path = Path(args.previous) if args.previous else CACHE_FILE
        if cache_path.is_file():
            try:
                prev_elements = json.loads(cache_path.read_text(encoding="utf-8"))
            except Exception:
                pass
        output_elements = diff_elements(elements, prev_elements)
    else:
        output_elements = elements

    # Save cache for future diffs
    try:
        CACHE_FILE.write_text(json.dumps(elements), encoding="utf-8")
    except Exception:
        pass

    indent = 2 if args.pretty else None
    json_str = json.dumps(output_elements, indent=indent)

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json_str, encoding="utf-8")
        print(f"Layout written to {out_path} ({len(output_elements)} elements)")
    else:
        print(json_str)

    return 0


def cmd_find(args: argparse.Namespace) -> int:
    """Searches for elements matching criteria and outputs target center coordinates."""
    elements: List[Dict[str, Any]] = []
    if args.input_file:
        in_path = Path(args.input_file)
        if not in_path.is_file():
            print(f"Error: File not found '{in_path}'", file=sys.stderr)
            return 1
        content = in_path.read_text(encoding="utf-8")
        if in_path.suffix.lower() == ".json":
            elements = json.loads(content)
        else:
            elements = parse_hierarchy_xml(content)
    else:
        success, msg, elements = dump_from_device(args.serial)
        if not success:
            if args.mock_sample:
                elements = get_sample_elements()
            else:
                print(f"Error: {msg}", file=sys.stderr)
                return 1

    matches = []
    for elem in elements:
        if args.text and args.text.lower() not in elem.get("text", "").lower():
            continue
        if args.resource_id and args.resource_id not in elem.get("resourceId", ""):
            continue
        if args.content_desc and args.content_desc.lower() not in elem.get("contentDesc", "").lower():
            continue
        if args.class_name and args.class_name not in elem.get("class", ""):
            continue
        matches.append(elem)

    if not matches:
        print("No matching elements found.", file=sys.stderr)
        return 1

    if args.first:
        matches = [matches[0]]

    if args.json:
        print(json.dumps(matches, indent=2))
    else:
        for idx, m in enumerate(matches, 1):
            center = m.get("center", [0, 0])
            print(f"Match #{idx}:")
            print(f"  Class:       {m.get('class')}")
            print(f"  Resource ID: {m.get('resourceId')}")
            print(f"  Text:        '{m.get('text')}'")
            print(f"  ContentDesc: '{m.get('contentDesc')}'")
            print(f"  Bounds:      {m.get('bounds')}")
            print(f"  Center:      {center[0]} {center[1]}")
            print(f"  State:       {', '.join(m.get('state', [])) or 'none'}")
            print(f"  Tap Command: adb shell input tap {center[0]} {center[1]}")
    return 0


def cmd_capture_screen(args: argparse.Namespace) -> int:
    """Captures screenshot using adb or android-cli."""
    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    android_cli = shutil.which("android")
    if android_cli:
        cmd = [android_cli, "screen", "capture", "-o", str(out_path)]
        if args.serial:
            cmd.extend([f"--device={args.serial}"])
        code, stdout, stderr = run_cmd(cmd)
        if code == 0:
            print(f"Screenshot saved to {out_path} via android-cli.")
            return 0

    # Fallback to ADB screencap
    adb = shutil.which("adb")
    if not adb and sys.platform.startswith("win"):
        local_app = os.environ.get("LOCALAPPDATA", "")
        adb_cand = Path(local_app) / "Android" / "Sdk" / "platform-tools" / "adb.exe"
        if adb_cand.is_file():
            adb = str(adb_cand)

    if not adb:
        print("Error: adb not found for screenshot capture.", file=sys.stderr)
        return 1

    adb_base = [adb]
    if args.serial:
        adb_base.extend(["-s", args.serial])

    # Use exec-out screencap -p
    try:
        with open(out_path, "wb") as f:
            proc = subprocess.run(
                adb_base + ["exec-out", "screencap", "-p"],
                stdout=f,
                stderr=subprocess.PIPE,
                timeout=30,
            )
            if proc.returncode == 0 and out_path.stat().st_size > 0:
                print(f"Screenshot saved to {out_path} ({out_path.stat().st_size} bytes).")
                return 0
            else:
                print(f"screencap failed: {proc.stderr.decode('utf-8', errors='replace')}", file=sys.stderr)
                return 1
    except Exception as e:
        print(f"Failed to capture screen: {e}", file=sys.stderr)
        return 1


def get_sample_elements() -> List[Dict[str, Any]]:
    """Synthetic layout hierarchy for headless test environments."""
    return [
        {
            "key": 1001,
            "class": "android.widget.TextView",
            "resourceId": "com.example.profile:id/title_header",
            "text": "User Profile",
            "contentDesc": "Profile Screen Header",
            "interactions": [],
            "state": [],
            "bounds": "[48,96][1032,180]",
            "center": [540, 138],
            "off-screen": False,
        },
        {
            "key": 1002,
            "class": "android.widget.EditText",
            "resourceId": "com.example.profile:id/username_input",
            "text": "Alex Rivera",
            "contentDesc": "Username input field",
            "interactions": ["clickable", "focusable"],
            "state": ["focused"],
            "bounds": "[48,220][1032,320]",
            "center": [540, 270],
            "off-screen": False,
        },
        {
            "key": 1003,
            "class": "android.widget.Button",
            "resourceId": "com.example.profile:id/save_preferences_button",
            "text": "Save Preferences",
            "contentDesc": "Save Preferences Button",
            "interactions": ["clickable", "focusable"],
            "state": [],
            "bounds": "[200,600][880,720]",
            "center": [540, 660],
            "off-screen": False,
        },
    ]


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Android UI Layout Hierarchy Inspector - Dumps UI elements, calculates tap coordinates, and diffs layout states."
    )
    subparsers = parser.add_subparsers(dest="command", help="Subcommand to execute")

    # dump
    p_dump = subparsers.add_parser("dump", help="Dump UI hierarchy to JSON")
    p_dump.add_argument("--serial", help="Device serial number")
    p_dump.add_argument("--output", "-o", help="Output file path for JSON")
    p_dump.add_argument("--pretty", "-p", action="store_true", help="Pretty print JSON output")
    p_dump.add_argument("--diff", "-d", action="store_true", help="Dump only changed elements since previous call")
    p_dump.add_argument("--previous", help="Path to previous JSON dump for diffing")
    p_dump.add_argument("--xml-source", help="Use local XML/JSON file instead of live device")
    p_dump.add_argument("--mock-sample", action="store_true", help="Use synthetic sample layout if device is absent")

    # find
    p_find = subparsers.add_parser("find", help="Find element in layout and calculate center coordinates")
    p_find.add_argument("--serial", help="Device serial number")
    p_find.add_argument("--text", help="Match text (substring, case-insensitive)")
    p_find.add_argument("--resource-id", help="Match resource-id")
    p_find.add_argument("--content-desc", help="Match contentDescription")
    p_find.add_argument("--class-name", help="Match class name")
    p_find.add_argument("--input-file", help="Path to JSON or XML file containing layout")
    p_find.add_argument("--first", action="store_true", help="Return only the first matching element")
    p_find.add_argument("--json", action="store_true", help="Output match results as JSON")
    p_find.add_argument("--mock-sample", action="store_true", help="Use synthetic sample layout if device is absent")

    # capture-screen
    p_screen = subparsers.add_parser("capture-screen", help="Capture screenshot to PNG")
    p_screen.add_argument("--serial", help="Device serial number")
    p_screen.add_argument("--output", "-o", required=True, help="Destination path for PNG file")

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return 0

    if args.command == "dump":
        return cmd_dump(args)
    elif args.command == "find":
        return cmd_find(args)
    elif args.command == "capture-screen":
        return cmd_capture_screen(args)

    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
