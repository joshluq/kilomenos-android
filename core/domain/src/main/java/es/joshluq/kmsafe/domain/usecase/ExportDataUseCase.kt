package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.BackupData
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Domain interface to export application data.
 * Can export as JSON (full backup) or CSV (history only).
 */
interface ExportDataUseCase : FlowUseCase<ExportDataUseCase.Input, ExportDataUseCase.Output> {
    data class Input(val format: Format) : UseCaseInput

    enum class Format { JSON, CSV }

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data class Success(val content: String, val format: Format) : Output
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExportDataUseCaseImpl @Inject constructor(
    private val repository: DataManagementRepository,
    private val logger: LoggerKit
) : ExportDataUseCase {

    override fun invoke(input: ExportDataUseCase.Input): Flow<ExportDataUseCase.Output> {
        logger.d("ExportDataUseCase", "Exporting data in format: ${input.format}")
        return repository.getFullBackupData()
            .flatMapLatest { data ->
                flow {
                    val result = when (input.format) {
                        ExportDataUseCase.Format.JSON -> Json.encodeToString<BackupData>(data)
                        ExportDataUseCase.Format.CSV -> repository.generateHistoryCsv(data.history)
                    }
                    logger.i("ExportDataUseCase", "Export successful")
                    emit(ExportDataUseCase.Output.Success(result, input.format) as ExportDataUseCase.Output)
                }
            }
            .onStart { emit(ExportDataUseCase.Output.Progress) }
            .catch {
                logger.e("ExportDataUseCase", "Export failed: ${it.message}")
                emit(ExportDataUseCase.Output.Failure(it.message ?: "Unknown error"))
            }
    }
}
