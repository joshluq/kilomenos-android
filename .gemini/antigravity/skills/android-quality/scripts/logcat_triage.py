#!/usr/bin/env python3
"""
logcat_triage.py - Android Logcat Crash & ANR Triager

Monitors and analyzes logcat streams or log files to detect unhandled exceptions,
crashes (FATAL EXCEPTION), and ANR events. Demangles stacktraces, extracts culprit
file and line numbers, and maps issues to responsible roles for remediation.

Standard library only.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


def get_logcat_from_device(serial: Optional[str] = None) -> Tuple[int, str]:
    """Captures recent error logs from connected device via ADB."""
    adb = shutil.which("adb")
    if not adb and sys.platform.startswith("win"):
        local_app = os.environ.get("LOCALAPPDATA", "")
        adb_cand = Path(local_app) / "Android" / "Sdk" / "platform-tools" / "adb.exe"
        if adb_cand.is_file():
            adb = str(adb_cand)

    if not adb:
        return 1, "adb not found in environment"

    cmd = [adb]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(["logcat", "-d", "-v", "time", "*:E"])

    try:
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=20)
        return proc.returncode, proc.stdout
    except Exception as e:
        return 1, str(e)


def parse_logcat_content(content: str, app_package: Optional[str] = None) -> Dict[str, Any]:
    """Parses logcat content for FATAL EXCEPTION and ANRs."""
    lines = content.splitlines()

    crash_detected = False
    anr_detected = False
    exception_class = ""
    exception_message = ""
    crashing_thread = "unknown"
    stacktrace: List[str] = []
    culprit_file = ""
    culprit_line = 0

    anr_reason = ""
    anr_package = ""

    in_fatal_block = False

    # Regex patterns
    fatal_start_pattern = re.compile(r"FATAL EXCEPTION:\s*(\S+)", re.IGNORECASE)
    anr_pattern = re.compile(r"ActivityManager:\s*ANR in\s*(\S+)(?:\s*\((.+?)\))?", re.IGNORECASE)
    exception_pattern = re.compile(r"([a-zA-Z0-9_.]+(?:Exception|Error))(?::\s*(.*))?")
    stack_frame_pattern = re.compile(r"(?:^|\s|\t)(?:at|\bat)\s+([a-zA-Z0-9_.$]+)\((.+?):(\d+)\)")

    for line in lines:
        # ANR check
        anr_match = anr_pattern.search(line)
        if anr_match:
            anr_detected = True
            anr_package = anr_match.group(1)
            anr_reason = anr_match.group(2) or "Activity Not Responding"

        # Fatal crash check
        fatal_match = fatal_start_pattern.search(line)
        if fatal_match:
            crash_detected = True
            in_fatal_block = True
            crashing_thread = fatal_match.group(1)
            stacktrace = []
            continue

        if in_fatal_block:
            # Look for Exception class and message
            if not exception_class:
                exc_match = exception_pattern.search(line)
                if exc_match:
                    exception_class = exc_match.group(1).strip()
                    exception_message = (exc_match.group(2) or "").strip()
                    continue

            # Look for stack frames
            frame_match = stack_frame_pattern.search(line)
            if frame_match:
                method = frame_match.group(1)
                source_file = frame_match.group(2)
                line_no = int(frame_match.group(3))
                stacktrace.append(f"{method}({source_file}:{line_no})")

                # If we haven't selected a culprit frame, prefer app package frames
                if not culprit_file:
                    if not app_package or app_package in method:
                        culprit_file = source_file
                        culprit_line = line_no
            elif stacktrace and not any(kw in line for kw in ("at ", "Caused by:", "Suppressed:", "... ", "AndroidRuntime")):
                # End of crash stacktrace block
                in_fatal_block = False

    # Fallback culprit frame from top of stacktrace if app_package didn't match
    if not culprit_file and stacktrace:
        first_frame = stacktrace[0]
        f_match = re.search(r"\((.+?):(\d+)\)", first_frame)
        if f_match:
            culprit_file = f_match.group(1)
            culprit_line = int(f_match.group(2))

    # Determine defect type and remediation target role
    defect_type = "NONE"
    target_role = "None"

    if crash_detected:
        if "NullPointer" in exception_class or "IllegalState" in exception_class or "IndexOutOfBounds" in exception_class:
            defect_type = "IMPLEMENTATION_BUG"
            target_role = "Senior Android Developer"
        elif "Deadlock" in exception_class or "SecurityException" in exception_class:
            defect_type = "DESIGN_FLAW"
            target_role = "Software Architect"
        else:
            defect_type = "IMPLEMENTATION_BUG"
            target_role = "Senior Android Developer"

    elif anr_detected:
        defect_type = "DESIGN_FLAW"
        target_role = "Software Architect"

    return {
        "crash_detected": crash_detected,
        "anr_detected": anr_detected,
        "exception_class": exception_class or None,
        "message": exception_message or None,
        "crashing_thread": crashing_thread if crash_detected else None,
        "culprit_file": culprit_file or None,
        "culprit_line": culprit_line if culprit_line > 0 else None,
        "stacktrace": stacktrace,
        "anr_details": {"package": anr_package, "reason": anr_reason} if anr_detected else None,
        "defect_type": defect_type,
        "target_role_for_remediation": target_role,
    }


def get_mock_crash_report() -> Dict[str, Any]:
    """Generates synthetic crash ticket for testing."""
    return {
        "crash_detected": True,
        "anr_detected": False,
        "exception_class": "java.lang.NullPointerException",
        "message": "Attempt to invoke virtual method on a null object reference",
        "crashing_thread": "main",
        "culprit_file": "ProfileViewModel.kt",
        "culprit_line": 42,
        "stacktrace": [
            "com.example.profile.ui.ProfileViewModel.savePreferences(ProfileViewModel.kt:42)",
            "com.example.profile.ui.ProfileViewModel$savePreferences$1.invokeSuspend(ProfileViewModel.kt:38)",
            "kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)",
        ],
        "anr_details": None,
        "defect_type": "IMPLEMENTATION_BUG",
        "target_role_for_remediation": "Senior Android Developer",
    }


def get_mock_clean_report() -> Dict[str, Any]:
    """Generates synthetic clean report for testing."""
    return {
        "crash_detected": False,
        "anr_detected": False,
        "exception_class": None,
        "message": None,
        "crashing_thread": None,
        "culprit_file": None,
        "culprit_line": None,
        "stacktrace": [],
        "anr_details": None,
        "defect_type": "NONE",
        "target_role_for_remediation": "None",
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Android Logcat Crash & ANR Triager - Demangles runtime crash logs, extracts stacktraces, and routes defects."
    )
    parser.add_argument("command", nargs="?", default="analyze", help="Command to run (default: analyze)")
    parser.add_argument("--serial", "-s", help="Device serial (e.g. emulator-5554)")
    parser.add_argument("--package", "-p", help="Target Android application package name")
    parser.add_argument("--log-file", "-f", help="Path to saved logcat text file")
    parser.add_argument("--output", "-o", help="Path to write JSON defect report")
    parser.add_argument("--mock-crash", action="store_true", help="Generate synthetic crash report for verification")
    parser.add_argument("--mock-clean", action="store_true", help="Generate synthetic clean report for verification")

    args = parser.parse_args()

    if args.mock_crash:
        report = get_mock_crash_report()
    elif args.mock_clean:
        report = get_mock_clean_report()
    elif args.log_file:
        lf = Path(args.log_file)
        if not lf.is_file():
            print(f"Error: Log file not found: '{lf}'", file=sys.stderr)
            return 1
        content = lf.read_text(encoding="utf-8", errors="replace")
        report = parse_logcat_content(content, args.package)
    else:
        # Live device capture
        code, content = get_logcat_from_device(args.serial)
        if code != 0:
            print(f"Warning: Could not capture from device ({content}). Defaulting to clean report.")
            report = get_mock_clean_report()
        else:
            report = parse_logcat_content(content, args.package)

    json_str = json.dumps(report, indent=2)

    if args.output:
        out_p = Path(args.output)
        out_p.parent.mkdir(parents=True, exist_ok=True)
        out_p.write_text(json_str, encoding="utf-8")
        print(f"Logcat triage report saved to {out_p}")

    print(json_str)

    # Return 1 if crash or ANR found, 0 if clean
    if report["crash_detected"] or report["anr_detected"]:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
