package es.joshluq.kmsafe.ui.signup

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingStrategy
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.components.text.CanvasKitRichText
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.login.components.BrandingSection
import es.joshluq.kmsafe.ui.util.safeClick
import kotlinx.coroutines.flow.Flow

@Composable
fun SignupRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToWelcomeDiscovery: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SignupScreen(
        state = state,
        effects = viewModel.effects,
        onEvent = viewModel::sendEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToWelcomeDiscovery = onNavigateToWelcomeDiscovery,
        onNavigateToPremiumPaywall = onNavigateToPremiumPaywall
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    state: State,
    effects: Flow<Effect>? = null,
    onEvent: (Event) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToWelcomeDiscovery: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(effects) {
        effects?.collect { effect ->
            when (effect) {
                Effect.NavigateBack -> onNavigateBack()
                Effect.NavigateToDashboard -> onNavigateToDashboard()
                Effect.NavigateToWelcomeDiscovery -> onNavigateToWelcomeDiscovery()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
            }
        }
    }

    CanvasKitLoadingScaffold(
        isLoading = false,
        loadingStrategy = CanvasKitLoadingStrategy.OverlayFullscreen,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.signup_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = safeClick {
                            keyboardController?.hide()
                            onNavigateBack()
                        }
                    ) {
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
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandingSection(subtitle = stringResource(R.string.signup_subtitle))

                Column(
                    modifier = Modifier
                        .offset(y = (-40).dp)
                        .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CanvasKitCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Column(
                                modifier = Modifier.padding(CanvasKitTheme.spacing.md),
                                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
                            ) {
                                CanvasKitTextField(
                                    value = state.name,
                                    onValueChange = { onEvent(Event.OnNameChanged(it)) },
                                    label = stringResource(R.string.signup_name_label),
                                    placeholder = stringResource(R.string.signup_name_placeholder),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = CanvasKitTheme.colors.brandAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitTextField(
                                    value = state.email,
                                    onValueChange = { onEvent(Event.OnEmailChanged(it)) },
                                    label = stringResource(R.string.login_email_label),
                                    placeholder = stringResource(R.string.login_email_placeholder),
                                    errorText = state.emailError?.asString(),
                                    isError = state.emailError != null,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = CanvasKitTheme.colors.brandAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitTextField(
                                    value = state.password,
                                    onValueChange = { onEvent(Event.OnPasswordChanged(it)) },
                                    label = stringResource(R.string.login_password_label),
                                    placeholder = stringResource(R.string.login_password_placeholder),
                                    errorText = state.passwordError?.asString(),
                                    isError = state.passwordError != null,
                                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = CanvasKitTheme.colors.brandAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { onEvent(Event.OnTogglePasswordVisibility) }) {
                                            Icon(
                                                imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = CanvasKitTheme.colors.textSecondary
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        if (state.isSignupEnabled) onEvent(Event.OnSignupClicked)
                                    }),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitButton(
                                    text = stringResource(R.string.signup_button),
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onEvent(Event.OnSignupClicked)
                                    },
                                    enabled = state.isSignupEnabled,
                                    loading = state.isLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                PrivacyPolicyLink(
                                    onTermsClick = { uriHandler.openUri(BuildConfig.TERMS_URL) },
                                    onPrivacyClick = { uriHandler.openUri(BuildConfig.PRIVACY_URL) }
                                )
                            }

                            // Silent Interaction Block Overlay
                            if (state.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .pointerInput(Unit) {
                                            // Consume all touch events without visual changes
                                        }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Toast-style Banner
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(Event.OnDismissError) }
            )
        }

        if (state.showUserConflictWarning) {
            CanvasKitConfirmDialog(
                title = stringResource(R.string.login_conflict_title),
                message = stringResource(R.string.login_conflict_message),
                confirmText = stringResource(R.string.login_conflict_confirm),
                cancelText = stringResource(R.string.profile_logout_cancel),
                onConfirm = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onEvent(Event.OnConfirmUserConflict)
                },
                onDismissRequest = { onEvent(Event.OnDismissUserConflict) },
                isDestructive = true
            )
        }
    }
}

@Composable
private fun PrivacyPolicyLink(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val termsText = stringResource(R.string.profile_terms_conditions)
    val privacyText = stringResource(R.string.profile_privacy_policy)
    val fullString = stringResource(R.string.signup_privacy_agreement, termsText, privacyText)

    // Splitting the string to maintain i18n while using the RichText DSL
    val parts = fullString.split(termsText, privacyText)

    CanvasKitRichText(
        modifier = Modifier.fillMaxWidth(),
        style = CanvasKitTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
        color = CanvasKitTheme.colors.textSecondary
    ) {
        if (parts.isNotEmpty()) append(parts[0])
        appendLink(termsText) { onTermsClick() }
        if (parts.size > 1) append(parts[1])
        appendLink(privacyText) { onPrivacyClick() }
        if (parts.size > 2) append(parts[2])
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun SignupScreenPreview() {
    CanvasKitTheme {
        SignupScreen(
            state = State(
                name = "Juan Pérez",
                email = "juan@example.com",
                isSignupEnabled = true
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToDashboard = {},
            onNavigateToWelcomeDiscovery = {},
            onNavigateToPremiumPaywall = {}
        )
    }
}
