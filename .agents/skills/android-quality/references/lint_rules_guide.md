# Android Lint & Code Quality Rules Guide

## 1. Gating Policy
The Dev-to-QA transition imposes a zero-tolerance gate on:
1. `Fatal` lint violations (build-breaking correctness/security bugs)
2. `Error` lint violations (must be resolved prior to handoff)
3. `ktlint` style violations (must adhere to formatting and naming rules)

Warnings are logged and reported. In new code modules, warnings must be 0 or explicitly suppressed with justification.

---

## 2. Core Categories
- **Correctness**: API misuse, missing permissions, invalid XML layouts, nullability mismatches.
- **Security**: Insecure cryptographic keys, exported components without permissions, cleartext network traffic.
- **Performance**: Unoptimized image draws, inefficient collections, memory leaks.
- **Usability & Accessibility**: Missing `contentDescription`, interactive targets smaller than 48dp.
- **Compose Rules**: Mutable state inside composables without `remember`, Composable naming conventions (nouns with uppercase initial).

---

## 3. Pre-Handoff Commands
```bash
# Compilation check
./gradlew compileDebugKotlin

# Code style check
./gradlew ktlintCheck

# Static analysis
./gradlew lintDebug

# Unit test suite
./gradlew testDebugUnitTest
```
