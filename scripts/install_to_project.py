#!/usr/bin/env python3
"""
Android Agentic Ecosystem - Project Installer & Synchronizer

Bootstraps the Android Agentic Ecosystem (Skills, Schemas, Templates, AGENTS.md)
into any target Android repository.

Usage:
    python scripts/install_to_project.py --target C:/Users/user/AndroidStudioProjects/MyProject
    python scripts/install_to_project.py --target ../MyOtherApp --mode antigravity
"""

import argparse
import os
import shutil
import sys
from pathlib import Path

def copy_dir(src: Path, dst: Path, overwrite: bool = True):
    if not src.exists():
        return
    dst.mkdir(parents=True, exist_ok=True)
    for item in src.iterdir():
        if item.name in ('__pycache__', '.git', '.gradle', 'build'):
            continue
        target = dst / item.name
        if item.is_dir():
            copy_dir(item, target, overwrite)
        else:
            if not target.exists() or overwrite:
                shutil.copy2(item, target)

def install_ecosystem(target_dir: Path, mode: str = 'both', overwrite: bool = True):
    source_root = Path(__file__).resolve().parent.parent
    target_dir = target_dir.resolve()

    if not target_dir.exists():
        print(f"[ERROR] Target directory does not exist: {target_dir}")
        sys.exit(1)

    print(f"===========================================================")
    print(f"   Bootstrapping Android Agentic Ecosystem")
    print(f"   Source : {source_root}")
    print(f"   Target : {target_dir}")
    print(f"   Mode   : {mode}")
    print(f"===========================================================")

    # 1. Install Antigravity Skills (.gemini/antigravity/skills)
    if mode in ('antigravity', 'both'):
        skills_dst = target_dir / '.gemini' / 'antigravity' / 'skills'
        skills_src = source_root / 'skills'
        print(f"\n[1] Installing {len(list(skills_src.iterdir()))} Skills to {skills_dst}...")
        copy_dir(skills_src, skills_dst, overwrite)
        print("    -> Skills installed successfully.")

    # 2. Install SDD Contracts, Schemas, Templates, Walkthrough & OpenSpec
    if mode in ('standalone', 'both'):
        for folder in ('schemas', 'templates', 'walkthrough', 'openspec'):
            src_f = source_root / folder
            dst_f = target_dir / folder
            print(f"\n[2] Installing {folder} to {dst_f}...")
            copy_dir(src_f, dst_f, overwrite)

        # Copy AGENTS.md and PROJECT.md ONLY if not already existing
        for doc in ('AGENTS.md', 'PROJECT.md'):
            doc_src = source_root / doc
            doc_dst = target_dir / doc
            if doc_src.exists() and not doc_dst.exists():
                print(f"    -> Copying {doc} to target root...")
                shutil.copy2(doc_src, doc_dst)
            elif doc_dst.exists():
                print(f"    -> Target already has {doc}, preserving existing file.")

        # Copy verification scripts & tests
        scripts_dst = target_dir / 'scripts'
        scripts_src = source_root / 'scripts'
        copy_dir(scripts_src, scripts_dst, overwrite)

        tests_dst = target_dir / 'tests'
        tests_src = source_root / 'tests'
        copy_dir(tests_src, tests_dst, overwrite)

    print("\n===========================================================")
    print("   Install Complete! Your Android project is now equipped")
    print("   with Spec-Driven Multi-Agent Development & Test Harness.")
    print("===========================================================")

def main():
    parser = argparse.ArgumentParser(description="Install Android Agentic Ecosystem into a project.")
    parser.add_argument("--target", "-t", required=True, help="Target Android project root directory")
    parser.add_argument("--mode", "-m", choices=['antigravity', 'standalone', 'both'], default='both',
                        help="Installation mode: 'antigravity' (skills into .gemini), 'standalone' (schemas/templates), or 'both'")
    parser.add_argument("--no-overwrite", action='store_true', help="Do not overwrite existing files")

    args = parser.parse_args()
    target_path = Path(args.target)
    install_ecosystem(target_path, mode=args.mode, overwrite=not args.no_overwrite)

if __name__ == '__main__':
    main()
