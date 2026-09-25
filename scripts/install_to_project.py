#!/usr/bin/env python3
"""
Android Agentic Ecosystem - Project Installer & Hub-and-Spoke Deployer

Bootstraps the Agentic Ecosystem (Core Governance, Schemas, Templates, Atlassian Bridge, OpenSpec)
into any target repository with platform-specific discipline profiles:
- Android (Kotlin / Jetpack Compose / Gradle)
- Backend (Supabase / PostgreSQL / Edge Functions / Deno)
- Web (HTML/CSS / Astro / SEO / Legal Compliance)
- All (Comprehensive multi-disciplinary installation)

Usage:
    python scripts/install_to_project.py --target ../KmSafe --profile android
    python scripts/install_to_project.py --target ../kilomenos-backend --profile backend
    python scripts/install_to_project.py --target ../kilomenos-web --profile web
"""

import argparse
import json
import os
import shutil
import sys
from pathlib import Path
from typing import Optional, Set

CORE_SKILLS = {
    'openspec',
    'atlassian-bridge',
    'idea-lab',
}

PROFILE_SKILLS = {
    'android': CORE_SKILLS | {
        'android-staff-engineer-compose',
        'android-testing',
        'android-device',
        'android-quality',
        'new-feature',
        'fix-bug',
        'performance-optimization-tips',
        'po-digital-experience-fintech',
        'senior-debugging-engineer',
    },
    'backend': CORE_SKILLS | {
        'supabase-db-triage',
        'supabase-edge-functions',
        'senior-debugging-engineer',
        'new-feature',
        'fix-bug',
    },
    'web': CORE_SKILLS | {
        'legal-compliance-audit',
        'web-lighthouse-seo',
        'new-feature',
        'fix-bug',
    },
    'all': None,  # All skills
}

PROFILE_CONFIG_DEFAULTS = {
    'android': {"board_id": "1", "label": "android"},
    'backend': {"board_id": "1", "label": "backend"},
    'web': {"board_id": "1", "label": "web"},
    'all': {"board_id": "1", "label": ""},
}


def copy_dir(src: Path, dst: Path, overwrite: bool = True, filter_names: Optional[Set[str]] = None):
    if not src.exists():
        return
    dst.mkdir(parents=True, exist_ok=True)
    for item in src.iterdir():
        if item.name in ('__pycache__', '.git', '.gradle', 'build'):
            continue
        if filter_names is not None and item.name not in filter_names:
            continue
        target = dst / item.name
        if item.is_dir():
            copy_dir(item, target, overwrite)
        else:
            if not target.exists() or overwrite:
                shutil.copy2(item, target)


def generate_profile_config(target_dir: Path, profile: str):
    config_file = target_dir / '.atlassian_config.json.example'
    meta = PROFILE_CONFIG_DEFAULTS.get(profile, PROFILE_CONFIG_DEFAULTS['all'])
    config_data = {
        "url": "https://joshluq-dev.atlassian.net",
        "email": "josh.luque@gmail.com",
        "api_token": "PASTE_YOUR_API_TOKEN_HERE",
        "project_key": "KILOMENOS",
        "board_id": meta["board_id"],
        "label": meta["label"],
        "confluence_space": "KILOMENOS"
    }
    config_file.write_text(json.dumps(config_data, indent=2) + "\n", encoding="utf-8")
    print(f"    -> Generated {config_file.name} for profile '{profile}'.")


def install_ecosystem(target_dir: Path, mode: str = 'both', profile: str = 'android', overwrite: bool = True, update_agents: bool = False):
    source_root = Path(__file__).resolve().parent.parent
    target_dir = target_dir.resolve()

    if not target_dir.exists():
        print(f"[ERROR] Target directory does not exist: {target_dir}")
        sys.exit(1)

    allowed_skills = PROFILE_SKILLS.get(profile)

    print("===========================================================")
    print("   Hub & Spoke Agentic Ecosystem Deployer")
    print(f"   Source  : {source_root}")
    print(f"   Target  : {target_dir}")
    print(f"   Profile : {profile.upper()} ({len(allowed_skills) if allowed_skills else 'ALL'} skills)")
    print(f"   Mode    : {mode}")
    print("===========================================================")

    # 1. Install Skills (.gemini/antigravity/skills and .agents/skills)
    if mode in ('antigravity', 'both'):
        skills_dst = target_dir / '.gemini' / 'antigravity' / 'skills'
        skills_src = source_root / 'skills' if (source_root / 'skills').exists() else (source_root / '.agents' / 'skills')
        print(f"\n[1] Installing filtered skills for profile '{profile}'...")
        copy_dir(skills_src, skills_dst, overwrite, filter_names=allowed_skills)
        agents_skills_dst = target_dir / '.agents' / 'skills'
        if agents_skills_dst.parent.exists():
            copy_dir(skills_src, agents_skills_dst, overwrite, filter_names=allowed_skills)
        installed = [p.name for p in skills_dst.iterdir() if p.is_dir()] if skills_dst.exists() else []
        print(f"    -> Installed {len(installed)} skills: {', '.join(sorted(installed))}")

    # 2. Install SDD Contracts, Schemas, Templates, Walkthrough & OpenSpec
    if mode in ('standalone', 'both'):
        for folder in ('schemas', 'templates', 'walkthrough', 'openspec'):
            src_f = source_root / folder
            dst_f = target_dir / folder
            print(f"\n[2] Installing {folder} to {dst_f}...")
            copy_dir(src_f, dst_f, overwrite)

        # Copy PROJECT.md ONLY if not already existing (preserve project-specific configuration)
        proj_src = source_root / 'PROJECT.md'
        proj_dst = target_dir / 'PROJECT.md'
        if proj_src.exists() and not proj_dst.exists():
            print(f"    -> Copying PROJECT.md to target root...")
            shutil.copy2(proj_src, proj_dst)
        elif proj_dst.exists():
            print(f"    -> Target already has PROJECT.md, preserving existing file.")

        # Copy or update AGENTS.md
        agents_src = source_root / 'AGENTS.md'
        agents_dst = target_dir / 'AGENTS.md'
        if agents_src.exists():
            if not agents_dst.exists() or update_agents:
                action_str = "Updating" if agents_dst.exists() else "Copying"
                print(f"    -> {action_str} AGENTS.md in target root...")
                shutil.copy2(agents_src, agents_dst)
            else:
                print(f"    -> Target already has AGENTS.md, preserving existing file (pass --update-agents to refresh).")

        # Copy core scripts & tests
        scripts_dst = target_dir / 'scripts'
        scripts_src = source_root / 'scripts'
        copy_dir(scripts_src, scripts_dst, overwrite)

        tests_dst = target_dir / 'tests'
        tests_src = source_root / 'tests'
        copy_dir(tests_src, tests_dst, overwrite)

        # Generate profile-tailored .atlassian_config.json.example
        generate_profile_config(target_dir, profile)

    print("\n===========================================================")
    print(f"   🎉 Deployment Complete for Profile: {profile.upper()}")
    print("   Project equipped with Spec-Driven Multi-Agent Governance.")
    print("===========================================================")


def main():
    parser = argparse.ArgumentParser(description="Install and configure Agentic Ecosystem by profile.")
    parser.add_argument("--target", "-t", required=True, help="Target project root directory")
    parser.add_argument("--profile", "-p", choices=['android', 'backend', 'web', 'all'], default='android',
                        help="Platform discipline profile: 'android', 'backend', 'web', or 'all' (default: android)")
    parser.add_argument("--mode", "-m", choices=['antigravity', 'standalone', 'both'], default='both',
                        help="Installation mode: 'antigravity', 'standalone', or 'both' (default: both)")
    parser.add_argument("--no-overwrite", action='store_true', help="Do not overwrite existing files")
    parser.add_argument("--update-agents", action='store_true', help="Overwrite AGENTS.md with the latest ecosystem role specifications")

    args = parser.parse_args()
    target_path = Path(args.target)
    install_ecosystem(target_path, mode=args.mode, profile=args.profile, overwrite=not args.no_overwrite, update_agents=args.update_agents)


if __name__ == '__main__':
    main()
