package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.TripRoute
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject

class AddOdometerRecordUseCase @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository
) : FlowUseCase<AddOdometerRecordUseCase.Input, AddOdometerRecordUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        val contract = rentingRepository.getContract().firstOrNull()

        if (contract == null) {
            emit(Output.Failure("No renting contract found") as Output)
            return@flow
        }

        val record = OdometerRecord(
            id = UUID.randomUUID().toString(),
            contractId = contract.id,
            timestamp = input.timestamp,
            odometerValue = input.odometerValue,
            isInitialRecord = false,
            label = input.label,
            fuelAmount = input.fuelAmount,
            hasRoute = input.encodedPolyline != null
        )

        val route = input.encodedPolyline?.let {
            TripRoute(
                recordId = record.id,
                encodedPolyline = it,
                pointCount = input.pointCount ?: 0
            )
        }

        historyRepository.saveRecord(record, route)
        emit(Output.Success as Output)
    }
        .onStart { emit(Output.Progress) }
        .catch { emit(Output.Failure(it.message ?: "Unknown error")) }

    data class Input(
        val odometerValue: Int,
        val timestamp: Long,
        val label: String? = null,
        val fuelAmount: Double? = null,
        val encodedPolyline: String? = null,
        val pointCount: Int? = null
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        data class Failure(val message: String) : Output
        object Success : Output
    }
}
