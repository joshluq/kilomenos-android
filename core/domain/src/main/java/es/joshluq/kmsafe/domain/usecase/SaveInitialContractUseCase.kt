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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    override fun invoke(input: Input): Flow<Output> = flow {
        emit(Output.Progress)
        
        // 1. Get current user once. We don't want to restart if the user flow emits again.
        val user = authRepository.getCurrentUser().first()
        val userId = user?.id ?: ""
        
        // 2. Generate a fixed ID for this operation to avoid multiple vehicles if retried/restarted
        val contractId = input.contract.id.ifBlank { UUID.randomUUID().toString() }
        logger.d("SaveInitialContractUseCase", "Executing save for contract ID: $contractId")

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

        // 3. Save Records Locally FIRST (Crucial for reconciliation in saveContract sync)
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

        // 4. Save Contract (Triggers remote sync)
        // We collect the flow from repository and wait for the final ID (could be remote ID)
        val finalId = rentingRepository.saveContract(contractToSave).first()
        logger.i("SaveInitialContractUseCase", "Contract saved/synced with ID: $finalId")

        // 5. Ensure it's selected and emit success
        rentingRepository.selectContract(finalId).first()
        emit(Output.Success(finalId))
        
    }.catch { e ->
        logger.e("SaveInitialContractUseCase", "Failed to save contract", e)
        emit(Output.Failure)
    }

    data class Input(val contract: RentingContract) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data class Success(val contractId: String) : Output
    }
}
