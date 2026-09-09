package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import es.joshluq.kmsafe.infrastructure.local.datasource.AppOverlayDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateless implementation of [AppOverlayRepository] delegating to [AppOverlayDataSource].
 */
@Singleton
class AppOverlayRepositoryImpl @Inject constructor(
    private val appOverlayDataSource: AppOverlayDataSource
) : AppOverlayRepository {

    override fun observeOverlay(): Flow<AppOverlayState> =
        appOverlayDataSource.observeOverlay()

    override suspend fun setOverlay(state: AppOverlayState) {
        appOverlayDataSource.updateOverlay(state)
    }

    override suspend fun clearOverlay() {
        appOverlayDataSource.updateOverlay(AppOverlayState.None)
    }
}
