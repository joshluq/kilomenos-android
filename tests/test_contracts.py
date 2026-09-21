#!/usr/bin/env python3
import json
import unittest
from pathlib import Path
from scripts.schema_validator import validate_schema

class TestContracts(unittest.TestCase):
    def setUp(self):
        self.root = Path(__file__).resolve().parent.parent
        self.schemas_dir = self.root / 'schemas'

    def test_all_expected_schemas_exist(self):
        expected = [
            'po_to_architect_handoff.schema.json',
            'architect_to_dev_handoff.schema.json',
            'dev_to_qa_handoff.schema.json',
            'qa_verdict_handoff.schema.json',
            'dev_to_architect_escalation.schema.json'
        ]
        for name in expected:
            schema_path = self.schemas_dir / name
            self.assertTrue(schema_path.is_file(), f'Missing schema: {name}')
            with open(schema_path, encoding='utf-8') as f:
                data = json.load(f)
            self.assertEqual(data.get('type'), 'object')
            self.assertIn('required', data)

    def test_schema_templates_exist(self):
        templates_dir = self.root / 'templates'
        expected = [
            'prd-template.md',
            'adr-template.md',
            'component-spec-template.md',
            'qa-report-template.md',
            'contract-escalation-template.md'
        ]
        for name in expected:
            self.assertTrue((templates_dir / name).is_file(), f'Missing template: {name}')

if __name__ == '__main__':
    unittest.main()
