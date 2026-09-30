#!/usr/bin/env python3
"""
audit_context_budget.py - Context Budget and Token Footprint Auditor for Agentic Workspaces.

Measures the word count, character count, and estimated LLM token footprint
of agent instructions (AGENTS.md, copilot_instructions.md, SKILL.md files).
Flags context bloat and emits actionable compression recommendations.

Usage:
    python audit_context_budget.py --target .
    python audit_context_budget.py --target . --json
    python audit_context_budget.py --target /path/to/project --verbose
"""

import argparse
import json
import math
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

# Recommended budget thresholds (in word count)
BUDGET_THRESHOLDS = {
    "copilot_instructions": {"warn": 800, "fail": 1500, "label": "Copilot Instructions"},
    "agents_md": {"warn": 3500, "fail": 6000, "label": "AGENTS.md Role Specification"},
    "skill_md": {"warn": 1200, "fail": 2500, "label": "Skill Manifest"},
    "generic_doc": {"warn": 2000, "fail": 4000, "label": "Generic Document"},
}

# Average tokens per word ratio for mixed Markdown/Code instructions
TOKENS_PER_WORD = 1.33


def estimate_tokens(text: str) -> int:
    """Estimates token count from text using character and word heuristics."""
    words = len(text.split())
    chars = len(text)
    # Blend word count and char-based tokenization (~3.8 chars per token in code/technical English)
    token_est_words = words * TOKENS_PER_WORD
    token_est_chars = chars / 3.8
    return math.ceil((token_est_words + token_est_chars) / 2.0)


def analyze_file(file_path: Path, category: str) -> Dict[str, Any]:
    """Analyzes a single file's context footprint."""
    content = file_path.read_text(encoding="utf-8", errors="replace")
    lines = content.splitlines()
    words = len(content.split())
    chars = len(content)
    tokens = estimate_tokens(content)

    thresholds = BUDGET_THRESHOLDS.get(category, BUDGET_THRESHOLDS["generic_doc"])
    status = "PASS"
    message = "Within budget"

    if words > thresholds["fail"]:
        status = "FAIL"
        message = f"Exceeds critical limit ({words} > {thresholds['fail']} words)"
    elif words > thresholds["warn"]:
        status = "WARN"
        message = f"Approaching limit ({words} > {thresholds['warn']} words)"

    return {
        "file": str(file_path),
        "name": file_path.name,
        "category": category,
        "lines": len(lines),
        "words": words,
        "chars": chars,
        "estimated_tokens": tokens,
        "warn_threshold": thresholds["warn"],
        "fail_threshold": thresholds["fail"],
        "status": status,
        "message": message,
    }


def audit_context_budget(target_dir: Path) -> Dict[str, Any]:
    """Scans and audits all instruction files in target directory."""
    results: List[Dict[str, Any]] = []

    # 1. Check AGENTS.md
    for agent_path in [target_dir / "AGENTS.md", target_dir / "profiles" / "android" / "AGENTS.md", target_dir / "profiles" / "backend" / "AGENTS.md"]:
        if agent_path.is_file():
            results.append(analyze_file(agent_path, "agents_md"))

    # 2. Check copilot_instructions.md
    copilot_path = target_dir / ".github" / "copilot_instructions.md"
    if copilot_path.is_file():
        results.append(analyze_file(copilot_path, "copilot_instructions"))

    # 3. Check all SKILL.md files
    skills_dirs = [
        target_dir / "skills",
        target_dir / ".gemini" / "antigravity" / "skills",
        target_dir / ".agents" / "skills",
    ]
    seen_skills = set()
    for s_dir in skills_dirs:
        if not s_dir.is_dir():
            continue
        for skill_folder in s_dir.iterdir():
            if not skill_folder.is_dir() or skill_folder.name in seen_skills:
                continue
            skill_md = skill_folder / "SKILL.md"
            if skill_md.is_file():
                seen_skills.add(skill_folder.name)
                results.append(analyze_file(skill_md, "skill_md"))

    total_words = sum(r["words"] for r in results)
    total_tokens = sum(r["estimated_tokens"] for r in results)
    failed_items = [r for r in results if r["status"] == "FAIL"]
    warned_items = [r for r in results if r["status"] == "WARN"]

    overall_status = "PASS"
    if failed_items:
        overall_status = "FAIL"
    elif warned_items:
        overall_status = "WARN"

    recommendations: List[str] = []
    if failed_items:
        for f in failed_items:
            recommendations.append(
                f"Compress '{f['name']}' ({f['words']} words). Remove repetitive examples or extract into references."
            )
    if warned_items:
        for w in warned_items:
            recommendations.append(
                f"Notice: '{w['name']}' has {w['words']} words. Consider trimming conversational filler."
            )
    if not recommendations:
        recommendations.append("All instructions adhere strictly to the lean context budget.")

    return {
        "overall_status": overall_status,
        "files_analyzed": len(results),
        "total_words": total_words,
        "total_estimated_tokens": total_tokens,
        "passed_count": len([r for r in results if r["status"] == "PASS"]),
        "warned_count": len(warned_items),
        "failed_count": len(failed_items),
        "details": results,
        "recommendations": recommendations,
    }


def main():
    parser = argparse.ArgumentParser(description="Audit context token budgets for agentic instructions.")
    parser.add_argument("--target", default=".", help="Target project root directory (default: current dir)")
    parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    parser.add_argument("--verbose", action="store_true", help="Include all files in console output")
    args = parser.parse_args()

    target_path = Path(args.target).resolve()
    report = audit_context_budget(target_path)

    if args.json:
        print(json.dumps(report, indent=2))
        sys.exit(1 if report["overall_status"] == "FAIL" else 0)

    print("===========================================================")
    print("   📊 CONTEXT BUDGET & TOKEN FOOTPRINT AUDIT")
    print(f"   Target : {target_path}")
    print(f"   Status : {report['overall_status']}")
    print(f"   Tokens : ~{report['total_estimated_tokens']:,} tokens across {report['files_analyzed']} files")
    print("===========================================================")
    print(f"   Passed : {report['passed_count']} | Warnings: {report['warned_count']} | Failures: {report['failed_count']}")
    print("-----------------------------------------------------------")

    for item in report["details"]:
        if args.verbose or item["status"] != "PASS":
            flag = "✅" if item["status"] == "PASS" else ("⚠️ " if item["status"] == "WARN" else "❌")
            print(f" {flag} [{item['status']:4s}] {item['name']:30s} | {item['words']:5d} words | ~{item['estimated_tokens']:5d} tokens | {item['message']}")

    print("-----------------------------------------------------------")
    print("💡 Recommendations:")
    for rec in report["recommendations"]:
        print(f"   - {rec}")
    print("===========================================================")

    sys.exit(1 if report["overall_status"] == "FAIL" else 0)


if __name__ == "__main__":
    main()
