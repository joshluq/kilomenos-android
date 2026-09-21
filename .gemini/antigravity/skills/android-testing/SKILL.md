---
name: android-testing
description: Standards and automated generators for Android unit tests (JUnit 4/5, MockK, Turbine) and Jetpack Compose UI tests (ComposeTestRule, Semantics, Robolectric), including test execution and AC traceability.
---
# Android Testing & Verification Skill

## 1. Overview & Purpose
The `android-testing` skill establishes modern testing standards, automated test generators, and execution parsers for Modern Android Development (MAD). It ensures that every Android application component—from business logic and ViewModel state flows to Jetpack Compose UI semantics—has automated, deterministic verification with direct traceability to requirements.

Key capabilities:
1. **ViewModel StateFlow Verification**: Asynchronous state assertions using CashApp Turbine and Kotlin Coroutines test dispatchers.
2. **Type-Safe Mocking**: Suspending function stubs and invocation verifications using MockK (`coEvery`, `coVerify`).
3. **Jetpack Compose UI Semantics Testing**: Screen component testing using `ComposeTestRule` with test tags, semantics matchers, and simulated user actions.
4. **Fast Headless JVM Execution**: Executing Compose UI tests on the local JVM via Robolectric without requiring an emulator.
5. **Acceptance Criteria Traceability**: 1-to-1 mapping between PRD requirements (`AC-xx`) and automated test execution results.
6. **Automated Scaffolding**: Instant generation of idiomatic Kotlin test suites directly from Component Interface Specifications (`COMP-SPEC-*.md`).

---

## 2. Integration with Agent Roles

### Senior Android Developer
- **Unit Test Scaffolding**: Uses `generate_test_scaffold.py` to auto-generate `*ViewModelTest.kt` directly from the Architect's Component Specification before implementing code (Test-Driven Development).
- **Pre-Handoff Verification**: Runs `test_runner.py` to confirm all unit tests pass and capture metrics for the `dev_to_qa_handoff.json` contract.

### QA / Testing Engineer
- **AC Coverage Auditing**: Uses `test_runner.py` with `--prd` cross-referencing to confirm 100% of declared PRD Acceptance Criteria are covered by passing tests.
- **UI Test Expansion**: Extends generated Compose tests with edge cases, invalid inputs, and stress conditions to produce the final `qa_verdict_handoff.json`.

---

## 3. Testing Architecture & Patterns

### 3.1 ViewModel StateFlow Testing with MockK & Turbine
Testing ViewModels requires managing Kotlin Coroutines dispatchers and observing `StateFlow` emissions deterministically.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val updatePreferencesUseCase: UpdatePreferencesUseCase = mockk()
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        clearAllMocks()
        viewModel = ProfileViewModel(
            getProfileUseCase = getProfileUseCase,
            updatePreferencesUseCase = updatePreferencesUseCase,
            dispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun ac01_initialState_emitsLoadingThenSuccess() = runTest {
        val testProfile = UserProfile(id = "user_123", username = "Alex Rivera", isDarkMode = false)
        coEvery { getProfileUseCase() } returns Result.success(testProfile)

        viewModel.uiState.test {
            assertEquals(ProfileUiState.Loading, awaitItem())
            viewModel.handleAction(ProfileUiAction.LoadProfile)
            val success = awaitItem() as ProfileUiState.Success
            assertEquals("Alex Rivera", success.profile.username)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { getProfileUseCase() }
    }
}
```

#### Key Coroutine Testing Rules:
- Always use `MainDispatcherRule` to override `Dispatchers.Main` with `StandardTestDispatcher`.
- In Turbine assertion blocks, always conclude with `cancelAndIgnoreRemainingEvents()` to prevent infinite hot flow timeouts.
- For suspending functions, use `coEvery` and `coVerify` instead of standard `every` and `verify`.

---

### 3.2 Jetpack Compose UI Testing with Semantics Trees
Compose tests interact with the UI semantics tree rather than raw view hierarchies.

```kotlin
@RunWith(AndroidJUnit4::class) // Or RobolectricTestRunner for headless JVM execution
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ac02_emptyUsername_disablesSaveButton() {
        composeTestRule.setContent {
            ProfileScreen(
                state = ProfileUiState.Success(
                    profile = UserProfile(id = "1", username = "", isDarkMode = false)
                ),
                onAction = {}
            )
        }

        composeTestRule.onNodeWithTag("save_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun ac03_validInput_triggersSaveActionOnTap() {
        var actionDispatched: ProfileUiAction? = null
        composeTestRule.setContent {
            ProfileScreen(
                state = ProfileUiState.Success(
                    profile = UserProfile(id = "1", username = "Alex", isDarkMode = false)
                ),
                onAction = { action -> actionDispatched = action }
            )
        }

        composeTestRule.onNodeWithTag("save_button")
            .assertIsEnabled()
            .performClick()

        assertTrue(actionDispatched is ProfileUiAction.SavePreferences)
    }
}
```

#### Core Semantics Matchers & Actions:
| Matcher / Action | Purpose |
|---|---|
| `hasTestTag("tag")` | Finds node by Modifier `testTag("tag")` |
| `hasText("text", ignoreCase = true)` | Finds node containing matching text |
| `assertIsDisplayed()` | Verifies node is visible on screen |
| `assertIsEnabled()` / `assertIsNotEnabled()` | Verifies interactive enabled state |
| `performClick()` | Simulates user click gesture |
| `performTextInput("value")` | Enters text into focused text field |

---

## 4. Helper Scripts

The skill provides two runnable Python 3 CLI utilities in `skills/android-testing/scripts/`:

```
skills/android-testing/
├── SKILL.md
└── scripts/
    ├── test_runner.py
    └── generate_test_scaffold.py
```

### Script 1: `test_runner.py`
Executes Gradle unit tests and parses standard JUnit XML test reports, extracting test counts, failure details, and Acceptance Criteria coverage mappings.

#### Usage:
```bash
# Run Gradle unit tests and parse results
python skills/android-testing/scripts/test_runner.py --project-dir ./app --variant debug --output test_summary.json

# Parse existing JUnit XML directory without running Gradle
python skills/android-testing/scripts/test_runner.py --xml-dir app/build/test-results/testDebugUnitTest --output test_summary.json

# Parse results and cross-reference with PRD Acceptance Criteria
python skills/android-testing/scripts/test_runner.py --xml-dir app/build/test-results/testDebugUnitTest --prd docs/prd/PRD-001.md
```

#### Output Schema (`test_summary.json`):
```json
{
  "total_tests": 12,
  "passed": 12,
  "failed": 0,
  "skipped": 0,
  "duration_ms": 3450,
  "all_tests_passed": true,
  "failures": [],
  "ac_coverage": {
    "AC-01": "PASSED",
    "AC-02": "PASSED",
    "AC-03": "PASSED"
  },
  "ac_traceability_complete": true
}
```

---

### Script 2: `generate_test_scaffold.py`
Parses a Component Interface Specification (`COMP-SPEC-*.md`) or Kotlin class and generates production-ready `*ViewModelTest.kt` or `*ScreenTest.kt` boilerplate.

#### Usage:
```bash
# Generate ViewModel test scaffold from Component Spec
python skills/android-testing/scripts/generate_test_scaffold.py \
  --spec docs/specs/COMP-SPEC-001.md \
  --type viewmodel \
  --output app/src/test/java/com/example/profile/ProfileViewModelTest.kt

# Generate Compose Screen test scaffold
python skills/android-testing/scripts/generate_test_scaffold.py \
  --spec docs/specs/COMP-SPEC-001.md \
  --type screen \
  --output app/src/test/java/com/example/profile/ProfileScreenTest.kt
```

#### Generated Features:
- Complete Kotlin imports (MockK, Turbine, JUnit 4, Coroutines).
- Pre-configured `MainDispatcherRule`.
- Instantiated mocks with `@Before clearAllMocks()`.
- Traceable test stubs named `ac01_<scenario>_<outcome>()` matching each requirement in the spec.
