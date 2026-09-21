#!/usr/bin/env python3
"""
Adversarial Stress Test Suite for Milestone 1 Contract Schemas.
Role: Challenger (critic / empirical specialist)

Tests:
1. Valid baseline payloads for all 4 schemas
2. Missing required properties (root and nested)
3. Boundary value violations (min_sdk, target_sdk, coverage_percentage, line_count, total_tests, minLength, minItems)
4. Invalid enum values and const violations
5. Malformed pattern / regex violations
6. Type corruption (strings for ints, ints for booleans, objects for arrays, malformed timestamps)
7. Additional property injection (root and nested objects)
8. Cross-field / semantic integrity analysis (e.g. verdict=PASS with blocker issues, AC verified=false with PASS)
"""

import os
import sys
import json
import re
from datetime import datetime

# ==============================================================================
# 1. Zero-Dependency Draft-07 Validator
# ==============================================================================

class ValidationError(Exception):
    def __init__(self, message, path="root"):
        super().__init__(f"{path}: {message}")
        self.path = path
        self.message = message

def validate_iso8601_datetime(value):
    if not isinstance(value, str):
        return False
    # ISO-8601 regex pattern
    pattern = r'^\d{4}-\d{2}-\d{2}[Tt]\d{2}:\d{2}:\d{2}(\.\d+)?([Zz]|([+-]\d{2}:\d{2}))?$'
    if not re.match(pattern, value):
        return False
    try:
        # Basic calendar sanity check
        base_part = value[:19].replace('T', ' ').replace('t', ' ')
        datetime.strptime(base_part, "%Y-%m-%d %H:%M:%S")
        return True
    except Exception:
        return False

def validate_schema(instance, schema, path="root"):
    """Validates an instance against a JSON Schema (Draft-07)."""
    if not isinstance(schema, dict):
        return

    # Check type
    if "type" in schema:
        expected_type = schema["type"]
        if expected_type == "object":
            if not isinstance(instance, dict):
                raise ValidationError(f"expected object, got {type(instance).__name__}", path)
        elif expected_type == "array":
            if not isinstance(instance, list):
                raise ValidationError(f"expected array, got {type(instance).__name__}", path)
        elif expected_type == "string":
            if not isinstance(instance, str):
                raise ValidationError(f"expected string, got {type(instance).__name__}", path)
        elif expected_type == "integer":
            if not isinstance(instance, int) or isinstance(instance, bool):
                raise ValidationError(f"expected integer, got {type(instance).__name__}", path)
        elif expected_type == "number":
            if not isinstance(instance, (int, float)) or isinstance(instance, bool):
                raise ValidationError(f"expected number, got {type(instance).__name__}", path)
        elif expected_type == "boolean":
            if not isinstance(instance, bool):
                raise ValidationError(f"expected boolean, got {type(instance).__name__}", path)
        elif expected_type == "null":
            if instance is not None:
                raise ValidationError(f"expected null, got {type(instance).__name__}", path)

    # Check const
    if "const" in schema:
        if instance != schema["const"]:
            raise ValidationError(f"value {repr(instance)} does not match const {repr(schema['const'])}", path)

    # Check enum
    if "enum" in schema:
        if instance not in schema["enum"]:
            raise ValidationError(f"value {repr(instance)} not in enum {schema['enum']}", path)

    # Check string constraints
    if isinstance(instance, str):
        if "minLength" in schema and len(instance) < schema["minLength"]:
            raise ValidationError(f"string length {len(instance)} < minLength {schema['minLength']}", path)
        if "maxLength" in schema and len(instance) > schema["maxLength"]:
            raise ValidationError(f"string length {len(instance)} > maxLength {schema['maxLength']}", path)
        if "pattern" in schema:
            if not re.search(schema["pattern"], instance):
                raise ValidationError(f"string '{instance}' does not match pattern '{schema['pattern']}'", path)
        if schema.get("format") == "date-time":
            if not validate_iso8601_datetime(instance):
                raise ValidationError(f"string '{instance}' is not a valid ISO-8601 date-time", path)

    # Check number constraints
    if isinstance(instance, (int, float)) and not isinstance(instance, bool):
        if "minimum" in schema and instance < schema["minimum"]:
            raise ValidationError(f"value {instance} < minimum {schema['minimum']}", path)
        if "maximum" in schema and instance > schema["maximum"]:
            raise ValidationError(f"value {instance} > maximum {schema['maximum']}", path)

    # Check array constraints
    if isinstance(instance, list):
        if "minItems" in schema and len(instance) < schema["minItems"]:
            raise ValidationError(f"array items count {len(instance)} < minItems {schema['minItems']}", path)
        if "maxItems" in schema and len(instance) > schema["maxItems"]:
            raise ValidationError(f"array items count {len(instance)} > maxItems {schema['maxItems']}", path)
        if "items" in schema:
            item_schema = schema["items"]
            for idx, item in enumerate(instance):
                validate_schema(item, item_schema, f"{path}[{idx}]")

    # Check object constraints
    if isinstance(instance, dict):
        # required fields
        if "required" in schema:
            for req in schema["required"]:
                if req not in instance:
                    raise ValidationError(f"missing required property '{req}'", path)
        
        # properties validation
        properties = schema.get("properties", {})
        for prop, val in instance.items():
            if prop in properties:
                validate_schema(val, properties[prop], f"{path}.{prop}")
            elif schema.get("additionalProperties") is False:
                raise ValidationError(f"additional property '{prop}' is not allowed", path)
            elif isinstance(schema.get("additionalProperties"), dict):
                validate_schema(val, schema["additionalProperties"], f"{path}.{prop}")

    # Check allOf
    if "allOf" in schema:
        for idx, sub_schema in enumerate(schema["allOf"]):
            validate_schema(instance, sub_schema, f"{path}.allOf[{idx}]")

    # Check if / then / else (Draft-07 conditional validation)
    if "if" in schema:
        if_valid = True
        try:
            validate_schema(instance, schema["if"], f"{path}.if")
        except ValidationError:
            if_valid = False

        if if_valid:
            if "then" in schema:
                validate_schema(instance, schema["then"], f"{path}.then")
        else:
            if "else" in schema:
                validate_schema(instance, schema["else"], f"{path}.else")


# ==============================================================================
# 2. Baseline Valid Payload Generators
# ==============================================================================

def make_valid_po_payload():
    return {
        "handoff_id": "HANDOFF-PO-ARCH-FEAT001-V1",
        "schema_version": "1.0.0",
        "timestamp": "2026-09-17T14:30:00Z",
        "sender": {
            "role": "Product Owner",
            "agent_id": "po-agent-01"
        },
        "recipient": {
            "role": "Software Architect",
            "agent_id": "arch-agent-01"
        },
        "feature_id": "FEAT-001",
        "feature_title": "User Profile & Preferences Management",
        "prd_path": "docs/prd/PRD-FEAT-001.md",
        "user_stories": [
            {
                "story_id": "US-01",
                "role": "mobile user",
                "want": "view and update my profile settings offline",
                "so_that": "my preferences persist seamlessly across network drops"
            },
            {
                "story_id": "US-02",
                "role": "accessibility user",
                "want": "navigate all profile controls with TalkBack",
                "so_that": "I can configure preferences independently"
            }
        ],
        "functional_requirements": [
            {
                "fr_id": "FR-01",
                "title": "Offline Preference Persistence",
                "statement": "The system shall persist profile settings to local DataStore immediately upon modification."
            },
            {
                "fr_id": "FR-02",
                "title": "TalkBack Accessibility Support",
                "statement": "The system shall expose screen reader semantics and labels on all profile interactive elements."
            }
        ],
        "acceptance_criteria": [
            {
                "ac_id": "AC-01",
                "scenario": "Successful profile load",
                "given": "user has saved local profile data",
                "when": "user opens the profile screen",
                "then": "profile information is displayed within 16ms"
            },
            {
                "ac_id": "AC-02",
                "scenario": "Offline mutation persistence",
                "given": "device is offline",
                "when": "user toggles dark mode preference",
                "then": "preference is updated locally in DataStore and UI reflects change"
            }
        ],
        "non_functional_requirements": {
            "min_sdk": 24,
            "target_sdk": 35,
            "offline_supported": True,
            "accessibility_required": True,
            "performance_budget_ms": 16
        }
    }

def make_valid_arch_payload():
    return {
        "handoff_id": "HANDOFF-ARCH-DEV-FEAT001-V1",
        "schema_version": "1.0.0",
        "timestamp": "2026-09-17T14:35:00Z",
        "sender": {
            "role": "Software Architect",
            "agent_id": "arch-agent-01"
        },
        "recipient": {
            "role": "Senior Android Developer",
            "agent_id": "dev-agent-01"
        },
        "feature_id": "FEAT-001",
        "po_handoff_ref": "HANDOFF-PO-ARCH-FEAT001-V1",
        "adr_path": "docs/adr/ADR-001.md",
        "component_spec_path": "docs/specs/COMP-SPEC-001.md",
        "remediation_cycle": 0,
        "architecture_pattern": "MVI",
        "module_targets": [":feature:profile", ":core:domain", ":core:data"],
        "component_contracts": [
            {
                "component_name": "ProfileScreen",
                "package_name": "com.example.app.feature.profile.ui",
                "layer": "UI",
                "state_models": ["ProfileUiState"],
                "action_events": ["ProfileUiAction.Refresh", "ProfileUiAction.ToggleTheme"],
                "exposed_flows": ["StateFlow<ProfileUiState>"]
            }
        ],
        "file_manifest_plan": [
            {
                "target_path": "feature/profile/src/main/kotlin/ProfileScreen.kt",
                "action": "CREATE",
                "purpose": "Stateless Jetpack Compose UI component"
            },
            {
                "target_path": "feature/profile/src/main/kotlin/ProfileViewModel.kt",
                "action": "CREATE",
                "purpose": "MVI ViewModel managing StateFlow<ProfileUiState>"
            }
        ],
        "dependencies": [
            {
                "coordinate": "androidx.compose.material3:material3:1.2.0",
                "scope": "implementation"
            }
        ]
    }

def make_valid_dev_payload():
    return {
        "handoff_id": "HANDOFF-DEV-QA-FEAT001-V1",
        "schema_version": "1.0.0",
        "timestamp": "2026-09-17T14:40:00Z",
        "sender": {
            "role": "Senior Android Developer",
            "agent_id": "dev-agent-01"
        },
        "recipient": {
            "role": "QA/Testing Engineer",
            "agent_id": "qa-agent-01"
        },
        "feature_id": "FEAT-001",
        "architect_handoff_ref": "HANDOFF-ARCH-DEV-FEAT001-V1",
        "remediation_cycle": 0,
        "implementation_manifest": {
            "module": ":feature:profile",
            "entry_point_composable": "com.example.app.feature.profile.ui.ProfileScreen",
            "viewmodel_class": "com.example.app.feature.profile.ui.ProfileViewModel",
            "repository_class": "com.example.app.feature.profile.data.ProfileRepositoryImpl"
        },
        "code_changes": [
            {
                "file_path": "feature/profile/src/main/kotlin/ProfileScreen.kt",
                "action": "CREATED",
                "language": "kotlin",
                "line_count": 85
            },
            {
                "file_path": "feature/profile/src/main/kotlin/ProfileViewModel.kt",
                "action": "CREATED",
                "language": "kotlin",
                "line_count": 110
            }
        ],
        "developer_test_summary": {
            "unit_tests_run": 8,
            "unit_tests_passed": 8,
            "unit_tests_failed": 0,
            "test_frameworks_used": ["JUnit5", "MockK", "Turbine"],
            "test_files": [
                "feature/profile/src/test/kotlin/ProfileViewModelTest.kt"
            ]
        },
        "lint_and_sanity": {
            "compilation_clean": True,
            "ktlint_clean": True,
            "detekt_clean": True
        }
    }

def make_valid_qa_payload():
    return {
        "handoff_id": "HANDOFF-QA-VERDICT-FEAT001-V1",
        "schema_version": "1.0.0",
        "timestamp": "2026-09-17T14:45:00Z",
        "sender": {
            "role": "QA/Testing Engineer",
            "agent_id": "qa-agent-01"
        },
        "recipient": {
            "role": "Product Owner",
            "agent_id": "po-agent-01"
        },
        "feature_id": "FEAT-001",
        "dev_handoff_ref": "HANDOFF-DEV-QA-FEAT001-V1",
        "verdict": "PASS",
        "qa_report_path": "docs/qa/QA-REPORT-FEAT-001.md",
        "remediation_cycle": 0,
        "test_execution_summary": {
            "total_tests": 12,
            "passed": 12,
            "failed": 0,
            "skipped": 0,
            "coverage_percentage": 92.5
        },
        "acceptance_matrix": [
            {
                "ac_id": "AC-01",
                "verified": True,
                "verifying_test_case": "ProfileViewModelTest#testInitialLoad",
                "notes": "Verified state emitted within 10ms"
            },
            {
                "ac_id": "AC-02",
                "verified": True,
                "verifying_test_case": "ProfileViewModelTest#testOfflineToggle",
                "notes": "DataStore update verified"
            }
        ],
        "ui_inspection_summary": {
            "compose_semantics_valid": True,
            "touch_targets_compliant": True,
            "accessibility_labels_present": True
        },
        "issues": []
    }

def make_valid_escalation_payload():
    return {
        "handoff_id": "HANDOFF-DEV-ARCH-ESC-FEAT001-C1",
        "schema_version": "1.0.0",
        "timestamp": "2026-09-17T14:40:00Z",
        "sender": {
            "role": "Senior Android Developer",
            "agent_id": "dev-agent-01"
        },
        "recipient": {
            "role": "Software Architect",
            "agent_id": "arch-agent-01"
        },
        "feature_id": "FEAT-001",
        "architect_handoff_ref": "HANDOFF-ARCH-DEV-FEAT001-V1",
        "escalation_report_path": "docs/escalations/ESCALATION-FEAT-001-01.md",
        "remediation_cycle": 1,
        "escalation_category": "UNCOMPILABLE_INTERFACE",
        "contract_violations": [
            {
                "target_artifact": "COMPONENT_SPEC",
                "artifact_path": "docs/specs/COMP-SPEC-FEAT-001.md",
                "symbol_or_signature": "ProfileUiAction.SubmitFeedback",
                "violation_category": "SYNTAX_OR_TYPE_ERROR",
                "violation_description": "Interface specifies invalid generic bounds causing compiler error",
                "reproduction_snippet": "data class SubmitFeedback(val payload: String) : ProfileUiAction"
            }
        ],
        "compilation_diagnostics": {
            "compiler_attempted": True,
            "error_count": 1,
            "primary_error_code": "e: Incompatible type parameter bounds",
            "compiler_errors": [
                "e: ProfileViewModel.kt: (34, 12): Incompatible type parameter bounds"
            ]
        },
        "proposed_remediation": {
            "summary": "Replace invalid generic type with String",
            "suggested_contract_change": "data class SubmitFeedback(val payload: String) : ProfileUiAction",
            "requires_adr_revision": False,
            "requires_component_spec_revision": True,
            "suggested_dependencies": []
        },
        "blocks_implementation": True
    }


# ==============================================================================
# 3. Test Harness Execution
# ==============================================================================

class TestResults:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.failures = []

    def assert_valid(self, name, payload, schema):
        try:
            validate_schema(payload, schema)
            self.passed += 1
            # print(f"  [PASS] {name} (Accepted valid payload)")
        except ValidationError as e:
            self.failed += 1
            msg = f"  [UNEXPECTED FAIL] {name}: Expected valid, got error: {e}"
            self.failures.append(msg)
            print(msg)

    def assert_invalid(self, name, payload, schema, expected_msg_substring=None):
        try:
            validate_schema(payload, schema)
            self.failed += 1
            msg = f"  [UNEXPECTED PASS] {name}: Expected rejection, but payload was ACCEPTED!"
            self.failures.append(msg)
            print(msg)
        except ValidationError as e:
            if expected_msg_substring and expected_msg_substring.lower() not in str(e).lower():
                self.failed += 1
                msg = f"  [ERROR MISMATCH] {name}: Rejected but wrong error. Got: '{e}', expected substring: '{expected_msg_substring}'"
                self.failures.append(msg)
                print(msg)
            else:
                self.passed += 1
                # print(f"  [PASS] {name} (Correctly rejected: {e})")

def run_all_challenges():
    results = TestResults()
    
    # Load schemas
    schemas_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "schemas"))
    po_schema = json.load(open(os.path.join(schemas_dir, "po_to_architect_handoff.schema.json"), encoding="utf-8"))
    arch_schema = json.load(open(os.path.join(schemas_dir, "architect_to_dev_handoff.schema.json"), encoding="utf-8"))
    dev_schema = json.load(open(os.path.join(schemas_dir, "dev_to_qa_handoff.schema.json"), encoding="utf-8"))
    qa_schema = json.load(open(os.path.join(schemas_dir, "qa_verdict_handoff.schema.json"), encoding="utf-8"))
    escalation_schema = json.load(open(os.path.join(schemas_dir, "dev_to_architect_escalation.schema.json"), encoding="utf-8"))
    
    print("\n=======================================================")
    print("CHALLENGE SUITE 1: Baseline Valid Payloads")
    print("=======================================================")
    results.assert_valid("PO Valid Baseline", make_valid_po_payload(), po_schema)
    results.assert_valid("Arch Valid Baseline", make_valid_arch_payload(), arch_schema)
    results.assert_valid("Dev Valid Baseline", make_valid_dev_payload(), dev_schema)
    results.assert_valid("QA Valid Baseline", make_valid_qa_payload(), qa_schema)
    results.assert_valid("Escalation Valid Baseline", make_valid_escalation_payload(), escalation_schema)

    print("\n=======================================================")
    print("CHALLENGE SUITE 2: Missing Required Properties")
    print("=======================================================")
    # PO schema required fields
    for req in po_schema["required"]:
        bad = make_valid_po_payload()
        del bad[req]
        results.assert_invalid(f"PO missing '{req}'", bad, po_schema, f"missing required property '{req}'")
    
    # PO nested required: sender.role, sender.agent_id
    bad = make_valid_po_payload()
    del bad["sender"]["role"]
    results.assert_invalid("PO missing sender.role", bad, po_schema, "missing required property 'role'")

    bad = make_valid_po_payload()
    del bad["non_functional_requirements"]["min_sdk"]
    results.assert_invalid("PO missing NFR min_sdk", bad, po_schema, "missing required property 'min_sdk'")

    # Arch schema required fields
    for req in arch_schema["required"]:
        bad = make_valid_arch_payload()
        del bad[req]
        results.assert_invalid(f"Arch missing '{req}'", bad, arch_schema, f"missing required property '{req}'")

    # Dev schema required fields
    for req in dev_schema["required"]:
        bad = make_valid_dev_payload()
        del bad[req]
        results.assert_invalid(f"Dev missing '{req}'", bad, dev_schema, f"missing required property '{req}'")

    bad = make_valid_dev_payload()
    del bad["lint_and_sanity"]["compilation_clean"]
    results.assert_invalid("Dev missing compilation_clean", bad, dev_schema, "missing required property 'compilation_clean'")

    bad = make_valid_dev_payload()
    del bad["developer_test_summary"]["unit_tests_failed"]
    results.assert_invalid("Dev missing unit_tests_failed", bad, dev_schema, "missing required property 'unit_tests_failed'")

    # QA schema required fields
    for req in qa_schema["required"]:
        bad = make_valid_qa_payload()
        del bad[req]
        results.assert_invalid(f"QA missing '{req}'", bad, qa_schema, f"missing required property '{req}'")

    # Escalation schema required fields
    for req in escalation_schema["required"]:
        bad = make_valid_escalation_payload()
        del bad[req]
        results.assert_invalid(f"Escalation missing '{req}'", bad, escalation_schema, f"missing required property '{req}'")

    bad = make_valid_escalation_payload()
    del bad["sender"]["role"]
    results.assert_invalid("Escalation missing sender.role", bad, escalation_schema, "missing required property 'role'")

    bad = make_valid_escalation_payload()
    del bad["contract_violations"][0]["violation_description"]
    results.assert_invalid("Escalation missing violation_description", bad, escalation_schema, "missing required property 'violation_description'")

    print("\n=======================================================")
    print("CHALLENGE SUITE 3: Numeric Boundaries & Constraints")
    print("=======================================================")
    # min_sdk boundary: minimum 24
    bad = make_valid_po_payload()
    bad["non_functional_requirements"]["min_sdk"] = 23
    results.assert_invalid("PO min_sdk = 23", bad, po_schema, "< minimum 24")
    
    good = make_valid_po_payload()
    good["non_functional_requirements"]["min_sdk"] = 24
    results.assert_valid("PO min_sdk = 24 boundary", good, po_schema)

    good = make_valid_po_payload()
    good["non_functional_requirements"]["min_sdk"] = 25
    results.assert_valid("PO min_sdk = 25", good, po_schema)

    bad = make_valid_po_payload()
    bad["non_functional_requirements"]["min_sdk"] = -1
    results.assert_invalid("PO min_sdk = -1", bad, po_schema, "< minimum 24")

    # target_sdk boundary: minimum 34
    bad = make_valid_po_payload()
    bad["non_functional_requirements"]["target_sdk"] = 33
    results.assert_invalid("PO target_sdk = 33", bad, po_schema, "< minimum 34")

    good = make_valid_po_payload()
    good["non_functional_requirements"]["target_sdk"] = 34
    results.assert_valid("PO target_sdk = 34 boundary", good, po_schema)

    good = make_valid_po_payload()
    good["non_functional_requirements"]["target_sdk"] = 35
    results.assert_valid("PO target_sdk = 35", good, po_schema)

    # QA coverage_percentage boundary: 0.0 <= coverage <= 100.0
    bad = make_valid_qa_payload()
    bad["test_execution_summary"]["coverage_percentage"] = -0.1
    results.assert_invalid("QA coverage_percentage = -0.1", bad, qa_schema, "< minimum 0.0")

    good = make_valid_qa_payload()
    good["test_execution_summary"]["coverage_percentage"] = 0.0
    results.assert_valid("QA coverage_percentage = 0.0 boundary", good, qa_schema)

    good = make_valid_qa_payload()
    good["test_execution_summary"]["coverage_percentage"] = 100.0
    results.assert_valid("QA coverage_percentage = 100.0 boundary", good, qa_schema)

    bad = make_valid_qa_payload()
    bad["test_execution_summary"]["coverage_percentage"] = 100.1
    results.assert_invalid("QA coverage_percentage = 100.1", bad, qa_schema, "> maximum 100.0")

    # QA total_tests boundary: minimum 1
    bad = make_valid_qa_payload()
    bad["test_execution_summary"]["total_tests"] = 0
    results.assert_invalid("QA total_tests = 0", bad, qa_schema, "< minimum 1")

    # QA passed / failed / skipped: minimum 0
    bad = make_valid_qa_payload()
    bad["test_execution_summary"]["failed"] = -1
    results.assert_invalid("QA failed = -1", bad, qa_schema, "< minimum 0")

    # Dev line_count boundary: minimum 1
    bad = make_valid_dev_payload()
    bad["code_changes"][0]["line_count"] = 0
    results.assert_invalid("Dev line_count = 0", bad, dev_schema, "< minimum 1")

    bad = make_valid_dev_payload()
    bad["developer_test_summary"]["unit_tests_run"] = 0
    results.assert_invalid("Dev unit_tests_run = 0", bad, dev_schema, "< minimum 1")

    # Feature title minLength 3
    bad = make_valid_po_payload()
    bad["feature_title"] = "UI"
    results.assert_invalid("PO feature_title length 2", bad, po_schema, "< minLength 3")

    good = make_valid_po_payload()
    good["feature_title"] = "UIX"
    results.assert_valid("PO feature_title length 3", good, po_schema)

    # Functional requirements string length constraints
    bad = make_valid_po_payload()
    bad["functional_requirements"][0]["title"] = "ab"
    results.assert_invalid("PO FR title length 2", bad, po_schema, "< minLength 3")

    bad = make_valid_po_payload()
    bad["functional_requirements"][0]["statement"] = "abcd"
    results.assert_invalid("PO FR statement length 4", bad, po_schema, "< minLength 5")

    # remediation_cycle boundary across schemas (0..3)
    for name, schema, payload_fn in [
        ("Arch", arch_schema, make_valid_arch_payload),
        ("Dev", dev_schema, make_valid_dev_payload),
        ("QA", qa_schema, make_valid_qa_payload),
        ("Escalation", escalation_schema, make_valid_escalation_payload)
    ]:
        bad = payload_fn()
        bad["remediation_cycle"] = -1
        results.assert_invalid(f"{name} remediation_cycle = -1", bad, schema, "< minimum 0")

        good = payload_fn()
        good["remediation_cycle"] = 0
        results.assert_valid(f"{name} remediation_cycle = 0 boundary", good, schema)

        good = payload_fn()
        good["remediation_cycle"] = 3
        results.assert_valid(f"{name} remediation_cycle = 3 boundary", good, schema)

        bad = payload_fn()
        bad["remediation_cycle"] = 4
        results.assert_invalid(f"{name} remediation_cycle = 4", bad, schema, "> maximum 3")

    print("\n=======================================================")
    print("CHALLENGE SUITE 4: Empty Arrays (minItems: 1 Violation)")
    print("=======================================================")
    # PO user_stories
    bad = make_valid_po_payload()
    bad["user_stories"] = []
    results.assert_invalid("PO user_stories = []", bad, po_schema, "< minItems 1")

    # PO acceptance_criteria
    bad = make_valid_po_payload()
    bad["acceptance_criteria"] = []
    results.assert_invalid("PO acceptance_criteria = []", bad, po_schema, "< minItems 1")

    # PO functional_requirements
    bad = make_valid_po_payload()
    bad["functional_requirements"] = []
    results.assert_invalid("PO functional_requirements = []", bad, po_schema, "< minItems 1")

    # Arch module_targets
    bad = make_valid_arch_payload()
    bad["module_targets"] = []
    results.assert_invalid("Arch module_targets = []", bad, arch_schema, "< minItems 1")

    # Arch component_contracts
    bad = make_valid_arch_payload()
    bad["component_contracts"] = []
    results.assert_invalid("Arch component_contracts = []", bad, arch_schema, "< minItems 1")

    # Arch file_manifest_plan
    bad = make_valid_arch_payload()
    bad["file_manifest_plan"] = []
    results.assert_invalid("Arch file_manifest_plan = []", bad, arch_schema, "< minItems 1")

    # Dev code_changes
    bad = make_valid_dev_payload()
    bad["code_changes"] = []
    results.assert_invalid("Dev code_changes = []", bad, dev_schema, "< minItems 1")

    # QA acceptance_matrix
    bad = make_valid_qa_payload()
    bad["acceptance_matrix"] = []
    results.assert_invalid("QA acceptance_matrix = []", bad, qa_schema, "< minItems 1")

    # Escalation contract_violations
    bad = make_valid_escalation_payload()
    bad["contract_violations"] = []
    results.assert_invalid("Escalation contract_violations = []", bad, escalation_schema, "< minItems 1")

    print("\n=======================================================")
    print("CHALLENGE SUITE 5: Enums, Constants & Const Violations")
    print("=======================================================")
    # PO sender.role const violation
    bad = make_valid_po_payload()
    bad["sender"]["role"] = "Software Architect"
    results.assert_invalid("PO sender role = Software Architect", bad, po_schema, "does not match const 'Product Owner'")

    # Arch architecture_pattern enum violation
    bad = make_valid_arch_payload()
    bad["architecture_pattern"] = "MVC"
    results.assert_invalid("Arch pattern = MVC", bad, arch_schema, "not in enum")

    bad = make_valid_arch_payload()
    bad["architecture_pattern"] = "VIPER"
    results.assert_invalid("Arch pattern = VIPER", bad, arch_schema, "not in enum")

    bad = make_valid_arch_payload()
    bad["component_contracts"][0]["layer"] = "DATABASE"
    results.assert_invalid("Arch contract layer = DATABASE", bad, arch_schema, "not in enum")

    bad = make_valid_arch_payload()
    bad["file_manifest_plan"][0]["action"] = "MOVE"
    results.assert_invalid("Arch file action = MOVE", bad, arch_schema, "not in enum")

    # Dev language enum violation
    bad = make_valid_dev_payload()
    bad["code_changes"][0]["language"] = "java"
    results.assert_invalid("Dev language = java", bad, dev_schema, "not in enum")

    bad = make_valid_dev_payload()
    bad["code_changes"][0]["language"] = "python"
    results.assert_invalid("Dev language = python", bad, dev_schema, "not in enum")

    # Dev code action = DELETED is now a valid action
    good = make_valid_dev_payload()
    good["code_changes"][0]["action"] = "DELETED"
    results.assert_valid("Dev code action = DELETED", good, dev_schema)

    # Dev code action = PURGED is rejected
    bad = make_valid_dev_payload()
    bad["code_changes"][0]["action"] = "PURGED"
    results.assert_invalid("Dev code action = PURGED", bad, dev_schema, "not in enum")

    # Dev unit_tests_failed const violation (MUST be 0)
    good = make_valid_dev_payload()
    good["developer_test_summary"]["unit_tests_failed"] = 0
    results.assert_valid("Dev unit_tests_failed = 0", good, dev_schema)

    bad = make_valid_dev_payload()
    bad["developer_test_summary"]["unit_tests_failed"] = 1
    results.assert_invalid("Dev unit_tests_failed = 1", bad, dev_schema, "does not match const 0")

    # Dev lint_and_sanity const violations (MUST be True!)
    bad = make_valid_dev_payload()
    bad["lint_and_sanity"]["compilation_clean"] = False
    results.assert_invalid("Dev compilation_clean = False", bad, dev_schema, "does not match const True")

    bad = make_valid_dev_payload()
    bad["lint_and_sanity"]["ktlint_clean"] = False
    results.assert_invalid("Dev ktlint_clean = False", bad, dev_schema, "does not match const True")

    bad = make_valid_dev_payload()
    bad["lint_and_sanity"]["detekt_clean"] = False
    results.assert_invalid("Dev detekt_clean = False", bad, dev_schema, "does not match const True")

    # Escalation blocks_implementation const violation (MUST be True)
    bad = make_valid_escalation_payload()
    bad["blocks_implementation"] = False
    results.assert_invalid("Escalation blocks_implementation = False", bad, escalation_schema, "does not match const True")

    # Escalation category enum
    bad = make_valid_escalation_payload()
    bad["escalation_category"] = "INVALID_CATEGORY"
    results.assert_invalid("Escalation category invalid", bad, escalation_schema, "not in enum")

    # QA recipient role enum includes Remediation Router
    good = make_valid_qa_payload()
    good["recipient"]["role"] = "Remediation Router"
    results.assert_valid("QA recipient.role = Remediation Router", good, qa_schema)

    good = make_valid_qa_payload()
    good["recipient"]["role"] = "Automated Remediation Router"
    results.assert_valid("QA recipient.role = Automated Remediation Router", good, qa_schema)

    bad = make_valid_qa_payload()
    bad["recipient"]["role"] = "Security Lead"
    results.assert_invalid("QA recipient.role = Security Lead", bad, qa_schema, "not in enum")

    # QA verdict enum
    bad = make_valid_qa_payload()
    bad["verdict"] = "ACCEPTED"
    results.assert_invalid("QA verdict = ACCEPTED", bad, qa_schema, "not in enum")

    bad = make_valid_qa_payload()
    bad["verdict"] = "FAIL"
    results.assert_invalid("QA verdict = FAIL", bad, qa_schema, "not in enum")

    # QA issue severity enum
    bad = make_valid_qa_payload()
    bad["verdict"] = "FAIL_REVISE"
    bad["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "LOW",
        "title": "Minor formatting issue",
        "reproduction_steps": ["Step 1"],
        "expected": "Good",
        "actual": "Bad",
        "target_role_for_remediation": "Senior Android Developer",
        "upstream_artifact_ref": "docs/specs/COMP-SPEC-001.md"
    }]
    results.assert_invalid("QA issue severity = LOW", bad, qa_schema, "not in enum")

    # QA target_role_for_remediation enum
    bad = make_valid_qa_payload()
    bad["verdict"] = "FAIL_REVISE"
    bad["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "CRITICAL",
        "title": "Crash on resume",
        "reproduction_steps": ["Step 1"],
        "expected": "No crash",
        "actual": "Crash",
        "target_role_for_remediation": "Security Engineer",
        "upstream_artifact_ref": "docs/specs/COMP-SPEC-001.md"
    }]
    results.assert_invalid("QA remediation target = Security Engineer", bad, qa_schema, "not in enum")

    print("\n=======================================================")
    print("CHALLENGE SUITE 6: Regex Pattern Violations")
    print("=======================================================")
    # handoff_id patterns
    bad = make_valid_po_payload()
    bad["handoff_id"] = "HANDOFF-INVALID-001"
    results.assert_invalid("PO handoff_id prefix invalid", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["feature_id"] = "FEAT-1"  # requires at least 3 digits
    results.assert_invalid("PO feature_id only 1 digit", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["feature_id"] = "FEATURE-001"
    results.assert_invalid("PO feature_id wrong prefix", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["user_stories"][0]["story_id"] = "US-1"  # requires at least 2 digits
    results.assert_invalid("PO story_id only 1 digit", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["functional_requirements"][0]["fr_id"] = "REQ-01"
    results.assert_invalid("PO FR fr_id pattern REQ-01", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["functional_requirements"][0]["fr_id"] = "FR-1"
    results.assert_invalid("PO FR fr_id pattern FR-1 (<2 digits)", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["acceptance_criteria"][0]["ac_id"] = "AC-1"  # requires at least 2 digits
    results.assert_invalid("PO ac_id only 1 digit", bad, po_schema, "does not match pattern")

    bad = make_valid_po_payload()
    bad["prd_path"] = "docs/prd/PRD-FEAT-001.pdf"
    results.assert_invalid("PO prd_path not markdown", bad, po_schema, "does not match pattern")

    bad = make_valid_arch_payload()
    bad["adr_path"] = "docs/adr/ADR-001.txt"
    results.assert_invalid("Arch adr_path not markdown", bad, arch_schema, "does not match pattern")

    bad = make_valid_dev_payload()
    bad["architect_handoff_ref"] = "HANDOFF-DEV-QA-001"  # must be ARCH-DEV
    results.assert_invalid("Dev ref wrong handoff type", bad, dev_schema, "does not match pattern")

    bad = make_valid_escalation_payload()
    bad["handoff_id"] = "HANDOFF-INVALID-ESC-001"
    results.assert_invalid("Escalation handoff_id invalid pattern", bad, escalation_schema, "does not match pattern")

    bad = make_valid_escalation_payload()
    bad["architect_handoff_ref"] = "HANDOFF-DEV-QA-001"
    results.assert_invalid("Escalation architect_handoff_ref invalid pattern", bad, escalation_schema, "does not match pattern")

    bad = make_valid_qa_payload()
    bad["verdict"] = "FAIL_REVISE"
    bad["issues"] = [{
        "issue_id": "BUG-01",  # must be DEF-[0-9]{2,}
        "severity": "CRITICAL",
        "title": "Crash",
        "reproduction_steps": ["Step 1"],
        "expected": "Good",
        "actual": "Crash",
        "target_role_for_remediation": "Senior Android Developer",
        "upstream_artifact_ref": "docs/specs/COMP-SPEC-001.md"
    }]
    results.assert_invalid("QA issue_id BUG-01 pattern violation", bad, qa_schema, "does not match pattern")

    print("\n=======================================================")
    print("CHALLENGE SUITE 7: Type Inversions & Malformed Payloads")
    print("=======================================================")
    # min_sdk as string
    bad = make_valid_po_payload()
    bad["non_functional_requirements"]["min_sdk"] = "24"
    results.assert_invalid("PO min_sdk as string '24'", bad, po_schema, "expected integer, got str")

    # offline_supported as string
    bad = make_valid_po_payload()
    bad["non_functional_requirements"]["offline_supported"] = "true"
    results.assert_invalid("PO offline_supported as string", bad, po_schema, "expected boolean, got str")

    # timestamp malformed
    bad = make_valid_po_payload()
    bad["timestamp"] = "yesterday at 3pm"
    results.assert_invalid("PO timestamp natural language", bad, po_schema, "not a valid ISO-8601")

    bad = make_valid_po_payload()
    bad["timestamp"] = "2026-99-99T99:99:99Z"
    results.assert_invalid("PO timestamp invalid calendar date", bad, po_schema, "not a valid ISO-8601")

    # user_stories as dictionary instead of list
    bad = make_valid_po_payload()
    bad["user_stories"] = {"story_id": "US-01"}
    results.assert_invalid("PO user_stories as dict", bad, po_schema, "expected array, got dict")

    # boolean for integer
    bad = make_valid_dev_payload()
    bad["code_changes"][0]["line_count"] = True
    results.assert_invalid("Dev line_count as boolean True", bad, dev_schema, "expected integer, got bool")

    print("\n=======================================================")
    print("CHALLENGE SUITE 8: Injected Properties (additionalProperties: false)")
    print("=======================================================")
    # Top-level injected property
    bad = make_valid_po_payload()
    bad["__injected_malicious_key__"] = "trojan"
    results.assert_invalid("PO top-level injected key", bad, po_schema, "additional property '__injected_malicious_key__' is not allowed")

    # Injected in nested sender
    bad = make_valid_po_payload()
    bad["sender"]["is_admin"] = True
    results.assert_invalid("PO sender injected key", bad, po_schema, "additional property 'is_admin' is not allowed")

    # Injected in NFR
    bad = make_valid_po_payload()
    bad["non_functional_requirements"]["allow_root_access"] = True
    results.assert_invalid("PO NFR injected key", bad, po_schema, "additional property 'allow_root_access' is not allowed")

    # Injected in Arch component_contracts
    bad = make_valid_arch_payload()
    bad["component_contracts"][0]["sql_table"] = "users"
    results.assert_invalid("Arch contract injected key", bad, arch_schema, "additional property 'sql_table' is not allowed")

    # Injected in Dev lint_and_sanity
    bad = make_valid_dev_payload()
    bad["lint_and_sanity"]["ignore_all_errors"] = True
    results.assert_invalid("Dev sanity injected bypass key", bad, dev_schema, "additional property 'ignore_all_errors' is not allowed")

    # Injected in QA test_execution_summary
    bad = make_valid_qa_payload()
    bad["test_execution_summary"]["faked_results"] = True
    results.assert_invalid("QA summary injected key", bad, qa_schema, "additional property 'faked_results' is not allowed")

    # Injected in QA issue
    bad = make_valid_qa_payload()
    bad["verdict"] = "FAIL_REVISE"
    bad["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "CRITICAL",
        "title": "Crash",
        "reproduction_steps": ["Step 1"],
        "expected": "Good",
        "actual": "Crash",
        "target_role_for_remediation": "Senior Android Developer",
        "upstream_artifact_ref": "docs/specs/COMP-SPEC-001.md",
        "bounty": 10000
    }]
    results.assert_invalid("QA issue injected key 'bounty'", bad, qa_schema, "additional property 'bounty' is not allowed")

    # Injected in Escalation root
    bad = make_valid_escalation_payload()
    bad["__injected_malicious_key__"] = "trojan"
    results.assert_invalid("Escalation root injected key", bad, escalation_schema, "additional property '__injected_malicious_key__' is not allowed")

    # Injected in Escalation contract_violations
    bad = make_valid_escalation_payload()
    bad["contract_violations"][0]["unallowed_property"] = 123
    results.assert_invalid("Escalation violation injected key", bad, escalation_schema, "additional property 'unallowed_property' is not allowed")

    print("\n=======================================================")
    print("CHALLENGE SUITE 9: Semantic & Cross-Field Edge Analysis")
    print("=======================================================")
    # Test QA with verdict=PASS and empty issues (Should PASS schema)
    qa_pass = make_valid_qa_payload()
    results.assert_valid("QA PASS verdict with empty issues", qa_pass, qa_schema)

    # Test QA with verdict=PASS and issues (MUST BE REJECTED via allOf/if/then)
    qa_pass_with_issues = make_valid_qa_payload()
    qa_pass_with_issues["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "BLOCKER",
        "title": "Data wipe on start",
        "reproduction_steps": ["Open app"],
        "expected": "No wipe",
        "actual": "Wipes data",
        "target_role_for_remediation": "Senior Android Developer",
        "upstream_artifact_ref": "docs/specs/COMP-SPEC-001.md"
    }]
    results.assert_invalid("QA PASS verdict with issues rejected (maxItems: 0)", qa_pass_with_issues, qa_schema, "> maxItems 0")

    # Test QA with verdict=PASS and failed tests (MUST BE REJECTED via allOf/if/then)
    qa_pass_with_failed = make_valid_qa_payload()
    qa_pass_with_failed["test_execution_summary"]["failed"] = 1
    results.assert_invalid("QA PASS verdict with failed tests rejected (failed: 0)", qa_pass_with_failed, qa_schema, "does not match const 0")

    # Test QA with verdict=PASS and unverified AC (MUST BE REJECTED via allOf/if/then)
    qa_pass_with_unverified = make_valid_qa_payload()
    qa_pass_with_unverified["acceptance_matrix"][0]["verified"] = False
    results.assert_invalid("QA PASS verdict with unverified AC rejected (verified: true)", qa_pass_with_unverified, qa_schema, "does not match const True")

    # Test QA with verdict=FAIL_REVISE and empty issues (MUST BE REJECTED via allOf/if/then)
    qa_fail_empty_issues = make_valid_qa_payload()
    qa_fail_empty_issues["verdict"] = "FAIL_REVISE"
    qa_fail_empty_issues["recipient"]["role"] = "Senior Android Developer"
    qa_fail_empty_issues["test_execution_summary"]["failed"] = 1
    qa_fail_empty_issues["acceptance_matrix"][0]["verified"] = False
    qa_fail_empty_issues["issues"] = []
    results.assert_invalid("QA FAIL_REVISE verdict with empty issues rejected (minItems: 1)", qa_fail_empty_issues, qa_schema, "< minItems 1")

    # Test QA with verdict=FAIL_REVISE and 1 issue (Should PASS schema)
    qa_fail = make_valid_qa_payload()
    qa_fail["verdict"] = "FAIL_REVISE"
    qa_fail["recipient"]["role"] = "Senior Android Developer"
    qa_fail["test_execution_summary"]["failed"] = 1
    qa_fail["acceptance_matrix"][0]["verified"] = False
    qa_fail["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "CRITICAL",
        "title": "Crash on resume",
        "reproduction_steps": ["Launch", "Background", "Resume"],
        "expected": "State restored",
        "actual": "NullPointerException",
        "target_role_for_remediation": "Senior Android Developer",
        "upstream_artifact_ref": "feature/profile/ProfileViewModel.kt:42"
    }]
    results.assert_valid("QA FAIL_REVISE verdict with defect ticket", qa_fail, qa_schema)

    # Test QA with verdict=FAIL_REVISE and Remediation Router recipient (Should PASS schema)
    qa_fail_router = make_valid_qa_payload()
    qa_fail_router["verdict"] = "FAIL_REVISE"
    qa_fail_router["recipient"]["role"] = "Remediation Router"
    qa_fail_router["test_execution_summary"]["failed"] = 1
    qa_fail_router["acceptance_matrix"][0]["verified"] = False
    qa_fail_router["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "BLOCKER",
        "title": "Contract broken",
        "reproduction_steps": ["Compile"],
        "expected": "Build passes",
        "actual": "Build fails",
        "target_role_for_remediation": "Software Architect",
        "upstream_artifact_ref": "docs/specs/COMP-SPEC-001.md"
    }]
    results.assert_valid("QA FAIL_REVISE with Remediation Router recipient", qa_fail_router, qa_schema)

    # Test QA defect ticket missing upstream_artifact_ref (MUST BE REJECTED)
    qa_fail_missing_upstream = make_valid_qa_payload()
    qa_fail_missing_upstream["verdict"] = "FAIL_REVISE"
    qa_fail_missing_upstream["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "CRITICAL",
        "title": "Crash",
        "reproduction_steps": ["Step 1"],
        "expected": "Good",
        "actual": "Crash",
        "target_role_for_remediation": "Senior Android Developer"
    }]
    results.assert_invalid("QA issue missing upstream_artifact_ref rejected", qa_fail_missing_upstream, qa_schema, "missing required property 'upstream_artifact_ref'")

    # Test QA defect ticket with upstream_artifact_ref < 3 chars (MUST BE REJECTED)
    qa_fail_short_upstream = make_valid_qa_payload()
    qa_fail_short_upstream["verdict"] = "FAIL_REVISE"
    qa_fail_short_upstream["issues"] = [{
        "issue_id": "DEF-01",
        "severity": "CRITICAL",
        "title": "Crash",
        "reproduction_steps": ["Step 1"],
        "expected": "Good",
        "actual": "Crash",
        "target_role_for_remediation": "Senior Android Developer",
        "upstream_artifact_ref": "ab"
    }]
    results.assert_invalid("QA issue upstream_artifact_ref < 3 chars rejected", qa_fail_short_upstream, qa_schema, "< minLength 3")

    print("\n=======================================================")
    print("CHALLENGE SUITE 10: Unicode, Extreme Values & Fuzzing")
    print("=======================================================")
    # Unicode and emoji support in text fields
    unicode_payload = make_valid_po_payload()
    unicode_payload["feature_title"] = "Profile 🚀 Jetpack \u200b Zero-Width 現代的なAndroid"
    unicode_payload["user_stories"][0]["want"] = "Arabic: تطبيق أندرويد, Hindi: एंड्रॉइड ऐप"
    results.assert_valid("PO Unicode / Emoji / Multilingual support", unicode_payload, po_schema)

    # Forward-compatible high target SDK (e.g. Android 100)
    sdk_payload = make_valid_po_payload()
    sdk_payload["non_functional_requirements"]["target_sdk"] = 100
    results.assert_valid("PO Target SDK forward compatibility (SDK 100)", sdk_payload, po_schema)

    # Extreme budget values
    extreme_payload = make_valid_po_payload()
    extreme_payload["non_functional_requirements"]["performance_budget_ms"] = 1000000
    results.assert_valid("PO Extreme integer performance budget", extreme_payload, po_schema)

    # Zero coverage edge case (valid lower boundary)
    zero_cov = make_valid_qa_payload()
    zero_cov["test_execution_summary"]["coverage_percentage"] = 0.0
    results.assert_valid("QA 0.0% coverage lower bound", zero_cov, qa_schema)

    # 100.0% coverage edge case (valid upper boundary)
    full_cov = make_valid_qa_payload()
    full_cov["test_execution_summary"]["coverage_percentage"] = 100.0
    results.assert_valid("QA 100.0% coverage upper bound", full_cov, qa_schema)

    print("\n=======================================================")
    print("CHALLENGE SUITE SUMMARY")
    print(f"Total Tests Executed: {results.passed + results.failed}")
    print(f"Tests Passed: {results.passed}")
    print(f"Tests Failed: {results.failed}")
    print("=======================================================\n")

    if results.failed > 0:
        print("FAILURES DETECTED:")
        for f in results.failures:
            print(f)
        return False
    return True

if __name__ == "__main__":
    success = run_all_challenges()
    sys.exit(0 if success else 1)
