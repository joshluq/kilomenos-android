package es.joshluq.kmsafe.ui.profile.preferences

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.util.safeClick

@Composable
fun PreferencesRoute(
    onNavigateBack: () -> Unit,
    onShowPrivacyOptions: () -> Unit
) {
    val viewModel: PreferencesViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateBack -> onNavigateBack()
                Effect.ShowPrivacyOptions -> onShowPrivacyOptions()
            }
        }
    }

    PreferencesScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_preferences_option),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { onEvent(Event.OnBackClicked) }) {
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
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CanvasKitTheme.spacing.md)
            ) {
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                Text(
                    text = stringResource(R.string.preferences_account_settings_title),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = CanvasKitTheme.spacing.xs)
                )

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xs))

                CanvasKitCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        PreferenceSwitchItem(
                            label = stringResource(R.string.preferences_remember_email_label),
                            description = stringResource(R.string.preferences_remember_email_desc),
                            checked = state.rememberEmail,
                            onCheckedChange = { onEvent(Event.OnRememberEmailToggled(it)) }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.5f)
                        )

                        PreferenceSwitchItem(
                            label = stringResource(R.string.preferences_projection_banner_label),
                            description = stringResource(R.string.preferences_projection_banner_desc),
                            checked = state.showProjectionBanner,
                            onCheckedChange = { onEvent(Event.OnProjectionBannerToggled(it)) }
                        )
                    }
                }

                if (state.isPrivacyOptionsRequired) {
                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                    Text(
                        text = stringResource(R.string.profile_privacy_section),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = CanvasKitTheme.spacing.xs)
                    )

                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xs))

                    CanvasKitCard(
                        onClick = safeClick { onEvent(Event.OnManagePrivacyClicked) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.brandAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.sm))
                            Text(
                                text = "Gestionar preferencias de anuncios",
                                style = CanvasKitTheme.typography.bodyLarge,
                                color = CanvasKitTheme.colors.textPrimary
                            )
                        }
                    }
                }
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
    }
}

@Composable
private fun PreferenceSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = CanvasKitTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.textPrimary
            )
            Text(
                text = description,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CanvasKitTheme.colors.onBrandAccent,
                checkedTrackColor = CanvasKitTheme.colors.brandAccent
            )
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun PreferencesScreenPreview() {
    CanvasKitTheme {
        PreferencesScreen(
            state = State(rememberEmail = true, isLoading = false, isPrivacyOptionsRequired = true),
            onEvent = {}
        )
    }
}
