# Kotlin Coroutines & Turbine StateFlow Testing Guide

## 1. Overview
Unit testing modern Android ViewModels requires managing Coroutines dispatchers and testing asynchronous `StateFlow` emissions without race conditions or flaky thread sleeps.

---

## 2. Dispatcher Management: MainDispatcherRule
Android's `Dispatchers.Main` relies on the Android OS Looper, which is unavailable in standard JVM unit tests. We must override it with a `TestDispatcher`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

---

## 3. CashApp Turbine Testing Pattern
Turbine provides a fluent, sequential assertion API for Kotlin Flows:

```kotlin
@Test
fun testProfileStateFlow() = runTest {
    viewModel.uiState.test {
        // Assert initial emission
        assertEquals(ProfileUiState.Loading, awaitItem())

        // Trigger ViewModel event
        viewModel.handleAction(ProfileUiAction.LoadProfile)

        // Assert subsequent emission
        val state = awaitItem()
        assertTrue(state is ProfileUiState.Success)

        // Crucial: always cancel to prevent infinite wait on hot flows!
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## 4. MockK with Suspending Functions
```kotlin
// Stubbing suspending functions
coEvery { repository.fetchUser("123") } returns Result.success(mockUser)

// Verifying suspending invocations
coVerify(exactly = 1) { repository.fetchUser("123") }
```
