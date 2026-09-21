#!/usr/bin/env python3
"""
device_runner.py - Android Device & Emulator Lifecycle Runner

Handles Android SDK discovery, virtual device (AVD) management,
emulator startup/shutdown orchestration, and APK installation.

Standard library only. Works across Windows, macOS, and Linux.
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple


def find_android_sdk() -> Optional[Path]:
    """Discovers Android SDK directory from environment variables or standard OS paths."""
    # 1. Environment variables
    for env_var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        val = os.environ.get(env_var)
        if val and os.path.isdir(val):
            return Path(val).resolve()

    # 2. Standard platform paths
    candidates = []
    if sys.platform.startswith("win"):
        local_app_data = os.environ.get("LOCALAPPDATA", "")
        if local_app_data:
            candidates.append(Path(local_app_data) / "Android" / "Sdk")
        user_profile = os.environ.get("USERPROFILE", "")
        if user_profile:
            candidates.append(Path(user_profile) / "AppData" / "Local" / "Android" / "Sdk")
    elif sys.platform == "darwin":
        candidates.append(Path.home() / "Library" / "Android" / "sdk")
    else:  # Linux / Unix
        candidates.append(Path.home() / "Android" / "Sdk")
        candidates.append(Path("/usr/lib/android-sdk"))

    for candidate in candidates:
        if candidate.is_dir():
            return candidate.resolve()
    return None


def get_tool_paths(sdk_path: Optional[Path]) -> Dict[str, Optional[str]]:
    """Locates adb and emulator executables."""
    tools: Dict[str, Optional[str]] = {"adb": None, "emulator": None, "android_cli": None}

    # Check PATH first
    for tool in ("adb", "emulator", "android"):
        found = shutil.which(tool)
        if found:
            key = "android_cli" if tool == "android" else tool
            tools[key] = found

    # Fallback to SDK directories
    if sdk_path and sdk_path.is_dir():
        exe_ext = ".exe" if sys.platform.startswith("win") else ""
        if not tools["adb"]:
            adb_candidate = sdk_path / "platform-tools" / f"adb{exe_ext}"
            if adb_candidate.is_file():
                tools["adb"] = str(adb_candidate.resolve())

        if not tools["emulator"]:
            emulator_candidate = sdk_path / "emulator" / f"emulator{exe_ext}"
            if emulator_candidate.is_file():
                tools["emulator"] = str(emulator_candidate.resolve())

    return tools


def run_cmd(cmd: List[str], timeout: Optional[int] = None) -> Tuple[int, str, str]:
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
    except subprocess.TimeoutExpired:
        return 124, "", f"Command timed out after {timeout} seconds: {' '.join(cmd)}"
    except Exception as e:
        return 1, "", str(e)


def get_connected_devices(adb_path: Optional[str]) -> List[str]:
    """Returns list of online connected device serials."""
    if not adb_path:
        return []
    code, stdout, _ = run_cmd([adb_path, "devices"])
    if code != 0:
        return []
    devices = []
    for line in stdout.splitlines()[1:]:
        parts = line.strip().split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices


def list_avds(tools: Dict[str, Optional[str]]) -> List[str]:
    """Lists available Android Virtual Devices."""
    # 1. Try emulator -list-avds
    if tools["emulator"]:
        code, stdout, _ = run_cmd([tools["emulator"], "-list-avds"])
        if code == 0 and stdout:
            return [line.strip() for line in stdout.splitlines() if line.strip()]

    # 2. Check ~/.android/avd
    avd_dir = Path.home() / ".android" / "avd"
    if avd_dir.is_dir():
        avds = []
        for item in avd_dir.glob("*.ini"):
            avds.append(item.stem)
        return sorted(avds)
    return []


def cmd_check_env(args: argparse.Namespace) -> int:
    """Discovers environment, tools, and connected devices."""
    sdk = find_android_sdk()
    tools = get_tool_paths(sdk)
    devices = get_connected_devices(tools["adb"])
    avds = list_avds(tools)

    report = {
        "status": "READY" if (tools["adb"] and (devices or avds)) else "DEGRADED",
        "sdk_path": str(sdk) if sdk else None,
        "adb_path": tools["adb"],
        "emulator_path": tools["emulator"],
        "android_cli_path": tools["android_cli"],
        "connected_devices": devices,
        "available_avds": avds,
    }

    if args.json:
        print(json.dumps(report, indent=2))
    else:
        print("=== Android Environment Status ===")
        print(f"SDK Location:       {report['sdk_path'] or 'NOT FOUND'}")
        print(f"ADB Executable:     {report['adb_path'] or 'NOT FOUND'}")
        print(f"Emulator Binary:    {report['emulator_path'] or 'NOT FOUND'}")
        print(f"Android CLI:        {report['android_cli_path'] or 'NOT FOUND (using SDK fallbacks)'}")
        print(f"Connected Devices:  {', '.join(devices) if devices else 'None'}")
        print(f"Installed AVDs:     {', '.join(avds) if avds else 'None'}")
    return 0


def cmd_list_avds(args: argparse.Namespace) -> int:
    """Lists available AVDs."""
    sdk = find_android_sdk()
    tools = get_tool_paths(sdk)
    avds = list_avds(tools)
    if args.json:
        print(json.dumps({"available_avds": avds}, indent=2))
    else:
        if not avds:
            print("No AVDs found.")
        else:
            print("Available AVDs:")
            for avd in avds:
                print(f"  - {avd}")
    return 0


def cmd_start_emulator(args: argparse.Namespace) -> int:
    """Starts an emulator and waits for boot completion."""
    sdk = find_android_sdk()
    tools = get_tool_paths(sdk)

    if not tools["emulator"]:
        print("Error: emulator binary not found.", file=sys.stderr)
        return 1

    avd_name = args.avd
    available = list_avds(tools)
    if not avd_name:
        if not available:
            print("Error: No AVDs available to launch.", file=sys.stderr)
            return 1
        avd_name = available[0]
        print(f"No AVD specified, defaulting to: {avd_name}")
    elif avd_name not in available:
        print(f"Warning: '{avd_name}' not found in listed AVDs. Attempting to start anyway...")

    cmd = [tools["emulator"], "-avd", avd_name]
    if args.headless:
        cmd.extend(["-no-window", "-no-audio", "-no-boot-anim", "-gpu", "swiftshader_indirect"])

    print(f"Launching emulator with command: {' '.join(cmd)}")
    if args.dry_run:
        print("[DRY RUN] Emulator start simulated successfully.")
        return 0

    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if sys.platform.startswith("win") else 0,
        )
    except Exception as e:
        print(f"Failed to spawn emulator process: {e}", file=sys.stderr)
        return 1

    # Poll for boot completion if adb is available
    if not tools["adb"]:
        print(f"Emulator process launched (PID: {proc.pid}). ADB not available to poll boot.")
        return 0

    print(f"Waiting for emulator to complete boot (timeout: {args.timeout}s)...")
    start_time = time.time()
    booted_serial = None

    while time.time() - start_time < args.timeout:
        devices = get_connected_devices(tools["adb"])
        emulators = [d for d in devices if d.startswith("emulator-")]
        if emulators:
            serial = emulators[0]
            code, stdout, _ = run_cmd([tools["adb"], "-s", serial, "shell", "getprop", "sys.boot_completed"])
            if code == 0 and stdout.strip() == "1":
                booted_serial = serial
                break
        time.sleep(2)

    if booted_serial:
        print(f"Emulator '{avd_name}' successfully booted: {booted_serial}")
        if args.json:
            print(json.dumps({"status": "BOOTED", "serial": booted_serial, "avd": avd_name}, indent=2))
        return 0
    else:
        print(f"Error: Emulator '{avd_name}' did not boot within {args.timeout} seconds.", file=sys.stderr)
        return 1


def cmd_stop_emulator(args: argparse.Namespace) -> int:
    """Stops a running emulator."""
    sdk = find_android_sdk()
    tools = get_tool_paths(sdk)
    if not tools["adb"]:
        print("Error: adb binary not found.", file=sys.stderr)
        return 1

    serial = args.serial
    if not serial:
        devices = get_connected_devices(tools["adb"])
        emulators = [d for d in devices if d.startswith("emulator-")]
        if not emulators:
            print("No running emulators detected.")
            return 0
        serial = emulators[0]

    print(f"Stopping emulator {serial}...")
    if args.dry_run:
        print("[DRY RUN] Emukill command simulated.")
        return 0

    code, stdout, stderr = run_cmd([tools["adb"], "-s", serial, "emu", "kill"])
    if code == 0:
        print(f"Emulator {serial} killed successfully.")
        return 0
    else:
        print(f"Failed to stop emulator {serial}: {stderr or stdout}", file=sys.stderr)
        return 1


def cmd_install_app(args: argparse.Namespace) -> int:
    """Installs an APK and optionally launches its main component."""
    sdk = find_android_sdk()
    tools = get_tool_paths(sdk)
    if not tools["adb"] and not tools["android_cli"]:
        print("Error: neither adb nor android-cli found.", file=sys.stderr)
        return 1

    apk_path = Path(args.apk)
    if not apk_path.is_file() and not args.dry_run:
        print(f"Error: APK file not found at '{apk_path}'.", file=sys.stderr)
        return 1

    serial = args.serial
    if not serial:
        devices = get_connected_devices(tools["adb"])
        if not devices and not args.dry_run:
            print("Error: No connected devices or emulators found.", file=sys.stderr)
            return 1
        serial = devices[0] if devices else "emulator-5554"

    # Delta install via android-cli if available, else standard adb install
    if tools["android_cli"] and args.delta:
        cmd = [tools["android_cli"], "install", "--use-delta-install", f"--device={serial}", f"--apks={apk_path}"]
    else:
        cmd = [tools["adb"], "-s", serial, "install", "-r", "-d", str(apk_path)]

    print(f"Installing APK on {serial}: {' '.join(cmd)}")
    if args.dry_run:
        print("[DRY RUN] APK install simulated successfully.")
        return 0

    code, stdout, stderr = run_cmd(cmd, timeout=120)
    if code != 0:
        print(f"Installation failed: {stderr or stdout}", file=sys.stderr)
        return 1

    print(f"Successfully installed {apk_path.name} on {serial}.")

    if args.launch:
        launch_cmd = [tools["adb"], "-s", serial, "shell", "am", "start", "-n", args.launch]
        print(f"Launching activity: {' '.join(launch_cmd)}")
        l_code, l_out, l_err = run_cmd(launch_cmd)
        if l_code != 0:
            print(f"Launch failed: {l_err or l_out}", file=sys.stderr)
            return 1
        print(f"Activity {args.launch} launched.")

    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Android Device & Emulator Runner - Orchestrates SDK discovery, AVD lifecycle, and APK deployment."
    )
    subparsers = parser.add_subparsers(dest="command", help="Subcommand to execute")

    # check-env
    p_check = subparsers.add_parser("check-env", help="Check SDK, tools, and connected devices")
    p_check.add_argument("--json", action="store_true", help="Output report in JSON format")

    # list-avds
    p_avd = subparsers.add_parser("list-avds", help="List available Android Virtual Devices")
    p_avd.add_argument("--json", action="store_true", help="Output as JSON array")

    # start-emulator
    p_start = subparsers.add_parser("start-emulator", help="Start an Android Virtual Device")
    p_start.add_argument("--avd", help="Name of the AVD to launch (e.g. Pixel_10a)")
    p_start.add_argument("--headless", action="store_true", help="Run in headless mode (-no-window -gpu swiftshader_indirect)")
    p_start.add_argument("--timeout", type=int, default=120, help="Boot timeout in seconds (default: 120)")
    p_start.add_argument("--json", action="store_true", help="Output result as JSON")
    p_start.add_argument("--dry-run", action="store_true", help="Simulate launch without executing process")

    # stop-emulator
    p_stop = subparsers.add_parser("stop-emulator", help="Gracefully stop a running emulator")
    p_stop.add_argument("--serial", help="Serial number of the emulator (e.g. emulator-5554)")
    p_stop.add_argument("--dry-run", action="store_true", help="Simulate shutdown without executing command")

    # install-app
    p_install = subparsers.add_parser("install-app", help="Install APK on target device")
    p_install.add_argument("--apk", required=True, help="Path to APK file")
    p_install.add_argument("--serial", help="Target device serial number")
    p_install.add_argument("--delta", action="store_true", default=True, help="Use delta installation if available")
    p_install.add_argument("--launch", help="Component to launch after install (e.g. com.example.app/.MainActivity)")
    p_install.add_argument("--dry-run", action="store_true", help="Simulate installation without device")

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return 0

    command_handlers = {
        "check-env": cmd_check_env,
        "list-avds": cmd_list_avds,
        "start-emulator": cmd_start_emulator,
        "stop-emulator": cmd_stop_emulator,
        "install-app": cmd_install_app,
    }

    handler = command_handlers.get(args.command)
    if handler:
        return handler(args)
    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
