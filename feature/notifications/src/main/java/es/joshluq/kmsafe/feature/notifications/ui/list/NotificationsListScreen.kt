package es.joshluq.kmsafe.feature.notifications.ui.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonSize
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.chips.CanvasKitChip
import es.joshluq.canvaskit.components.chips.CanvasKitChipVariant
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.feature.notifications.R
import es.joshluq.kmsafe.feature.notifications.ui.mapper.NotificationTextResolver
import es.joshluq.kmsafe.feature.notifications.ui.model.NotificationUiItem

/**
 * Pure, stateless screen implementing the 4-Layer visual hierarchy for notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListScreen(
    uiState: NotificationsListUiState,
    onAction: (NotificationsListUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                centeredTitle = true,
                navigationIcon = {
                    val navBackDesc = stringResource(R.string.notifications_nav_back)
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = navBackDesc
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        val markAllReadDesc = stringResource(R.string.notifications_mark_all_read)
                        IconButton(
                            onClick = { onAction(NotificationsListEvent.MarkAllAsReadClicked) },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = markAllReadDesc
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.brandAccent
                            )
                        }
                    }
                }
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (uiState.unreadCount > 0) {
                            stringResource(R.string.notifications_pending_count, uiState.unreadCount)
                        } else {
                            stringResource(R.string.notifications_all_caught_up)
                        },
                        style = CanvasKitTheme.typography.headingMedium,
                        color = if (uiState.unreadCount > 0) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.success
                    )
                    Text(
                        text = stringResource(R.string.notifications_subtitle),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            }


            // =================================================================
            // PRO PAYWALL BANNER (When 403 PREMIUM_REQUIRED occurs)
            // =================================================================
            if (uiState.isPremiumRequiredBannerVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.notifications_pro_banner_title),
                                style = CanvasKitTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CanvasKitTheme.colors.brandAccent
                            )
                            val closeBannerDesc = stringResource(R.string.notifications_pro_banner_close)
                            IconButton(
                                onClick = { onAction(NotificationsListEvent.DismissPremiumBanner) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = closeBannerDesc
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = CanvasKitTheme.colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.notifications_pro_banner_desc),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CanvasKitButton(
                            text = stringResource(R.string.notifications_pro_banner_cta),
                            onClick = { onAction(NotificationsListEvent.UpgradeToProClicked) },
                            size = CanvasKitButtonSize.Small,
                            variant = CanvasKitButtonVariant.Primary
                        )
                    }
                }
            }

            // =================================================================
            // LAYER 2: Contextual Decision Radar (Topic Filter Chips)
            // =================================================================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CanvasKitChip(
                        variant = CanvasKitChipVariant.Outlined,
                        selected = uiState.selectedTopic == null,
                        onClick = { onAction(NotificationsListEvent.TopicSelected(null)) },
                        label = { Text(stringResource(R.string.notifications_filter_all)) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                }
                items(NotificationTopic.entries.toTypedArray()) { topic ->
                    val labelRes = when (topic) {
                        NotificationTopic.SUBSCRIPTION -> R.string.notifications_filter_subscription
                        NotificationTopic.PROJECTION -> R.string.notifications_filter_projection
                        NotificationTopic.FLEET -> R.string.notifications_filter_fleet
                        NotificationTopic.SYSTEM -> R.string.notifications_filter_system
                    }
                    CanvasKitChip(
                        variant = CanvasKitChipVariant.Outlined,
                        selected = uiState.selectedTopic == topic,
                        onClick = { onAction(NotificationsListEvent.TopicSelected(topic)) },
                        label = { Text(stringResource(labelRes)) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            // =================================================================
            // LAYER 4: Intelligent Diagnostic Feed (Notifications LazyColumn)
            // =================================================================
            if (uiState.notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.textSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.notifications_empty_filter),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = uiState.notifications,
                        key = { it.id }
                    ) { item ->
                        NotificationItemRow(
                            notification = item,
                            onClick = { onAction(NotificationsListEvent.NotificationClicked(item.notification)) },
                            onDelete = { onAction(NotificationsListEvent.DeleteNotificationClicked(item.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: NotificationUiItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = !notification.isRead
    val vehicleName = notification.vehicleName
    val canNavigate = when (notification.topic) {
        NotificationTopic.PROJECTION -> notification.isForActiveVehicle
        NotificationTopic.SYSTEM -> notification.isForActiveVehicle
        NotificationTopic.SUBSCRIPTION -> true
        else -> !notification.notification.deepLinkUri.isNullOrBlank()
    }

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

    val topicLabel = notification.topicLabel.asString()
    val titleText = notification.title.asString()
    val bodyText = notification.body.asString()
    val unreadPrefix = if (isUnread) stringResource(R.string.notifications_unread_prefix) else ""

    CanvasKitCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription =
                    "$unreadPrefix$topicLabel${if (!vehicleName.isNullOrBlank()) ", $vehicleName" else ""}, $titleText"
            },
        selected = isUnread
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Topic tag
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .border(width = 0.5.dp, color = badgeBorder, shape = CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = topicLabel,
                            style = CanvasKitTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeTextColor
                        )
                    }

                    if (!vehicleName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CanvasKitTheme.colors.backgroundSecondary)
                                .border(
                                    width = 0.5.dp,
                                    color = CanvasKitTheme.colors.borderSubtle,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = vehicleName,
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CanvasKitTheme.colors.brandAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = titleText,
                    style = CanvasKitTheme.typography.bodyMedium.copy(
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = CanvasKitTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (bodyText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bodyText,
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val deleteDesc = stringResource(R.string.notifications_delete_content_description)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = deleteDesc
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (canNavigate) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview(name = "NotificationsListScreen - Content")
@Preview(name = "NotificationsListScreen - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNotificationsListScreen() {
    val notif1 = Notification(
        id = "1",
        topic = NotificationTopic.PROJECTION,
        title = "",
        body = "",
        priority = NotificationPriority.CRITICAL,
        status = NotificationStatus.UNREAD,
        deepLinkUri = "kmsafe://feature/projection",
        data = mapOf("is_over_limit" to true, "excess_km" to "1.500")
    )
    val notif2 = Notification(
        id = "2",
        topic = NotificationTopic.SUBSCRIPTION,
        title = "",
        body = "",
        priority = NotificationPriority.INFO,
        status = NotificationStatus.READ
    )
    val localized1 = NotificationTextResolver.resolve(notif1)
    val localized2 = NotificationTextResolver.resolve(notif2)

    CanvasKitTheme {
        NotificationsListScreen(
            uiState = NotificationsListUiState(
                unreadCount = 2,
                notifications = listOf(
                    NotificationUiItem(
                        id = notif1.id,
                        notification = notif1,
                        title = localized1.title,
                        body = localized1.body,
                        actionLabel = localized1.actionLabel,
                        topicLabel = localized1.topicLabel,
                        topic = notif1.topic,
                        priority = notif1.priority,
                        status = notif1.status,
                        isRead = false,
                        timestampMillis = notif1.timestampMillis,
                        vehicleId = null,
                        vehicleName = "Audi A3",
                        isForActiveVehicle = true
                    ),
                    NotificationUiItem(
                        id = notif2.id,
                        notification = notif2,
                        title = localized2.title,
                        body = localized2.body,
                        actionLabel = localized2.actionLabel,
                        topicLabel = localized2.topicLabel,
                        topic = notif2.topic,
                        priority = notif2.priority,
                        status = notif2.status,
                        isRead = true,
                        timestampMillis = notif2.timestampMillis,
                        vehicleId = null,
                        vehicleName = null,
                        isForActiveVehicle = true
                    )
                )
            ),
            onAction = {},
            onNavigateBack = {}
        )
    }
}
