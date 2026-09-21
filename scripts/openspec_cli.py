#!/usr/bin/env python3
"""
openspec_cli.py - OpenSpec CLI Tool for Android Agentic Ecosystem

Manages living specifications, delta changes, and SDD lifecycles.
Integrates the OpenSpec 3-phase workflow (Propose -> Apply -> Archive)
with the Android Agentic Ecosystem's deterministic test harness.

Usage:
    python scripts/openspec_cli.py propose FEAT-002 --title "Export PDF Receipts"
    python scripts/openspec_cli.py validate FEAT-002
    python scripts/openspec_cli.py archive FEAT-002
    python scripts/openspec_cli.py list
"""

import argparse
import datetime
import os
import re
import shutil
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

ROOT_DIR = Path(__file__).resolve().parent.parent
OPENSPEC_DIR = ROOT_DIR / "openspec"
CHANGES_DIR = OPENSPEC_DIR / "changes"
SPECS_DIR = OPENSPEC_DIR / "specs"
ARCHIVE_DIR = OPENSPEC_DIR / "archive"


# ==============================================================================
# 1. SCAFFOLDING TEMPLATES
# ==============================================================================

PROPOSAL_TEMPLATE = """# Change Proposal: {title}
**Change ID**: `{change_id}`
**Status**: Proposed
**Created At**: {created_at}

## 1. Intent & Business Value
Describe the user problem this change solves, target user personas, and expected impact.

## 2. Scope of Changes
- **Target Modules**: (e.g. `:feature:expenses`, `:core:domain`)
- **Key Capabilities**:
  - Capability 1: ...
  - Capability 2: ...

## 3. Dependencies & Compatibility
- **Dependencies**: (e.g. Android API 26+, CanvasKit design tokens)
- **Breaking Changes**: None (Preserves backwards compatibility)
"""

DESIGN_TEMPLATE = """# Architecture & Technical Design: {title}
**Change ID**: `{change_id}`
**Architectural Standards**: Clean Architecture, MVI, 4-Layer Jetpack Compose

## 1. MVI State & Actions ("The Being")
### 1.1 UI State Model (`{change_id}UiState`)
```kotlin
data class {pascal_id}UiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### 1.2 UI Actions Hierarchy (`{change_id}UiAction`)
```kotlin
sealed interface {pascal_id}UiAction {{
    data object Submit : {pascal_id}UiAction
    data class OnInputChanged(val text: String) : {pascal_id}UiAction
}}
```

## 2. Domain & UseCases ("The Doing")
- `Execute{pascal_id}UseCase`: Orchestrates domain business logic.
- Repository: Interfaces defined in `:core:domain`, implemented in `:core:infrastructure`.

## 3. 4-Layer Compose Presentation
- **Layer 1 (Screen)**: Destination entrypoint with Navigation 3 key scoping.
- **Layer 2 (Coordinator)**: State observation and side-effect channel bridge.
- **Layer 3 (Content)**: Stateless layout accepting State and Action lambdas.
- **Layer 4 (Components)**: Atomic widgets using CanvasKit tokens.
"""

TASKS_TEMPLATE = """# Implementation Tasks: {title}
**Change ID**: `{change_id}`

## Phase 1: Specifications & Architecture (Product Owner & Architect)
- [ ] Refine Given/When/Then Acceptance Criteria in delta specification
- [ ] Complete Technical Design with MVI state definitions (`design.md`)
- [ ] Validate architectural handoff payload against schema

## Phase 2: Implementation (Senior Android Developer)
- [ ] Implement Domain UseCase in `:core:domain` with 100% unit test coverage
- [ ] Implement MVI ViewModel and UI state transitions in feature module
- [ ] Implement 4-Layer Jetpack Compose Screen and CanvasKit components
- [ ] Run linting and static analysis (`ktlint`, `detekt`) with 0 warnings

## Phase 3: Independent Verification (QA Engineer)
- [ ] Map 100% of Acceptance Criteria to automated test methods
- [ ] Execute Turbine Coroutine tests on StateFlow transitions
- [ ] Execute Compose UI tests (`ComposeTestRule`, `assertIsDisplayed`)
- [ ] Verify Quality Gate verdict PASS with 0 failures
"""

DELTA_SPEC_TEMPLATE = """# Delta Specification: {title}
**Domain**: `{change_id}`

## Added Requirements

### REQ-{change_id}-001: Core Operational Invariant
The system must validate and process the primary transaction.
- **Given** an authenticated user on the `{title}` screen
- **When** the primary action is submitted with valid inputs
- **Then** the domain UseCase executes and updates the MVI UI state accordingly.
"""


# ==============================================================================
# 2. CORE OPENSPEC COMMANDS
# ==============================================================================

def to_pascal_case(text: str) -> str:
    cleaned = re.sub(r"[^a-zA-Z0-9]", " ", text)
    return "".join(word.capitalize() for word in cleaned.split())


def cmd_propose(change_id: str, title: Optional[str] = None) -> bool:
    """Scaffolds a new OpenSpec change directory with templates."""
    change_dir = CHANGES_DIR / change_id
    if change_dir.exists():
        print(f"[ERROR] Change directory already exists: {change_dir}")
        return False

    display_title = title or change_id.replace("_", " ").replace("-", " ").title()
    pascal_id = to_pascal_case(change_id)
    created_at = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    change_dir.mkdir(parents=True, exist_ok=True)
    specs_sub = change_dir / "specs"
    specs_sub.mkdir(parents=True, exist_ok=True)

    # 1. Write proposal.md
    (change_dir / "proposal.md").write_text(
        PROPOSAL_TEMPLATE.format(title=display_title, change_id=change_id, created_at=created_at),
        encoding="utf-8"
    )

    # 2. Write design.md
    (change_dir / "design.md").write_text(
        DESIGN_TEMPLATE.format(title=display_title, change_id=change_id, pascal_id=pascal_id),
        encoding="utf-8"
    )

    # 3. Write tasks.md
    (change_dir / "tasks.md").write_text(
        TASKS_TEMPLATE.format(title=display_title, change_id=change_id),
        encoding="utf-8"
    )

    # 4. Write delta spec
    (specs_sub / "delta_spec.md").write_text(
        DELTA_SPEC_TEMPLATE.format(title=display_title, change_id=change_id),
        encoding="utf-8"
    )

    print(f"===========================================================")
    print(f"   OpenSpec Proposal Created: {change_id}")
    print(f"===========================================================")
    print(f"   Directory : {change_dir}")
    print(f"   Files     : proposal.md, design.md, tasks.md, specs/delta_spec.md")
    print(f"   Next step : Execute '/opsx apply {change_id}' to begin development.")
    print(f"===========================================================")
    return True


def cmd_validate(change_id: str) -> Tuple[bool, List[str]]:
    """Validates an active change directory against OpenSpec and SDD criteria."""
    change_dir = CHANGES_DIR / change_id
    if not change_dir.is_dir():
        return False, [f"Change directory does not exist: {change_dir}"]

    errors = []
    required_files = ["proposal.md", "design.md", "tasks.md"]
    for rf in required_files:
        f = change_dir / rf
        if not f.is_file():
            errors.append(f"Missing required OpenSpec artifact: {rf}")
        elif len(f.read_text(encoding="utf-8").strip()) < 50:
            errors.append(f"Artifact {rf} is incomplete or too short (< 50 characters).")

    # Check tasks.md checkboxes
    tasks_file = change_dir / "tasks.md"
    if tasks_file.is_file():
        content = tasks_file.read_text(encoding="utf-8")
        total_tasks = len(re.findall(r"-\s*\[[ xX]\]", content))
        completed_tasks = len(re.findall(r"-\s*\[[xX]\]", content))
        if total_tasks == 0:
            errors.append("tasks.md does not contain any checklist tasks (- [ ]).")
        else:
            print(f"[*] Task progress: {completed_tasks}/{total_tasks} completed.")

    is_valid = len(errors) == 0
    if is_valid:
        print(f"✅ OpenSpec validation PASSED for '{change_id}'.")
    else:
        print(f"❌ OpenSpec validation FAILED for '{change_id}':")
        for err in errors:
            print(f"   - {err}")
    return is_valid, errors


def cmd_archive(change_id: str, force: bool = False) -> bool:
    """Merges delta specs into living specs and archives the change directory."""
    change_dir = CHANGES_DIR / change_id
    if not change_dir.is_dir():
        print(f"[ERROR] Change directory not found: {change_dir}")
        return False

    # Check validation and completed tasks
    tasks_file = change_dir / "tasks.md"
    if tasks_file.is_file() and not force:
        content = tasks_file.read_text(encoding="utf-8")
        pending_tasks = len(re.findall(r"-\s*\[\s*\]", content))
        if pending_tasks > 0:
            print(f"[ERROR] Cannot archive '{change_id}': {pending_tasks} task(s) still pending in tasks.md.")
            print("        Mark all tasks complete with '- [x]' or use --force.")
            return False

    # 1. Merge delta specs into living specs
    specs_sub = change_dir / "specs"
    if specs_sub.is_dir():
        for delta_file in specs_sub.glob("*.md"):
            delta_content = delta_file.read_text(encoding="utf-8")
            target_living = SPECS_DIR / f"features_{change_id.lower().replace('-', '_')}.md"
            print(f"[*] Consolidating delta spec into living spec: {target_living.name}...")
            header = f"\n\n<!-- Consolidated from {change_id} on {datetime.date.today()} -->\n"
            if target_living.exists():
                with open(target_living, "a", encoding="utf-8") as f:
                    f.write(header + delta_content)
            else:
                with open(target_living, "w", encoding="utf-8") as f:
                    f.write(f"# Living Specification: {change_id}\n" + delta_content)

    # 2. Move to archive
    ARCHIVE_DIR.mkdir(parents=True, exist_ok=True)
    archive_dest = ARCHIVE_DIR / change_id
    if archive_dest.exists():
        shutil.rmtree(archive_dest)
    shutil.move(str(change_dir), str(archive_dest))

    print(f"===========================================================")
    print(f"   🎉 OpenSpec Change Successfully Archived: {change_id}")
    print(f"===========================================================")
    print(f"   Source of Truth updated in : {SPECS_DIR}")
    print(f"   Archived record stored in  : {archive_dest}")
    print(f"===========================================================")
    return True


def cmd_list():
    """Lists active changes and living specifications."""
    print("===========================================================")
    print("   OPENSPEC REPOSITORY INVENTORY")
    print("===========================================================")

    active_changes = [d.name for d in CHANGES_DIR.iterdir() if d.is_dir() and d.name != ".gitkeep"] if CHANGES_DIR.exists() else []
    print(f"\n[1] Active Changes ({len(active_changes)}):")
    if active_changes:
        for c in active_changes:
            print(f"    - {c}")
    else:
        print("    (No active changes in progress)")

    living_specs = list(SPECS_DIR.rglob("*.md")) if SPECS_DIR.exists() else []
    print(f"\n[2] Living Specifications ({len(living_specs)}):")
    if living_specs:
        for s in living_specs:
            rel = s.relative_to(SPECS_DIR)
            print(f"    - {rel}")
    else:
        print("    (No living specifications found)")

    archived = [d.name for d in ARCHIVE_DIR.iterdir() if d.is_dir() and d.name != ".gitkeep"] if ARCHIVE_DIR.exists() else []
    print(f"\n[3] Archived Changes ({len(archived)}):")
    if archived:
        for a in archived:
            print(f"    - {a}")
    else:
        print("    (No archived changes)")
    print("===========================================================")


# ==============================================================================
# 3. CLI ARGUMENT PARSER
# ==============================================================================

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="OpenSpec Lifecycle CLI for Android Agentic Ecosystem.")
    subparsers = parser.add_subparsers(dest="command", help="OpenSpec command to execute")

    # propose
    p_propose = subparsers.add_parser("propose", help="Create a new change proposal.")
    p_propose.add_argument("change_id", help="Unique change ID (e.g. FEAT-002, BUG-014)")
    p_propose.add_argument("--title", "-t", help="Descriptive title of the change")

    # validate
    p_val = subparsers.add_parser("validate", help="Validate an active change directory.")
    p_val.add_argument("change_id", help="Unique change ID")

    # archive
    p_arch = subparsers.add_parser("archive", help="Archive a completed and verified change.")
    p_arch.add_argument("change_id", help="Unique change ID")
    p_arch.add_argument("--force", "-f", action="store_true", help="Force archive even with pending tasks")

    # list
    subparsers.add_parser("list", help="List active changes and living specifications.")

    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    if args.command == "propose":
        success = cmd_propose(args.change_id, args.title)
        sys.exit(0 if success else 1)
    elif args.command == "validate":
        success, _ = cmd_validate(args.change_id)
        sys.exit(0 if success else 1)
    elif args.command == "archive":
        success = cmd_archive(args.change_id, force=args.force)
        sys.exit(0 if success else 1)
    elif args.command == "list":
        cmd_list()
        sys.exit(0)


if __name__ == "__main__":
    main()
