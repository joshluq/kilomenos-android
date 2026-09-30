#!/usr/bin/env python3
"""
audit_context_pruning.py - Context Pruning, Redundancy, and Contradiction Auditor.

Inspects system instructions, governance files (AGENTS.md, copilot_instructions.md),
and skill manifests (SKILL.md) to detect:
1. Zero-Signal Platitudes: Obvious, generic instructions that waste context and tokens.
2. Cross-File Redundancies: Duplicate directives across files that cause context dilution.
3. Contradictory Instructions: Incompatible rules causing model decision paralysis.

Emits actionable pruning recommendations with estimated token savings.

Usage:
    python audit_context_pruning.py --target .
    python audit_context_pruning.py --target . --json
    python audit_context_pruning.py --target . --report docs/ecosystem/PRUNING_REPORT.md
"""

import argparse
import json
import math
import os
import re
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

# Patterns for obvious, generic instructions that LLMs already know
PLATITUDE_PATTERNS = [
    (
        r"\b(write|produce|generate)\s+(?:[\w,\s]+?\s+)?(clean|readable|maintainable|robust|high[- ]quality)\s+code\b",
        "Generic platitude: LLMs already prioritize clean code; wastes prompt attention without adding verifiable domain constraints."
    ),
    (
        r"\bfollow\s+(?:standard\s+|industry\s+|official\s+)?(best\s+practices|coding\s+standards|conventions)\b",
        "Vague platitude: Does not enforce an objective, testable project invariant."
    ),
    (
        r"\bensure\s+(?:the\s+)?code\s+is\s+free\s+of\s+(bugs|errors|syntax\s+errors|defects)\b",
        "Trivial instruction: Does not provide an automated test or verification gate."
    ),
    (
        r"\bremember\s+to\s+import\s+(?:all\s+)?(?:necessary|required)\s+(libraries|packages|modules|dependencies)\b",
        "Trivial instruction: Language compiler or linter already enforces imports."
    ),
    (
        r"\buse\s+(?:[\w,\s]+?\s+)?(clear|descriptive|meaningful|good)\s+(variable|function|method|class)\s+names\b",
        "Generic platitude: Standard baseline known by LLMs; adds zero domain signal."
    ),
    (
        r"\b(?:test|verify)\s+(?:your|the)\s+code\s+thoroughly\b",
        "Vague instruction: Lacks a concrete test command, runner, or coverage metric."
    ),
    (
        r"\bmake\s+sure\s+(?:everything|it)\s+works\s+(?:properly|as\s+expected|correctly)\b",
        "Zero-signal filler: Lacks objective criteria."
    ),
    (
        r"\bbe\s+(helpful|polite|accurate|concise|careful)\b",
        "Conversational fluff: LLM system baseline; redundant in agentic code specifications."
    ),
]

# Patterns for mutually incompatible directives
CONTRADICTION_PAIRS = [
    {
        "id": "AUTONOMY_VS_HALT",
        "pattern_a": r"\b(execute fully autonomously|do not ask|never prompt the user|run without confirmation|never ask questions)\b",
        "pattern_b": r"\b(mandatory halt gate|stop immediately|prompt the developer|await approval before|stop and ask)\b",
        "description": "Conflict between autonomous execution directive and mandatory halt/approval requirement."
    },
    {
        "id": "MUTABLE_VS_IMMUTABLE_STATE",
        "pattern_a": r"\b(immutable (ui )?state|val properties only|immutable stateflow)\b",
        "pattern_b": r"\b(mutable (ui )?state|mutablelivedata|var properties in state)\b",
        "description": "Conflict between immutable state invariant and mutable state patterns."
    },
    {
        "id": "FAIL_FAST_VS_SILENT_FALLBACK",
        "pattern_a": r"\b(fail fast|throw (explicit |typed )?exception|never suppress errors)\b",
        "pattern_b": r"\b(suppress (all )?errors|silently ignore|return default fallback on error|catch-all empty)\b",
        "description": "Conflict between fail-fast error reporting and silent fallback suppression."
    }
]


def clean_directive_text(text: str) -> str:
    """Strips markdown bullets, code blocks, bold markers, and punctuation."""
    t = re.sub(r"^[-*#0-9.]+\s*", "", text.strip())
    t = re.sub(r"[*`_\[\]()]", " ", t)
    t = re.sub(r"\s+", " ", t)
    return t.strip().lower()


def tokenize_words(text: str) -> Set[str]:
    """Extracts non-stopword tokens for similarity comparison."""
    stopwords = {
        "the", "a", "an", "is", "are", "and", "or", "in", "on", "of", "to", "for",
        "with", "by", "from", "at", "as", "that", "this", "it", "be", "all", "must"
    }
    words = set(re.findall(r"\b[a-z]{3,}\b", text.lower()))
    return words - stopwords


def jaccard_similarity(set_a: Set[str], set_b: Set[str]) -> float:
    """Calculates Jaccard index between two token sets."""
    if not set_a or not set_b:
        return 0.0
    intersection = len(set_a & set_b)
    union = len(set_a | set_b)
    return intersection / union if union > 0 else 0.0


def scan_file_for_platitudes(file_path: Path) -> List[Dict[str, Any]]:
    """Detects zero-signal platitudes in a single file."""
    findings = []
    lines = file_path.read_text(encoding="utf-8", errors="replace").splitlines()

    for idx, line in enumerate(lines, start=1):
        clean_line = line.strip()
        if not clean_line or clean_line.startswith("<!--") or clean_line.startswith("```"):
            continue

        for pat, reason in PLATITUDE_PATTERNS:
            match = re.search(pat, clean_line, re.IGNORECASE)
            if match:
                words_count = len(clean_line.split())
                findings.append({
                    "file": str(file_path),
                    "name": file_path.name,
                    "line": idx,
                    "type": "ZERO_SIGNAL_PLATITUDE",
                    "matched": match.group(0),
                    "text": clean_line[:120],
                    "tokens_wasted": math.ceil(words_count * 1.33),
                    "reason": reason,
                    "action": "Safe to delete completely."
                })
                break
    return findings


def extract_directives(file_path: Path) -> List[Dict[str, Any]]:
    """Extracts numbered or bulleted directives from markdown."""
    directives = []
    lines = file_path.read_text(encoding="utf-8", errors="replace").splitlines()

    for idx, line in enumerate(lines, start=1):
        stripped = line.strip()
def is_negative_constraint(line: str) -> bool:
    """Checks if a directive is explicitly forbidding or restricting an action."""
    neg_words = [
        r"\bnever\b", r"\bprohibit", r"\bforbidden\b", r"\bstrictly\s+no\b",
        r"\bdo\s+not\b", r"\bavoid\b", r"\bmust\s+not\b", r"\bwithout\b"
    ]
    for nw in neg_words:
        if re.search(nw, line, re.IGNORECASE):
            return True
    return False


def extract_directives(file_path: Path, target_dir: Path) -> List[Dict[str, Any]]:
    """Extracts numbered or bulleted directives from markdown."""
    directives = []
    lines = file_path.read_text(encoding="utf-8", errors="replace").splitlines()
    try:
        rel_path = file_path.relative_to(target_dir).as_posix()
    except Exception:
        rel_path = file_path.name

    for idx, line in enumerate(lines, start=1):
        stripped = line.strip()
        if re.match(r"^([-*]|\d+\.)\s+\S+", stripped):
            clean = clean_directive_text(stripped)
            tokens = tokenize_words(clean)
            if len(tokens) >= 5:  # Meaningful directive
                directives.append({
                    "file": file_path,
                    "rel_path": rel_path,
                    "name": file_path.name,
                    "line": idx,
                    "raw": stripped,
                    "clean": clean,
                    "tokens": tokens,
                })
    return directives


def detect_cross_file_redundancies(directives_by_file: Dict[Path, List[Dict[str, Any]]]) -> List[Dict[str, Any]]:
    """Identifies nearly identical directives duplicated across different functional files."""
    redundancies = []
    all_files = list(directives_by_file.keys())

    # Sort files so AGENTS.md / copilot_instructions.md act as canonical reference (SSOT)
    def file_priority(p: Path) -> int:
        if p.name == "AGENTS.md" and "profiles" not in str(p):
            return 0
        if "copilot_instructions.md" in p.name:
            return 1
        return 2

    all_files.sort(key=file_priority)
    seen_pairs: Set[Tuple[str, int, str, int]] = set()

    for i in range(len(all_files)):
        file_a = all_files[i]
        dirs_a = directives_by_file[file_a]

        for j in range(i + 1, len(all_files)):
            file_b = all_files[j]
            dirs_b = directives_by_file[file_b]

            # Don't compare root AGENTS.md with profile AGENTS.md templates
            if "profiles" in str(file_a) and "profiles" not in str(file_b):
                continue
            if "profiles" in str(file_b) and "profiles" not in str(file_a):
                continue
            if file_a == file_b:
                continue

            for d_a in dirs_a:
                for d_b in dirs_b:
                    sim = jaccard_similarity(d_a["tokens"], d_b["tokens"])
                    if sim >= 0.75:
                        pair_key = (str(d_a["file"]), d_a["line"], str(d_b["file"]), d_b["line"])
                        if pair_key in seen_pairs:
                            continue
                        seen_pairs.add(pair_key)

                        redundancies.append({
                            "type": "REDUNDANT_DIRECTIVE",
                            "similarity": round(sim, 2),
                            "canonical_file": d_a["rel_path"],
                            "canonical_line": d_a["line"],
                            "canonical_text": d_a["raw"][:100],
                            "duplicate_file": d_b["rel_path"],
                            "duplicate_line": d_b["line"],
                            "duplicate_text": d_b["raw"][:100],
                            "tokens_wasted": math.ceil(len(d_b["raw"].split()) * 1.33),
                            "action": f"Consider referencing SSOT in '{d_a['rel_path']}:L{d_a['line']}'."
                        })
    return redundancies


def detect_contradictions(files_to_check: List[Path], target_dir: Path) -> List[Dict[str, Any]]:
    """Checks for logical contradictions within the active instruction set."""
    contradictions = []

    file_contents = {}
    for f in files_to_check:
        try:
            rel = f.relative_to(target_dir).as_posix()
        except Exception:
            rel = f.name
        file_contents[rel] = f.read_text(encoding="utf-8", errors="replace").splitlines()

    for pair in CONTRADICTION_PAIRS:
        matches_a = []
        matches_b = []

        for rel, lines in file_contents.items():
            for idx, line in enumerate(lines, start=1):
                clean_l = line.strip()
                if not clean_l or clean_l.startswith("```"):
                    continue

                if re.search(pair["pattern_a"], clean_l, re.IGNORECASE):
                    # If pattern_a matched, check if it's an affirmative stance
                    matches_a.append({"file": rel, "line": idx, "text": clean_l[:100]})

                if re.search(pair["pattern_b"], clean_l, re.IGNORECASE):
                    # If pattern_b matched, verify if it's an affirmative violation or a negative prohibition
                    is_neg = is_negative_constraint(clean_l)
                    # For MUTABLE_VS_IMMUTABLE_STATE and FAIL_FAST_VS_SILENT_FALLBACK:
                    # A negative constraint ("NEVER allow mutable UI state") actually affirms immutability!
                    if pair["id"] in ("MUTABLE_VS_IMMUTABLE_STATE", "FAIL_FAST_VS_SILENT_FALLBACK") and is_neg:
                        continue
                    matches_b.append({"file": rel, "line": idx, "text": clean_l[:100]})

        if matches_a and matches_b:
            contradictions.append({
                "type": "CONTRADICTION",
                "id": pair["id"],
                "description": pair["description"],
                "side_a": matches_a[0],
                "side_b": matches_b[0],
                "action": "Clarify the operational precedence between these two rules."
            })

    return contradictions


def audit_context_pruning(target_dir: Path) -> Dict[str, Any]:
    """Runs a complete context pruning, redundancy, and contradiction audit."""
    target_dir = target_dir.resolve()

    # Discover candidate instruction files
    instruction_files: List[Path] = []

    for name in ["AGENTS.md"]:
        for candidate in [target_dir / name, target_dir / "profiles" / "android" / name, target_dir / "profiles" / "backend" / name]:
            if candidate.is_file():
                instruction_files.append(candidate)

    copilot_file = target_dir / ".github" / "copilot_instructions.md"
    if copilot_file.is_file():
        instruction_files.append(copilot_file)

    seen_skills = set()
    skills_dirs = [target_dir / "skills", target_dir / ".agents" / "skills", target_dir / ".gemini" / "antigravity" / "skills"]
    for s_dir in skills_dirs:
        if s_dir.is_dir():
            for sm in s_dir.rglob("SKILL.md"):
                skill_name = sm.parent.name
                if skill_name not in seen_skills:
                    seen_skills.add(skill_name)
                    instruction_files.append(sm)

    # 1. Platitudes
    platitudes = []
    for f in instruction_files:
        platitudes.extend(scan_file_for_platitudes(f))

    # 2. Redundancies
    directives_by_file = {}
    for f in instruction_files:
        dirs = extract_directives(f, target_dir)
        if dirs:
            directives_by_file[f] = dirs

    redundancies = detect_cross_file_redundancies(directives_by_file)

    # 3. Contradictions
    contradictions = detect_contradictions(instruction_files, target_dir)

    total_tokens_wasted = sum(p["tokens_wasted"] for p in platitudes) + sum(r["tokens_wasted"] for r in redundancies)

    status = "PASS"
    if contradictions:
        status = "FAIL"
    elif platitudes or redundancies:
        status = "WARN"

    return {
        "status": status,
        "target": str(target_dir),
        "files_analyzed": len(instruction_files),
        "platitudes_count": len(platitudes),
        "redundancies_count": len(redundancies),
        "contradictions_count": len(contradictions),
        "estimated_prunable_tokens": total_tokens_wasted,
        "platitudes": platitudes,
        "redundancies": redundancies,
        "contradictions": contradictions,
    }


def generate_pruning_report(report: Dict[str, Any], output_path: Path):
    """Outputs a markdown pruning and optimization report."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    status_icon = "🟢" if report["status"] == "PASS" else ("🟡" if report["status"] == "WARN" else "🔴")

    lines = [
        "# Context Pruning & Optimization Report",
        f"**Target**: `{report['target']}`  ",
        f"**Status**: {status_icon} **{report['status']}**  ",
        f"**Potential Token Savings**: ~**{report['estimated_prunable_tokens']:,}** tokens",
        "",
        "---",
        "",
        f"## 1. Zero-Signal Platitudes & Obvious Rules ({report['platitudes_count']})",
        "These lines state generic LLM conventions without adding domain-specific constraints. Safe to delete to save context tokens:",
        "",
    ]

    if report["platitudes"]:
        lines.append("| File | Line | Snippet | Reason | Tokens Saved |")
        lines.append("| :--- | :--- | :--- | :--- | :--- |")
        for p in report["platitudes"]:
            lines.append(f"| `{p['name']}` | `{p['line']}` | `{p['matched']}` | {p['reason']} | ~{p['tokens_wasted']} |")
    else:
        lines.append("✅ Zero obvious platitudes found. All instructions carry substantive domain signal.")

    lines.extend([
        "",
        "---",
        "",
        f"## 2. Cross-File Redundancies & Duplications ({report['redundancies_count']})",
        "These directives are repeated across multiple files. Keep in Single Source of Truth (SSOT) and replace duplicates with references:",
        "",
    ])

    if report["redundancies"]:
        lines.append("| Canonical (SSOT) | Line | Duplicate File | Line | Similarity | Action |")
        lines.append("| :--- | :--- | :--- | :--- | :--- | :--- |")
        for r in report["redundancies"]:
            lines.append(f"| `{r['canonical_file']}` | `{r['canonical_line']}` | `{r['duplicate_file']}` | `{r['duplicate_line']}` | {int(r['similarity'] * 100)}% | {r['action']} |")
    else:
        lines.append("✅ Zero cross-file duplicate directives found.")

    lines.extend([
        "",
        "---",
        "",
        f"## 3. Potential Contradictions ({report['contradictions_count']})",
        "Directives that may create conflicting directives for the agent:",
        "",
    ])

    if report["contradictions"]:
        for c in report["contradictions"]:
            lines.append(f"### ⚠️ Conflict: {c['id']}")
            lines.append(f"- **Description**: {c['description']}")
            lines.append(f"- **Rule A**: `{c['side_a']['file']}:{c['side_a']['line']}`: {c['side_a']['text']}")
            lines.append(f"- **Rule B**: `{c['side_b']['file']}:{c['side_b']['line']}`: {c['side_b']['text']}")
            lines.append(f"- **Action**: {c['action']}")
            lines.append("")
    else:
        lines.append("✅ Zero contradictory directives detected.")

    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"[+] Wrote context pruning report to: {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Audit context instructions for platitudes, redundancies, and contradictions.")
    parser.add_argument("--target", default=".", help="Target workspace directory (default: current dir)")
    parser.add_argument("--report", help="Path to write Markdown summary report")
    parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    args = parser.parse_args()

    target_dir = Path(args.target).resolve()
    report = audit_context_pruning(target_dir)

    if args.json:
        print(json.dumps(report, indent=2))
        sys.exit(1 if report["status"] == "FAIL" else 0)

    print("===========================================================")
    print("   ✂️ CONTEXT PRUNING & INSTRUCTION PURITY AUDIT")
    print(f"   Target        : {target_dir}")
    print(f"   Status        : {report['status']}")
    print(f"   Files Audited : {report['files_analyzed']}")
    print("===========================================================")
    print(f" 1. Zero-Signal Platitudes : {report['platitudes_count']} found")
    print(f" 2. Cross-File Duplicates  : {report['redundancies_count']} found")
    print(f" 3. Rule Contradictions    : {report['contradictions_count']} found")
    print(f" 4. Prunable Token Budget  : ~{report['estimated_prunable_tokens']:,} tokens can be safely trimmed")
    print("-----------------------------------------------------------")

    if report["contradictions"]:
        for c in report["contradictions"]:
            print(f" ❌ CONTRADICTION [{c['id']}]: {c['description']}")
            print(f"    Side A : {c['side_a']['file']}:{c['side_a']['line']} ({c['side_a']['text']})")
            print(f"    Side B : {c['side_b']['file']}:{c['side_b']['line']} ({c['side_b']['text']})")
            print()

    if report["platitudes"]:
        print(f" 💡 Found {report['platitudes_count']} generic platitudes to prune:")
        for p in report["platitudes"][:5]:  # Preview first 5
            print(f"    - [{p['name']}:{p['line']}] '{p['matched']}': {p['reason']}")
        if len(report["platitudes"]) > 5:
            print(f"    ... and {len(report['platitudes']) - 5} more.")

    if report["redundancies"]:
        print(f"\n 🔁 Found {report['redundancies_count']} duplicate directives across files (keep in SSOT):")
        for r in report["redundancies"][:5]:  # Preview first 5
            print(f"    - Duplicate in '{r['duplicate_file']}:{r['duplicate_line']}' -> Canonical in '{r['canonical_file']}:{r['canonical_line']}' ({int(r['similarity'] * 100)}% match)")
        if len(report["redundancies"]) > 5:
            print(f"    ... and {len(report['redundancies']) - 5} more.")

    print("===========================================================")

    if args.report:
        generate_pruning_report(report, Path(args.report).resolve())

    sys.exit(1 if report["status"] == "FAIL" else 0)


if __name__ == "__main__":
    main()
