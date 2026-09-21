#!/usr/bin/env python3
"""
Adversarial Challenger Test Suite — Milestone 1 Iteration 2
Agent: challenger_m1_iter2_1
Engine: Zero-dependency Draft-07 validator engine verified from test_m1_contracts_stress

Empirically challenges:
1. qa_verdict_handoff.schema.json conditional Draft-07 constraints (PASS vs FAIL_REVISE)
2. dev_to_qa_handoff.schema.json unit_tests_failed constraint and actions
3. remediation_cycle boundary limits across all schemas
4. dev_to_architect_escalation.schema.json validity and boundaries
5. po_to_architect_handoff.schema.json functional_requirements synchronization
"""

import os
import sys
import json
import copy

# Add current and parent dir to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from test_m1_contracts_stress import validate_schema, ValidationError

class CustomValidator:
    def __init__(self, schema, name="schema"):
        self.schema = schema
        self.name = name

    def iter_errors(self, instance):
        try:
            validate_schema(instance, self.schema)
            return []
        except ValidationError as e:
            return [e]

def load_schema(rel_path):
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    full_path = os.path.join(base_dir, rel_path)
    with open(full_path, "r", encoding="utf-8") as f:
        return json.load(f)

# Load schemas
schema_qa = load_schema("schemas/qa_verdict_handoff.schema.json")
schema_dev_qa = load_schema("schemas/dev_to_qa_handoff.schema.json")
schema_dev_arch = load_schema("schemas/dev_to_architect_escalation.schema.json")
schema_arch_dev = load_schema("schemas/architect_to_dev_handoff.schema.json")
schema_po_arch = load_schema("schemas/po_to_architect_handoff.schema.json")

val_qa = CustomValidator(schema_qa, "qa_verdict")
val_dev_qa = CustomValidator(schema_dev_qa, "dev_to_qa")
val_dev_arch = CustomValidator(schema_dev_arch, "dev_to_arch")
val_arch_dev = CustomValidator(schema_arch_dev, "arch_to_dev")
val_po_arch = CustomValidator(schema_po_arch, "po_to_arch")

# -------------------------------------------------------------
# Baselines
# -------------------------------------------------------------
baseline_qa_pass = {
    "handoff_id": "HANDOFF-QA-VERDICT-FEAT001-V1",
    "schema_version": "1.0.0",
    "timestamp": "2026-09-17T15:00:00Z",
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
        "coverage_percentage": 94.5
    },
    "acceptance_matrix": [
        {
            "ac_id": "AC-01",
            "verified": True,
            "verifying_test_case": "ProfileViewModelTest.testUpdateNameSuccess",
            "notes": "Verified state emission and DataStore write"
        }
    ],
    "issues": []
}

baseline_qa_fail = {
    "handoff_id": "HANDOFF-QA-VERDICT-FEAT001-V1",
    "schema_version": "1.0.0",
    "timestamp": "2026-09-17T15:00:00Z",
    "sender": {
        "role": "QA/Testing Engineer",
        "agent_id": "qa-agent-01"
    },
    "recipient": {
        "role": "Remediation Router",
        "agent_id": "router-01"
    },
    "feature_id": "FEAT-001",
    "dev_handoff_ref": "HANDOFF-DEV-QA-FEAT001-V1",
    "verdict": "FAIL_REVISE",
    "qa_report_path": "docs/qa/QA-REPORT-FEAT-001.md",
    "remediation_cycle": 1,
    "test_execution_summary": {
        "total_tests": 12,
        "passed": 10,
        "failed": 2,
        "skipped": 0,
        "coverage_percentage": 83.3
    },
    "acceptance_matrix": [
        {
            "ac_id": "AC-01",
            "verified": False,
            "verifying_test_case": "ProfileViewModelTest.testUpdateNameFailure",
            "notes": "Failed with NullPointerException"
        }
    ],
    "issues": [
        {
            "issue_id": "DEF-01",
            "severity": "CRITICAL",
            "title": "NPE on null username",
            "reproduction_steps": ["Enter null name", "Click submit"],
            "expected": "Validation error displayed",
            "actual": "Crash with NPE",
            "target_role_for_remediation": "Senior Android Developer",
            "upstream_artifact_ref": "Source File: ProfileViewModel.kt:45"
        }
    ]
}

baseline_dev_qa = {
    "handoff_id": "HANDOFF-DEV-QA-FEAT001-V1",
    "schema_version": "1.0.0",
    "timestamp": "2026-09-17T14:45:00Z",
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
        "entry_point_composable": "ProfileScreen",
        "viewmodel_class": "ProfileViewModel"
    },
    "code_changes": [
        {
            "file_path": "feature/profile/src/main/kotlin/ProfileScreen.kt",
            "action": "CREATED",
            "language": "kotlin",
            "line_count": 120
        },
        {
            "file_path": "feature/profile/src/main/kotlin/OldFile.kt",
            "action": "DELETED",
            "language": "kotlin",
            "line_count": 40
        }
    ],
    "developer_test_summary": {
        "unit_tests_run": 8,
        "unit_tests_passed": 8,
        "unit_tests_failed": 0,
        "test_frameworks_used": ["JUnit4", "MockK", "Turbine"],
        "test_files": ["feature/profile/src/test/ProfileViewModelTest.kt"]
    },
    "lint_and_sanity": {
        "compilation_clean": True,
        "ktlint_clean": True,
        "detekt_clean": True
    }
}

baseline_dev_arch = {
    "handoff_id": "HANDOFF-DEV-ARCH-ESC-FEAT001-V1",
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
            "symbol_or_signature": "fun updateProfile(id: String): Flow<Nothing>",
            "violation_category": "SYNTAX_OR_TYPE_ERROR",
            "violation_description": "Return type Flow<Nothing> cannot emit meaningful state to Composable",
            "reproduction_snippet": "val flow: Flow<Nothing> = emptyFlow()"
        }
    ],
    "compilation_diagnostics": {
        "compiler_attempted": True,
        "error_count": 1,
        "primary_error_code": "e: Type mismatch",
        "compiler_errors": ["e: ProfileViewModel.kt: (24, 5): Type mismatch: inferred type is Flow<Nothing> but Flow<ProfileUiState> was expected"]
    },
    "proposed_remediation": {
        "summary": "Change signature from Flow<Nothing> to Flow<ProfileUiState>",
        "suggested_contract_change": "fun updateProfile(id: String): Flow<ProfileUiState>",
        "requires_adr_revision": False,
        "requires_component_spec_revision": True
    },
    "blocks_implementation": True
}

baseline_arch_dev = {
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
    "adr_path": "docs/adr/ADR-FEAT-001.md",
    "component_spec_path": "docs/specs/COMP-SPEC-FEAT-001.md",
    "remediation_cycle": 0,
    "architecture_pattern": "MVI",
    "module_targets": [":feature:profile", ":core:model", ":core:data"],
    "component_contracts": [
        {
            "component_name": "ProfileComponent",
            "package_name": "com.example.profile",
            "layer": "VIEWMODEL",
            "state_models": ["ProfileUiState"],
            "action_events": ["ProfileUiAction.UpdateName", "ProfileUiAction.Save"],
            "exposed_flows": ["StateFlow<ProfileUiState>"]
        }
    ],
    "file_manifest_plan": [
        {
            "target_path": "feature/profile/src/main/kotlin/ProfileViewModel.kt",
            "action": "CREATE",
            "purpose": "Manages UDF state stream for profile screen"
        },
        {
            "target_path": "feature/profile/src/main/kotlin/LegacyProfileView.kt",
            "action": "DELETE",
            "purpose": "Remove deprecated XML legacy view"
        }
    ],
    "dependencies": [
        {
            "coordinate": "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0",
            "scope": "implementation"
        }
    ]
}

# -------------------------------------------------------------
# Test Execution
# -------------------------------------------------------------
tests_passed = 0
tests_failed = 0

def run_test(name, validator, payload, should_pass, desc=""):
    global tests_passed, tests_failed
    errors = validator.iter_errors(payload)
    is_valid = len(errors) == 0
    if is_valid == should_pass:
        tests_passed += 1
        print(f"  [PASS] {name}: expected valid={should_pass}, got valid={is_valid}")
    else:
        tests_failed += 1
        error_msgs = [str(e) for e in errors]
        print(f"  [FAIL] {name}: expected valid={should_pass}, got valid={is_valid}. Errors: {error_msgs}")

print("\n=======================================================")
print("1. EMPIRICAL CHALLENGE: QA Verdict Conditional Draft-07 Constraints")
print("=======================================================")
run_test("QA_PASS_Baseline", val_qa, baseline_qa_pass, True)
run_test("QA_FAIL_Baseline", val_qa, baseline_qa_fail, True)

# PASS with failed > 0 -> must FAIL
p = copy.deepcopy(baseline_qa_pass)
p["test_execution_summary"]["failed"] = 1
run_test("QA_PASS_failed_1", val_qa, p, False)

p["test_execution_summary"]["failed"] = 5
run_test("QA_PASS_failed_5", val_qa, p, False)

# PASS with unverified AC (verified == false) -> must FAIL
p = copy.deepcopy(baseline_qa_pass)
p["acceptance_matrix"][0]["verified"] = False
run_test("QA_PASS_unverified_AC_single", val_qa, p, False)

p = copy.deepcopy(baseline_qa_pass)
p["acceptance_matrix"].append({
    "ac_id": "AC-02",
    "verified": False,
    "verifying_test_case": "Test2",
    "notes": "notes"
})
run_test("QA_PASS_unverified_AC_in_multiple", val_qa, p, False)

# PASS with non-empty issues -> must FAIL
p = copy.deepcopy(baseline_qa_pass)
p["issues"] = baseline_qa_fail["issues"]
run_test("QA_PASS_with_issues", val_qa, p, False)

# FAIL_REVISE with empty issues -> must FAIL
p = copy.deepcopy(baseline_qa_fail)
p["issues"] = []
run_test("QA_FAIL_REVISE_empty_issues", val_qa, p, False)

# FAIL_REVISE with 1 issue -> must PASS
p = copy.deepcopy(baseline_qa_fail)
run_test("QA_FAIL_REVISE_1_issue", val_qa, p, True)

# FAIL_REVISE with 2 issues -> must PASS
p = copy.deepcopy(baseline_qa_fail)
p["issues"].append({
    "issue_id": "DEF-02",
    "severity": "MAJOR",
    "title": "Missing talkback description",
    "reproduction_steps": ["Turn on talkback"],
    "expected": "Reads label",
    "actual": "Reads unlabelled button",
    "target_role_for_remediation": "Senior Android Developer",
    "upstream_artifact_ref": "PRD-FEAT-001"
})
run_test("QA_FAIL_REVISE_2_issues", val_qa, p, True)

# Recipient role: Remediation Router -> must PASS
p = copy.deepcopy(baseline_qa_fail)
p["recipient"]["role"] = "Remediation Router"
run_test("QA_recipient_Remediation_Router", val_qa, p, True)

# Recipient role: Automated Remediation Router -> must PASS
p = copy.deepcopy(baseline_qa_fail)
p["recipient"]["role"] = "Automated Remediation Router"
run_test("QA_recipient_Automated_Remediation_Router", val_qa, p, True)

# Recipient role: Invalid -> must FAIL
p = copy.deepcopy(baseline_qa_fail)
p["recipient"]["role"] = "Security Operations"
run_test("QA_recipient_invalid_role", val_qa, p, False)

# Upstream artifact ref in issues missing -> must FAIL
p = copy.deepcopy(baseline_qa_fail)
del p["issues"][0]["upstream_artifact_ref"]
run_test("QA_issue_missing_upstream_ref", val_qa, p, False)

# Upstream artifact ref too short (<3) -> must FAIL
p = copy.deepcopy(baseline_qa_fail)
p["issues"][0]["upstream_artifact_ref"] = "AB"
run_test("QA_issue_upstream_ref_too_short", val_qa, p, False)


print("\n=======================================================")
print("2. EMPIRICAL CHALLENGE: Dev-to-QA Unit Tests Failed Rate & Actions")
print("=======================================================")
run_test("DEV_QA_Baseline", val_dev_qa, baseline_dev_qa, True)

# unit_tests_failed > 0 -> must FAIL
p = copy.deepcopy(baseline_dev_qa)
p["developer_test_summary"]["unit_tests_failed"] = 1
run_test("DEV_QA_unit_tests_failed_1", val_dev_qa, p, False)

p["developer_test_summary"]["unit_tests_failed"] = 10
run_test("DEV_QA_unit_tests_failed_10", val_dev_qa, p, False)

# unit_tests_failed < 0 -> must FAIL
p = copy.deepcopy(baseline_dev_qa)
p["developer_test_summary"]["unit_tests_failed"] = -1
run_test("DEV_QA_unit_tests_failed_negative", val_dev_qa, p, False)

# unit_tests_failed missing -> must FAIL
p = copy.deepcopy(baseline_dev_qa)
del p["developer_test_summary"]["unit_tests_failed"]
run_test("DEV_QA_unit_tests_failed_missing", val_dev_qa, p, False)

# unit_tests_failed string "0" -> must FAIL
p = copy.deepcopy(baseline_dev_qa)
p["developer_test_summary"]["unit_tests_failed"] = "0"
run_test("DEV_QA_unit_tests_failed_string", val_dev_qa, p, False)

# DELETED action supported -> must PASS
p = copy.deepcopy(baseline_dev_qa)
p["code_changes"] = [{
    "file_path": "feature/profile/src/main/kotlin/OldFile.kt",
    "action": "DELETED",
    "language": "kotlin",
    "line_count": 40
}]
run_test("DEV_QA_action_DELETED", val_dev_qa, p, True)

# Invalid action enum -> must FAIL
p = copy.deepcopy(baseline_dev_qa)
p["code_changes"][0]["action"] = "DELETE"
run_test("DEV_QA_action_DELETE_invalid", val_dev_qa, p, False)

p = copy.deepcopy(baseline_dev_qa)
p["code_changes"][0]["action"] = "PURGED"
run_test("DEV_QA_action_PURGED_invalid", val_dev_qa, p, False)


print("\n=======================================================")
print("3. EMPIRICAL CHALLENGE: Remediation Cycle Boundaries Across Schemas")
print("=======================================================")
schemas_to_test = [
    ("QA_Verdict", val_qa, baseline_qa_pass),
    ("Dev_To_QA", val_dev_qa, baseline_dev_qa),
    ("Dev_To_Arch_Escalation", val_dev_arch, baseline_dev_arch),
    ("Arch_To_Dev", val_arch_dev, baseline_arch_dev),
]

for name, val, base in schemas_to_test:
    # 0 -> PASS
    p = copy.deepcopy(base)
    p["remediation_cycle"] = 0
    run_test(f"{name}_cycle_0", val, p, True)

    # 1 -> PASS
    p = copy.deepcopy(base)
    p["remediation_cycle"] = 1
    run_test(f"{name}_cycle_1", val, p, True)

    # 3 -> PASS
    p = copy.deepcopy(base)
    p["remediation_cycle"] = 3
    run_test(f"{name}_cycle_3", val, p, True)

    # -1 -> FAIL
    p = copy.deepcopy(base)
    p["remediation_cycle"] = -1
    run_test(f"{name}_cycle_neg_1", val, p, False)

    # 4 -> FAIL
    p = copy.deepcopy(base)
    p["remediation_cycle"] = 4
    run_test(f"{name}_cycle_4", val, p, False)

    # string "0" -> FAIL
    p = copy.deepcopy(base)
    p["remediation_cycle"] = "0"
    run_test(f"{name}_cycle_string", val, p, False)

    # float 2.5 -> FAIL
    p = copy.deepcopy(base)
    p["remediation_cycle"] = 2.5
    run_test(f"{name}_cycle_float", val, p, False)

    # missing remediation_cycle -> FAIL
    p = copy.deepcopy(base)
    del p["remediation_cycle"]
    run_test(f"{name}_cycle_missing", val, p, False)


print("\n=======================================================")
print("4. EMPIRICAL CHALLENGE: Dev-To-Architect Escalation Boundary & Invalid Payloads")
print("=======================================================")
run_test("DEV_ARCH_Baseline", val_dev_arch, baseline_dev_arch, True)

# blocks_implementation: false -> must FAIL (const: true)
p = copy.deepcopy(baseline_dev_arch)
p["blocks_implementation"] = False
run_test("DEV_ARCH_blocks_impl_false", val_dev_arch, p, False)

# blocks_implementation missing -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
del p["blocks_implementation"]
run_test("DEV_ARCH_blocks_impl_missing", val_dev_arch, p, False)

# contract_violations empty array -> must FAIL (minItems: 1)
p = copy.deepcopy(baseline_dev_arch)
p["contract_violations"] = []
run_test("DEV_ARCH_violations_empty", val_dev_arch, p, False)

# escalation_category invalid -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["escalation_category"] = "UNHANDLED_ERROR"
run_test("DEV_ARCH_invalid_escalation_category", val_dev_arch, p, False)

# valid escalation categories -> all 5 must PASS
for cat in [
    "UNCOMPILABLE_INTERFACE",
    "DEPENDENCY_UNSATISFIED",
    "UDF_CONTRACT_VIOLATION",
    "CONCURRENCY_MODEL_CONFLICT",
    "SPECIFICATION_DEFICIT"
]:
    p = copy.deepcopy(baseline_dev_arch)
    p["escalation_category"] = cat
    run_test(f"DEV_ARCH_cat_{cat}", val_dev_arch, p, True)

# violation_category invalid -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["contract_violations"][0]["violation_category"] = "RANDOM_SYNTAX"
run_test("DEV_ARCH_invalid_violation_category", val_dev_arch, p, False)

# target_artifact invalid -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["contract_violations"][0]["target_artifact"] = "SOURCE_CODE"
run_test("DEV_ARCH_invalid_target_artifact", val_dev_arch, p, False)

# violation_description too short (< 10) -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["contract_violations"][0]["violation_description"] = "short"
run_test("DEV_ARCH_violation_desc_too_short", val_dev_arch, p, False)

# compilation_diagnostics.error_count negative -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["compilation_diagnostics"]["error_count"] = -1
run_test("DEV_ARCH_error_count_negative", val_dev_arch, p, False)

# proposed_remediation.summary too short (< 10) -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["proposed_remediation"]["summary"] = "short"
run_test("DEV_ARCH_proposed_summary_too_short", val_dev_arch, p, False)

# sender.role wrong role -> must FAIL (const: Senior Android Developer)
p = copy.deepcopy(baseline_dev_arch)
p["sender"]["role"] = "Software Architect"
run_test("DEV_ARCH_wrong_sender_role", val_dev_arch, p, False)

# recipient.role wrong role -> must FAIL (const: Software Architect)
p = copy.deepcopy(baseline_dev_arch)
p["recipient"]["role"] = "Senior Android Developer"
run_test("DEV_ARCH_wrong_recipient_role", val_dev_arch, p, False)

# additionalProperties at root injected -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["unauthorized_payload_extension"] = "injected_value"
run_test("DEV_ARCH_injected_root_prop", val_dev_arch, p, False)

# additionalProperties in contract_violations injected -> must FAIL
p = copy.deepcopy(baseline_dev_arch)
p["contract_violations"][0]["extra_field"] = "bad"
run_test("DEV_ARCH_injected_violation_prop", val_dev_arch, p, False)


print("\n=======================================================")
print("5. EMPIRICAL CHALLENGE: PO-To-Architect Functional Requirements")
print("=======================================================")
baseline_po = {
    "handoff_id": "HANDOFF-PO-ARCH-FEAT001-V1",
    "schema_version": "1.0.0",
    "timestamp": "2026-09-17T14:30:00Z",
    "sender": {"role": "Product Owner", "agent_id": "po-01"},
    "recipient": {"role": "Software Architect", "agent_id": "arch-01"},
    "feature_id": "FEAT-001",
    "feature_title": "Profile Feature",
    "prd_path": "docs/prd/PRD-FEAT-001.md",
    "user_stories": [{
        "story_id": "US-01",
        "role": "user",
        "want": "see profile",
        "so_that": "manage settings"
    }],
    "functional_requirements": [{
        "fr_id": "FR-01",
        "title": "Offline Persistence",
        "statement": "Persist profile to DataStore"
    }],
    "acceptance_criteria": [{
        "ac_id": "AC-01",
        "scenario": "View profile",
        "given": "User opened app",
        "when": "Profile loaded",
        "then": "Show details"
    }],
    "non_functional_requirements": {
        "min_sdk": 24,
        "target_sdk": 34,
        "offline_supported": True,
        "accessibility_required": True
    }
}
run_test("PO_ARCH_Baseline", val_po_arch, baseline_po, True)

# missing functional_requirements -> must FAIL
p = copy.deepcopy(baseline_po)
del p["functional_requirements"]
run_test("PO_ARCH_missing_functional_requirements", val_po_arch, p, False)

# empty functional_requirements -> must FAIL (minItems: 1)
p = copy.deepcopy(baseline_po)
p["functional_requirements"] = []
run_test("PO_ARCH_empty_functional_requirements", val_po_arch, p, False)

# invalid fr_id pattern -> must FAIL
p = copy.deepcopy(baseline_po)
p["functional_requirements"][0]["fr_id"] = "REQUIREMENT-1"
run_test("PO_ARCH_invalid_fr_id_pattern", val_po_arch, p, False)


print(f"\n=======================================================")
print(f"EMPIRICAL CHALLENGER TEST RESULTS")
print(f"Total Tests Executed: {tests_passed + tests_failed}")
print(f"Tests Passed: {tests_passed}")
print(f"Tests Failed: {tests_failed}")
print(f"=======================================================")

if __name__ == '__main__':
    if tests_failed > 0:
        sys.exit(1)
    sys.exit(0)
