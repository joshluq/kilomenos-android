package es.joshluq.kmsafe.feature.auth.login

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingStrategy
import es.joshluq.canvaskit.components.text.CanvasKitRichText
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.feature.auth.R
import es.joshluq.kmsafe.core.ui.R as CoreR
import es.joshluq.kmsafe.feature.auth.login.components.BrandingSection
import es.joshluq.kmsafe.core.ui.util.safeClick
import kotlinx.coroutines.flow.Flow

@Composable
fun LoginRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LoginScreen(
        state = state,
        effects = viewModel.effects,
        onEvent = viewModel::sendEvent,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToPremiumPaywall = onNavigateToPremiumPaywall,
        onNavigateToSignup = onNavigateToSignup,
        onTriggerGoogleSignIn = {
            viewModel.triggerGoogleSignIn(context)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: State,
    effects: Flow<Effect>? = null,
    onEvent: (Event) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onTriggerGoogleSignIn: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(effects) {
        effects?.collect { effect ->
            when (effect) {
                Effect.NavigateToDashboard -> onNavigateToDashboard()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
                Effect.TriggerGoogleSignIn -> onTriggerGoogleSignIn()
            }
        }
    }

    CanvasKitLoadingScaffold(
        isLoading = false, // Disable global overlay
        loadingStrategy = CanvasKitLoadingStrategy.OverlayFullscreen,
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandingSection(
                    modifier = Modifier.fillMaxWidth(),
                    subtitle = stringResource(R.string.login_subtitle),
                    topPadding = true
                )

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
                                    value = state.email,
                                    onValueChange = { onEvent(Event.OnEmailChanged(it)) },
                                    label = stringResource(R.string.login_email_label),
                                    placeholder = stringResource(R.string.login_email_placeholder),
                                    errorText = state.emailError?.asString(),
                                    isError = state.emailError != null,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = stringResource(R.string.login_email_label),
                                            tint = CanvasKitTheme.colors.brandAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = {
                                        focusManager.moveFocus(
                                            FocusDirection.Next
                                        )
                                    }),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitTextField(
                                    value = state.password,
                                    onValueChange = { onEvent(Event.OnPasswordChanged(it)) },
                                    label = stringResource(R.string.login_password_label),
                                    placeholder = stringResource(R.string.login_password_placeholder),
                                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.login_password_label),
                                            tint = CanvasKitTheme.colors.brandAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { onEvent(Event.OnTogglePasswordVisibility) }) {
                                            Icon(
                                                imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = stringResource(
                                                    if (state.isPasswordVisible) CoreR.string.acc_close else CoreR.string.acc_open_menu
                                                ),
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
                                        if (state.isLoginEnabled) onEvent(Event.OnLoginClicked)
                                    }),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitButton(
                                    text = stringResource(R.string.login_button),
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onEvent(Event.OnLoginClicked)
                                    },
                                    enabled = state.isLoginEnabled,
                                    loading = state.isLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = CanvasKitTheme.colors.borderSubtle
                                    )
                                    Text(
                                        text = stringResource(R.string.login_divider_or),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        style = CanvasKitTheme.typography.labelSmall,
                                        color = CanvasKitTheme.colors.textSecondary
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = CanvasKitTheme.colors.borderSubtle
                                    )
                                }

                                CanvasKitButton(
                                    text = stringResource(R.string.login_google_button),
                                    icon = Icons.Default.Email, // Icon should be Google but keeping it simple for now
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onEvent(Event.OnGoogleSignInClicked)
                                    },
                                    variant = CanvasKitButtonVariant.Secondary,
                                    modifier = Modifier.fillMaxWidth().testTag("google_login_button"),
                                    enabled = !state.isLoading
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

                    Spacer(modifier = Modifier.height(32.dp))

                    CanvasKitButton(
                        text = stringResource(R.string.login_create_account),
                        enabled = !state.isLoading,
                        variant = CanvasKitButtonVariant.Ghost,
                        onClick = safeClick {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onNavigateToSignup()
                        },
                        modifier = Modifier.testTag("signup_link")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LegalFooter(
                        onTermsClick = { uriHandler.openUri(state.termsUrl) },
                        onPrivacyClick = { uriHandler.openUri(state.privacyUrl) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Toast-style Banner (Error)
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
                cancelText = stringResource(CoreR.string.profile_logout_cancel),
                onConfirm = { onEvent(Event.OnConfirmUserConflict) },
                onDismissRequest = { onEvent(Event.OnDismissUserConflict) },
                isDestructive = true
            )
        }
    }
}

@Composable
private fun LegalFooter(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val termsText = stringResource(CoreR.string.profile_terms_conditions)
    val privacyText = stringResource(CoreR.string.profile_privacy_policy)
    val fullString = stringResource(R.string.login_privacy_agreement, termsText, privacyText)

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
internal fun LoginScreenPreview() {
    CanvasKitTheme {
        LoginScreen(
            state = State(
                email = "josh@kmsafe.es",
                isLoginEnabled = true
            ),
            onEvent = {},
            onNavigateToDashboard = {},
            onNavigateToPremiumPaywall = {},
            onNavigateToSignup = {}
        )
    }
}
