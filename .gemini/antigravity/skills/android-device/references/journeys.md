# Android Journey XML Test Specification

## 1. Overview
A Journey is a declarative XML test defining a sequential user workflow. The XML document serves as the absolute source of truth for app behavior. If the application diverges, freezes, or crashes, the journey test fails.

---

## 2. XML Schema
```xml
<journey name="Profile Flow">
  <description>Verify username update and persistence</description>
  <actions>
    <action>Tap the "Username" input</action>
    <action>Type "Alex Rivera" into the field</action>
    <action>Tap the "Save Preferences" button</action>
    <action>Verify that "Preferences saved successfully" is visible on the screen</action>
  </actions>
</journey>
```

---

## 3. Step Classification
Steps inside `<actions>` fall into two main categories:

### 3.1 Interaction Steps
Initiate gestures on the UI:
- `Tap the "<Element>"` / `Click "<Element>"`: Resolves target and emits tap.
- `Type "<Text>" into the field`: Focuses element and emits text input.
- `Swipe <direction>` / `Scroll down`: Emits swipe gesture.

### 3.2 Expectation Verification Steps
Evaluate screen state without mutating the UI:
- `Verify that "<Text>" is visible on the screen`
- `Check if "<Element>" is displayed`

If an expectation fails, the step is marked `FAILED`, the journey halts immediately, and subsequent steps are marked `SKIPPED`.

---

## 4. Execution Reporting Format
The runner emits a structured JSON evaluation summary:
```json
{
  "journey": "Profile Flow",
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
      "commands": ["adb shell input tap 540 270"],
      "comment": "Tapped target at (540, 270)"
    }
  ]
}
```
