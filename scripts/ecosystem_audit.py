#!/usr/bin/env python3
"""
scripts/ecosystem_audit.py - Direct entry point for Ecosystem Health & Optimization Audit.

Delegates execution to the installed ecosystem-optimizer skill.
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

candidates = [
    ROOT / "skills" / "ecosystem-optimizer" / "scripts" / "ecosystem_audit.py",
    ROOT / ".agents" / "skills" / "ecosystem-optimizer" / "scripts" / "ecosystem_audit.py",
    ROOT / ".gemini" / "antigravity" / "skills" / "ecosystem-optimizer" / "scripts" / "ecosystem_audit.py",
]

skill_script = None
for c in candidates:
    if c.is_file():
        skill_script = c
        break

if not skill_script:
    print("[ERROR] ecosystem_audit.py could not be found in skills/, .agents/skills/ or .gemini/antigravity/skills/")
    sys.exit(1)

if str(skill_script.parent) not in sys.path:
    sys.path.insert(0, str(skill_script.parent))

try:
    from ecosystem_audit import *
    if __name__ == "__main__":
        main()
except ImportError as err:
    print(f"[ERROR] Could not load ecosystem_audit: {err}")
    sys.exit(1)
