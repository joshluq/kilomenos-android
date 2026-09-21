"""
test_skills.py - Validation test suite for Antigravity skills specifications and structure.
Verifies YAML frontmatter integrity, naming compliance, and helper script placement.
"""

import os
import re
import unittest
from pathlib import Path


class TestSkillsSpecification(unittest.TestCase):
    def setUp(self):
        self.project_root = Path(__file__).resolve().parent.parent
        if (self.project_root / "skills").is_dir():
            self.skills_dir = self.project_root / "skills"
        elif (self.project_root / ".gemini" / "antigravity" / "skills").is_dir():
            self.skills_dir = self.project_root / ".gemini" / "antigravity" / "skills"
        else:
            self.skills_dir = self.project_root / "skills"

    def test_all_expected_skills_exist(self):
        expected_skills = [
            "android-device",
            "android-quality",
            "android-testing",
            "android-staff-engineer-compose",
            "fix-bug",
            "idea-lab",
            "new-feature",
            "performance-optimization-tips",
            "po-digital-experience-fintech",
            "senior-debugging-engineer",
            "openspec",
        ]
        for skill in expected_skills:
            skill_folder = self.skills_dir / skill
            self.assertTrue(
                skill_folder.is_dir(),
                f"Skill directory missing: {skill_folder}",
            )
            skill_md = skill_folder / "SKILL.md"
            self.assertTrue(
                skill_md.is_file(),
                f"SKILL.md missing in {skill_folder}",
            )

    def test_skill_manifest_frontmatter_integrity(self):
        expected_skills = [
            "android-device",
            "android-quality",
            "android-testing",
            "android-staff-engineer-compose",
            "fix-bug",
            "idea-lab",
            "new-feature",
            "performance-optimization-tips",
            "po-digital-experience-fintech",
            "senior-debugging-engineer",
            "openspec",
        ]

        for skill_name in expected_skills:
            skill_md = self.skills_dir / skill_name / "SKILL.md"
            content = skill_md.read_text(encoding="utf-8")

            # Must start with frontmatter block
            self.assertTrue(
                content.startswith("---"),
                f"{skill_name}/SKILL.md does not start with frontmatter delimiter '---'",
            )

            # Match frontmatter YAML
            frontmatter_match = re.match(r"^---\r?\n(.*?)\r?\n---", content, re.DOTALL)
            self.assertIsNotNone(
                frontmatter_match,
                f"Frontmatter block not closed properly with '---' in {skill_name}/SKILL.md",
            )
            frontmatter_text = frontmatter_match.group(1)

            # Extract name
            name_match = re.search(r"^name:\s*(\S+)", frontmatter_text, re.MULTILINE)
            self.assertIsNotNone(name_match, f"Missing 'name' in frontmatter of {skill_name}")
            declared_name = name_match.group(1).strip()
            self.assertEqual(
                declared_name,
                skill_name,
                f"Frontmatter name '{declared_name}' does not match directory '{skill_name}'",
            )

            # Extract description
            desc_match = re.search(r"^description:\s*(.+)$", frontmatter_text, re.MULTILINE)
            self.assertIsNotNone(desc_match, f"Missing 'description' in frontmatter of {skill_name}")
            description = desc_match.group(1).strip()
            self.assertGreaterEqual(
                len(description),
                20,
                f"Description in {skill_name}/SKILL.md is too short (< 20 characters): '{description}'",
            )

    def test_all_eight_helper_scripts_exist(self):
        expected_scripts = [
            ("android-device", "device_runner.py"),
            ("android-device", "inspect_layout.py"),
            ("android-device", "run_journey.py"),
            ("android-testing", "test_runner.py"),
            ("android-testing", "generate_test_scaffold.py"),
            ("android-quality", "lint_triage.py"),
            ("android-quality", "logcat_triage.py"),
            ("android-quality", "sanity_check.py"),
            ("senior-debugging-engineer", "crash_listener.py"),
        ]

        for skill_name, script_name in expected_scripts:
            script_path = self.skills_dir / skill_name / "scripts" / script_name
            self.assertTrue(
                script_path.is_file(),
                f"Helper script missing: {script_path}",
            )


if __name__ == "__main__":
    unittest.main()
