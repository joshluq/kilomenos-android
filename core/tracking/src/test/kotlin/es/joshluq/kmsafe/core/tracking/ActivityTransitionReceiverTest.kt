package es.joshluq.kmsafe.core.tracking

import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.TrackingMode
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ActivityTransitionReceiverTest {

    private val logger: LoggerKit = mockk(relaxed = true)
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val trackingRepository: TrackingRepository = mockk()
    private val context: Context = mockk(relaxed = true)
    private val stopServiceCommand: (Context, String) -> Unit = mockk(relaxed = true)
    private val startServiceCommand: (Context, Intent) -> Unit = mockk(relaxed = true)

    private lateinit var receiver: ActivityTransitionReceiver

    @Before
    fun setUp() {
        mockkStatic(ActivityTransitionResult::class)
        mockkObject(TrackingDeviceCache)

        val baseReceiver = ActivityTransitionReceiver().apply {
            this.logger = this@ActivityTransitionReceiverTest.logger
            this.checkFeatureAccessUseCase = this@ActivityTransitionReceiverTest.checkFeatureAccessUseCase
            this.trackingRepository = this@ActivityTransitionReceiverTest.trackingRepository
            this.stopServiceCommand = this@ActivityTransitionReceiverTest.stopServiceCommand
            this.startServiceCommand = this@ActivityTransitionReceiverTest.startServiceCommand
        }

        try {
            val injectedField = Hilt_ActivityTransitionReceiver::class.java.getDeclaredField("injected")
            injectedField.isAccessible = true
            injectedField.set(baseReceiver, true)
        } catch (_: Exception) {
        }

        receiver = io.mockk.spyk(baseReceiver)
        every { receiver.goAsync() } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun `given free user then activity transition events are discarded immediately`() = runTest {
        receiver.coroutineScope = this
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = false)
        )
        every { trackingRepository.isTracking } returns flowOf(false)
        every { trackingRepository.trackingMode } returns flowOf(TrackingMode.MANUAL)

        val intent = mockk<Intent>()
        every { intent.action } returns "es.joshluq.kmsafe.core.tracking.ActivityTransitionReceiver"
        every { ActivityTransitionResult.hasResult(intent) } returns true

        receiver.onReceive(context, intent)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { startServiceCommand(any(), any()) }
        verify(exactly = 0) { stopServiceCommand(any(), any()) }
    }

    @Test
    fun `given premium user with active manual trip then walking transition does not stop tracking`() = runTest {
        receiver.coroutineScope = this
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { trackingRepository.isTracking } returns flowOf(true)
        every { trackingRepository.trackingMode } returns flowOf(TrackingMode.MANUAL)
        every { TrackingDeviceCache.getLinkedMac(any()) } returns null
        every { TrackingDeviceCache.isBluetoothConnected() } returns false

        val intent = mockk<Intent>()
        every { intent.action } returns "es.joshluq.kmsafe.core.tracking.ActivityTransitionReceiver"
        every { ActivityTransitionResult.hasResult(intent) } returns true

        val transitionResult = mockk<ActivityTransitionResult>()
        val walkingEvent = ActivityTransitionEvent(
            DetectedActivity.WALKING,
            ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            1000L
        )
        every { transitionResult.transitionEvents } returns listOf(walkingEvent)
        every { ActivityTransitionResult.extractResult(intent) } returns transitionResult

        receiver.onReceive(context, intent)
        testScheduler.advanceUntilIdle()

        // Verifies no stop command is dispatched to service
        verify(exactly = 0) { stopServiceCommand(any(), any()) }
        verify(exactly = 0) { startServiceCommand(any(), any()) }
    }

    @Test
    fun `given premium user with active automatic trip then walking transition stops tracking via ACTION_STOP_AUTOMATIC`() = runTest {
        receiver.coroutineScope = this
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { trackingRepository.isTracking } returns flowOf(true)
        every { trackingRepository.trackingMode } returns flowOf(TrackingMode.AUTOMATIC)
        every { TrackingDeviceCache.getLinkedMac(any()) } returns null
        every { TrackingDeviceCache.isBluetoothConnected() } returns false

        val intent = mockk<Intent>()
        every { intent.action } returns "es.joshluq.kmsafe.core.tracking.ActivityTransitionReceiver"
        every { ActivityTransitionResult.hasResult(intent) } returns true

        val transitionResult = mockk<ActivityTransitionResult>()
        val walkingEvent = ActivityTransitionEvent(
            DetectedActivity.WALKING,
            ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            1000L
        )
        every { transitionResult.transitionEvents } returns listOf(walkingEvent)
        every { ActivityTransitionResult.extractResult(intent) } returns transitionResult

        receiver.onReceive(context, intent)
        testScheduler.advanceUntilIdle()

        // Verifies stop command is dispatched with ACTION_STOP_AUTOMATIC
        verify(exactly = 1) { stopServiceCommand(context, LocationTrackingService.ACTION_STOP_AUTOMATIC) }
    }
}
