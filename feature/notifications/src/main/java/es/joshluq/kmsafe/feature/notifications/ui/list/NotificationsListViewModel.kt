package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.model.contractId
import es.joshluq.kmsafe.domain.model.isForVehicle
import es.joshluq.kmsafe.domain.usecase.DeleteNotificationUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.MarkAllNotificationsAsReadUseCase
import es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveActiveNotificationsUseCase
import es.joshluq.kmsafe.domain.usecase.SyncNotificationsUseCase
import es.joshluq.kmsafe.feature.notifications.ui.mapper.NotificationTextResolver
import es.joshluq.kmsafe.feature.notifications.ui.model.NotificationUiItem
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI ViewModel managing state for [NotificationsListScreen].
 * Integrates Local-First Room observation with remote sync and Pro paywall handling.
 */
@HiltViewModel
class NotificationsListViewModel @Inject constructor(
    private val observeActiveNotificationsUseCase: ObserveActiveNotificationsUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val markAllNotificationsAsReadUseCase: MarkAllNotificationsAsReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val syncNotificationsUseCase: SyncNotificationsUseCase,
    private val getRentingContractUseCase: GetRentingContractUseCase,
    private val getAllContractsUseCase: GetAllContractsUseCase,
    private val logger: LoggerKit
) : ScreenViewModel<NotificationsListState, NotificationsListEvent, NotificationsListEffect>() {

    override fun createInitialState(): NotificationsListState = NotificationsListState.Empty

    private var allNotifications: List<Notification> = emptyList()

    init {
        observeNotifications()
        observeContracts()
        triggerSync()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            observeActiveNotificationsUseCase(ObserveActiveNotificationsUseCase.Input())
                .catch { e ->
                    logger.e("NotificationsListViewModel", "Error observing notifications", e)
                    updateState { copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { output ->
                    when (output) {
                        is ObserveActiveNotificationsUseCase.Output.Success -> {
                            allNotifications = output.notifications
                            applyFiltersAndEmit()
                        }
                    }
                }
        }
    }

    private fun observeContracts() {
        viewModelScope.launch {
            getRentingContractUseCase(GetRentingContractUseCase.Input)
                .catch { e -> logger.e("NotificationsListViewModel", "Error observing active contract", e) }
                .collect { output ->
                    if (output is GetRentingContractUseCase.Output.Success) {
                        updateState { copy(activeVehicleId = output.contract.id) }
                        applyFiltersAndEmit()
                    }
                }
        }
        viewModelScope.launch {
            getAllContractsUseCase(GetAllContractsUseCase.Input)
                .catch { e -> logger.e("NotificationsListViewModel", "Error observing all contracts", e) }
                .collect { output ->
                    if (output is GetAllContractsUseCase.Output.Success) {
                        val names = output.contracts.associate { it.id to it.vehicleName }
                        updateState { copy(vehicleNames = names) }
                        applyFiltersAndEmit()
                    }
                }
        }
    }

    private fun triggerSync() {
        viewModelScope.launch {
            updateState { copy(isSyncing = true) }
            val result = syncNotificationsUseCase(SyncNotificationsUseCase.Input)
            result.fold(
                onSuccess = { output ->
                    when (output) {
                        is SyncNotificationsUseCase.Output.Success -> {
                            updateState { copy(isSyncing = false) }
                        }
                        SyncNotificationsUseCase.Output.PremiumRequired -> {
                            updateState {
                                copy(
                                    isSyncing = false,
                                    isPremiumRequiredBannerVisible = true
                                )
                            }
                        }
                        is SyncNotificationsUseCase.Output.Failure -> {
                            updateState { copy(isSyncing = false) }
                        }
                    }
                },
                onFailure = {
                    updateState { copy(isSyncing = false) }
                }
            )
        }
    }

    private fun applyFiltersAndEmit() {
        val topic = state.value.selectedTopic
        val filtered = if (topic == null) {
            allNotifications
        } else {
            allNotifications.filter { it.topic == topic }
        }
        val unread = allNotifications.count { it.status == NotificationStatus.UNREAD || !it.isRead }
        val activeVehicleId = state.value.activeVehicleId
        val vehicleNames = state.value.vehicleNames

        val uiItems = filtered.map { notif ->
            val texts = NotificationTextResolver.resolve(notif)
            val vehicleId = notif.data?.get("contract_id") as? String
            val vehicleName = vehicleId?.let { vehicleNames[it] }
            val isForActiveVehicle = vehicleId == null || activeVehicleId == null || vehicleId == activeVehicleId

            NotificationUiItem(
                id = notif.id,
                notification = notif,
                title = texts.title,
                body = texts.body,
                actionLabel = texts.actionLabel,
                topicLabel = texts.topicLabel,
                topic = notif.topic,
                priority = notif.priority,
                status = notif.status,
                isRead = notif.isRead,
                timestampMillis = notif.timestampMillis,
                vehicleId = vehicleId,
                vehicleName = vehicleName,
                isForActiveVehicle = isForActiveVehicle
            )
        }

        updateState {
            copy(
                isLoading = false,
                unreadCount = unread,
                notifications = uiItems
            )
        }
    }

    override fun handleEvent(event: NotificationsListEvent) {
        logger.d("NotificationsListViewModel", "Handling event: $event")
        when (event) {
            is NotificationsListEvent.TopicSelected -> {
                updateState { copy(selectedTopic = event.topic) }
                applyFiltersAndEmit()
            }
            is NotificationsListEvent.NotificationClicked -> {
                handleNotificationClick(event.notification)
            }
            is NotificationsListEvent.MarkAsReadClicked -> {
                viewModelScope.launch {
                    markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(event.notificationId))
                }
            }
            NotificationsListEvent.MarkAllAsReadClicked -> {
                viewModelScope.launch {
                    markAllNotificationsAsReadUseCase(MarkAllNotificationsAsReadUseCase.Input)
                }
            }
            is NotificationsListEvent.DeleteNotificationClicked -> {
                viewModelScope.launch {
                    deleteNotificationUseCase(DeleteNotificationUseCase.Input(event.notificationId))
                }
            }
            NotificationsListEvent.DismissPremiumBanner -> {
                updateState { copy(isPremiumRequiredBannerVisible = false) }
            }
            NotificationsListEvent.UpgradeToProClicked -> {
                launchEffect(NotificationsListEffect.NavigateToPaywall)
            }
            NotificationsListEvent.Refresh -> {
                triggerSync()
            }
            NotificationsListEvent.DismissError -> {
                updateState { copy(errorMessage = null) }
            }
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        allNotifications = allNotifications.map {
            if (it.id == notification.id) it.copy(status = NotificationStatus.READ, isRead = true) else it
        }
        applyFiltersAndEmit()

        when (notification.topic) {
            NotificationTopic.PROJECTION -> {
                navigateIfForCurrentVehicle(notification, "Projection") {
                    launchEffect(NotificationsListEffect.NavigateToProjection)
                }
            }
            NotificationTopic.SYSTEM -> {
                navigateIfForCurrentVehicle(notification, "system") { contractId ->
                    if (contractId == null) return@navigateIfForCurrentVehicle
                    launchEffect(NotificationsListEffect.NavigateToEditContract(contractId))
                }
            }
            NotificationTopic.SUBSCRIPTION -> {
                launchEffect(NotificationsListEffect.NavigateToPaywall)
            }
            else -> {
                val deepLink = notification.deepLinkUri
                if (!deepLink.isNullOrBlank()) {
                    launchEffect(NotificationsListEffect.NavigateToDeepLink(deepLink))
                }
            }
        }

        viewModelScope.launch {
            runCatching {
                markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(notification.id))
            }
        }
    }

    private inline fun navigateIfForCurrentVehicle(
        notification: Notification,
        topicLabel: String,
        onNavigate: (contractId: String?) -> Unit
    ) {
        val activeVehicleId = state.value.activeVehicleId
        if (notification.isForVehicle(activeVehicleId)) {
            onNavigate(notification.contractId)
        } else {
            logger.d(
                "NotificationsListViewModel",
                "Skipping $topicLabel navigation for non-active vehicle: ${notification.contractId} (active: $activeVehicleId)"
            )
        }
    }

    // Compatibility delegates
    val uiState get() = state
    fun onAction(action: NotificationsListEvent) = sendEvent(action)
}
