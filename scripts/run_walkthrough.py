#!/usr/bin/env python3
import os
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import json
from pathlib import Path
from scripts.schema_validator import validate_schema

def run():
    print('=== Running End-to-End Walkthrough Pipeline (FEAT-001) ===')
    root = Path(__file__).resolve().parent.parent
    handoffs_dir = root / 'walkthrough' / 'handoffs'
    schemas_dir = root / 'schemas'
    docs_dir = root / 'walkthrough' / 'docs'

    # Stage 1: PO Handoff
    po_file = handoffs_dir / 'po_to_architect_FEAT-001.json'
    po_schema = schemas_dir / 'po_to_architect_handoff.schema.json'
    with open(po_file, encoding='utf-8') as f:
        po_data = json.load(f)
    with open(po_schema, encoding='utf-8') as f:
        validate_schema(po_data, json.load(f))
    print('[STAGE 1: PO -> ARCHITECT] Validated successfully.')

    # Stage 2: Architect Handoff
    arch_file = handoffs_dir / 'architect_to_dev_FEAT-001.json'
    arch_schema = schemas_dir / 'architect_to_dev_handoff.schema.json'
    with open(arch_file, encoding='utf-8') as f:
        arch_data = json.load(f)
    with open(arch_schema, encoding='utf-8') as f:
        validate_schema(arch_data, json.load(f))
    assert arch_data['po_handoff_ref'] == po_data['handoff_id'], 'Handoff reference mismatch'
    print('[STAGE 2: ARCHITECT -> DEV] Validated successfully (Link to PO confirmed).')

    # Stage 3: Dev Handoff
    dev_file = handoffs_dir / 'dev_to_qa_FEAT-001.json'
    dev_schema = schemas_dir / 'dev_to_qa_handoff.schema.json'
    with open(dev_file, encoding='utf-8') as f:
        dev_data = json.load(f)
    with open(dev_schema, encoding='utf-8') as f:
        validate_schema(dev_data, json.load(f))
    assert dev_data['architect_handoff_ref'] == arch_data['handoff_id'], 'Handoff reference mismatch'
    for change in dev_data['code_changes']:
        fp = root / change['file_path']
        assert fp.is_file(), f'Code file missing: {fp}'
    print('[STAGE 3: DEV -> QA] Validated successfully (Code files verified on disk).')

    # Stage 4: QA Verdict
    qa_file = handoffs_dir / 'qa_verdict_FEAT-001.json'
    qa_schema = schemas_dir / 'qa_verdict_handoff.schema.json'
    with open(qa_file, encoding='utf-8') as f:
        qa_data = json.load(f)
    with open(qa_schema, encoding='utf-8') as f:
        validate_schema(qa_data, json.load(f))
    assert qa_data['dev_handoff_ref'] == dev_data['handoff_id'], 'Handoff reference mismatch'
    assert qa_data['verdict'] == 'PASS', 'QA Verdict is not PASS'
    po_acs = {ac['ac_id'] for ac in po_data['acceptance_criteria']}
    qa_acs = {m['ac_id'] for m in qa_data['acceptance_matrix'] if m['verified']}
    assert po_acs.issubset(qa_acs), f'Not all PO criteria verified: {po_acs - qa_acs}'
    print('[STAGE 4: QA VERDICT] Validated successfully (100% Acceptance Criteria verified).')

    report_path = root / 'walkthrough' / 'WALKTHROUGH_REPORT.md'
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write('# FEAT-001 Walkthrough Execution Audit Report\n\n')
        f.write('- **Feature ID**: FEAT-001\n')
        f.write('- **Pipeline Execution**: SUCCESS\n')
        f.write('- **Gate Verdict**: PASS (APPROVED FOR RELEASE)\n')
        f.write('- **Acceptance Criteria Verified**: ' + ', '.join(sorted(po_acs)) + '\n')
        f.write('- **Total Tests Executed**: ' + str(qa_data['test_execution_summary']['total_tests']) + '\n')
        f.write('- **Code Coverage**: ' + str(qa_data['test_execution_summary']['coverage_percentage']) + '%\n')
    print(f'Wrote audit report to {report_path}')
    print('=== WALKTHROUGH PIPELINE COMPLETED SUCCESSFULLY ===')

if __name__ == '__main__':
    run()
