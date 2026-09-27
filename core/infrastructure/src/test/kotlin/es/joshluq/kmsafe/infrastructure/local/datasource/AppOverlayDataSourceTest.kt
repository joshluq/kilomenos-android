package es.joshluq.kmsafe.infrastructure.local.datasource

import es.joshluq.kmsafe.domain.model.AppOverlayState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppOverlayDataSourceTest {

    private lateinit var dataSource: AppOverlayDataSource

    @Before
    fun setUp() {
        dataSource = AppOverlayDataSource()
    }

    @Test
    fun `initial overlay state is None`() = runTest {
        assertEquals(AppOverlayState.None, dataSource.observeOverlay().first())
    }

    @Test
    fun `updateOverlay updates overlay state`() = runTest {
        dataSource.updateOverlay(AppOverlayState.LoggingOut)
        assertEquals(AppOverlayState.LoggingOut, dataSource.observeOverlay().first())
    }

    @Test
    fun `updateOverlay does not overwrite LoggingOut with SubscriptionDowngraded`() = runTest {
        dataSource.updateOverlay(AppOverlayState.LoggingOut)
        assertEquals(AppOverlayState.LoggingOut, dataSource.observeOverlay().first())

        dataSource.updateOverlay(AppOverlayState.SubscriptionDowngraded())
        assertEquals(AppOverlayState.LoggingOut, dataSource.observeOverlay().first())
    }

    @Test
    fun `updateOverlay does not overwrite AccountDeletion with SubscriptionDowngraded`() = runTest {
        dataSource.updateOverlay(AppOverlayState.AccountDeletion())
        assertEquals(AppOverlayState.AccountDeletion(), dataSource.observeOverlay().first())

        dataSource.updateOverlay(AppOverlayState.SubscriptionDowngraded())
        assertEquals(AppOverlayState.AccountDeletion(), dataSource.observeOverlay().first())
    }

    @Test
    fun `updateOverlay allows clearing LoggingOut with None`() = runTest {
        dataSource.updateOverlay(AppOverlayState.LoggingOut)
        assertEquals(AppOverlayState.LoggingOut, dataSource.observeOverlay().first())

        dataSource.updateOverlay(AppOverlayState.None)
        assertEquals(AppOverlayState.None, dataSource.observeOverlay().first())
    }
}
