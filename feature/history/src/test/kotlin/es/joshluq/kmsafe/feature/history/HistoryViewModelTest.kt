package es.joshluq.kmsafe.feature.history

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RecordWithIndicator
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetHistoryUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getHistoryUseCase: GetHistoryUseCase = mockk()
    private val deleteOdometerRecordUseCase: DeleteOdometerRecordUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val monetizationConfig: MonetizationConfig = mockk()
    private val dispatchers: DispatcherProvider = mockk {
        every { main } returns testDispatcher
        every { io } returns testDispatcher
        every { default } returns testDispatcher
        every { unconfined } returns testDispatcher
    }
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleRecord1 = OdometerRecord(
        id = "rec-1",
        contractId = "contract-1",
        timestamp = 1700000000000L,
        odometerValue = 100.0,
        isInitialRecord = false,
        label = "Commute to work"
    )

    private val sampleRecord2 = OdometerRecord(
        id = "rec-2",
        contractId = "contract-1",
        timestamp = 1700100000000L,
        odometerValue = 150.0,
        isInitialRecord = false,
        label = "Weekend getaway"
    )

    private val sampleRecordsWithIndicator = listOf(
        RecordWithIndicator(sampleRecord1, isOverLimit = false),
        RecordWithIndicator(sampleRecord2, isOverLimit = true)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { monetizationConfig.getBannerAdUnitId() } returns "ad_unit_123"
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getHistoryUseCase(GetHistoryUseCase.Input(forceRefresh = false)) } returns flowOf(
            GetHistoryUseCase.Output.Success(
                contractId = "contract-1",
                initialRecord = null,
                allRecords = sampleRecordsWithIndicator,
                totalKms = 250.0,
                totalRecordsCount = 2
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            getHistoryUseCase = getHistoryUseCase,
            deleteOdometerRecordUseCase = deleteOdometerRecordUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            monetizationConfig = monetizationConfig,
            dispatchers = dispatchers,
            logger = logger
        )
    }

    @Test
    fun `given history loaded when initialized then state contains records and groups`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals(2, viewModel.state.value.allRecords.size)
        assertEquals(250.0, viewModel.state.value.totalKms, 0.01)
        assertEquals(2, viewModel.state.value.totalRecordsCount)
        assertEquals("ad_unit_123", viewModel.state.value.adUnitId)
        assertTrue(viewModel.state.value.filteredGroups.isNotEmpty())
    }

    @Test
    fun `given search query changed then filters records by label`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(HistoryEvent.OnSearchQueryChanged("Commute"))
        advanceUntilIdle()

        assertEquals("Commute", viewModel.state.value.searchQuery)
        val matchingRecords = viewModel.state.value.filteredGroups.values.flatten()
        assertEquals(1, matchingRecords.size)
        assertEquals("rec-1", matchingRecords.first().record.id)
    }

    @Test
    fun `given grouping mode changed then regroups filtered records`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(HistoryEvent.OnGroupingModeChanged(HistoryGroupingMode.DAY))
        advanceUntilIdle()

        assertEquals(HistoryGroupingMode.DAY, viewModel.state.value.groupingMode)
        assertTrue(viewModel.state.value.filteredGroups.isNotEmpty())
    }

    @Test
    fun `given toggle group expansion event then updates expandedGroups in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val groupKey = viewModel.state.value.filteredGroups.keys.first()
        viewModel.sendEvent(HistoryEvent.OnToggleGroupExpansion(groupKey))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.expandedGroups.contains(groupKey))

        viewModel.sendEvent(HistoryEvent.OnToggleGroupExpansion(groupKey))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.expandedGroups.contains(groupKey))
    }

    @Test
    fun `given view detail event then updates selectedDailyRecords in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(HistoryEvent.OnViewDetail(sampleRecordsWithIndicator))
        advanceUntilIdle()

        assertEquals(sampleRecordsWithIndicator, viewModel.state.value.selectedDailyRecords)

        viewModel.sendEvent(HistoryEvent.OnDismissDetail)
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedDailyRecords)
    }

    @Test
    fun `given record clicked then emits NavigateToDetail effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<HistoryEffect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        advanceUntilIdle()

        viewModel.sendEvent(HistoryEvent.OnRecordClicked(sampleRecordsWithIndicator.first()))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(HistoryEffect.NavigateToDetail("rec-1"), effects.first())
    }

    @Test
    fun `given delete records event then calls deleteOdometerRecordUseCase`() = runTest(testDispatcher) {
        every { deleteOdometerRecordUseCase(any()) } returns flowOf(
            DeleteOdometerRecordUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(HistoryEvent.OnDeleteRecords(listOf(sampleRecord1)))
        advanceUntilIdle()

        coVerify { deleteOdometerRecordUseCase(DeleteOdometerRecordUseCase.Input(sampleRecord1)) }
    }
}
