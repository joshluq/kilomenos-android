---
name: android-device
description: Manages Android virtual devices (emulators), APK deployment, UI hierarchy inspection via layout dumps, screen coordinate resolution, ADB gesture simulation, and journey XML test execution.
---
# Android Device & Emulator Skill

## 1. Overview & Purpose
The `android-device` skill provides automation, CLI interfaces, and standard operational procedures for managing Android execution environments. It bridges the gap between high-level agentic decisions and low-level device interaction, supporting:
1. **SDK & Toolchain Discovery**: Automatic discovery and validation of Android SDK, platform tools (`adb`), emulator binaries, and environment configurations.
2. **AVD & Emulator Lifecycle**: Querying available AVDs, headless and GPU-accelerated boot orchestration with boot completion polling, and graceful shutdown.
3. **Application Deployment**: Incremental delta installation and APK installation with automatic activity launching.
4. **UI Layout Inspection**: Dumping live view hierarchies into structured, queryable JSON with coordinate resolution, bounding box calculations, and state diffing.
5. **Simulated User Input**: Calculating center coordinates and executing taps, swipes, scrolls, and text input via ADB shell.
6. **Journey XML Test Execution**: Running end-to-end user journeys defined in declarative XML with structured JSON execution verdicts.

---

## 2. Integration with Agent Roles

This skill is primarily consumed by two lifecycle roles defined in `AGENTS.md`:

### Senior Android Developer
- **Smoke Testing**: Validates newly compiled debug APKs on a live or headless emulator before triggering the Dev-to-QA handoff.
- **Delta Installation**: Deploys rapid incremental updates during local iteration without full reinstall overhead.
- **Layout Inspection**: Inspects Composable nodes and semantics trees to diagnose layout clipping or misaligned bounding boxes.

### QA / Testing Engineer
- **Journey Execution**: Executes declarative XML test journeys (`run_journey.py`) against staging builds to verify multi-step end-to-end flows.
- **Regression Verification**: Takes layout diffs (`inspect_layout.py --diff`) before and after user interactions to verify state transitions.
- **Visual & Semantics Audits**: Resolves UI coordinates to ensure interactive targets satisfy accessibility touch target dimensions (>= 48dp).

---

## 3. Toolchain & Helper Scripts

The skill provides three runnable Python 3 CLI utilities in `skills/android-device/scripts/`. All utilities use Python standard library only and support standard `--help` inspection.

```
skills/android-device/
├── SKILL.md
└── scripts/
    ├── device_runner.py
    ├── inspect_layout.py
    └── run_journey.py
```

### Script 1: `device_runner.py`
Orchestrates SDK discovery, AVD lifecycle, and APK deployment.

#### Usage:
```bash
# Discover environment and connected devices
python skills/android-device/scripts/device_runner.py check-env

# List installed Android Virtual Devices
python skills/android-device/scripts/device_runner.py list-avds

# Start an emulator in headless mode with boot verification
python skills/android-device/scripts/device_runner.py start-emulator --avd Pixel_10a --headless --timeout 120

# Install APK and optionally launch main activity
python skills/android-device/scripts/device_runner.py install-app --apk app/build/outputs/apk/debug/app-debug.apk --serial emulator-5554 --launch com.example.app/.MainActivity

# Gracefully terminate an emulator
python skills/android-device/scripts/device_runner.py stop-emulator --serial emulator-5554
```

#### Dual-Mode Execution Architecture
- **Preferred Path**: If the `android` CLI binary is detected in `PATH`, `device_runner.py` uses high-level commands (`android emulator start`, `android install --use-delta-install`).
- **Resilient Fallback**: If `android` CLI is absent, it automatically resolves `$ANDROID_HOME` or standard local SDK paths (`%LOCALAPPDATA%\Android\Sdk` on Windows, `~/Library/Android/sdk` on macOS, `~/Android/Sdk` on Linux) and invokes `emulator.exe` and `adb.exe` directly.

---

### Script 2: `inspect_layout.py`
Captures, parses, and searches the on-screen UI hierarchy.

#### Usage:
```bash
# Dump complete screen hierarchy to JSON
python skills/android-device/scripts/inspect_layout.py dump --serial emulator-5554 --pretty --output layout.json

# Dump only elements that changed since the last dump
python skills/android-device/scripts/inspect_layout.py dump --serial emulator-5554 --diff --output layout_diff.json

# Find an element by visible text and get center coordinates
python skills/android-device/scripts/inspect_layout.py find --serial emulator-5554 --text "Save Profile"

# Find an element by resource ID
python skills/android-device/scripts/inspect_layout.py find --serial emulator-5554 --resource-id "com.example.app:id/submit_button"

# Capture a screenshot
python skills/android-device/scripts/inspect_layout.py capture-screen --serial emulator-5554 --output screen.png
```

#### Node Structure in `layout.json`:
```json
[
  {
    "key": 142857,
    "class": "android.widget.Button",
    "resourceId": "com.example.profile:id/save_button",
    "text": "Save Preferences",
    "contentDesc": "Save User Preferences",
    "interactions": ["clickable", "focusable"],
    "state": ["focused"],
    "bounds": "[120,450][360,530]",
    "center": [240, 490],
    "off-screen": false
  }
]
```

---

### Script 3: `run_journey.py`
Executes declarative journey XML test suites and emits structured JSON reports.

#### Schema of Journey XML:
```xml
<journey name="Profile Update Flow">
  <description>Verify that user can update username and save preferences</description>
  <actions>
    <action>Tap the "Username" input</action>
    <action>Type "Alex Rivera" into the field</action>
    <action>Tap the "Save Preferences" button</action>
    <action>Verify that "Preferences saved successfully" is visible on the screen</action>
  </actions>
</journey>
```

#### Usage:
```bash
# Execute journey against live emulator
python skills/android-device/scripts/run_journey.py --file tests/journeys/profile_update.xml --serial emulator-5554 --output report.json

# Dry-run validation (validates XML syntax, steps, and ADB command generation without attached device)
python skills/android-device/scripts/run_journey.py --file tests/journeys/profile_update.xml --dry-run
```

#### Output Schema:
```json
{
  "journey": "Profile Update Flow",
  "verdict": "PASSED",
  "total_steps": 4,
  "passed_steps": 4,
  "failed_steps": 0,
  "skipped_steps": 0,
  "results": [
    {
      "step": 1,
      "action": "Tap the \"Username\" input",
      "status": "PASSED",
      "commands": ["adb -s emulator-5554 shell input tap 240 320"],
      "comment": "Element resolved and tapped"
    }
  ]
}
```

---

## 4. Operational Best Practices & Interaction Rules

1. **State Verification Before Typing**:
   Always verify that a text input element has `"focused"` in its `"state"` list before emitting `adb shell input text`. If not focused, emit a tap on its `center` coordinates first.
2. **Slow Scrolling**:
   When scrolling a scrollable container, provide the 5th argument to `adb shell input swipe` (duration in ms, e.g., 500ms) to ensure smooth scrolling without triggering momentum fling.
3. **Dynamic Wait Before Layout Diffing**:
   Allow asynchronous UI transitions and network responses to settle (1.5–3 seconds) before executing `inspect_layout.py dump --diff`.
4. **Headless Execution on CI**:
   Always include `-no-window -no-audio -no-boot-anim -gpu swiftshader_indirect` when launching emulators in headless environments or background agents.
