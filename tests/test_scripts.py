"""
test_scripts.py - Unit test suite for all 8 Android specialized development helper scripts.
Validates CLI argument parsers, core algorithmic logic, XML parsers, and JSON output schemas.
"""

import importlib.util
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent


def load_module(module_name: str, file_path: Path):
    """Dynamically loads a Python module from file path."""
    spec = importlib.util.spec_from_file_location(module_name, str(file_path))
    mod = importlib.util.module_from_spec(spec)
    sys.modules[module_name] = mod
    spec.loader.exec_module(mod)
    return mod


class TestHelperScripts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if (PROJECT_ROOT / "skills").is_dir():
            skills_dir = PROJECT_ROOT / "skills"
        elif (PROJECT_ROOT / ".gemini" / "antigravity" / "skills").is_dir():
            skills_dir = PROJECT_ROOT / ".gemini" / "antigravity" / "skills"
        else:
            skills_dir = PROJECT_ROOT / "skills"
        cls.device_runner = load_module(
            "device_runner", skills_dir / "android-device" / "scripts" / "device_runner.py"
        )
        cls.inspect_layout = load_module(
            "inspect_layout", skills_dir / "android-device" / "scripts" / "inspect_layout.py"
        )
        cls.run_journey = load_module(
            "run_journey", skills_dir / "android-device" / "scripts" / "run_journey.py"
        )
        cls.test_runner = load_module(
            "test_runner", skills_dir / "android-testing" / "scripts" / "test_runner.py"
        )
        cls.generate_test_scaffold = load_module(
            "generate_test_scaffold",
            skills_dir / "android-testing" / "scripts" / "generate_test_scaffold.py",
        )
        cls.lint_triage = load_module(
            "lint_triage", skills_dir / "android-quality" / "scripts" / "lint_triage.py"
        )
        cls.logcat_triage = load_module(
            "logcat_triage", skills_dir / "android-quality" / "scripts" / "logcat_triage.py"
        )
        cls.sanity_check = load_module(
            "sanity_check", skills_dir / "android-quality" / "scripts" / "sanity_check.py"
        )
        cls.crash_listener = load_module(
            "crash_listener",
            skills_dir / "senior-debugging-engineer" / "scripts" / "crash_listener.py",
        )
        cls.openspec_cli = load_module(
            "openspec_cli",
            PROJECT_ROOT / "scripts" / "openspec_cli.py",
        )
        cls.atlassian_bridge = load_module(
            "atlassian_bridge",
            PROJECT_ROOT / "scripts" / "atlassian_bridge.py",
        )
        cls.install_to_project = load_module(
            "install_to_project",
            PROJECT_ROOT / "scripts" / "install_to_project.py",
        )

    # 1. device_runner.py tests
    def test_device_runner_parser_and_env(self):
        parser = self.device_runner.build_parser()
        self.assertIsNotNone(parser)
        help_text = parser.format_help()
        self.assertIn("check-env", help_text)
        self.assertIn("start-emulator", help_text)

        # Test SDK discovery function
        sdk = self.device_runner.find_android_sdk()
        tools = self.device_runner.get_tool_paths(sdk)
        self.assertIsInstance(tools, dict)
        self.assertIn("adb", tools)
        self.assertIn("emulator", tools)

    # 2. inspect_layout.py tests
    def test_inspect_layout_parsing_and_center_calculation(self):
        # Bounds parsing
        b_str = "[100,200][300,600]"
        x1, y1, x2, y2 = self.inspect_layout.parse_bounds(b_str)
        self.assertEqual((x1, y1, x2, y2), (100, 200, 300, 600))

        # Center calculation
        center = self.inspect_layout.calculate_center(x1, y1, x2, y2)
        self.assertEqual(center, [200, 400])

        # XML parsing
        sample_xml = """<?xml version="1.0" encoding="UTF-8"?>
        <hierarchy rotation="0">
          <node bounds="[0,0][1080,2400]" class="android.widget.FrameLayout">
            <node bounds="[50,100][500,200]" class="android.widget.TextView" text="Hello Android" resource-id="com.example:id/title" />
            <node bounds="[50,250][500,350]" class="android.widget.Button" text="Submit" resource-id="com.example:id/btn" clickable="true" />
          </node>
        </hierarchy>"""

        elements = self.inspect_layout.parse_hierarchy_xml(sample_xml)
        self.assertEqual(len(elements), 3)

        # Diffing test
        changed = self.inspect_layout.diff_elements(elements, [])
        self.assertEqual(len(changed), 3)

        same_diff = self.inspect_layout.diff_elements(elements, elements)
        self.assertEqual(len(same_diff), 0)

    # 3. run_journey.py tests
    def test_run_journey_parsing_and_evaluation(self):
        journey_xml = """<journey name="Login Flow">
          <description>Test user login</description>
          <actions>
            <action>Tap the "Username" input</action>
            <action>Type "admin" into the field</action>
            <action>Tap the "Login" button</action>
            <action>Verify that "Dashboard" is visible on the screen</action>
          </actions>
        </journey>"""

        with tempfile.NamedTemporaryFile(mode="w", suffix=".xml", delete=False) as f:
            f.write(journey_xml)
            temp_path = Path(f.name)

        try:
            name, desc, actions = self.run_journey.parse_journey_xml(temp_path)
            self.assertEqual(name, "Login Flow")
            self.assertEqual(len(actions), 4)

            report = self.run_journey.run_journey(temp_path, serial="emulator-5554", dry_run=True)
            self.assertEqual(report["verdict"], "PASSED")
            self.assertEqual(report["total_steps"], 4)
            self.assertEqual(report["passed_steps"], 4)
            self.assertEqual(report["failed_steps"], 0)
        finally:
            if temp_path.is_file():
                temp_path.unlink()

    # 4. test_runner.py tests
    def test_test_runner_junit_parsing_and_ac_mapping(self):
        junit_xml = """<?xml version="1.0" encoding="UTF-8"?>
        <testsuite name="com.example.profile.ProfileViewModelTest" tests="3" skipped="0" failures="0" errors="0" time="0.85">
          <testcase name="ac01_whenInitialLoad_emitsLoadingThenSuccess" classname="com.example.profile.ProfileViewModelTest" time="0.3"/>
          <testcase name="ac02_whenInvalidInput_emitsError" classname="com.example.profile.ProfileViewModelTest" time="0.25"/>
          <testcase name="ac03_whenSavePreferences_updatesState" classname="com.example.profile.ProfileViewModelTest" time="0.3"/>
        </testsuite>"""

        with tempfile.NamedTemporaryFile(mode="w", suffix=".xml", delete=False) as f:
            f.write(junit_xml)
            temp_xml = Path(f.name)

        try:
            parsed = self.test_runner.parse_junit_results([temp_xml])
            self.assertEqual(parsed["total_tests"], 3)
            self.assertEqual(parsed["passed"], 3)
            self.assertEqual(parsed["failed"], 0)
            self.assertTrue(parsed["all_tests_passed"])

            # Verify AC mapping
            self.assertIn("AC-01", parsed["ac_coverage"])
            self.assertIn("AC-02", parsed["ac_coverage"])
            self.assertIn("AC-03", parsed["ac_coverage"])
            self.assertEqual(parsed["ac_coverage"]["AC-01"], "PASSED")
        finally:
            if temp_xml.is_file():
                temp_xml.unlink()

    # 5. generate_test_scaffold.py tests
    def test_generate_test_scaffold_viewmodel_and_screen(self):
        spec_text = """# Component Specification: ProfileViewModel
Package: `com.example.profile`
Dependencies:
- `GetProfileUseCase`
- `UpdatePreferencesUseCase`

Acceptance Criteria:
### AC-01: Initial state emits profile
### AC-02: Username update updates state
"""
        with tempfile.NamedTemporaryFile(mode="w", suffix=".md", delete=False) as f:
            f.write(spec_text)
            temp_spec = Path(f.name)

        try:
            parsed = self.generate_test_scaffold.parse_component_spec(temp_spec)
            self.assertEqual(parsed["comp_name"], "ProfileViewModel")
            self.assertEqual(parsed["package"], "com.example.profile")
            self.assertEqual(len(parsed["acs"]), 2)

            vm_kotlin = self.generate_test_scaffold.generate_viewmodel_test_kotlin(parsed)
            self.assertIn("class ProfileViewModelTest", vm_kotlin)
            self.assertIn("MainDispatcherRule", vm_kotlin)
            self.assertIn("app.cash.turbine.test", vm_kotlin)
            self.assertIn("ac01_", vm_kotlin)
            self.assertIn("ac02_", vm_kotlin)

            screen_kotlin = self.generate_test_scaffold.generate_screen_test_kotlin(parsed)
            self.assertIn("class ProfileScreenTest", screen_kotlin)
            self.assertIn("createComposeRule()", screen_kotlin)
        finally:
            if temp_spec.is_file():
                temp_spec.unlink()

    # 6. lint_triage.py tests
    def test_lint_triage_xml_parsing_and_gating(self):
        lint_xml = """<?xml version="1.0" encoding="UTF-8"?>
        <issues format="6" by="lint 8.2.0">
          <issue id="HardcodedText" severity="Warning" message="Hardcoded string" category="Internationalization" priority="5">
            <location file="app/src/main/res/layout/activity_main.xml" line="12" column="9"/>
          </issue>
          <issue id="InsecureRandom" severity="Fatal" message="Insecure PRNG used" category="Security" priority="9">
            <location file="app/src/main/kotlin/com/example/Auth.kt" line="45" column="15"/>
          </issue>
        </issues>"""

        with tempfile.NamedTemporaryFile(mode="w", suffix=".xml", delete=False) as f:
            f.write(lint_xml)
            temp_xml = Path(f.name)

        try:
            issues = self.lint_triage.parse_android_lint_xml(temp_xml)
            self.assertEqual(len(issues), 2)

            summary = self.lint_triage.triage_issues(issues, max_warnings=1)
            self.assertFalse(summary["lint_clean"])  # Fatal blocks gate
            self.assertEqual(summary["fatal_count"], 1)
            self.assertEqual(summary["warning_count"], 1)

            # Check defect routing
            sec_issue = [i for i in issues if i["id"] == "InsecureRandom"][0]
            self.assertEqual(sec_issue["target_role_for_remediation"], "Software Architect")
        finally:
            if temp_xml.is_file():
                temp_xml.unlink()

    # 7. logcat_triage.py tests
    def test_logcat_triage_crash_and_anr_extraction(self):
        logcat_text = """09-17 15:30:12.123 1024 1024 E AndroidRuntime: FATAL EXCEPTION: main
09-17 15:30:12.123 1024 1024 E AndroidRuntime: Process: com.example.profile, PID: 1024
09-17 15:30:12.123 1024 1024 E AndroidRuntime: java.lang.NullPointerException: Required user profile was null
09-17 15:30:12.123 1024 1024 E AndroidRuntime: \tat com.example.profile.ui.ProfileViewModel.savePreferences(ProfileViewModel.kt:42)
09-17 15:30:12.123 1024 1024 E AndroidRuntime: \tat com.example.profile.ui.ProfileViewModel$save$1.invokeSuspend(ProfileViewModel.kt:38)
09-17 15:30:12.123 1024 1024 E AndroidRuntime: \tat kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
"""
        report = self.logcat_triage.parse_logcat_content(logcat_text, app_package="com.example.profile")
        self.assertTrue(report["crash_detected"])
        self.assertEqual(report["exception_class"], "java.lang.NullPointerException")
        self.assertEqual(report["crashing_thread"], "main")
        self.assertEqual(report["culprit_file"], "ProfileViewModel.kt")
        self.assertEqual(report["culprit_line"], 42)
        self.assertEqual(report["target_role_for_remediation"], "Senior Android Developer")
        self.assertEqual(report["defect_type"], "IMPLEMENTATION_BUG")

    # 8. sanity_check.py tests
    def test_sanity_check_handoff_validation(self):
        clean_handoff = self.sanity_check.generate_sample_dev_handoff("FEAT-001")
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            f.write(json.dumps(clean_handoff))
            temp_json = Path(f.name)

        try:
            is_clean, errors, data = self.sanity_check.validate_dev_handoff_json(temp_json)
            self.assertTrue(is_clean, f"Errors: {errors}")
            self.assertEqual(len(errors), 0)

            # Test invalid payload fails gate
            clean_handoff["lint_and_sanity"]["compilation_clean"] = False
            temp_json.write_text(json.dumps(clean_handoff))
            is_clean_fail, errors_fail, _ = self.sanity_check.validate_dev_handoff_json(temp_json)
            self.assertFalse(is_clean_fail)
            self.assertTrue(any("compilation_clean" in e for e in errors_fail))
        finally:
            if temp_json.is_file():
                temp_json.unlink()

    def test_kilomenos_key_schema_acceptance(self):
        clean_handoff = self.sanity_check.generate_sample_dev_handoff("KILOMENOS-14")
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            f.write(json.dumps(clean_handoff))
            temp_json = Path(f.name)
        try:
            is_clean, errors, data = self.sanity_check.validate_dev_handoff_json(temp_json)
            self.assertTrue(is_clean, f"Errors: {errors}")
            self.assertEqual(data["feature_id"], "KILOMENOS-14")
        finally:
            if temp_json.is_file():
                temp_json.unlink()

    # 9. crash_listener.py tests
    def test_crash_listener_parser(self):
        parser = self.crash_listener.build_parser()
        self.assertIsNotNone(parser)
        help_text = parser.format_help()
        self.assertIn("--live", help_text)
        self.assertIn("--dump", help_text)
        self.assertIn("--file", help_text)
        self.assertIn("--simulate", help_text)
        self.assertIn("--package", help_text)

    def test_crash_listener_simulation_npe(self):
        sim_log = self.crash_listener.generate_simulation("npe", "es.joshluq.kmsafe")
        crashes = self.crash_listener.parse_logcat_text(sim_log, "es.joshluq.kmsafe")
        self.assertEqual(len(crashes), 1)
        crash = crashes[0]
        self.assertEqual(crash.crash_type, "FATAL_EXCEPTION")
        self.assertEqual(crash.exception_class, "java.lang.NullPointerException")
        self.assertEqual(crash.thread_name, "main")
        primary = crash.primary_app_frame
        self.assertIsNotNone(primary)
        self.assertEqual(primary["file"], "ExpensesViewModel.kt")
        self.assertEqual(primary["line"], 42)
        self.assertIn("es.joshluq.kmsafe", primary["class_name"])

    def test_crash_listener_simulation_compose(self):
        sim_log = self.crash_listener.generate_simulation("compose", "es.joshluq.kmsafe")
        crashes = self.crash_listener.parse_logcat_text(sim_log, "es.joshluq.kmsafe")
        self.assertEqual(len(crashes), 1)
        crash = crashes[0]
        self.assertEqual(crash.exception_class, "java.lang.IllegalStateException")
        primary = crash.primary_app_frame
        self.assertIsNotNone(primary)
        self.assertEqual(primary["file"], "OverviewCard.kt")
        self.assertEqual(primary["line"], 64)

    def test_crash_listener_simulation_anr(self):
        sim_log = self.crash_listener.generate_simulation("anr", "es.joshluq.kmsafe")
        crashes = self.crash_listener.parse_logcat_text(sim_log, "es.joshluq.kmsafe")
        self.assertEqual(len(crashes), 1)
        crash = crashes[0]
        self.assertEqual(crash.crash_type, "ANR")
        self.assertEqual(crash.process_name, "es.joshluq.kmsafe")
        self.assertEqual(crash.thread_name, "main")

    def test_crash_listener_defect_ticket_generation(self):
        sim_log = self.crash_listener.generate_simulation("npe", "es.joshluq.kmsafe")
        crashes = self.crash_listener.parse_logcat_text(sim_log, "es.joshluq.kmsafe")
        defect = crashes[0].to_defect_issue()
        self.assertTrue(defect["issue_id"].startswith("ISSUE-CRASH-"))
        self.assertEqual(defect["severity"], "CRITICAL")
        self.assertEqual(defect["target_role_for_remediation"], "Senior Android Developer")
        self.assertEqual(defect["upstream_artifact_ref"], "ExpensesViewModel.kt:42")
        self.assertGreaterEqual(len(defect["reproduction_steps"]), 2)
        self.assertIn("ExpensesViewModel", defect["expected"])
        self.assertIn("NullPointerException", defect["actual"])
        # Validate markdown report generation
        md_report = crashes[0].to_markdown_report()
        self.assertIn("# Incident Diagnostic Report", md_report)
        self.assertIn("ExpensesViewModel.kt:42", md_report)

    # 10. openspec_cli.py tests
    def test_openspec_lifecycle(self):
        change_id = "TEST-UNIT-01"
        try:
            # 1. Propose
            success = self.openspec_cli.cmd_propose(change_id, "Test Unit Feature")
            self.assertTrue(success)
            change_dir = self.openspec_cli.CHANGES_DIR / change_id
            self.assertTrue(change_dir.is_dir())
            self.assertTrue((change_dir / "proposal.md").is_file())
            self.assertTrue((change_dir / "design.md").is_file())
            self.assertTrue((change_dir / "tasks.md").is_file())
            self.assertTrue((change_dir / "specs" / "delta_spec.md").is_file())

            # 2. Validate
            is_valid, errors = self.openspec_cli.cmd_validate(change_id)
            self.assertTrue(is_valid, f"Validation errors: {errors}")

            # 3. Mark tasks as complete
            tasks_content = (change_dir / "tasks.md").read_text(encoding="utf-8")
            (change_dir / "tasks.md").write_text(tasks_content.replace("- [ ]", "- [x]"), encoding="utf-8")

            # 4. Archive
            arch_success = self.openspec_cli.cmd_archive(change_id)
            self.assertTrue(arch_success)
            self.assertTrue((self.openspec_cli.ARCHIVE_DIR / change_id).is_dir())
        finally:
            # Clean up
            import shutil
            shutil.rmtree(self.openspec_cli.CHANGES_DIR / change_id, ignore_errors=True)
            shutil.rmtree(self.openspec_cli.ARCHIVE_DIR / change_id, ignore_errors=True)
            spec_file = self.openspec_cli.SPECS_DIR / f"features_{change_id.lower().replace('-', '_')}.md"
            if spec_file.is_file():
                spec_file.unlink()

    # 11. atlassian_bridge.py tests
    def test_atlassian_bridge_parser(self):
        parser = self.atlassian_bridge.build_parser()
        self.assertIsNotNone(parser)
        help_text = parser.format_help()
        self.assertIn("--url", help_text)
        self.assertIn("--email", help_text)
        self.assertIn("--simulate", help_text)
        self.assertIn("check", help_text)
        self.assertIn("fetch", help_text)
        self.assertIn("comment", help_text)
        self.assertIn("report-crash", help_text)
        self.assertIn("sync-confluence", help_text)

    def test_atlassian_bridge_simulation_operations(self):
        client = self.atlassian_bridge.AtlassianClient(simulate=True)
        self.assertTrue(client.configured)

        # 1. Test connection
        conn = client.test_connection()
        self.assertTrue(conn["success"])
        self.assertTrue(conn["simulated"])

        # 2. Get issue
        issue = client.get_jira_issue("KMSAFE-104")
        self.assertEqual(issue["key"], "KMSAFE-104")
        self.assertIn("Acceptance Criteria", issue["description"])

        # 3. Create issue
        created = client.create_jira_issue("KMSAFE", "New Feature", "Description")
        self.assertTrue(created["key"].startswith("KMSAFE-"))

        # 4. Add comment
        comment = client.add_jira_comment("KMSAFE-104", "Clarification needed on VAT")
        self.assertTrue(comment["simulated"])

        # 5. Publish confluence page
        page = client.publish_confluence_page("DEV", "Living Spec: Expenses", "# Markdown")
        self.assertEqual(page["space"], "DEV")
        self.assertEqual(page["id"], "20001")

    # 12. install_to_project.py Hub & Spoke Profiles tests
    def test_install_to_project_profiles(self):
        skills_src = PROJECT_ROOT / "skills" if (PROJECT_ROOT / "skills").exists() else (PROJECT_ROOT / ".agents" / "skills")
        dummy_skills = ['supabase-db-triage', 'supabase-edge-functions', 'legal-compliance-audit', 'web-lighthouse-seo']
        created_dummies = []
        for s in dummy_skills:
            d = skills_src / s
            if not d.exists():
                d.mkdir(parents=True, exist_ok=True)
                (d / "SKILL.md").write_text(f"---\nname: {s}\n---\nMock skill", encoding="utf-8")
                created_dummies.append(d)

        try:
            with tempfile.TemporaryDirectory() as tmp_dir:
                tmp_root = Path(tmp_dir)

                # Test Backend profile
                backend_target = tmp_root / "backend"
                backend_target.mkdir()
                self.install_to_project.install_ecosystem(backend_target, profile="backend", mode="both")

                skills_installed = [p.name for p in (backend_target / ".gemini" / "antigravity" / "skills").iterdir() if p.is_dir()]
                self.assertIn("supabase-db-triage", skills_installed)
                self.assertIn("supabase-edge-functions", skills_installed)
                self.assertIn("openspec", skills_installed)
                self.assertIn("atlassian-bridge", skills_installed)
                self.assertNotIn("android-staff-engineer-compose", skills_installed)
                self.assertNotIn("legal-compliance-audit", skills_installed)

                # Verify profile config
                config_ex = backend_target / ".atlassian_config.json.example"
                self.assertTrue(config_ex.exists())
                config_json = json.loads(config_ex.read_text(encoding="utf-8"))
                self.assertEqual(config_json["board_id"], "1")
                self.assertEqual(config_json["label"], "backend")

                # Test Web profile
                web_target = tmp_root / "web"
                web_target.mkdir()
                self.install_to_project.install_ecosystem(web_target, profile="web", mode="both")

                web_skills = [p.name for p in (web_target / ".gemini" / "antigravity" / "skills").iterdir() if p.is_dir()]
                self.assertIn("legal-compliance-audit", web_skills)
                self.assertIn("web-lighthouse-seo", web_skills)
                self.assertNotIn("supabase-db-triage", web_skills)
                self.assertNotIn("android-device", web_skills)
        finally:
            import shutil
            for d in created_dummies:
                shutil.rmtree(d, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()

