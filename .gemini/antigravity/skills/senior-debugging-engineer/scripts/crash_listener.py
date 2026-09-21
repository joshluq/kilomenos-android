#!/usr/bin/env python3
"""
crash_listener.py - Real-Time Crash Listener and Auto-Triage Tool for Android

Monitors, captures, and triages Android crashes (FATAL EXCEPTION, ANR, Native Tombstones)
from ADB Logcat, isolates the exact root-cause file and line number within the application,
and generates SDD-compliant defect tickets ready for the /fix-bug remediation workflow.

Usage:
    python crash_listener.py --dump --package es.joshluq.kmsafe
    python crash_listener.py --live --timeout 30 --package es.joshluq.kmsafe
    python crash_listener.py --file path/to/logcat.txt --output-json defect.json
    python crash_listener.py --simulate npe --package es.joshluq.kmsafe
"""

import argparse
import datetime
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


# ==============================================================================
# 1. PARSER AND CRASH DATA MODELS
# ==============================================================================

class CrashEvent:
    def __init__(
        self,
        crash_type: str,
        exception_class: str,
        message: str,
        thread_name: str,
        process_name: str,
        pid: Optional[str] = None,
        stack_frames: Optional[List[str]] = None,
        app_frames: Optional[List[Dict[str, Any]]] = None,
        raw_log: str = "",
    ):
        self.crash_type = crash_type  # 'FATAL_EXCEPTION', 'ANR', 'NATIVE_CRASH'
        self.exception_class = exception_class
        self.message = message
        self.thread_name = thread_name
        self.process_name = process_name
        self.pid = pid
        self.stack_frames = stack_frames or []
        self.app_frames = app_frames or []
        self.raw_log = raw_log

    @property
    def primary_app_frame(self) -> Optional[Dict[str, Any]]:
        return self.app_frames[0] if self.app_frames else None

    def to_defect_issue(self, feature_id: str = "BUG") -> Dict[str, Any]:
        """Generates a defect item compatible with qa_verdict_handoff.schema.json."""
        timestamp = datetime.datetime.now().strftime("%Y%m%d%H%M%S")
        issue_id = f"ISSUE-CRASH-{timestamp}"

        primary = self.primary_app_frame
        if primary:
            source_ref = f"{primary['file']}:{primary['line']}"
            call_site = f"{primary['class_name']}.{primary['method_name']}()"
        else:
            source_ref = f"Runtime:{self.exception_class}"
            call_site = "Unknown (Framework or external library)"

        is_anr = self.crash_type == "ANR"
        severity = "BLOCKER" if is_anr or "OutOfMemory" in self.exception_class else "CRITICAL"

        reproduction_steps = [
            f"1. Launch application '{self.process_name}'.",
            f"2. Trigger operational flow leading to {call_site}.",
            f"3. Observe crash/freeze caused by {self.exception_class} on thread '{self.thread_name}'.",
        ]

        expected_text = (
            f"The application should execute {call_site} smoothly without unhandled exceptions "
            f"or blocking the {self.thread_name} thread."
        )
        actual_text = (
            f"Crash detected: {self.exception_class}: {self.message} "
            f"at {source_ref} (Thread: {self.thread_name})."
        )

        return {
            "issue_id": issue_id,
            "severity": severity,
            "target_role_for_remediation": "Senior Android Developer",
            "upstream_artifact_ref": source_ref,
            "reproduction_steps": reproduction_steps,
            "expected": expected_text,
            "actual": actual_text,
            "metadata": {
                "crash_type": self.crash_type,
                "exception_class": self.exception_class,
                "message": self.message,
                "thread": self.thread_name,
                "process": self.process_name,
                "pid": self.pid,
                "detected_at": datetime.datetime.now().isoformat(),
            },
        }

    def to_markdown_report(self) -> str:
        """Generates a senior debugging engineering diagnostic report."""
        primary = self.primary_app_frame
        file_line = f"`{primary['file']}:{primary['line']}`" if primary else "N/A"
        method_ref = f"`{primary['class_name']}.{primary['method_name']}()`" if primary else "N/A"

        md = [
            f"# Incident Diagnostic Report: {self.crash_type}",
            f"",
            f"- **Detection Time**: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"- **Target Process**: `{self.process_name}` (PID: `{self.pid or 'N/A'}`)",
            f"- **Causing Thread**: `{self.thread_name}`",
            f"- **Exception**: `{self.exception_class}`",
            f"- **Message**: `{self.message}`",
            f"- **Suspect File**: {file_line}",
            f"- **Suspect Function**: {method_ref}",
            f"",
            f"---",
            f"",
            f"## 1. Root Cause Summary",
            f"The application encountered an uncaught `{self.exception_class}` while executing on thread `{self.thread_name}`.",
            f"Failure occurred at {file_line} inside {method_ref}.",
            f"",
            f"```text",
            f"{self.exception_class}: {self.message}",
        ]
        if primary:
            md.append(f"    at {primary['class_name']}.{primary['method_name']}({primary['file']}:{primary['line']})")
        md.extend([
            f"```",
            f"",
            f"## 2. Application Stack Trace Frames",
        ])

        if self.app_frames:
            for idx, frame in enumerate(self.app_frames, start=1):
                md.append(f"{idx}. `{frame['class_name']}.{frame['method_name']}` ({frame['file']}:{frame['line']})")
        else:
            md.append("_No internal application frames found. Crash occurred directly in Android platform or external SDK._")

        md.extend([
            f"",
            f"## 3. Recommended Remediation Strategy (Senior Debugging / Staff Dev)",
            f"1. Open {file_line} and inspect state invariants preceding the call.",
            f"2. Add defensive nullability / state guard checks or verify CoroutineDispatcher context.",
            f"3. Run `/fix-bug` to author a focused unit test reproducing this exact stack trace before implementing the fix.",
            f"",
            f"---",
            f"*Generated by Senior Debugging Engineer Auto-Triage Tool (`crash_listener.py`)*",
        ])

        return "\n".join(md)


# ==============================================================================
# 2. LOGCAT LOGIC & CRASH DETECTOR
# ==============================================================================

STACK_LINE_REGEX = re.compile(
    r"\bat\s+([a-zA-Z0-9_$]+(?:\.[a-zA-Z0-9_$]+)*)\.([a-zA-Z0-9_$<>]+)\(([^:)]+)(?::(\d+))?\)"
)

def parse_stack_line(line: str) -> Optional[Dict[str, Any]]:
    """Parses a single 'at package.Class.method(File.kt:123)' stack trace line."""
    m = STACK_LINE_REGEX.search(line)
    if not m:
        return None
    class_name, method_name, file_name, line_num = m.groups()
    return {
        "class_name": class_name,
        "method_name": method_name,
        "file": file_name,
        "line": int(line_num) if line_num else 1,
        "raw": line.strip(),
    }


def parse_logcat_text(text: str, target_package: Optional[str] = None) -> List[CrashEvent]:
    """Parses raw logcat content and returns detected CrashEvent instances."""
    crashes: List[CrashEvent] = []
    lines = text.splitlines()

    i = 0
    while i < len(lines):
        line = lines[i]

        # 1. Detect FATAL EXCEPTION
        if "FATAL EXCEPTION:" in line:
            thread_match = re.search(r"FATAL EXCEPTION:\s*([^\r\n]+)", line)
            thread_name = thread_match.group(1).strip() if thread_match else "unknown"

            process_name = "unknown"
            pid = None
            raw_chunk = [line]

            # Look ahead for Process: ...
            j = i + 1
            while j < len(lines) and j < i + 10:
                sub = lines[j]
                raw_chunk.append(sub)
                proc_m = re.search(r"Process:\s*([^,\s]+)(?:,\s*PID:\s*(\d+))?", sub)
                if proc_m:
                    process_name = proc_m.group(1).strip()
                    pid = proc_m.group(2)
                    break
                j += 1

            # Look for Exception class and message
            exc_class = "RuntimeException"
            exc_msg = "Unknown error"
            k = j + 1
            while k < len(lines) and k < j + 10:
                sub = lines[k]
                raw_chunk.append(sub)
                exc_m = re.search(r"([a-zA-Z0-9_$.]+(?:Exception|Error|Failure))(?::\s*(.*))?", sub)
                if exc_m:
                    exc_class = exc_m.group(1).strip()
                    exc_msg = (exc_m.group(2) or "").strip()
                    break
                k += 1

            # Read stack trace frames
            stack_frames = []
            app_frames = []
            m = k + 1
            while m < len(lines):
                frame_line = lines[m]
                if not frame_line.strip() or ("at " not in frame_line and "Caused by:" not in frame_line):
                    if len(stack_frames) > 0 and not frame_line.strip().startswith("..."):
                        break
                raw_chunk.append(frame_line)
                stack_frames.append(frame_line.strip())

                parsed = parse_stack_line(frame_line)
                if parsed:
                    if target_package:
                        if target_package in parsed["class_name"]:
                            app_frames.append(parsed)
                    else:
                        # Auto-filter out standard android/java internal frames
                        if not any(parsed["class_name"].startswith(p) for p in ("android.", "java.", "kotlin.", "androidx.")):
                            app_frames.append(parsed)
                m += 1

            # Package filter check
            if not target_package or target_package in process_name or any(target_package in f["class_name"] for f in app_frames):
                crashes.append(
                    CrashEvent(
                        crash_type="FATAL_EXCEPTION",
                        exception_class=exc_class,
                        message=exc_msg,
                        thread_name=thread_name,
                        process_name=process_name,
                        pid=pid,
                        stack_frames=stack_frames,
                        app_frames=app_frames,
                        raw_log="\n".join(raw_chunk),
                    )
                )
            i = m
            continue

        # 2. Detect ANR
        elif "ANR in " in line or "Application Not Responding: " in line:
            anr_m = re.search(r"(?:ANR in|Application Not Responding:)\s*([^\s(]+)", line)
            proc = anr_m.group(1).strip() if anr_m else "unknown"
            raw_chunk = [line]

            # Read next lines for reason/threads
            reason = "Main thread blocked"
            j = i + 1
            while j < len(lines) and j < i + 15:
                raw_chunk.append(lines[j])
                if "Reason:" in lines[j]:
                    reason = lines[j].split("Reason:", 1)[1].strip()
                j += 1

            if not target_package or target_package in proc:
                crashes.append(
                    CrashEvent(
                        crash_type="ANR",
                        exception_class="ApplicationNotResponding",
                        message=reason,
                        thread_name="main",
                        process_name=proc,
                        raw_log="\n".join(raw_chunk),
                    )
                )
            i = j
            continue

        i += 1

    return crashes


# ==============================================================================
# 3. ADB RUNNERS AND LIVE MONITORING
# ==============================================================================

def run_adb_dump(device: Optional[str] = None) -> str:
    """Executes `adb logcat -d -b crash -b main` to capture recent crash logs."""
    cmd = ["adb"]
    if device:
        cmd.extend(["-s", device])
    cmd.extend(["logcat", "-d", "-b", "crash", "-b", "main"])

    try:
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=False)
        return proc.stdout
    except FileNotFoundError:
        print("[WARNING] 'adb' executable not found in PATH. Ensure Android SDK platform-tools are installed.")
        return ""


def run_adb_live_stream(
    target_package: Optional[str] = None,
    timeout_seconds: int = 30,
    device: Optional[str] = None,
) -> List[CrashEvent]:
    """Listens to ADB logcat in real-time until a crash is caught or timeout expires."""
    cmd = ["adb"]
    if device:
        cmd.extend(["-s", device])
    # Clear previous buffer first
    subprocess.run(cmd + ["logcat", "-c"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
    cmd.extend(["logcat", "-v", "time", "-b", "crash", "-b", "main"])

    print(f"[*] Starting live ADB crash listener (timeout: {timeout_seconds}s)...")
    if target_package:
        print(f"[*] Filtering for package: '{target_package}'")

    try:
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
    except FileNotFoundError:
        print("[ERROR] 'adb' not found. Cannot start live stream.")
        return []

    collected_lines = []
    start_time = time.time()
    detected_crashes: List[CrashEvent] = []

    try:
        while time.time() - start_time < timeout_seconds:
            if proc.poll() is not None:
                break
            line = proc.stdout.readline()
            if not line:
                time.sleep(0.05)
                continue
            collected_lines.append(line)

            if "FATAL EXCEPTION:" in line or "ANR in " in line:
                print(f"[!] Crash trigger detected! Reading stack trace...")
                # Allow a brief moment for remainder of stack trace to flush
                time.sleep(0.8)
                while True:
                    extra = proc.stdout.readline()
                    if not extra:
                        break
                    collected_lines.append(extra)
                    if len(collected_lines) > 200:
                        break

                detected_crashes = parse_logcat_text("".join(collected_lines), target_package)
                if detected_crashes:
                    break
    except KeyboardInterrupt:
        print("\n[*] Interrupted by user.")
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=2)
        except Exception:
            proc.kill()

    return detected_crashes


# ==============================================================================
# 4. SIMULATION DATA GENERATOR (FOR TESTS AND OFFLINE DEMOS)
# ==============================================================================

SIMULATED_CRASHES = {
    "npe": """
09-21 21:30:15.120 10245 10245 E AndroidRuntime: FATAL EXCEPTION: main
09-21 21:30:15.120 10245 10245 E AndroidRuntime: Process: {package}, PID: 10245
09-21 21:30:15.120 10245 10245 E AndroidRuntime: java.lang.NullPointerException: Parameter specified as non-null is null: method {package}.feature.expenses.ExpensesViewModel.onAction, parameter action
09-21 21:30:15.120 10245 10245 E AndroidRuntime: 	at {package}.feature.expenses.ExpensesViewModel.onAction(ExpensesViewModel.kt:42)
09-21 21:30:15.120 10245 10245 E AndroidRuntime: 	at {package}.feature.expenses.ExpensesScreenKt$ExpensesScreen$1$1.invoke(ExpensesScreen.kt:88)
09-21 21:30:15.120 10245 10245 E AndroidRuntime: 	at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1034)
09-21 21:30:15.120 10245 10245 E AndroidRuntime: 	at android.os.Handler.dispatchMessage(Handler.java:106)
09-21 21:30:15.120 10245 10245 E AndroidRuntime: 	at android.os.Looper.loopOnce(Looper.java:201)
""",
    "anr": """
09-21 21:32:00.500  1000  1050 E ActivityManager: ANR in {package} (ActivityRecord{xxx {package}/.MainActivity})
09-21 21:32:00.500  1000  1050 E ActivityManager: PID: 10245
09-21 21:32:00.500  1000  1050 E ActivityManager: Reason: Input dispatching timed out (Waiting to send key event because focused window has not finished processing the input event).
09-21 21:32:00.500  1000  1050 E ActivityManager: Load: 8.5 / 7.2 / 6.1
""",
    "compose": """
09-21 21:35:10.800 10245 10245 E AndroidRuntime: FATAL EXCEPTION: main
09-21 21:35:10.800 10245 10245 E AndroidRuntime: Process: {package}, PID: 10245
09-21 21:35:10.800 10245 10245 E AndroidRuntime: java.lang.IllegalStateException: Reading a state that was created after the snapshot was taken or in a snapshot that has not yet been applied
09-21 21:35:10.800 10245 10245 E AndroidRuntime: 	at androidx.compose.runtime.snapshots.SnapshotKt.readError(Snapshot.kt:1895)
09-21 21:35:10.800 10245 10245 E AndroidRuntime: 	at {package}.ui.overview.OverviewCardKt.BalanceMetric(OverviewCard.kt:64)
09-21 21:35:10.800 10245 10245 E AndroidRuntime: 	at {package}.ui.overview.OverviewScreenKt.OverviewScreen(OverviewScreen.kt:112)
09-21 21:35:10.800 10245 10245 E AndroidRuntime: 	at androidx.compose.ui.layout.LayoutNode.measure(LayoutNode.kt:1450)
""",
}

def generate_simulation(flavor: str = "npe", package: str = "es.joshluq.kmsafe") -> str:
    template = SIMULATED_CRASHES.get(flavor.lower(), SIMULATED_CRASHES["npe"])
    return template.replace("{package}", package)


# ==============================================================================
# 5. CLI ENTRYPOINT
# ==============================================================================

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Real-Time Crash Listener & Auto-Triage Tool for Senior Debugging Engineers."
    )
    parser.add_argument(
        "--live", action="store_true", help="Listen to ADB logcat in real-time until a crash is caught."
    )
    parser.add_argument(
        "--dump", action="store_true", help="Dump and inspect recent crashes from device via adb logcat -d."
    )
    parser.add_argument(
        "--file", type=str, help="Parse an existing logcat text file on disk."
    )
    parser.add_argument(
        "--simulate", choices=["npe", "anr", "compose"], help="Simulate a crash event for testing or CI."
    )
    parser.add_argument(
        "--package", type=str, default="es.joshluq.kmsafe", help="Target Android application package."
    )
    parser.add_argument(
        "--timeout", type=int, default=30, help="Live stream timeout in seconds (default: 30)."
    )
    parser.add_argument(
        "--device", "-s", type=str, help="Specific ADB device serial."
    )
    parser.add_argument(
        "--output-json", type=str, help="Path to save the generated JSON defect ticket."
    )
    parser.add_argument(
        "--output-md", type=str, help="Path to save the Markdown diagnostic report."
    )
    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()

    crashes: List[CrashEvent] = []

    if args.simulate:
        print(f"[*] Simulating crash flavor '{args.simulate}' for package '{args.package}'...")
        sim_log = generate_simulation(args.simulate, args.package)
        crashes = parse_logcat_text(sim_log, args.package)
    elif args.file:
        file_path = Path(args.file)
        if not file_path.is_file():
            print(f"[ERROR] File not found: {file_path}")
            sys.exit(1)
        print(f"[*] Parsing logcat file: {file_path}...")
        crashes = parse_logcat_text(file_path.read_text(encoding="utf-8", errors="replace"), args.package)
    elif args.live:
        crashes = run_adb_live_stream(args.package, args.timeout, args.device)
    else:  # Default to dump
        print(f"[*] Dumping recent crash logs from ADB for package '{args.package}'...")
        dump_text = run_adb_dump(args.device)
        if dump_text:
            crashes = parse_logcat_text(dump_text, args.package)

    if not crashes:
        print("✅ No matching crashes or ANRs detected. Application appears stable.")
        sys.exit(0)

    print(f"\n🚨 FOUND {len(crashes)} CRASH/ANR EVENT(S)!\n")

    for idx, crash in enumerate(crashes, start=1):
        print("=" * 65)
        print(f" EVENT #{idx}: [{crash.crash_type}] {crash.exception_class}")
        print("=" * 65)
        print(f"Message  : {crash.message}")
        print(f"Process  : {crash.process_name} (PID: {crash.pid or 'N/A'})")
        print(f"Thread   : {crash.thread_name}")

        primary = crash.primary_app_frame
        if primary:
            print(f"Root Ref : {primary['file']}:{primary['line']} -> {primary['class_name']}.{primary['method_name']}()")
        else:
            print(f"Root Ref : External / Framework Call")

        # Generate defect ticket
        defect_issue = crash.to_defect_issue()
        md_report = crash.to_markdown_report()

        if args.output_json:
            out_json = Path(args.output_json)
            out_json.parent.mkdir(parents=True, exist_ok=True)
            with open(out_json, "w", encoding="utf-8") as f:
                json.dump(defect_issue, f, indent=2)
            print(f"[*] Saved JSON defect ticket to: {out_json.resolve()}")

        if args.output_md:
            out_md = Path(args.output_md)
            out_md.parent.mkdir(parents=True, exist_ok=True)
            with open(out_md, "w", encoding="utf-8") as f:
                f.write(md_report)
            print(f"[*] Saved Markdown diagnostic report to: {out_md.resolve()}")

        print("\n--- Summary Diagnostic Report ---")
        print(md_report)
        print("-" * 65)

    sys.exit(0)


if __name__ == "__main__":
    main()
