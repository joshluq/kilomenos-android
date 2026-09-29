package es.joshluq.kmsafe.feature.notifications.ui.mapper

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.feature.notifications.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTextResolverTest {

    @Test
    fun `given subscription downgrade notification with event_type then resolves regionalized downgrade string resources`() {
        val notif = Notification(
            id = "test-1",
            topic = NotificationTopic.SUBSCRIPTION,
            title = "Tu plan ha cambiado a Free",
            body = "Tu período Premium ha finalizado.",
            priority = NotificationPriority.WARNING,
            status = NotificationStatus.UNREAD,
            data = mapOf("event_type" to "SUBSCRIPTION_DOWNGRADED", "sub_type" to "downgrade")
        )

        val resolved = NotificationTextResolver.resolve(notif)

        assertTrue(resolved.title is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_downgraded_title, (resolved.title as TextProvider.Resource).resId)
        assertTrue(resolved.body is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_downgraded_body, (resolved.body as TextProvider.Resource).resId)
        assertTrue(resolved.actionLabel is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_downgraded_action, (resolved.actionLabel as TextProvider.Resource).resId)
        assertEquals(R.string.notification_topic_subscription, (resolved.topicLabel as TextProvider.Resource).resId)
    }

    @Test
    fun `given subscription downgrade notification identified by title then resolves regionalized downgrade string resources`() {
        val notif = Notification(
            id = "test-2",
            topic = NotificationTopic.SUBSCRIPTION,
            title = "Plan cambiado a FREE",
            body = "Detalles de cancelación",
            priority = NotificationPriority.WARNING,
            status = NotificationStatus.UNREAD
        )

        val resolved = NotificationTextResolver.resolve(notif)

        assertTrue(resolved.title is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_downgraded_title, (resolved.title as TextProvider.Resource).resId)
        assertTrue(resolved.body is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_downgraded_body, (resolved.body as TextProvider.Resource).resId)
    }

    @Test
    fun `given subscription notification with non-downgrade custom title then resolves dynamic text`() {
        val notif = Notification(
            id = "test-3",
            topic = NotificationTopic.SUBSCRIPTION,
            title = "Promoción Especial Black Friday",
            body = "Descuento del 50% en suscripción anual",
            priority = NotificationPriority.INFO,
            status = NotificationStatus.UNREAD
        )

        val resolved = NotificationTextResolver.resolve(notif)

        assertTrue(resolved.title is TextProvider.Dynamic)
        assertEquals("Promoción Especial Black Friday", (resolved.title as TextProvider.Dynamic).value)
        assertTrue(resolved.body is TextProvider.Dynamic)
        assertEquals("Descuento del 50% en suscripción anual", (resolved.body as TextProvider.Dynamic).value)
    }

    @Test
    fun `given blank subscription notification then resolves default upgrade promo string resources`() {
        val notif = Notification(
            id = "test-4",
            topic = NotificationTopic.SUBSCRIPTION,
            title = "",
            body = "",
            priority = NotificationPriority.INFO,
            status = NotificationStatus.UNREAD
        )

        val resolved = NotificationTextResolver.resolve(notif)

        assertTrue(resolved.title is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_title, (resolved.title as TextProvider.Resource).resId)
        assertTrue(resolved.body is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_body, (resolved.body as TextProvider.Resource).resId)
        assertTrue(resolved.actionLabel is TextProvider.Resource)
        assertEquals(R.string.notification_subscription_action, (resolved.actionLabel as TextProvider.Resource).resId)
    }
}
