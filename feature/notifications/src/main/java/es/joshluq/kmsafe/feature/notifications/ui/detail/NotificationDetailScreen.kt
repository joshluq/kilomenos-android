package es.joshluq.kmsafe.feature.notifications.ui.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonSize
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.feature.notifications.R
import es.joshluq.kmsafe.feature.notifications.ui.mapper.NotificationTextResolver
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Pure, stateless screen displaying full details of a notification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    uiState: NotificationDetailUiState,
    onAction: (NotificationDetailUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.notification_detail_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    val backDescription = stringResource(R.string.notification_detail_back)
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = backDescription
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
               centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                uiState.notification != null -> {
                    val notif = uiState.notification
                    val localized = NotificationTextResolver.resolve(notif)
                    val (badgeBg, badgeBorder, badgeTextColor) = when (notif.priority) {
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

                    val topicLabel = localized.topicLabel.asString()
                    val titleText = localized.title.asString()
                    val bodyText = localized.body.asString()
                    val actionText = localized.actionLabel?.asString() ?: notif.actionLabel
                    val locale = LocalLocale.current.platformLocale

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", locale)
                    val formattedDate = dateFormat.format(Date(notif.timestampMillis))

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CanvasKitTheme.colors.backgroundSecondary)
                                .border(width = 1.dp, color = CanvasKitTheme.colors.borderSubtle, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(badgeBg)
                                            .border(width = 0.5.dp, color = badgeBorder, shape = CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = topicLabel,
                                            style = CanvasKitTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeTextColor
                                        )
                                    }

                                    Text(
                                        text = formattedDate,
                                        style = CanvasKitTheme.typography.labelSmall,
                                        color = CanvasKitTheme.colors.textSecondary
                                    )
                                }

                                Text(
                                    text = titleText,
                                    style = CanvasKitTheme.typography.headingMedium,
                                    color = CanvasKitTheme.colors.textPrimary
                                )
                            }
                        }

                        // Body text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CanvasKitTheme.colors.backgroundSecondary)
                                .border(width = 1.dp, color = CanvasKitTheme.colors.borderSubtle, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = bodyText.ifBlank { stringResource(R.string.notification_detail_empty_body) },
                                style = CanvasKitTheme.typography.bodyLarge,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }

                        // Action button (if deep link or action label present)
                        if (!actionText.isNullOrBlank() || !notif.deepLinkUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CanvasKitButton(
                                text = actionText ?: stringResource(R.string.notification_detail_view_in_app),
                                onClick = { onAction(NotificationDetailEvent.PrimaryActionClicked) },
                                variant = CanvasKitButtonVariant.Primary,
                                size = CanvasKitButtonSize.Large,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: stringResource(R.string.notification_detail_not_found),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "NotificationDetailScreen - Content")
@Preview(name = "NotificationDetailScreen - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNotificationDetailScreen() {
    CanvasKitTheme {
        NotificationDetailScreen(
            uiState = NotificationDetailUiState(
                notification = Notification(
                    id = "1",
                    topic = NotificationTopic.SUBSCRIPTION,
                    title = "Suscripción actualizada a Free",
                    body = "Tu plan Premium ha finalizado. Renueva para mantener proyecciones ilimitadas y sincronización automática.",
                    priority = NotificationPriority.CRITICAL,
                    status = NotificationStatus.READ,
                    deepLinkUri = "kmsafe://feature/premium",
                    actionLabel = "Renovar Plan"
                )
            ),
            onAction = {},
            onNavigateBack = {}
        )
    }
}
