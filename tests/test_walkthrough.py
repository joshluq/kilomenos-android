#!/usr/bin/env python3
import json
import unittest
from pathlib import Path
from scripts.schema_validator import validate_schema

class TestWalkthrough(unittest.TestCase):
    def setUp(self):
        self.root = Path(__file__).resolve().parent.parent
        self.wt = self.root / 'walkthrough'

    def test_handoff_files_exist(self):
        expected_handoffs = [
            'po_to_architect_FEAT-001.json',
            'architect_to_dev_FEAT-001.json',
            'dev_to_qa_FEAT-001.json',
            'qa_verdict_FEAT-001.json'
        ]
        for h in expected_handoffs:
            p = self.wt / 'handoffs' / h
            self.assertTrue(p.is_file(), f'Missing handoff JSON: {h}')

    def test_docs_exist(self):
        expected_docs = [
            'PRD-FEAT-001.md',
            'ADR-FEAT-001.md',
            'COMP-SPEC-FEAT-001.md',
            'QA-REPORT-FEAT-001.md'
        ]
        for d in expected_docs:
            p = self.wt / 'docs' / d
            self.assertTrue(p.is_file(), f'Missing doc: {d}')

    def test_kotlin_source_and_test_files_exist(self):
        expected_src = [
            'walkthrough/src/main/kotlin/com/example/profile/model/UserProfile.kt',
            'walkthrough/src/main/kotlin/com/example/profile/ui/ProfileUiState.kt',
            'walkthrough/src/main/kotlin/com/example/profile/ui/ProfileUiAction.kt',
            'walkthrough/src/main/kotlin/com/example/profile/ui/ProfileScreen.kt',
            'walkthrough/src/main/kotlin/com/example/profile/ui/ProfileViewModel.kt',
            'walkthrough/src/main/kotlin/com/example/profile/data/ProfileRepository.kt',
            'walkthrough/src/test/kotlin/com/example/profile/ProfileViewModelTest.kt',
            'walkthrough/src/test/kotlin/com/example/profile/ProfileScreenTest.kt'
        ]
        for f in expected_src:
            p = self.root / f
            self.assertTrue(p.is_file(), f'Missing Kotlin file: {f}')

    def test_walkthrough_schemas_validate(self):
        mapping = [
            ('po_to_architect_FEAT-001.json', 'po_to_architect_handoff.schema.json'),
            ('architect_to_dev_FEAT-001.json', 'architect_to_dev_handoff.schema.json'),
            ('dev_to_qa_FEAT-001.json', 'dev_to_qa_handoff.schema.json'),
            ('qa_verdict_FEAT-001.json', 'qa_verdict_handoff.schema.json')
        ]
        for h_file, s_file in mapping:
            h_path = self.wt / 'handoffs' / h_file
            s_path = self.root / 'schemas' / s_file
            with open(h_path, encoding='utf-8') as hf, open(s_path, encoding='utf-8') as sf:
                validate_schema(json.load(hf), json.load(sf))

if __name__ == '__main__':
    unittest.main()
