package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.BackupData
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Domain interface to import application data from a JSON backup string.
 */
interface ImportDataUseCase : FlowUseCase<ImportDataUseCase.Input, ImportDataUseCase.Output> {
    data class Input(val jsonContent: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data object Success : Output
    }
}

class ImportDataUseCaseImpl @Inject constructor(
    private val repository: DataManagementRepository,
    private val logger: LoggerKit
) : ImportDataUseCase {

    override fun invoke(input: ImportDataUseCase.Input): Flow<ImportDataUseCase.Output> = flow {
        logger.d("ImportDataUseCase", "Starting data import")
        try {
            val backupData = Json.decodeFromString<BackupData>(input.jsonContent)
            logger.i("ImportDataUseCase", "Parsed ${backupData.contracts.size} contracts from JSON")
            repository.restoreFromBackup(backupData)
                .map {
                    logger.i("ImportDataUseCase", "Restore successful")
                    ImportDataUseCase.Output.Success as ImportDataUseCase.Output
                }
                .collect { emit(it) }
        } catch (e: Exception) {
            logger.e("ImportDataUseCase", "Restore failed: ${e.message}")
            emit(ImportDataUseCase.Output.Failure(e.message ?: "Invalid backup file"))
        }
    }
        .onStart { emit(ImportDataUseCase.Output.Progress) }
        .catch {
            logger.e("ImportDataUseCase", "Import failed with error", it)
            emit(ImportDataUseCase.Output.Failure(it.message ?: "Unknown error"))
        }
}
