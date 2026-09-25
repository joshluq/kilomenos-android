package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain use case to observe active notifications, optionally filtered by topic.
 * Conforms to FoundationKit FlowUseCase architecture.
 */
interface ObserveActiveNotificationsUseCase :
    FlowUseCase<ObserveActiveNotificationsUseCase.Input, ObserveActiveNotificationsUseCase.Output> {

    data class Input(val topic: NotificationTopic? = null) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val notifications: List<Notification>) : Output
    }
}

class ObserveActiveNotificationsUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : ObserveActiveNotificationsUseCase {

    override fun invoke(input: ObserveActiveNotificationsUseCase.Input): Flow<ObserveActiveNotificationsUseCase.Output> {
        return repository.observeNotifications(input.topic).map { list ->
            ObserveActiveNotificationsUseCase.Output.Success(list)
        }
    }
}
