---
name: fix-bug
description: Surgical investigation, root-cause triage, and regression-proof fix protocol for KmSafe. Analyzes stacktraces, Logcat dumps, or anomalous behavior, authors a failing reproduction test, applies the minimal resilient fix, and verifies system sanity without cosmetic workarounds.
---
# Fix Bug -- Surgical Triage & Regression Defense (KmSafe)

This command executes a **systematic root-cause investigation** to diagnose and eradicate software defects, strictly rejecting cosmetic band-aids or silent exception suppression.

It coordinates senior-debugging-engineer, android-quality (logcat_triage.py), and performance-optimization-tips.

---

## When to Use This Command
- When troubleshooting a crash, ANR, or unhandled exception in KmSafe.
- When a business metric (balance, odometer, fuel/EV expense, penalty projection) yields incorrect calculations.
- When experiencing UI flickering, infinite recomposition loops, or Navigation 3 state leaks.

---

## Step-by-Step Execution Protocol

When the user invokes /fix-bug [description, logcat dump, or stacktrace]:

### Phase 1: Root-Cause Triage (Symptom vs. Defect)
1. **Identify the Broken Invariant**: Which architectural contract or domain invariant from AGENTS.md was violated?
2. **Execution Tracing**:
   - Was an I/O operation or heavy computation executed on Dispatchers.Main?
   - Is repository main-safety missing dispatchers.io?
   - Did a Navigation 3 instance leak occur due to a missing key in hiltViewModel?
   - Was a Foreground Service started outside the synchronous onReceive() lifecycle?
3. **Isolation**: Separate the immediate trigger (e.g., null pointer or malformed payload) from the underlying architectural defect.

### Phase 2: Author the Reproduction Test (Red Phase)
1. Before modifying production code, write an isolated unit test in the target module ([Feature]ViewModelTest, CalculateMetricsUseCaseTest, etc.).
2. The test must deterministically reproduce the failure and assert the expected contract, failing cleanly (RED).

### Phase 3: Surgical Correction (Green Phase)
1. Implement the minimal, robust fix addressing the verified root cause.
2. Invariant Checklist:
   - Prohibited: Suppressing errors with empty 	ry/catch blocks.
   - Prohibited: Adding mutable state to Repositories (they must remain stateless).
   - Prohibited: Placing business calculations inside ViewModels or Composables.

### Phase 4: Verification & Sanity Check
1. Execute the reproduction test to verify it now passes (GREEN).
2. Run pre-existing module tests to guarantee zero regressions.
3. Report back to the user:
   - **Root Cause Identified**: The definitive explanation of the defect.
   - **Fix Applied**: Summary of files and logic modified.
   - **Regression Test Added**: Path and test case safeguarding future releases.
