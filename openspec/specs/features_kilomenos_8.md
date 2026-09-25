# Living Specification: KILOMENOS-8
# Delta Specification: KILOMENOS-8

## 1. Specification Delta: Notification Navigation

### Modified Interface: `NotificationsListEffect`
- Add `NavigateToProjection : NotificationsListEffect`
- Add `NavigateToEditContract(val vehicleId: String) : NotificationsListEffect`

### Modified Class: `NotificationsListViewModel`
- In `handleNotificationClick(notification: Notification)`:
  - Add dispatch for `NavigateToProjection` when `notification.topic == NotificationTopic.PROJECTION`.
  - Add dispatch for `NavigateToEditContract(contractId)` when `notification.topic == NotificationTopic.SYSTEM`.
  - Add dispatch for `NavigateToPaywall` when `notification.topic == NotificationTopic.SUBSCRIPTION`.
  - Omit navigation to `NavigateToDetail` by default for these topics.
  - Launch `markNotificationAsReadUseCase` asynchronously in background coroutine without awaiting its network response.

### Modified Class: `NotificationsListRoute`
- Support parameters `onNavigateToProjection: () -> Unit` and `onNavigateToEditContract: (String) -> Unit`.
- Collect and forward new effects to corresponding lambda handlers.

### Modified Class: `AppNavigation`
- Bind `onNavigateToProjection` to `onNavigate(Destination.ProjectionAnalysis)`.
- Bind `onNavigateToEditContract` to `onNavigate(Destination.Onboarding(vehicleId = vehicleId, isEdit = true))`.

---

## 2. Traceability Matrix
- **AC-01** $\rightarrow$ `NotificationsListViewModelTest.testProjectionNotificationClickedEmitsNavigateToProjection()`
- **AC-02** $\rightarrow$ `NotificationsListViewModelTest.testSystemBluetoothNotificationClickedEmitsNavigateToEditContract()`
- **AC-03** $\rightarrow$ `NotificationsListViewModelTest.testNotificationClickedDispatchesImmediateEffectAndAsyncMarkAsRead()`
- **AC-04** $\rightarrow$ `NotificationsListViewModelTest.testSubscriptionNotificationClickedEmitsNavigateToPaywall()`
- **AC-05** $\rightarrow$ `NotificationsListViewModelTest.testNotificationClickedUpdatesReadStatusInDao()`
