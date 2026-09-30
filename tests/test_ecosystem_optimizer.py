"""
test_ecosystem_optimizer.py - Comprehensive Unit Test Suite for Ecosystem Optimizer.

Tests context budgeting, token estimation, cross-platform leakage detection,
and hub-to-spoke synchronization audit mechanics.
"""

import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SKILL_SCRIPTS = PROJECT_ROOT / "skills" / "ecosystem-optimizer" / "scripts"
if str(SKILL_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SKILL_SCRIPTS))

from audit_context_budget import (
    estimate_tokens,
    analyze_file,
    audit_context_budget,
    BUDGET_THRESHOLDS,
)
from lint_platform_leakage import (
    detect_target_profile,
    lint_file_for_profile,
    lint_platform_leakage,
)
from validate_spokes_sync import (
    file_sha256,
    check_spoke_sync,
)
from audit_context_pruning import (
    scan_file_for_platitudes,
    detect_cross_file_redundancies,
    detect_contradictions,
    audit_context_pruning,
    is_negative_constraint,
    extract_directives,
)
from ecosystem_audit import (
    audit_schemas,
    run_full_ecosystem_audit,
    generate_markdown_report,
)


class TestContextBudgetAuditor(unittest.TestCase):
    def test_estimate_tokens_heuristic(self):
        sample_text = "This is a short sentence containing ten words for token estimation."
        tokens = estimate_tokens(sample_text)
        self.assertGreater(tokens, 5)
        self.assertLess(tokens, 30)

    def test_analyze_file_pass_and_warn_limits(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            small_file = tmp_path / "small.md"
            small_file.write_text("Small document with just a few words.", encoding="utf-8")

            res_small = analyze_file(small_file, "skill_md")
            self.assertEqual(res_small["status"], "PASS")

            large_file = tmp_path / "large.md"
            # Write 1300 words to trigger WARN for skill_md (threshold 1200)
            large_file.write_text("word " * 1300, encoding="utf-8")
            res_large = analyze_file(large_file, "skill_md")
            self.assertEqual(res_large["status"], "WARN")

            huge_file = tmp_path / "huge.md"
            # Write 2600 words to trigger FAIL for skill_md (threshold 2500)
            huge_file.write_text("word " * 2600, encoding="utf-8")
            res_huge = analyze_file(huge_file, "skill_md")
            self.assertEqual(res_huge["status"], "FAIL")

    def test_audit_context_budget_on_project(self):
        report = audit_context_budget(PROJECT_ROOT)
        self.assertIn("overall_status", report)
        self.assertIn(report["overall_status"], ["PASS", "WARN"])
        self.assertGreater(report["files_analyzed"], 0)
        self.assertGreater(report["total_words"], 0)


class TestPlatformLeakageLinter(unittest.TestCase):
    def test_detect_target_profile(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)

            (tmp_path / "settings.gradle.kts").write_text("", encoding="utf-8")
            self.assertEqual(detect_target_profile(tmp_path), "android")

            (tmp_path / "settings.gradle.kts").unlink()
            (tmp_path / "supabase").mkdir()
            self.assertEqual(detect_target_profile(tmp_path), "backend")

    def test_leak_detection_backend_with_android_terms(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            dirty_file = tmp_path / "dirty_backend.md"
            dirty_file.write_text("This backend service should use ScreenViewModel and UiState for data.", encoding="utf-8")

            violations = lint_file_for_profile(dirty_file, "backend")
            self.assertGreaterEqual(len(violations), 2)
            matched_terms = [v["matched"] for v in violations]
            self.assertIn("ScreenViewModel", matched_terms)
            self.assertIn("UiState", matched_terms)

    def test_leak_detection_android_with_backend_terms(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            dirty_file = tmp_path / "dirty_android.md"
            dirty_file.write_text("The mobile client must enable Row Level Security on auth.uid()", encoding="utf-8")

            violations = lint_file_for_profile(dirty_file, "android")
            self.assertGreaterEqual(len(violations), 2)
            matched_terms = [v["matched"] for v in violations]
            self.assertIn("Row Level Security", matched_terms)
            self.assertIn("auth.uid()", matched_terms)

    def test_solutions_architect_exemption(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            exempt_file = tmp_path / "exempt.md"
            exempt_file.write_text(
                "## 2. Role 2: Solutions Architect\n"
                "Coordinates FCM push between Supabase and Android Compose app.\n",
                encoding="utf-8"
            )
            violations = lint_file_for_profile(exempt_file, "android")
            self.assertEqual(len(violations), 0)


class TestSpokeSyncValidator(unittest.TestCase):
    def test_file_sha256_line_ending_neutrality(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            p_crlf = Path(tmp_dir) / "crlf.txt"
            p_lf = Path(tmp_dir) / "lf.txt"
            p_crlf.write_bytes(b"line1\r\nline2\r\n")
            p_lf.write_bytes(b"line1\nline2\n")

            self.assertEqual(file_sha256(p_crlf), file_sha256(p_lf))

    def test_check_spoke_sync_matching_and_drift(self):
        with tempfile.TemporaryDirectory() as tmp_hub, tempfile.TemporaryDirectory() as tmp_spoke:
            hub_dir = Path(tmp_hub)
            spoke_dir = Path(tmp_spoke)

            (hub_dir / "schemas").mkdir()
            (spoke_dir / "schemas").mkdir()

            schema_content = '{"$schema": "http://json-schema.org/draft-07/schema#"}'
            (hub_dir / "schemas" / "po_to_architect_handoff.schema.json").write_text(schema_content, encoding="utf-8")
            (spoke_dir / "schemas" / "po_to_architect_handoff.schema.json").write_text(schema_content, encoding="utf-8")

            report = check_spoke_sync(hub_dir, spoke_dir)
            self.assertGreaterEqual(report["in_sync_count"], 1)

            # Introduce drift
            (spoke_dir / "schemas" / "po_to_architect_handoff.schema.json").write_text('{"drifted": true}', encoding="utf-8")
            report_drift = check_spoke_sync(hub_dir, spoke_dir)
            self.assertGreaterEqual(report_drift["drifted_count"], 1)


class TestMasterEcosystemAudit(unittest.TestCase):
    def test_audit_schemas_hub(self):
        res = audit_schemas(PROJECT_ROOT)
        self.assertEqual(res["status"], "PASS")
        self.assertGreaterEqual(res["passed"], 6)

    def test_run_full_ecosystem_audit(self):
        report = run_full_ecosystem_audit(PROJECT_ROOT, profile="auto", check_spokes=False)
        self.assertIn(report["overall_status"], ["PASS", "WARN"])
        self.assertEqual(report["leakage"]["status"], "PASS")

    def test_generate_markdown_report(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            out_file = Path(tmp_dir) / "report.md"
            report_data = {
                "timestamp": "2026-09-30T12:00:00Z",
                "target_dir": "/mock/target",
                "profile": "android",
                "overall_status": "PASS",
                "budget": {
                    "overall_status": "PASS",
                    "files_analyzed": 5,
                    "total_words": 1000,
                    "total_estimated_tokens": 1330,
                    "recommendations": ["Keep up the good work!"]
                },
                "leakage": {
                    "status": "PASS",
                    "violations_count": 0,
                    "violations": []
                },
                "schemas": {
                    "status": "PASS",
                    "schemas_checked": 6,
                    "passed": 6
                },
                "spokes": []
            }
            generate_markdown_report(report_data, out_file)
            self.assertTrue(out_file.is_file())
            content = out_file.read_text(encoding="utf-8")
            self.assertIn("Ecosystem Health & Optimization Report", content)
            self.assertIn("🟢 **PASS**", content)


class TestContextPruningAuditor(unittest.TestCase):
    def test_scan_file_for_platitudes(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir) / "fluffy.md"
            tmp_path.write_text(
                "# Guidelines\n"
                "- Write clean, readable code at all times.\n"
                "- Remember to import necessary libraries.\n"
                "- Use clear and descriptive variable names.\n"
                "- Strictly enforce Min SDK 24.\n",
                encoding="utf-8"
            )
            findings = scan_file_for_platitudes(tmp_path)
            self.assertEqual(len(findings), 3)
            matched = [f["matched"] for f in findings]
            self.assertTrue(any("clean" in m for m in matched))
            self.assertTrue(any("import" in m for m in matched))
            self.assertTrue(any("names" in m for m in matched))

    def test_detect_cross_file_redundancies(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_dir_path = Path(tmp_dir)
            f1 = tmp_dir_path / "file1.md"
            f2 = tmp_dir_path / "file2.md"
            f1.write_text(
                "- Strictly prohibit adding unrequested endpoints or hidden parameters to prevent scope creep.\n",
                encoding="utf-8"
            )
            f2.write_text(
                "- Strictly prohibit adding unrequested endpoints and hidden parameters to avoid scope creep.\n",
                encoding="utf-8"
            )

            d1 = extract_directives(f1, tmp_dir_path)
            d2 = extract_directives(f2, tmp_dir_path)
            directives_map = {f1: d1, f2: d2}

            redundancies = detect_cross_file_redundancies(directives_map)
            self.assertEqual(len(redundancies), 1)
            self.assertGreaterEqual(redundancies[0]["similarity"], 0.75)

    def test_detect_contradictions_autonomy_vs_halt(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_dir_path = Path(tmp_dir)
            f1 = tmp_dir_path / "auto.md"
            f2 = tmp_dir_path / "halt.md"
            f1.write_text("- Execute fully autonomously without asking any questions.\n", encoding="utf-8")
            f2.write_text("- Mandatory halt gate activated: stop immediately and prompt the developer.\n", encoding="utf-8")

            contradictions = detect_contradictions([f1, f2], tmp_dir_path)
            self.assertEqual(len(contradictions), 1)
            self.assertEqual(contradictions[0]["id"], "AUTONOMY_VS_HALT")

    def test_negative_constraint_does_not_false_alarm(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_dir_path = Path(tmp_dir)
            f1 = tmp_dir_path / "arch.md"
            f1.write_text(
                "- Immutable UI state models must be used exclusively.\n"
                "- NEVER allow mutable UI state in component contracts.\n",
                encoding="utf-8"
            )
            contradictions = detect_contradictions([f1], tmp_dir_path)
            self.assertEqual(len(contradictions), 0)

    def test_audit_context_pruning_on_project(self):
        report = audit_context_pruning(PROJECT_ROOT)
        self.assertIn("status", report)
        self.assertIn(report["status"], ["PASS", "WARN"])
        self.assertGreater(report["files_analyzed"], 0)


if __name__ == "__main__":
    unittest.main()
