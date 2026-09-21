# Jetpack Compose UI Testing Guide

## 1. Overview
Jetpack Compose tests do not inspect Android View hierarchies directly; instead, they operate on the Compose Semantics Tree.

---

## 2. Test Rules & Setup

### 2.1 AndroidJUnit4 vs Robolectric
```kotlin
// For Android Device / Emulator:
@RunWith(AndroidJUnit4::class)
class MyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
}

// For Headless Fast JVM Execution:
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MyScreenRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()
}
```

---

## 3. Semantics Tree Matchers
- `hasTestTag("tag_name")`: Matches composables with `Modifier.testTag("tag_name")`.
- `hasText("Label", ignoreCase = true)`: Matches visible or semantics text.
- `hasContentDescription("Description")`: Matches accessibility labels.
- `hasClickAction()`: Matches clickable components.
- `hasSetTextAction()`: Matches editable text fields.

---

## 4. Actions & Assertions
```kotlin
// Find node and assert visibility
composeTestRule.onNodeWithTag("submit_button")
    .assertIsDisplayed()
    .assertIsEnabled()
    .performClick()

// Text input
composeTestRule.onNodeWithTag("username_field")
    .performTextInput("Jane Doe")

// Verification of absence
composeTestRule.onNodeWithTag("loading_spinner")
    .assertDoesNotExist()
```
