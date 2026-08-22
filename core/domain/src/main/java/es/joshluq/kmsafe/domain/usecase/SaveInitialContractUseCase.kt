package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject

class SaveInitialContractUseCase @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<SaveInitialContractUseCase.Input, SaveInitialContractUseCase.Output> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("SaveInitialContractUseCase", "Saving new contract: ${input.contract.vehicleName}")

        return authRepository.getCurrentUser().flatMapLatest { user ->
            // 1. Prepare Contract & Records
            val userId = user?.id ?: ""
            val contractId = input.contract.id.ifBlank { UUID.randomUUID().toString() }

            val initialRecord = OdometerRecord(
                id = UUID.randomUUID().toString(),
                contractId = contractId,
                timestamp = input.contract.startDate,
                odometerValue = input.contract.startOdometer,
                isInitialRecord = true
            )

            val contractToSave = input.contract.copy(
                id = contractId,
                userId = userId,
                isSelected = true
            )

            // 2. Save Records Locally FIRST (Crucial for reconciliation in saveContract sync)
            historyRepository.saveRecord(initialRecord)
            logger.i("SaveInitialContractUseCase", "Initial record saved locally")

            if (input.contract.currentOdometer > input.contract.startOdometer) {
                val currentRecord = OdometerRecord(
                    id = UUID.randomUUID().toString(),
                    contractId = contractId,
                    timestamp = System.currentTimeMillis(),
                    odometerValue = input.contract.currentOdometer,
                    isInitialRecord = false
                )
                historyRepository.saveRecord(currentRecord)
                logger.i("SaveInitialContractUseCase", "Current odometer record saved locally")
            }

            // 3. Save Contract (Triggers remote sync and ID reconciliation)
            rentingRepository.saveContract(contractToSave)
                .flatMapLatest { savedId ->
                    logger.i("SaveInitialContractUseCase", "Contract saved/synced with ID: $savedId")
                    rentingRepository.selectContract(savedId).map {
                        Output.Success as Output
                    }
                }
        }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("SaveInitialContractUseCase", "Failed to save contract", it)
                emit(Output.Failure)
            }
    }

    data class Input(val contract: RentingContract) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}
