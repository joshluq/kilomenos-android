package es.joshluq.kmsafe.ui.datamanagement

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.ExportDataUseCase
import es.joshluq.kmsafe.domain.usecase.ImportDataUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class DataManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val exportDataUseCase: ExportDataUseCase = mockk()
    private val importDataUseCase: ImportDataUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DataManagementViewModel {
        return DataManagementViewModel(
            exportDataUseCase = exportDataUseCase,
            importDataUseCase = importDataUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `init checks feature access and sets isPremium`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isPremium)
    }

    @Test
    fun `export CSV succeeds and emits CreateFile effect`() = runTest(testDispatcher) {
        every { exportDataUseCase(ExportDataUseCase.Input(ExportDataUseCase.Format.CSV)) } returns flowOf(
            ExportDataUseCase.Output.Progress,
            ExportDataUseCase.Output.Success("col1,col2\nval1,val2", ExportDataUseCase.Format.CSV)
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnExportClicked(ExportDataUseCase.Format.CSV))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("col1,col2\nval1,val2", viewModel.state.value.lastExportedContent)
        assertEquals(1, effects.size)
        assertTrue(effects.first() is Effect.CreateFile)
        assertTrue((effects.first() as Effect.CreateFile).filename.endsWith(".csv"))
        verify { exportDataUseCase(ExportDataUseCase.Input(ExportDataUseCase.Format.CSV)) }
    }

    @Test
    fun `export JSON for free user shows premium limit`() = runTest(testDispatcher) {
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = false)
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isPremium)

        viewModel.sendEvent(Event.OnExportClicked(ExportDataUseCase.Format.JSON))
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.showPremiumLimit)

        viewModel.sendEvent(Event.OnDismissPremiumLimit)
        assertFalse(viewModel.state.value.showPremiumLimit)
    }

    @Test
    fun `export failure updates state with error`() = runTest(testDispatcher) {
        every { exportDataUseCase(ExportDataUseCase.Input(ExportDataUseCase.Format.CSV)) } returns flowOf(
            ExportDataUseCase.Output.Progress,
            ExportDataUseCase.Output.Failure("Disk error")
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnExportClicked(ExportDataUseCase.Format.CSV))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)

        viewModel.sendEvent(Event.OnDismissError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `import requested for premium user emits LaunchImportPicker`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnImportRequested)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.LaunchImportPicker), effects)
    }

    @Test
    fun `import requested for free user shows premium limit`() = runTest(testDispatcher) {
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = false)
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnImportRequested)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.showPremiumLimit)
    }

    @Test
    fun `import clicked success updates successMessage`() = runTest(testDispatcher) {
        every { importDataUseCase(ImportDataUseCase.Input("{json}")) } returns flowOf(
            ImportDataUseCase.Output.Progress,
            ImportDataUseCase.Output.Success
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnImportClicked("{json}"))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.successMessage != null)

        viewModel.sendEvent(Event.OnDismissError)
        assertNull(viewModel.state.value.successMessage)
    }

    @Test
    fun `import clicked failure updates error`() = runTest(testDispatcher) {
        every { importDataUseCase(ImportDataUseCase.Input("{corrupt}")) } returns flowOf(
            ImportDataUseCase.Output.Progress,
            ImportDataUseCase.Output.Failure("Invalid format")
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnImportClicked("{corrupt}"))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun `on upgrade clicked emits NavigateToPremiumPaywall`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnUpgradeClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.NavigateToPremiumPaywall), effects)
    }
}
