package es.joshluq.kmsafe.feature.notifications.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.feature.notifications.R
import es.joshluq.kmsafe.feature.notifications.ui.mapper.NotificationTextResolver

/**
 * Monochannel interactive notification pill for the top section of OverviewScreen.
 * Displays topic tag and title, ensuring >= 48dp touch target and CanvasKit design tokens.
 */
@Composable
fun NotificationPill(
    notification: Notification?,
    onPillClick: (Notification) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (notification == null) return@AnimatedVisibility

        val (badgeBg, badgeBorder, badgeTextColor) = when (notification.priority) {
            NotificationPriority.CRITICAL -> Triple(
                CanvasKitTheme.colors.error.copy(alpha = 0.12f),
                CanvasKitTheme.colors.error.copy(alpha = 0.4f),
                CanvasKitTheme.colors.error
            )
            NotificationPriority.WARNING -> Triple(
                CanvasKitTheme.colors.brandAccent.copy(alpha = 0.12f),
                CanvasKitTheme.colors.brandAccent.copy(alpha = 0.4f),
                CanvasKitTheme.colors.brandAccent
            )
            NotificationPriority.INFO -> Triple(
                CanvasKitTheme.colors.backgroundSecondary,
                CanvasKitTheme.colors.borderSubtle,
                CanvasKitTheme.colors.textPrimary
            )
        }

        val texts = NotificationTextResolver.resolve(notification)
        val topicString = texts.topicLabel.asString()
        val titleString = texts.title.asString()
        val pillContentDescription = stringResource(
            R.string.notification_pill_content_description,
            topicString,
            titleString
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("notification_pill_container"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CanvasKitTheme.colors.backgroundSecondary)
                    .border(width = 1.dp, color = badgeBorder, shape = RoundedCornerShape(22.dp))
                    .clickable(
                        onClick = { onPillClick(notification) }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = pillContentDescription
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Topic Tag Capsule
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .border(width = 0.5.dp, color = badgeBorder, shape = CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = topicString,
                            style = CanvasKitTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = titleString,
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(name = "NotificationPill - Critical")
@Preview(name = "NotificationPill - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNotificationPill() {
    CanvasKitTheme {
        NotificationPill(
            notification = Notification(
                id = "1",
                topic = NotificationTopic.PROJECTION,
                title = "Desviación de kilometraje detectada (+15%)",
                body = "Estás proyectando un exceso al final del contrato.",
                priority = NotificationPriority.WARNING,
                status = NotificationStatus.UNREAD,
                deepLinkUri = "kmsafe://feature/projection"
            ),
            onPillClick = {}
        )
    }
}
