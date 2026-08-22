package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.StationPriceVolatility
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to compute price volatility and savings insight for a given station and fuel type.
 */
class GetStationVolatilityUseCase @Inject constructor(
    private val stationRepository: ServiceStationRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetStationVolatilityUseCase.Input, GetStationVolatilityUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetStationVolatilityUseCase", "Computing volatility for station: ${input.stationId}, fuel: ${input.fuelType}")

        return stationRepository.getStationVolatility(input.stationId, input.fuelType)
            .map { volatility ->
                if (volatility != null) {
                    Output.Success(volatility)
                } else {
                    Output.Empty
                }
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("GetStationVolatilityUseCase", "Error computing station volatility", e)
                emit(Output.Failure)
            }
    }

    data class Input(
        val stationId: String,
        val fuelType: FuelType
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Empty : Output
        data class Success(val volatility: StationPriceVolatility) : Output
    }
}
