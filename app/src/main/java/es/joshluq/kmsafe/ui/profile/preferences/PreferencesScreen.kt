package es.joshluq.kmsafe.ui.profile.preferences

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
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
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val viewModel: PreferencesViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionsResult by navController.currentBackStackEntryAsState().value
        ?.savedStateHandle
        ?.getStateFlow<Boolean?>("permissions_granted", null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    LaunchedEffect(permissionsResult) {
        when (permissionsResult) {
            true -> {
                viewModel.sendEvent(Event.OnPermissionsRationaleSuccess)
                navController.currentBackStackEntry?.savedStateHandle?.set("permissions_granted", null)
            }
            false -> {
                viewModel.sendEvent(Event.OnAutoTrackingToggled(false))
                navController.currentBackStackEntry?.savedStateHandle?.set("permissions_granted", null)
            }
            else -> { /* No-op */ }
        }
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateBack -> onNavigateBack()
                Effect.ShowPrivacyOptions -> onShowPrivacyOptions()
                Effect.NavigateToPermissions -> onNavigateToPermissions()
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
    var showCredits by remember { mutableStateOf(false) }

    if (showCredits) {
        SoftwareCreditsDialog(onDismiss = { showCredits = false })
    }

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
                            contentDescription = stringResource(R.string.acc_back),
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
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

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.5f)
                        )

                        PreferenceSwitchItem(
                            label = stringResource(R.string.preferences_auto_tracking_label),
                            description = if (state.isUserPremium) {
                                stringResource(R.string.preferences_auto_tracking_desc)
                            } else {
                                stringResource(R.string.preferences_auto_tracking_locked_desc)
                            },
                            checked = state.autoTrackingEnabled,
                            onCheckedChange = { onEvent(Event.OnAutoTrackingToggled(it)) },
                            enabled = state.isUserPremium,
                            trailingIcon = if (!state.isUserPremium) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CanvasKitTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                Text(
                    text = stringResource(R.string.preferences_about_header),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = CanvasKitTheme.spacing.xs)
                )

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xs))

                CanvasKitCard(
                    onClick = { showCredits = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.sm))
                        Column {
                            Text(
                                text = stringResource(R.string.preferences_oss_licenses_label),
                                style = CanvasKitTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = CanvasKitTheme.colors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.preferences_oss_licenses_desc),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
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
                            modifier = Modifier.fillMaxWidth(),
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
                                text = stringResource(R.string.preferences_manage_consent_label),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoftwareCreditsDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CanvasKitTheme.colors.backgroundSecondary
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CanvasKitTopBar(
                    title = { Text(stringResource(R.string.preferences_oss_licenses_label)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.textPrimary
                            )
                        }
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.preferences_oss_description),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textSecondary
                    )

                    val libraries = listOf(
                        "Jetpack Compose" to "Android's modern toolkit for building native UI.",
                        "Dagger Hilt" to "Dependency injection library for Android.",
                        "Retrofit & OkHttp" to "Type-safe HTTP client and networking stack.",
                        "Room Persistence" to "Abstraction layer over SQLite for robust data access.",
                        "Kotlin Coroutines & Flow" to "Standard for asynchronous and reactive programming.",
                        "Coil" to "Image loading library for Android backed by Coroutines.",
                        "Firebase SDKs" to "Analytics, Crashlytics, and Remote Config services.",
                        "Compose Cropper" to "Image manipulation tool for vehicle photos.",
                        "Jackson" to "High-performance JSON processor."
                    )

                    libraries.forEach { (name, desc) ->
                        CanvasKitCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = name,
                                    style = CanvasKitTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CanvasKitTheme.colors.textPrimary
                                )
                                Text(
                                    text = desc,
                                    style = CanvasKitTheme.typography.labelSmall,
                                    color = CanvasKitTheme.colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.preferences_oss_license_generic),
                                    style = CanvasKitTheme.typography.labelSmall,
                                    color = CanvasKitTheme.colors.brandAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
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
                color = if (enabled) CanvasKitTheme.colors.textPrimary else CanvasKitTheme.colors.textSecondary
            )
            Text(
                text = description,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailingIcon?.invoke()
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CanvasKitTheme.colors.onBrandAccent,
                    checkedTrackColor = CanvasKitTheme.colors.brandAccent,
                    disabledCheckedTrackColor = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.5f),
                    disabledUncheckedTrackColor = CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun PreferencesScreenPreview() {
    CanvasKitTheme {
        PreferencesScreen(
            state = State(
                rememberEmail = true,
                isLoading = false,
                isPrivacyOptionsRequired = true,
                isUserPremium = false
            ),
            onEvent = {}
        )
    }
}
