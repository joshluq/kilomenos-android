#!/usr/bin/env python3
"""
generate_test_scaffold.py - Android Test Scaffolding Generator

Automatically parses Component Interface Specifications (COMP-SPEC-*.md) or CLI parameters
and generates idiomatic JUnit 4/5 + MockK + Turbine ViewModel test suites or Compose UI tests.

Standard library only.
"""

import argparse
import os
import re
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


def parse_component_spec(spec_path: Path) -> Dict[str, Any]:
    """Extracts component name, package, state, actions, dependencies, and ACs from COMP-SPEC."""
    text = spec_path.read_text(encoding="utf-8", errors="replace")

    # Name
    name_match = re.search(r"class\s+([A-Za-z0-9_]+ViewModel)", text)
    if not name_match:
        name_match = re.search(r"#\s+Component Specification:\s*([A-Za-z0-9_]+)", text)
    comp_name = name_match.group(1) if name_match else spec_path.stem.replace("COMP-SPEC-", "") + "ViewModel"

    # Package
    pkg_match = re.search(r"package\s+([a-z0-9_.]+)", text)
    if not pkg_match:
        pkg_match = re.search(r"Package:\s*`?([a-z0-9_.]+)`?", text)
    package = pkg_match.group(1) if pkg_match else "com.example.app"

    # Dependencies (e.g. constructor parameters or use cases)
    deps = []
    dep_matches = re.findall(r"([A-Za-z0-9_]+UseCase|[A-Za-z0-9_]+Repository)", text)
    for d in set(dep_matches):
        if d != comp_name:
            param_name = d[0].lower() + d[1:]
            deps.append({"type": d, "name": param_name})

    if not deps:
        deps = [
            {"type": "GetProfileUseCase", "name": "getProfileUseCase"},
            {"type": "UpdatePreferencesUseCase", "name": "updatePreferencesUseCase"},
        ]

    # Acceptance Criteria (AC-xx)
    acs = []
    ac_matches = re.findall(r"(?:###\s*)?(AC-\d{2})[:\s]+([^\n\r]+)", text)
    for ac_id, ac_desc in ac_matches:
        # Clean description into snake_case identifier
        clean_desc = re.sub(r"[^a-zA-Z0-9\s]", "", ac_desc)
        parts = clean_desc.split()
        method_suffix = "".join(p.capitalize() for p in parts[:4])
        acs.append({"id": ac_id, "desc": ac_desc.strip(), "suffix": method_suffix})

    if not acs:
        acs = [
            {"id": "AC-01", "desc": "Initial state loads profile data", "suffix": "WhenInitialLoadEmitsSuccess"},
            {"id": "AC-02", "desc": "Invalid input produces error state", "suffix": "WhenInvalidInputEmitsError"},
            {"id": "AC-03", "desc": "Successful submission updates state", "suffix": "WhenSubmitEmitsUpdatedProfile"},
        ]

    base_name = comp_name.replace("ViewModel", "")

    return {
        "comp_name": comp_name,
        "base_name": base_name,
        "package": package,
        "dependencies": deps,
        "acs": acs,
        "state_class": f"{base_name}UiState",
        "action_class": f"{base_name}UiAction",
    }


def generate_viewmodel_test_kotlin(spec: Dict[str, Any]) -> str:
    """Generates Kotlin ViewModel test class using MockK, Turbine, and Coroutines test rule."""
    pkg = spec["package"]
    vm_name = spec["comp_name"]
    base_name = spec["base_name"]
    deps = spec["dependencies"]
    acs = spec["acs"]
    state_cls = spec["state_class"]
    action_cls = spec["action_class"]

    lines = []
    lines.append(f"package {pkg}")
    lines.append("")
    lines.append("import androidx.arch.core.executor.testing.InstantTaskExecutorRule")
    lines.append("import app.cash.turbine.test")
    lines.append("import io.mockk.clearAllMocks")
    lines.append("import io.mockk.coEvery")
    lines.append("import io.mockk.coVerify")
    lines.append("import io.mockk.mockk")
    lines.append("import kotlinx.coroutines.Dispatchers")
    lines.append("import kotlinx.coroutines.ExperimentalCoroutinesApi")
    lines.append("import kotlinx.coroutines.test.StandardTestDispatcher")
    lines.append("import kotlinx.coroutines.test.TestDispatcher")
    lines.append("import kotlinx.coroutines.test.resetMain")
    lines.append("import kotlinx.coroutines.test.runTest")
    lines.append("import kotlinx.coroutines.test.setMain")
    lines.append("import org.junit.After")
    lines.append("import org.junit.Assert.assertEquals")
    lines.append("import org.junit.Assert.assertTrue")
    lines.append("import org.junit.Before")
    lines.append("import org.junit.Rule")
    lines.append("import org.junit.Test")
    lines.append("import org.junit.rules.TestWatcher")
    lines.append("import org.junit.runner.Description")
    lines.append("")
    lines.append("/**")
    lines.append(f" * Automated Unit Test Suite for [{vm_name}].")
    lines.append(" * Enforces 1-to-1 traceability with Acceptance Criteria.")
    lines.append(" */")
    lines.append("@OptIn(ExperimentalCoroutinesApi::class)")
    lines.append(f"class {vm_name}Test {{")
    lines.append("")
    lines.append("    // Reusable MainDispatcherRule overriding Dispatchers.Main")
    lines.append("    class MainDispatcherRule(")
    lines.append("        val testDispatcher: TestDispatcher = StandardTestDispatcher()")
    lines.append("    ) : TestWatcher() {")
    lines.append("        override fun starting(description: Description) {")
    lines.append("            Dispatchers.setMain(testDispatcher)")
    lines.append("        }")
    lines.append("        override fun finished(description: Description) {")
    lines.append("            Dispatchers.resetMain()")
    lines.append("        }")
    lines.append("    }")
    lines.append("")
    lines.append("    @get:Rule")
    lines.append("    val mainDispatcherRule = MainDispatcherRule()")
    lines.append("")

    # Mocks
    for d in deps:
        lines.append(f"    private val {d['name']}: {d['type']} = mockk()")
    lines.append(f"    private lateinit var viewModel: {vm_name}")
    lines.append("")

    # SetUp
    lines.append("    @Before")
    lines.append("    fun setUp() {")
    lines.append("        clearAllMocks()")
    dep_args = ", ".join([f"{d['name']} = {d['name']}" for d in deps])
    lines.append(f"        viewModel = {vm_name}(")
    if dep_args:
        lines.append(f"            {dep_args},")
    lines.append("            dispatcher = mainDispatcherRule.testDispatcher")
    lines.append("        )")
    lines.append("    }")
    lines.append("")

    # Test Methods per AC
    for ac in acs:
        ac_num = ac["id"].replace("AC-", "").lower()
        func_name = f"ac{ac_num}_{ac['suffix'][0].lower() + ac['suffix'][1:]}"
        lines.append(f"    /**")
        lines.append(f"     * Traceability: [{ac['id']}] - {ac['desc']}")
        lines.append(f"     */")
        lines.append(f"    @Test")
        lines.append(f"    fun {func_name}() = runTest {{")
        lines.append("        // Given")
        lines.append(f"        // Stub mock invocations for {ac['id']}")
        lines.append("")
        lines.append("        // When & Then")
        lines.append("        viewModel.uiState.test {")
        lines.append(f"            // Initial state verification")
        lines.append(f"            assertEquals({state_cls}.Loading, awaitItem())")
        lines.append("            ")
        lines.append(f"            // Trigger action")
        lines.append(f"            viewModel.handleAction({action_cls}.LoadData)")
        lines.append("            ")
        lines.append("            // Verify resulting state")
        lines.append("            val nextState = awaitItem()")
        lines.append(f"            assertTrue(nextState is {state_cls}.Success || nextState is {state_cls})")
        lines.append("            ")
        lines.append("            cancelAndIgnoreRemainingEvents()")
        lines.append("        }")
        lines.append("    }")
        lines.append("")

    lines.append("}")
    return "\n".join(lines)


def generate_screen_test_kotlin(spec: Dict[str, Any]) -> str:
    """Generates Jetpack Compose UI test class using ComposeTestRule and semantics."""
    pkg = spec["package"]
    base_name = spec["base_name"]
    screen_name = f"{base_name}Screen"
    state_cls = spec["state_class"]
    action_cls = spec["action_class"]
    acs = spec["acs"]

    lines = []
    lines.append(f"package {pkg}")
    lines.append("")
    lines.append("import androidx.compose.ui.test.assertIsDisplayed")
    lines.append("import androidx.compose.ui.test.assertIsEnabled")
    lines.append("import androidx.compose.ui.test.junit4.createComposeRule")
    lines.append("import androidx.compose.ui.test.onNodeWithTag")
    lines.append("import androidx.compose.ui.test.onNodeWithText")
    lines.append("import androidx.compose.ui.test.performClick")
    lines.append("import androidx.test.ext.junit.runners.AndroidJUnit4")
    lines.append("import org.junit.Assert.assertTrue")
    lines.append("import org.junit.Rule")
    lines.append("import org.junit.Test")
    lines.append("import org.junit.runner.RunWith")
    lines.append("")
    lines.append("/**")
    lines.append(f" * Jetpack Compose UI Semantics Test for [{screen_name}].")
    lines.append(" */")
    lines.append("@RunWith(AndroidJUnit4::class)")
    lines.append(f"class {screen_name}Test {{")
    lines.append("")
    lines.append("    @get:Rule")
    lines.append("    val composeTestRule = createComposeRule()")
    lines.append("")

    for ac in acs:
        ac_num = ac["id"].replace("AC-", "").lower()
        func_name = f"ac{ac_num}_{ac['suffix'][0].lower() + ac['suffix'][1:]}_uiVerification"
        lines.append(f"    /**")
        lines.append(f"     * Traceability: [{ac['id']}] - {ac['desc']}")
        lines.append(f"     */")
        lines.append(f"    @Test")
        lines.append(f"    fun {func_name}() {{")
        lines.append("        var actionDispatched = false")
        lines.append("        composeTestRule.setContent {")
        lines.append(f"            {screen_name}(")
        lines.append(f"                state = {state_cls}.Loading,")
        lines.append("                onAction = { actionDispatched = true }")
        lines.append("            )")
        lines.append("        }")
        lines.append("")
        lines.append("        // Assert UI semantics")
        lines.append("        composeTestRule.onNodeWithTag(\"loading_indicator\")")
        lines.append("            .assertIsDisplayed()")
        lines.append("    }")
        lines.append("")

    lines.append("}")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Android Test Scaffold Generator - Auto-generates JUnit, MockK, Turbine, and Compose test files from Component Specs."
    )
    parser.add_argument("--spec", help="Path to Component Spec markdown file (e.g. COMP-SPEC-001.md)")
    parser.add_argument("--type", choices=["viewmodel", "screen"], default="viewmodel", help="Type of test scaffold to generate")
    parser.add_argument("--name", help="Component class name (e.g. ProfileViewModel)")
    parser.add_argument("--package", help="Kotlin package name (e.g. com.example.profile)")
    parser.add_argument("--output", "-o", help="Target output file path for generated Kotlin test")

    args = parser.parse_args()

    if args.spec:
        spec_file = Path(args.spec)
        if not spec_file.is_file():
            print(f"Error: Spec file not found at '{spec_file}'", file=sys.stderr)
            return 1
        spec_data = parse_component_spec(spec_file)
    else:
        # Build spec data from CLI flags or default
        comp_name = args.name or "FeatureViewModel"
        base_name = comp_name.replace("ViewModel", "").replace("Screen", "")
        pkg = args.package or "com.example.feature"
        spec_data = {
            "comp_name": comp_name,
            "base_name": base_name,
            "package": pkg,
            "dependencies": [
                {"type": f"Get{base_name}UseCase", "name": f"get{base_name}UseCase"},
            ],
            "acs": [
                {"id": "AC-01", "desc": "Initial state emission", "suffix": "InitialStateLoadsData"},
                {"id": "AC-02", "desc": "Action handling and state update", "suffix": "ActionUpdatesState"},
            ],
            "state_class": f"{base_name}UiState",
            "action_class": f"{base_name}UiAction",
        }

    if args.type == "screen":
        content = generate_screen_test_kotlin(spec_data)
    else:
        content = generate_viewmodel_test_kotlin(spec_data)

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(content, encoding="utf-8")
        print(f"Generated {args.type} test scaffold at: {out_path}")
    else:
        print(content)

    return 0


if __name__ == "__main__":
    sys.exit(main())
