package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import es.joshluq.kmsafe.domain.service.DeviceTokenProvider
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import javax.inject.Inject

/**
 * Domain use case to register the device push token (FCM) and hardware fingerprint
 * with the backend database.
 * Conforms strictly to FoundationKit UseCase architecture.
 */
interface RegisterDeviceTokenUseCase :
    UseCase<RegisterDeviceTokenUseCase.Input, RegisterDeviceTokenUseCase.Output> {

    data class Input(val token: String? = null) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class RegisterDeviceTokenUseCaseImpl @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val deviceTokenProvider: DeviceTokenProvider,
    private val fingerprintProvider: FingerprintProvider
) : RegisterDeviceTokenUseCase {

    override suspend fun invoke(input: RegisterDeviceTokenUseCase.Input): Result<RegisterDeviceTokenUseCase.Output> {
        val tokenToRegister = input.token ?: deviceTokenProvider.getDeviceToken()
        if (tokenToRegister.isNullOrBlank()) {
            return Result.failure(IllegalStateException("FCM Token is null or blank"))
        }

        val deviceId = fingerprintProvider.getFingerprint()
        val result = notificationRepository.registerDeviceToken(
            token = tokenToRegister,
            deviceId = deviceId
        )

        return result.map { RegisterDeviceTokenUseCase.Output.Success }
    }
}
