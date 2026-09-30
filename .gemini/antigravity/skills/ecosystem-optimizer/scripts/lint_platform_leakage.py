#!/usr/bin/env python3
"""
lint_platform_leakage.py - Platform Domain Isolation and Boundary Linter.

Audits instruction files (AGENTS.md, copilot_instructions.md, skills) within a target
repository or profile directory to detect cross-platform terminology leaks (e.g. Android
Compose references leaking into Backend repos, or Supabase/Deno into Android mobile repos).

Usage:
    python lint_platform_leakage.py --target . --profile auto
    python lint_platform_leakage.py --target /path/to/kilomenos --profile backend
    python lint_platform_leakage.py --target /path/to/KmSafe --profile android
"""

import argparse
import json
import os
import re
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

FORBIDDEN_PATTERNS = {
    "backend": [
        (r"\bScreenViewModel\b", "Android FoundationKit Presentation pattern found in backend instructions"),
        (r"\bUiState\b", "Android MVI UI State interface found in backend instructions"),
        (r"\bUiEvent\b", "Android MVI UI Event interface found in backend instructions"),
        (r"\bUiEffect\b", "Android MVI UI Effect interface found in backend instructions"),
        (r"\bHiltViewModel\b", "Android Hilt dependency injection found in backend instructions"),
        (r"\b@Composable\b", "Jetpack Compose decorator found in backend instructions"),
        (r":app:assembleDevDebug", "Android Gradle assemble task found in backend instructions"),
        (r"\bktlint\b", "Android Kotlin linter referenced in backend instructions"),
        (r"\bCompose 4-Layer\b", "Android UI layering pattern found in backend instructions"),
    ],
    "android": [
        (r"\bHono\b", "Backend Hono web framework referenced in Android-specific instructions"),
        (r"supabase/functions", "Supabase Edge Functions referenced in Android client instructions"),
        (r"\bdeno\.land\b", "Deno runtime imports referenced in Android instructions"),
        (r"\bRow Level Security\b", "PostgreSQL database security RLS policy found in Android client instructions"),
        (r"\bauth\.uid\(\)", "PostgreSQL RLS expression found in Android client instructions"),
        (r"\bservice_role\b", "Supabase administrative secret key referenced in Android client instructions"),
        (r"RTDN webhook", "Backend webhook processing referenced in Android client instructions"),
    ],
    "web": [
        (r"\bScreenViewModel\b", "Android FoundationKit Presentation pattern found in web instructions"),
        (r":app:assembleDevDebug", "Android Gradle assemble task found in web instructions"),
        (r"\bRow Level Security\b", "PostgreSQL RLS referenced in static web instructions"),
    ]
}

# Exempt sections where cross-platform references are legitimate (e.g. Solutions Architect, System Overview)
CROSS_PLATFORM_EXEMPT_SECTIONS = [
    r"## 2\. Role 2: Solutions Architect",
    r"### Role 2: Solutions Architect",
    r"## 1\. Project Overview & Platform Topology",
    r"### Cross-Platform",
    r"### 1\.2 Platform Domain Isolation",
    r"### Platform Guardrails",
]

UNIVERSAL_CROSS_PLATFORM_SKILLS = {
    "fix-bug",
    "new-feature",
    "idea-lab",
    "openspec",
    "atlassian-bridge",
    "ecosystem-optimizer",
}


def detect_target_profile(target_dir: Path) -> str:
    """Infers profile from repository files."""
    if (target_dir / "settings.gradle.kts").is_file() or (target_dir / "build.gradle.kts").is_file():
        return "android"
    if (target_dir / "supabase").is_dir():
        return "backend"
    if (target_dir / "package.json").is_file():
        pkg_content = (target_dir / "package.json").read_text(encoding="utf-8", errors="replace")
        if "supabase" in pkg_content or "hono" in pkg_content:
            return "backend"
        if "astro" in pkg_content:
            return "web"
    if (target_dir / "astro.config.mjs").is_file():
        return "web"
    return "hub"


def is_line_exempt(line: str, current_section: str) -> bool:
    """Checks if a given line belongs to an exempt cross-platform section."""
    for exempt_pat in CROSS_PLATFORM_EXEMPT_SECTIONS:
        if re.search(exempt_pat, current_section, re.IGNORECASE):
            return True
    return False


def lint_file_for_profile(file_path: Path, profile: str) -> List[Dict[str, Any]]:
    """Lints a single markdown or text file for platform leakage."""
    skill_parent = file_path.parent.name
    if skill_parent in UNIVERSAL_CROSS_PLATFORM_SKILLS or file_path.stem in UNIVERSAL_CROSS_PLATFORM_SKILLS:
        return []

    patterns = FORBIDDEN_PATTERNS.get(profile, [])
    if not patterns:
        return []

    violations: List[Dict[str, Any]] = []
    lines = file_path.read_text(encoding="utf-8", errors="replace").splitlines()

    current_section = ""
    for idx, line in enumerate(lines, start=1):
        if line.startswith("#"):
            current_section = line

        if is_line_exempt(line, current_section):
            continue

        for pat, reason in patterns:
            match = re.search(pat, line)
            if match:
                violations.append({
                    "file": str(file_path),
                    "name": file_path.name,
                    "line": idx,
                    "matched": match.group(0),
                    "context": line.strip()[:100],
                    "reason": reason,
                    "severity": "ERROR",
                })

    return violations


def lint_platform_leakage(target_dir: Path, profile: Optional[str] = None) -> Dict[str, Any]:
    """Runs platform isolation checks on the given repository."""
    effective_profile = profile if profile and profile != "auto" else detect_target_profile(target_dir)

    all_violations: List[Dict[str, Any]] = []

    # If it's the ecosystem Hub, check specific profiles directories individually
    if effective_profile == "hub":
        profiles_dir = target_dir / "profiles"
        if profiles_dir.is_dir():
            for p_name in ["android", "backend", "web"]:
                p_folder = profiles_dir / p_name
                if p_folder.is_dir():
                    for f in p_folder.rglob("*.md"):
                        all_violations.extend(lint_file_for_profile(f, p_name))
        effective_profile = "hub (profiles audited)"
    else:
        # Check AGENTS.md, copilot_instructions.md, and skills
        files_to_check: List[Path] = []
        for cand in [target_dir / "AGENTS.md", target_dir / ".github" / "copilot_instructions.md"]:
            if cand.is_file():
                files_to_check.append(cand)

        skills_dirs = [target_dir / "skills", target_dir / ".agents" / "skills", target_dir / ".gemini" / "antigravity" / "skills"]
        for s_dir in skills_dirs:
            if s_dir.is_dir():
                for skill_md in s_dir.rglob("SKILL.md"):
                    files_to_check.append(skill_md)

        for f in files_to_check:
            all_violations.extend(lint_file_for_profile(f, effective_profile))

    status = "FAIL" if all_violations else "PASS"

    return {
        "status": status,
        "profile": effective_profile,
        "target": str(target_dir),
        "violations_count": len(all_violations),
        "violations": all_violations,
    }


def main():
    parser = argparse.ArgumentParser(description="Lint agent instructions for platform leakage.")
    parser.add_argument("--target", default=".", help="Target repository directory (default: current dir)")
    parser.add_argument("--profile", default="auto", choices=["android", "backend", "web", "auto"], help="Platform profile")
    parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    args = parser.parse_args()

    target_path = Path(args.target).resolve()
    result = lint_platform_leakage(target_path, args.profile)

    if args.json:
        print(json.dumps(result, indent=2))
        sys.exit(1 if result["status"] == "FAIL" else 0)

    print("===========================================================")
    print("   🛡️ PLATFORM DOMAIN ISOLATION & LEAKAGE LINTER")
    print(f"   Target  : {target_path}")
    print(f"   Profile : {result['profile'].upper()}")
    print(f"   Status  : {result['status']}")
    print(f"   Leaks   : {result['violations_count']} detected")
    print("===========================================================")

    if result["violations"]:
        for v in result["violations"]:
            print(f" ❌ [{v['name']}:{v['line']}] Matched '{v['matched']}'")
            print(f"    Reason : {v['reason']}")
            print(f"    Line   : {v['context']}")
            print()
    else:
        print(" ✅ Clean domain isolation! No cross-platform leakage detected.")

    print("===========================================================")
    sys.exit(1 if result["status"] == "FAIL" else 0)


if __name__ == "__main__":
    main()
