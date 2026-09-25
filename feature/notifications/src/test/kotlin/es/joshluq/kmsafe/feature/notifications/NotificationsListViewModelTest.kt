package es.joshluq.kmsafe.feature.notifications

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.DeleteNotificationUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.MarkAllNotificationsAsReadUseCase
import es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveActiveNotificationsUseCase
import es.joshluq.kmsafe.domain.usecase.SyncNotificationsUseCase
import es.joshluq.kmsafe.feature.notifications.ui.list.NotificationsListEffect
import es.joshluq.kmsafe.feature.notifications.ui.list.NotificationsListEvent
import es.joshluq.kmsafe.feature.notifications.ui.list.NotificationsListViewModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class NotificationsListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val observeActiveNotificationsUseCase: ObserveActiveNotificationsUseCase = mockk()
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase = mockk(relaxed = true)
    private val markAllNotificationsAsReadUseCase: MarkAllNotificationsAsReadUseCase = mockk(relaxed = true)
    private val deleteNotificationUseCase: DeleteNotificationUseCase = mockk(relaxed = true)
    private val syncNotificationsUseCase: SyncNotificationsUseCase = mockk(relaxed = true)
    private val getRentingContractUseCase: GetRentingContractUseCase = mockk()
    private val getAllContractsUseCase: GetAllContractsUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val notificationsFlow = MutableStateFlow<ObserveActiveNotificationsUseCase.Output>(
        ObserveActiveNotificationsUseCase.Output.Success(emptyList())
    )
    private val rentingContractFlow = MutableStateFlow<GetRentingContractUseCase.Output>(
        GetRentingContractUseCase.Output.Progress
    )
    private val allContractsFlow = MutableStateFlow<GetAllContractsUseCase.Output>(
        GetAllContractsUseCase.Output.Progress
    )
    private lateinit var viewModel: NotificationsListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeActiveNotificationsUseCase(any()) } returns notificationsFlow
        every { getRentingContractUseCase(any()) } returns rentingContractFlow
        every { getAllContractsUseCase(any()) } returns allContractsFlow
        coEvery { syncNotificationsUseCase(any()) } returns Result.success(SyncNotificationsUseCase.Output.Success(0))
        viewModel = NotificationsListViewModel(
            observeActiveNotificationsUseCase,
            markNotificationAsReadUseCase,
            markAllNotificationsAsReadUseCase,
            deleteNotificationUseCase,
            syncNotificationsUseCase,
            getRentingContractUseCase,
            getAllContractsUseCase,
            logger
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `given active notifications when emitted then uiState contains correct count and items`() = runTest(testDispatcher) {
        val sample = listOf(
            Notification(
                id = "1",
                topic = NotificationTopic.PROJECTION,
                title = "Exceso proyectado",
                body = "Atención al kilometraje",
                priority = NotificationPriority.CRITICAL,
                status = NotificationStatus.UNREAD
            ),
            Notification(
                id = "2",
                topic = NotificationTopic.SUBSCRIPTION,
                title = "Bienvenido a Premium",
                body = "Disfruta de tus ventajas",
                priority = NotificationPriority.INFO,
                status = NotificationStatus.READ
            )
        )
        notificationsFlow.value = ObserveActiveNotificationsUseCase.Output.Success(sample)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.unreadCount)
        assertEquals(2, state.notifications.size)
    }

    @Test
    fun `given topic filter selected when TopicSelected action then uiState filters list`() = runTest(testDispatcher) {
        val sample = listOf(
            Notification(
                id = "1",
                topic = NotificationTopic.PROJECTION,
                title = "Exceso proyectado",
                body = "Atención al kilometraje",
                priority = NotificationPriority.CRITICAL,
                status = NotificationStatus.UNREAD
            ),
            Notification(
                id = "2",
                topic = NotificationTopic.SUBSCRIPTION,
                title = "Bienvenido a Premium",
                body = "Disfruta de tus ventajas",
                priority = NotificationPriority.INFO,
                status = NotificationStatus.READ
            )
        )
        notificationsFlow.value = ObserveActiveNotificationsUseCase.Output.Success(sample)
        advanceUntilIdle()

        viewModel.sendEvent(NotificationsListEvent.TopicSelected(NotificationTopic.PROJECTION))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(NotificationTopic.PROJECTION, state.selectedTopic)
        assertEquals(1, state.notifications.size)
        assertEquals("1", state.notifications[0].id)
    }

    @Test
    fun `given notification with deep link clicked then marks read and sends NavigateToDeepLink effect`() = runTest(testDispatcher) {
        val notif = Notification(
            id = "fleet-1",
            topic = NotificationTopic.FLEET,
            title = "Aviso de flota",
            body = "Detalles",
            priority = NotificationPriority.INFO,
            status = NotificationStatus.UNREAD,
            deepLinkUri = "kmsafe://feature/fleet/details"
        )

        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notif))
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationsListEffect.NavigateToDeepLink)
        assertEquals("kmsafe://feature/fleet/details", (emittedEffect as NotificationsListEffect.NavigateToDeepLink).deepLinkUri)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("fleet-1")) }
        job.cancel()
    }

    @Test
    fun `given projection notification clicked then emits NavigateToProjection effect and marks read`() = runTest(testDispatcher) {
        val notif = Notification(
            id = "proj-1",
            topic = NotificationTopic.PROJECTION,
            title = "Exceso proyectado",
            body = "Detalles del exceso",
            priority = NotificationPriority.CRITICAL,
            status = NotificationStatus.UNREAD
        )

        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notif))
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationsListEffect.NavigateToProjection)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("proj-1")) }
        job.cancel()
    }

    @Test
    fun `given system bluetooth notification clicked then emits NavigateToEditContract effect and marks read`() = runTest(testDispatcher) {
        val notif = Notification(
            id = "bt-1",
            topic = NotificationTopic.SYSTEM,
            title = "Bluetooth no configurado",
            body = "Configura el Bluetooth",
            priority = NotificationPriority.WARNING,
            status = NotificationStatus.UNREAD,
            data = mapOf("contract_id" to "contract-abc", "deduplication_key" to "bt_missing_contract-abc")
        )

        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notif))
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationsListEffect.NavigateToEditContract)
        assertEquals("contract-abc", (emittedEffect as NotificationsListEffect.NavigateToEditContract).vehicleId)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("bt-1")) }
        job.cancel()
    }

    @Test
    fun `given subscription notification clicked then emits NavigateToPaywall effect and marks read`() = runTest(testDispatcher) {
        val notif = Notification(
            id = "sub-1",
            topic = NotificationTopic.SUBSCRIPTION,
            title = "Suscripción requerida",
            body = "Actualiza a Pro",
            priority = NotificationPriority.INFO,
            status = NotificationStatus.UNREAD
        )

        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notif))
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationsListEffect.NavigateToPaywall)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("sub-1")) }
        job.cancel()
    }

    @Test
    fun `given notification clicked then marks read optimistically in state`() = runTest(testDispatcher) {
        val notif = Notification(
            id = "notif-opt-1",
            topic = NotificationTopic.PROJECTION,
            title = "Aviso optimista",
            body = "Detalles",
            priority = NotificationPriority.INFO,
            status = NotificationStatus.UNREAD
        )
        notificationsFlow.value = ObserveActiveNotificationsUseCase.Output.Success(listOf(notif))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.unreadCount)

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notif))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.notifications.first().isRead)
        assertEquals(NotificationStatus.READ, viewModel.uiState.value.notifications.first().status)
    }

    @Test
    fun `given delete notification clicked then invokes deleteNotificationUseCase`() = runTest(testDispatcher) {
        viewModel.sendEvent(NotificationsListEvent.DeleteNotificationClicked("del-1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteNotificationUseCase(DeleteNotificationUseCase.Input("del-1")) }
    }

    @Test
    fun `given sync returns PremiumRequired then uiState displays premium banner`() = runTest(testDispatcher) {
        coEvery { syncNotificationsUseCase(any()) } returns Result.success(SyncNotificationsUseCase.Output.PremiumRequired)

        viewModel.sendEvent(NotificationsListEvent.Refresh)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPremiumRequiredBannerVisible)
    }

    @Test
    fun `given premium banner dismissed then uiState hides banner`() = runTest(testDispatcher) {
        coEvery { syncNotificationsUseCase(any()) } returns Result.success(SyncNotificationsUseCase.Output.PremiumRequired)
        viewModel.sendEvent(NotificationsListEvent.Refresh)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPremiumRequiredBannerVisible)

        viewModel.sendEvent(NotificationsListEvent.DismissPremiumBanner)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPremiumRequiredBannerVisible)
    }

    @Test
    fun `given upgrade to pro clicked then emits NavigateToPaywall effect`() = runTest(testDispatcher) {
        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.UpgradeToProClicked)
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationsListEffect.NavigateToPaywall)
        job.cancel()
    }

    @Test
    fun `given contracts observed then uiState updates activeVehicleId and vehicleNames`() = runTest(testDispatcher) {
        val contractA = RentingContract(
            id = "vehicle-a",
            vehicleName = "Toyota Corolla",
            startDate = 1000L,
            durationMonths = 36,
            totalKms = 30000.0,
            startOdometer = 0.0,
            currentOdometer = 0.0
        )
        val contractB = RentingContract(
            id = "vehicle-b",
            vehicleName = "Seat Ibiza",
            startDate = 1000L,
            durationMonths = 24,
            totalKms = 20000.0,
            startOdometer = 0.0,
            currentOdometer = 0.0
        )

        rentingContractFlow.value = GetRentingContractUseCase.Output.Success(contractA)
        allContractsFlow.value = GetAllContractsUseCase.Output.Success(listOf(contractA, contractB))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("vehicle-a", state.activeVehicleId)
        assertEquals(2, state.vehicleNames.size)
        assertEquals("Toyota Corolla", state.vehicleNames["vehicle-a"])
        assertEquals("Seat Ibiza", state.vehicleNames["vehicle-b"])
    }

    @Test
    fun `given projection notification for non-active vehicle when clicked then does not emit NavigateToProjection`() = runTest(testDispatcher) {
        val contractA = RentingContract(
            id = "vehicle-a",
            vehicleName = "Toyota Corolla",
            startDate = 1000L,
            durationMonths = 36,
            totalKms = 30000.0,
            startOdometer = 0.0,
            currentOdometer = 0.0
        )
        rentingContractFlow.value = GetRentingContractUseCase.Output.Success(contractA)
        advanceUntilIdle()

        val notifVehicleB = Notification(
            id = "notif-proj-b",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta vehículo B",
            body = "Detalle",
            priority = NotificationPriority.CRITICAL,
            status = NotificationStatus.UNREAD,
            data = mapOf("contract_id" to "vehicle-b")
        )

        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notifVehicleB))
        advanceUntilIdle()

        assertNull("Effect must not be emitted when clicking projection notification for non-active vehicle", emittedEffect)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("notif-proj-b")) }
        job.cancel()
    }

    @Test
    fun `given projection notification for active vehicle when clicked then emits NavigateToProjection`() = runTest(testDispatcher) {
        val contractA = RentingContract(
            id = "vehicle-a",
            vehicleName = "Toyota Corolla",
            startDate = 1000L,
            durationMonths = 36,
            totalKms = 30000.0,
            startOdometer = 0.0,
            currentOdometer = 0.0
        )
        rentingContractFlow.value = GetRentingContractUseCase.Output.Success(contractA)
        advanceUntilIdle()

        val notifVehicleA = Notification(
            id = "notif-proj-a",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta vehículo A",
            body = "Detalle",
            priority = NotificationPriority.CRITICAL,
            status = NotificationStatus.UNREAD,
            data = mapOf("contract_id" to "vehicle-a")
        )

        var emittedEffect: NotificationsListEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationsListEvent.NotificationClicked(notifVehicleA))
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationsListEffect.NavigateToProjection)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("notif-proj-a")) }
        job.cancel()
    }
}
