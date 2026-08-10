package es.joshluq.kmsafe.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialogContent
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.ui.util.safeClick

@Composable
fun ProfileRoute(
    onNavigateToDataManagement: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    onNavigateToWelcomeDiscovery: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateToDataManagement -> onNavigateToDataManagement()
                Effect.NavigateToVehicles -> onNavigateToVehicles()
                Effect.NavigateToPreferences -> onNavigateToPreferences()
                Effect.NavigateToLogin -> onNavigateToLogin()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
                Effect.NavigateToWelcomeDiscovery -> onNavigateToWelcomeDiscovery()
                is Effect.ShowMessage -> { /* Handle simple toast if needed */ }
            }
        }
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CanvasKitTheme.spacing.md)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
            ) {
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                state.user?.let { user ->
                    UserHeader(user)
                    SubscriptionCard(state.entitlements?.subscriptionLevel ?: SubscriptionLevel.FREE, onEvent)
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

                SettingsItem(
                    label = stringResource(R.string.profile_vehicles_option),
                    icon = Icons.Default.CarRental,
                    onClick = safeClick { onEvent(Event.OnVehiclesClicked) }
                )

                SettingsItem(
                    label = stringResource(R.string.profile_preferences_option),
                    icon = Icons.Default.Settings,
                    onClick = safeClick { onEvent(Event.OnPreferencesClicked) }
                )

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                Text(
                    text = stringResource(R.string.profile_data_management_title),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = CanvasKitTheme.spacing.xs)
                )

                SettingsItem(
                    label = stringResource(R.string.profile_data_management_option),
                    icon = Icons.Default.SdCard,
                    onClick = safeClick { onEvent(Event.OnDataManagementClicked) }
                )

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                // Privacy & Legal Section
                Text(
                    text = stringResource(R.string.profile_privacy_section),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = CanvasKitTheme.spacing.xs)
                )

                SettingsItem(
                    label = stringResource(R.string.profile_welcome_guide_option),
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    onClick = safeClick { onEvent(Event.OnWelcomeGuideClicked) }
                )

                SettingsItem(
                    label = stringResource(R.string.profile_privacy_policy),
                    icon = Icons.Default.PrivacyTip,
                    onClick = safeClick { uriHandler.openUri("https://kilomenos-dev.web.app/privacy.html") }
                )

                SettingsItem(
                    label = stringResource(R.string.profile_delete_account),
                    icon = Icons.Default.DeleteForever,
                    onClick = safeClick { onEvent(Event.OnDeleteAccountClicked) }
                )

                Spacer(modifier = Modifier.weight(1f))

                CanvasKitButton(
                    onClick = safeClick { onEvent(Event.OnLogoutClicked) },
                    variant = CanvasKitButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.profile_logout_button),
                        color = CanvasKitTheme.colors.error,
                        style = CanvasKitTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xl))
            }

            // Toast-style Banner (Error)
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(Event.OnDismissError) }
            )
        }

        if (state.showLogoutConfirmation) {
            val messageRes = if (state.entitlements?.subscriptionLevel == SubscriptionLevel.PREMIUM) {
                R.string.profile_logout_confirmation_message_premium
            } else {
                R.string.profile_logout_confirmation_message_free
            }
            CanvasKitDialog(
                onDismissRequest = { onEvent(Event.OnLogoutCancelled) }
            ) {
                CanvasKitDialogContent(
                    title = {
                        Text(
                            text = stringResource(R.string.profile_logout_confirmation_title),
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CanvasKitTheme.colors.textPrimary
                        )
                    },
                    content = {
                        Text(
                            text = stringResource(messageRes),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    },
                    buttons = {
                        TextButton(onClick = { onEvent(Event.OnLogoutCancelled) }) {
                            Text(
                                text = stringResource(R.string.profile_logout_cancel),
                                style = CanvasKitTheme.typography.labelLarge,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
                        CanvasKitButton(
                            onClick = safeClick { onEvent(Event.OnLogoutConfirmed) },
                            variant = CanvasKitButtonVariant.Ghost
                        ) { _ ->
                            Text(
                                text = stringResource(R.string.profile_logout_confirm),
                                style = CanvasKitTheme.typography.labelLarge,
                                color = CanvasKitTheme.colors.error
                            )
                        }
                    }
                )
            }
        }

        if (state.showDeleteConfirmation) {
            CanvasKitDialog(
                onDismissRequest = { onEvent(Event.OnDeleteAccountCancelled) }
            ) {
                CanvasKitDialogContent(
                    title = {
                        Text(
                            text = stringResource(R.string.profile_delete_account_confirmation_title),
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CanvasKitTheme.colors.textPrimary
                        )
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.profile_delete_account_confirmation_message),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    },
                    buttons = {
                        TextButton(onClick = { onEvent(Event.OnDeleteAccountCancelled) }) {
                            Text(
                                text = stringResource(R.string.profile_logout_cancel),
                                style = CanvasKitTheme.typography.labelLarge,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
                        CanvasKitButton(
                            onClick = safeClick { onEvent(Event.OnDeleteAccountConfirmed) },
                            variant = CanvasKitButtonVariant.Ghost
                        ) { _ ->
                            Text(
                                text = stringResource(R.string.profile_delete_account_confirm),
                                style = CanvasKitTheme.typography.labelLarge,
                                color = CanvasKitTheme.colors.error
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UserHeader(user: es.joshluq.kmsafe.domain.model.User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = user.name,
            style = CanvasKitTheme.typography.headingLarge,
            color = CanvasKitTheme.colors.textPrimary
        )
        Text(
            text = user.email,
            style = CanvasKitTheme.typography.bodyLarge,
            color = CanvasKitTheme.colors.textSecondary
        )
    }
}

@Composable
private fun SubscriptionCard(
    level: SubscriptionLevel,
    onEvent: (Event) -> Unit
) {
    val isPremium = level == SubscriptionLevel.PREMIUM
    CanvasKitCard(
        variant = CanvasKitCardVariant.Elevated,
        modifier = Modifier.fillMaxWidth(),
        onClick = if (!isPremium) safeClick { onEvent(Event.OnUpgradeClicked) } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremium) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.md))
                Column {
                    Text(
                        text = if (isPremium) {
                            stringResource(R.string.profile_subscription_premium)
                        } else {
                            stringResource(R.string.profile_subscription_free)
                        },
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isPremium) {
                            stringResource(R.string.profile_sync_enabled)
                        } else {
                            stringResource(R.string.profile_sync_disabled)
                        },
                        style = CanvasKitTheme.typography.labelSmall,
                        color = if (isPremium) CanvasKitTheme.colors.success else CanvasKitTheme.colors.textSecondary
                    )
                }
            }

            if (!isPremium) {
                CanvasKitButton(
                    onClick = safeClick { onEvent(Event.OnUpgradeClicked) },
                    variant = CanvasKitButtonVariant.Ghost
                ) { _ ->
                    Text(
                        text = stringResource(R.string.profile_upgrade_button),
                        style = CanvasKitTheme.typography.labelLarge,
                        color = CanvasKitTheme.colors.brandAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    CanvasKitCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.sm))
                Text(
                    text = label,
                    style = CanvasKitTheme.typography.bodyLarge,
                    color = CanvasKitTheme.colors.textPrimary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CanvasKitTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun ProfileScreenPreview() {
    CanvasKitTheme {
        ProfileScreen(
            state = State(),
            onEvent = {}
        )
    }
}
