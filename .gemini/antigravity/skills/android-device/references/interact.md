# Android Device UI Interaction Guide

## 1. UI Hierarchy Inspection
Use `python scripts/inspect_layout.py dump` to capture the current on-screen UI hierarchy.

### JSON Element Properties
Each element in the returned list contains:
- `key`: Numeric unique identifier for the node.
- `class`: Fully qualified Android or Compose view class (e.g., `android.widget.Button`, `androidx.compose.ui.platform.ComposeView`).
- `resourceId`: Application resource ID (e.g., `com.example.profile:id/save_button`).
- `text`: Visible string content inside the element.
- `contentDesc`: Accessibility content description.
- `interactions`: List of supported actions: `["clickable", "focusable", "scrollable", "checkable", "long-clickable", "password"]`.
- `state`: Active states: `["checked", "focused", "selected"]`.
- `bounds`: Screen pixel coordinates in `[minX,minY][maxX,maxY]` format.
- `center`: Calculated centroid `[centerX, centerY]` for direct tap targeting.
- `off-screen`: Boolean indicating if the element requires scrolling to become visible.

---

## 2. Interaction Procedures & Gestures

### 2.1 Tapping
Locate the element's `center` coordinate and emit:
```bash
adb shell input tap <centerX> <centerY>
```

### 2.2 Text Input
Before sending text input:
1. Ensure the field has `"focused"` in its `state` list. If not, tap its `center` first.
2. Replace whitespace in text strings with `%s` to prevent argument splitting:
```bash
adb shell input text "Alex%sRivera"
```

### 2.3 Scrolling & Swiping
When an element or its parent container is `"scrollable"`:
- Swipe up to scroll down:
```bash
# adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>
adb shell input swipe 540 800 540 200 500
```
Always specify a duration (>= 500ms) to ensure smooth scrolling and prevent runaway velocity flings.

---

## 3. Layout Diffing
To detect what changed after an action without overwhelming the context window:
```bash
python scripts/inspect_layout.py dump --diff
```
This compares against the cached previous dump and outputs only elements whose text, state, bounds, or presence changed.
