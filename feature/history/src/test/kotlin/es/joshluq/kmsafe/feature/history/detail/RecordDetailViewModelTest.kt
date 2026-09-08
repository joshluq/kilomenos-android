package es.joshluq.kmsafe.feature.history.detail

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetRouteUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateOdometerRecordUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getOdometerRecordUseCase: GetOdometerRecordUseCase = mockk()
    private val deleteOdometerRecordUseCase: DeleteOdometerRecordUseCase = mockk()
    private val updateOdometerRecordUseCase: UpdateOdometerRecordUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val getRouteUseCase: GetRouteUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val prevRecord = OdometerRecord(
        id = "rec-prev",
        contractId = "c1",
        timestamp = 1000L,
        odometerValue = 10000.0,
        isInitialRecord = true
    )

    private val currentRecord = OdometerRecord(
        id = "rec-target",
        contractId = "c1",
        timestamp = 2000L,
        odometerValue = 10500.0,
        isInitialRecord = false,
        fuelAmount = 35.0,
        label = "Madrid Trip"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getOdometerRecordUseCase(GetOdometerRecordUseCase.Input("rec-target")) } returns flowOf(
            GetOdometerRecordUseCase.Output.Success(currentRecord, prevRecord)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(recordId: String = "rec-target"): RecordDetailViewModel {
        return RecordDetailViewModel(
            recordId = recordId,
            getOdometerRecordUseCase = getOdometerRecordUseCase,
            deleteOdometerRecordUseCase = deleteOdometerRecordUseCase,
            updateOdometerRecordUseCase = updateOdometerRecordUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            getRouteUseCase = getRouteUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given record loaded when initialized then state contains record and calculated consumption`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals(currentRecord, viewModel.state.value.record)
        assertEquals(prevRecord, viewModel.state.value.previousRecord)
        // Consumption = (35.0 / (10500 - 10000)) * 100 = 7.0 L/100km
        assertEquals(7.0, viewModel.state.value.consumptionL100km ?: 0.0, 0.01)
    }

    @Test
    fun `given edit clicked event then opens edit dialog in state with current values`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(RecordDetailEvent.OnEditClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showEditDialog)
        assertEquals("10500.0", viewModel.state.value.editingOdometerValue)
        assertEquals("Madrid Trip", viewModel.state.value.editingLabel)
        assertEquals("35.0", viewModel.state.value.editingFuel)
    }

    @Test
    fun `given update record when saved then updates state and reloads record`() = runTest(testDispatcher) {
        every { updateOdometerRecordUseCase(any()) } returns flowOf(
            UpdateOdometerRecordUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(RecordDetailEvent.OnEditClicked)
        viewModel.sendEvent(RecordDetailEvent.OnEditingLabelChanged("Madrid Trip Updated"))
        viewModel.sendEvent(RecordDetailEvent.OnUpdateRecordClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showEditDialog)
        coVerify { updateOdometerRecordUseCase(any()) }
    }

    @Test
    fun `given delete confirmed when success then deletes record and navigates back`() = runTest(testDispatcher) {
        every { deleteOdometerRecordUseCase(any()) } returns flowOf(
            DeleteOdometerRecordUseCase.Output.Success
        )

        val effects = mutableListOf<RecordDetailEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(RecordDetailEvent.OnConfirmDelete)
        advanceUntilIdle()

        coVerify { deleteOdometerRecordUseCase(DeleteOdometerRecordUseCase.Input(currentRecord)) }
        assertEquals(1, effects.size)
        assertEquals(RecordDetailEffect.NavigateBack, effects.first())
    }

    @Test
    fun `given back clicked event then emits NavigateBack effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<RecordDetailEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(RecordDetailEvent.OnBackClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(RecordDetailEffect.NavigateBack, effects.first())
    }
}
