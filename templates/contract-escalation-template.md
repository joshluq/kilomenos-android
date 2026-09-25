# Technical Contract Escalation: [FEATURE_ID]

**Escalation ID**: ESC-[FEATURE_ID]-[SEQUENCE_ID]  
**Feature ID**: [KILOMENOS-XXX]  
**Date**: [YYYY-MM-DDTHH:MM:SSZ]  
**Sender**: Senior Android Developer ([Agent ID])  
**Recipient**: Software Architect ([Agent ID])  
**Remediation Cycle**: [1|2|3]  
**Upstream Contract Handoff Ref**: [HANDOFF-ARCH-DEV-XXX]  
**Target Specifications**:  
- ADR: `docs/adr/ADR-[KILOMENOS-XXX].md`  
- Component Spec: `docs/specs/COMP-SPEC-[KILOMENOS-XXX].md`  

---

## 1. Blocker Summary & Escalation Classification

- **Escalation Category**: [UNCOMPILABLE_INTERFACE | DEPENDENCY_UNSATISFIED | UDF_CONTRACT_VIOLATION | CONCURRENCY_MODEL_CONFLICT | SPECIFICATION_DEFICIT]
- **Executive Summary**: [Concise 1-2 sentence description explaining why the contract cannot be compiled or implemented as specified.]
- **Implementation Status**: BLOCKED (Zero production source code can be cleanly compiled or handed off to QA without contract revision).

---

## 2. Compilation & Diagnostic Evidence

### Compiler / Build Output
```
[Paste exact Kotlinc, Android Lint, or Gradle error log output here. Include file paths and line numbers.]
```

### Build & Toolchain Context
- **Kotlin Version**: [e.g. 1.9.24 / 2.0.0]
- **Compose Compiler Extension / BOM**: [e.g. 2024.04.01]
- **Min SDK**: [e.g. 24]
- **Target SDK**: [e.g. 35]
- **Gradle Configuration Target**: [e.g. :feature:profile:build.gradle.kts]

---

## 3. Detailed Contract Violations

### Violation 1: [Symbol / Method / Dependency Name]
- **Target Artifact**: [COMPONENT_SPEC | ADR | BUILD_CONFIG]
- **Offending Specification Section**: [e.g. Section 2: UI Action Contract, line 42]
- **Contract Specification Snippet**:
  ```kotlin
  // Offending interface or signature as specified by the Software Architect
  ```
- **Attempted Implementation & Failure Demonstration**:
  ```kotlin
  // Concrete code demonstrating compiler error, type contradiction, or missing action
  ```
- **Protocol Constraint Reference**:
  [Reference to AGENTS.md Senior Android Developer Strict Prohibition: "NEVER alter component contract method signatures, state models, or action types without an approved ADR and Component Spec revision."]

---

## 4. Proposed Architectural Resolution

### Suggested Contract Modification
[Provide the exact replacement Kotlin interface, data class, or dependency declaration.]

```kotlin
// Proposed corrected interface or signature
```

### Architectural Impact Assessment
- **ADR Revision Required**: [YES | NO]
- **Component Spec Revision Required**: [YES | NO]
- **Scope of Change**: [e.g. Local to feature ViewModel vs Core domain/data layer impact]
- **Downstream QA Acceptance Criteria Impact**: [e.g. No impact on PRD ACs / Requires updated test fixture]

---

## 5. Next Actions & Handoff
1. Software Architect evaluates this escalation artifact alongside `handoffs/dev_to_architect_escalation_[KILOMENOS-ID].json`.
2. Software Architect issues revised `ADR-[KILOMENOS-ID].md` and `COMP-SPEC-[KILOMENOS-ID].md`.
3. Software Architect re-issues `architect_to_dev_<id>.json` with incremented `remediation_cycle`.
4. If remediation cycle limit (3) is exceeded, Orchestrator halts execution for Product Owner and human supervisor review.
