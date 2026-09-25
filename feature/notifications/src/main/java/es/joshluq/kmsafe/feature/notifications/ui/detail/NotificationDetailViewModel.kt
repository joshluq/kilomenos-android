package es.joshluq.kmsafe.feature.notifications.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.usecase.GetNotificationByIdUseCase
import es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI ViewModel managing state for [NotificationDetailScreen].
 * Automatically marks the notification as READ upon opening.
 * Inherits [ScreenViewModel] adhering to FoundationKit architecture standards.
 */
@HiltViewModel
class NotificationDetailViewModel @Inject constructor(
    private val getNotificationByIdUseCase: GetNotificationByIdUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val logger: LoggerKit,
    savedStateHandle: SavedStateHandle
) : ScreenViewModel<NotificationDetailState, NotificationDetailEvent, NotificationDetailEffect>() {

    override fun createInitialState(): NotificationDetailState = NotificationDetailState.Empty

    init {
        val notificationId: String? = savedStateHandle["notificationId"]
        if (!notificationId.isNullOrBlank()) {
            loadNotification(notificationId)
        }
    }

    fun loadNotification(notificationId: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val result = getNotificationByIdUseCase(GetNotificationByIdUseCase.Input(notificationId))
            val notification = result.getOrNull()?.let {
                (it as? GetNotificationByIdUseCase.Output.Success)?.notification
            }
            if (notification != null) {
                // Auto-mark as read upon viewing
                markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(notificationId))
                updateState {
                    copy(
                        isLoading = false,
                        notification = notification,
                        errorMessage = null
                    )
                }
            } else {
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "Notificación no encontrada"
                    )
                }
            }
        }
    }

    override fun handleEvent(event: NotificationDetailEvent) {
        logger.d("NotificationDetailViewModel", "Handling event: $event")
        when (event) {
            NotificationDetailEvent.PrimaryActionClicked -> {
                val uri = state.value.notification?.deepLinkUri
                if (!uri.isNullOrBlank()) {
                    launchEffect(NotificationDetailEffect.NavigateToDeepLink(uri))
                }
            }
            NotificationDetailEvent.NavigateBackClicked -> {
                launchEffect(NotificationDetailEffect.NavigateBack)
            }
            NotificationDetailEvent.DismissError -> {
                updateState { copy(errorMessage = null) }
            }
        }
    }

    // Compatibility delegates
    val uiState get() = state
    fun onAction(action: NotificationDetailEvent) = sendEvent(action)
}
