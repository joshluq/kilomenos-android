# Living Specification: KILOMENOS-13
# Delta Specification: KILOMENOS-13

## 1. Specification Delta: Notification State & Navigation Flow

### Modified Component: `OverviewViewModel`
- **Method `observeNotifications()`**:
  - Filter: Keep only notifications satisfying `it.status == NotificationStatus.UNREAD && !it.isRead`.
  - Elimination of Fallback: Remove `?: output.notifications.firstOrNull()`.
- **Event `OnNotificationPillClicked`**:
  - State update: Immediately set `activeNotification = null`.
  - Effect dispatch: Immediately launch navigation effect (`Effect.NavigateToProjection` or `Effect.NavigateToOnboarding`).
  - Asynchronous Execution: Launch `markNotificationAsReadUseCase(notification.id)` inside `viewModelScope.launch(dispatcherProvider.io)` without blocking or awaiting completion before navigation.

### Modified Component: `NotificationRepositoryImpl`
- **Method `markAsRead(notificationId: String)`**:
  - When `response.isSuccessful == true`, invoke `notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")`.

---

## 2. Traceability Matrix
- **AC-01** $\rightarrow$ `OverviewViewModelTest.testActiveNotificationBecomesNullWhenNoUnreadExist()`
- **AC-02** $\rightarrow$ `OverviewViewModelTest.testPillClickEmitsImmediateNavigationEffectAndClearsPill()`
- **AC-03** $\rightarrow$ `NotificationRepositoryImplTest.testMarkAsReadUpdatesSyncStatusToSyncedOnSuccess()`
