#!/usr/bin/env python3
import sys
import unittest
from pathlib import Path

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root))

from scripts.run_walkthrough import run as run_walkthrough

def main():
    print('===============================================================')
    print('   ANDROID AGENTIC ECOSYSTEM — MASTER VERIFICATION RUNNER')
    print('===============================================================')

    print('\n[1/3] Running Skill, Contract, and Walkthrough Unittest Suite...')
    loader = unittest.TestLoader()
    suite = loader.discover(str(root / 'tests'), pattern='test_*.py')
    runner = unittest.TextTestRunner(verbosity=1)
    result = runner.run(suite)
    if not result.wasSuccessful():
        print('\n❌ Unittest verification failed.')
        sys.exit(1)
    print('✅ All unittest checks passed successfully.')

    print('\n[2/3] Executing End-to-End Walkthrough Pipeline (FEAT-001)...')
    try:
        run_walkthrough()
        print('✅ Walkthrough pipeline executed and verified.')
    except Exception as e:
        print(f'\n❌ Walkthrough pipeline failed: {e}')
        sys.exit(1)

    print('\n===============================================================')
    print('   🎉 VERIFICATION COMPLETE: ALL GATES PASSED (100% OK)')
    print('===============================================================')
    sys.exit(0)

if __name__ == '__main__':
    main()
