package es.joshluq.kmsafe.feature.expenses.stations

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteServiceStationUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.SaveServiceStationUseCase
import es.joshluq.kmsafe.domain.usecase.SetFavoriteStationUseCase
import es.joshluq.kmsafe.domain.usecase.SyncStationsUseCase
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAllServiceStationsUseCase: GetAllServiceStationsUseCase = mockk()
    private val saveServiceStationUseCase: SaveServiceStationUseCase = mockk()
    private val deleteServiceStationUseCase: DeleteServiceStationUseCase = mockk()
    private val setFavoriteStationUseCase: SetFavoriteStationUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val syncStationsUseCase: SyncStationsUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleStation1 = ServiceStation(
        id = "st-1",
        name = "Repsol Diagonal",
        brand = "Repsol",
        latitude = 41.3879,
        longitude = 2.1699,
        address = "Av Diagonal 123",
        isFavorite = false,
        availableEnergies = listOf(FuelType.GASOLINE_95, FuelType.DIESEL)
    )

    private val sampleStation2 = ServiceStation(
        id = "st-2",
        name = "Cepsa Gracia",
        brand = "Cepsa",
        latitude = 41.4000,
        longitude = 2.1500,
        address = "Carrer Gran de Gracia 45",
        isFavorite = true,
        availableEnergies = listOf(FuelType.GASOLINE_95)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getAllServiceStationsUseCase(GetAllServiceStationsUseCase.Input) } returns flowOf(
            GetAllServiceStationsUseCase.Output.Success(listOf(sampleStation1, sampleStation2))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): StationManagementViewModel {
        return StationManagementViewModel(
            getAllServiceStationsUseCase = getAllServiceStationsUseCase,
            saveServiceStationUseCase = saveServiceStationUseCase,
            deleteServiceStationUseCase = deleteServiceStationUseCase,
            setFavoriteStationUseCase = setFavoriteStationUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            syncStationsUseCase = syncStationsUseCase,
            logger = logger
        )
    }

    @Test
    fun `given stations loaded when initialized then state contains stations list`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals(2, viewModel.state.value.stations.size)
        assertEquals(2, viewModel.state.value.filteredStations.size)
    }

    @Test
    fun `given search query changed then filters stations by name or brand`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(StationManagementEvent.OnSearchQueryChanged("Repsol"))
        advanceUntilIdle()

        assertEquals("Repsol", viewModel.state.value.searchQuery)
        assertEquals(1, viewModel.state.value.filteredStations.size)
        assertEquals("st-1", viewModel.state.value.filteredStations.first().id)
    }

    @Test
    fun `given toggle favorite then updates favorite state and calls use case`() = runTest(testDispatcher) {
        every { setFavoriteStationUseCase(SetFavoriteStationUseCase.Input("st-1", true)) } returns flowOf(
            SetFavoriteStationUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(StationManagementEvent.OnToggleFavorite("st-1", isFavorite = true))
        advanceUntilIdle()

        coVerify { setFavoriteStationUseCase(SetFavoriteStationUseCase.Input("st-1", true)) }
    }

    @Test
    fun `given delete confirmed when success then deletes station and reloads`() = runTest(testDispatcher) {
        every { deleteServiceStationUseCase(DeleteServiceStationUseCase.Input("st-1")) } returns flowOf(
            DeleteServiceStationUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(StationManagementEvent.OnDeleteStation("st-1"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showDeleteConfirmation)
        assertEquals("st-1", viewModel.state.value.deleteTargetId)

        viewModel.sendEvent(StationManagementEvent.OnConfirmDeleteStation)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showDeleteConfirmation)
        assertNull(viewModel.state.value.deleteTargetId)
        coVerify { deleteServiceStationUseCase(DeleteServiceStationUseCase.Input("st-1")) }
    }

    @Test
    fun `given view detail clicked then emits NavigateToDetail effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<StationManagementEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(StationManagementEvent.OnViewDetail("st-1"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(StationManagementEffect.NavigateToDetail("st-1"), effects.first())
    }
}
