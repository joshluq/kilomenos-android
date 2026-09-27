package es.joshluq.kmsafe.infrastructure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationChannelManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val notificationManager: NotificationManager = mockk(relaxed = true)
    private lateinit var channelManager: NotificationChannelManagerImpl

    @Before
    fun setUp() {
        every { context.getSystemService(NotificationManager::class.java) } returns notificationManager
        channelManager = NotificationChannelManagerImpl(context).apply {
            sdkVersion = 26 // Android O
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getSubscriptionChannelId returns expected constant subscription_alerts`() {
        assertEquals("subscription_alerts", channelManager.getSubscriptionChannelId())
    }

    @Test
    fun `initializeChannels creates subscription_alerts channel on API 26 plus with high importance`() {
        var recordedManager: NotificationManager? = null
        var recordedId: String? = null
        var recordedName: String? = null
        var recordedImportance: Int? = null
        var recordedDesc: String? = null

        channelManager.channelCreator = { manager, id, name, importance, desc ->
            recordedManager = manager
            recordedId = id
            recordedName = name
            recordedImportance = importance
            recordedDesc = desc
        }

        channelManager.initializeChannels()

        assertEquals(notificationManager, recordedManager)
        assertEquals("subscription_alerts", recordedId)
        assertEquals("Alertas de Suscripción", recordedName)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, recordedImportance)
        assertEquals("Avisos sobre cambios en tu estado de suscripción y renovaciones", recordedDesc)
    }

    @Test
    fun `initializeChannels skips channel creation on pre-Oreo API less than 26`() {
        channelManager.sdkVersion = 24 // Nougat

        channelManager.initializeChannels()

        verify(exactly = 0) { notificationManager.createNotificationChannel(any()) }
    }
}
