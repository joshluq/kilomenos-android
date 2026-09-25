package es.joshluq.kmsafe.feature.notifications

import androidx.lifecycle.SavedStateHandle
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.usecase.GetNotificationByIdUseCase
import es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase
import es.joshluq.kmsafe.feature.notifications.ui.detail.NotificationDetailEffect
import es.joshluq.kmsafe.feature.notifications.ui.detail.NotificationDetailEvent
import es.joshluq.kmsafe.feature.notifications.ui.detail.NotificationDetailViewModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class NotificationDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getNotificationByIdUseCase: GetNotificationByIdUseCase = mockk()
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleNotification = Notification(
        id = "notif-100",
        topic = NotificationTopic.SUBSCRIPTION,
        title = "Aviso de suscripción",
        body = "Detalles del plan",
        priority = NotificationPriority.WARNING,
        status = NotificationStatus.UNREAD,
        deepLinkUri = "kmsafe://feature/premium",
        actionLabel = "Renovar"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `given savedStateHandle with id when created then loads notification and auto-marks as read`() = runTest(testDispatcher) {
        coEvery { getNotificationByIdUseCase(any()) } returns Result.success(
            GetNotificationByIdUseCase.Output.Success(sampleNotification)
        )

        val savedStateHandle = SavedStateHandle(mapOf("notificationId" to "notif-100"))
        val viewModel = NotificationDetailViewModel(
            getNotificationByIdUseCase,
            markNotificationAsReadUseCase,
            logger,
            savedStateHandle
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Aviso de suscripción", state.notification?.title)
        coVerify(exactly = 1) { markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input("notif-100")) }
    }

    @Test
    fun `given notification with deep link when PrimaryActionClicked then sends NavigateToDeepLink effect`() = runTest(testDispatcher) {
        coEvery { getNotificationByIdUseCase(any()) } returns Result.success(
            GetNotificationByIdUseCase.Output.Success(sampleNotification)
        )

        val savedStateHandle = SavedStateHandle(mapOf("notificationId" to "notif-100"))
        val viewModel = NotificationDetailViewModel(
            getNotificationByIdUseCase,
            markNotificationAsReadUseCase,
            logger,
            savedStateHandle
        )
        advanceUntilIdle()

        var emittedEffect: NotificationDetailEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationDetailEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationDetailEffect.NavigateToDeepLink)
        assertEquals("kmsafe://feature/premium", (emittedEffect as NotificationDetailEffect.NavigateToDeepLink).deepLinkUri)
        job.cancel()
    }

    @Test
    fun `when NavigateBackClicked then sends NavigateBack effect`() = runTest(testDispatcher) {
        coEvery { getNotificationByIdUseCase(any()) } returns Result.success(
            GetNotificationByIdUseCase.Output.Success(sampleNotification)
        )

        val savedStateHandle = SavedStateHandle(mapOf("notificationId" to "notif-100"))
        val viewModel = NotificationDetailViewModel(
            getNotificationByIdUseCase,
            markNotificationAsReadUseCase,
            logger,
            savedStateHandle
        )
        advanceUntilIdle()

        var emittedEffect: NotificationDetailEffect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(NotificationDetailEvent.NavigateBackClicked)
        advanceUntilIdle()

        assertTrue(emittedEffect is NotificationDetailEffect.NavigateBack)
        job.cancel()
    }
}
