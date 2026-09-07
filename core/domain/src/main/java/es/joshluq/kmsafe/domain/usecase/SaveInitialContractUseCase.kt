package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

/**
 * Domain interface to validate and save an initial renting contract and its odometer record.
 */
interface SaveInitialContractUseCase : FlowUseCase<SaveInitialContractUseCase.Input, SaveInitialContractUseCase.Output> {

    data class Input(val contract: RentingContract) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data class Success(val contractId: String) : Output
    }
}

class SaveInitialContractUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
    private val entitlementsRepository: EntitlementsRepository,
    private val logger: LoggerKit
) : SaveInitialContractUseCase {

    override fun invoke(input: SaveInitialContractUseCase.Input): Flow<SaveInitialContractUseCase.Output> = flow {
        emit(SaveInitialContractUseCase.Output.Progress)
        
        // 1. Authenticated User Check
        val user = authRepository.getCurrentUser().first()
        val userId = user?.id
        if (userId.isNullOrBlank()) {
            logger.w("SaveInitialContractUseCase", "Aborting: No authenticated user found")
            emit(SaveInitialContractUseCase.Output.Failure(KmError.Unauthenticated))
            return@flow
        }

        // 2. Entitlement Check: Multi-vehicle restriction for free users
        val existingContracts = rentingRepository.getAllContracts().first()
        if (existingContracts.isNotEmpty()) {
            val entitlements = entitlementsRepository.observeEntitlements().first()
            if (!entitlements.isFeatureActive(Feature.MULTI_VEHICLE)) {
                logger.w("SaveInitialContractUseCase", "Aborting: Multi-vehicle fleet not allowed for free user with existing contracts")
                emit(SaveInitialContractUseCase.Output.Failure(KmError.MultiVehicleLimitReached))
                return@flow
            }
        }

        // 3. Domain Validation
        val contract = input.contract
        if (contract.vehicleName.isBlank()) {
            logger.w("SaveInitialContractUseCase", "Aborting: Vehicle name is blank")
            emit(SaveInitialContractUseCase.Output.Failure(KmError.InvalidVehicleName))
            return@flow
        }

        if (contract.totalKms <= 0 || contract.durationMonths <= 0) {
            logger.w("SaveInitialContractUseCase", "Aborting: Invalid contract metrics (kms: ${contract.totalKms}, months: ${contract.durationMonths})")
            emit(SaveInitialContractUseCase.Output.Failure(KmError.InvalidContractMetrics))
            return@flow
        }
        
        val contractId = input.contract.id.ifBlank { UUID.randomUUID().toString() }
        logger.d("SaveInitialContractUseCase", "Executing save for contract ID: $contractId")

        val initialRecord = OdometerRecord(
            id = UUID.randomUUID().toString(),
            contractId = contractId,
            timestamp = input.contract.startDate,
            odometerValue = input.contract.startOdometer,
            isInitialRecord = true
        )

        // 3. Initial Baseline Record Creation
        val contractToSave = contract.copy(
            id = contractId,
            userId = userId
        )

        historyRepository.saveRecord(initialRecord)
        logger.i("SaveInitialContractUseCase", "Initial baseline record saved locally")

        // 3.1 Offset Record Creation if current odometer is greater than start odometer
        if (contractToSave.currentOdometer > contractToSave.startOdometer) {
            val drivenOffset = contractToSave.currentOdometer - contractToSave.startOdometer
            logger.d("SaveInitialContractUseCase", "Detected offset of $drivenOffset kms. Creating initial trip record.")
            
            val currentRecord = OdometerRecord(
                id = UUID.randomUUID().toString(),
                contractId = contractId,
                timestamp = System.currentTimeMillis(),
                odometerValue = drivenOffset,
                isInitialRecord = false
            )
            historyRepository.saveRecord(currentRecord)
            logger.i("SaveInitialContractUseCase", "Current odometer offset record saved locally")
        }

        // 4. Save Contract & Remote Sync
        val finalId = rentingRepository.saveContract(contractToSave).first()
        logger.i("SaveInitialContractUseCase", "Contract saved/synced with ID: $finalId")

        // 5. Activation
        rentingRepository.selectContract(finalId).first()
        emit(SaveInitialContractUseCase.Output.Success(finalId))
        
    }.catch { e ->
        logger.e("SaveInitialContractUseCase", "Failed to save contract", e)
        emit(SaveInitialContractUseCase.Output.Failure(KmError.UnknownError))
    }
}
