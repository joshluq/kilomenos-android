package es.joshluq.kmsafe.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import es.joshluq.canvaskit.components.feedback.CanvasKitDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialogContent
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingStrategy
import es.joshluq.canvaskit.components.text.CanvasKitRichText
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.login.components.BrandingSection
import es.joshluq.kmsafe.ui.util.safeClick
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CanvasKitCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
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
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitTextField(
                                    value = state.password,
                                    onValueChange = { onEvent(Event.OnPasswordChanged(it)) },
                                    label = stringResource(R.string.login_password_label),
                                    placeholder = "••••••••",
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
                                                contentDescription = stringResource(if (state.isPasswordVisible) R.string.acc_close else R.string.acc_open_menu),
                                                tint = CanvasKitTheme.colors.textSecondary
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CanvasKitButton(
                                    onClick = { onEvent(Event.OnLoginClicked) },
                                    enabled = state.isLoginEnabled,
                                    loading = state.isLoading,
                                    modifier = Modifier.fillMaxWidth()
                                ) { contentColor ->
                                    @Suppress("DEPRECATION")
                                    Text(
                                        text = stringResource(R.string.login_button),
                                        color = contentColor,
                                        style = CanvasKitTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = CanvasKitTheme.colors.borderSubtle)
                                    Text(
                                        text = " O ",
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        style = CanvasKitTheme.typography.labelSmall,
                                        color = CanvasKitTheme.colors.textSecondary
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = CanvasKitTheme.colors.borderSubtle)
                                }

                                CanvasKitButton(
                                    onClick = { onEvent(Event.OnGoogleSignInClicked) },
                                    variant = CanvasKitButtonVariant.Secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isLoading
                                ) { contentColor ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Email, // Icon should be Google but keeping it simple for now
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = contentColor
                                        )
                                        @Suppress("DEPRECATION")
                                        Text(
                                            modifier = Modifier.padding(start = 12.dp),
                                            text = "Continuar con Google",
                                            color = contentColor,
                                            style = CanvasKitTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
                        enabled = !state.isLoading,
                        variant = CanvasKitButtonVariant.Ghost,
                        onClick = safeClick { onNavigateToSignup() }
                    ) { contentColor ->
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.login_create_account),
                            color = contentColor,
                            style = CanvasKitTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    LegalFooter(
                        onTermsClick = { uriHandler.openUri(BuildConfig.TERMS_URL) },
                        onPrivacyClick = { uriHandler.openUri(BuildConfig.PRIVACY_URL) }
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
            CanvasKitDialog(
                onDismissRequest = { onEvent(Event.OnDismissUserConflict) }
            ) {
                CanvasKitDialogContent(
                    title = {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.login_conflict_title),
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CanvasKitTheme.colors.textPrimary
                        )
                    },
                    content = {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.login_conflict_message),
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    },
                    buttons = {
                        TextButton(onClick = { onEvent(Event.OnDismissUserConflict) }) {
                            @Suppress("DEPRECATION")
                            Text(
                                text = stringResource(R.string.profile_logout_cancel),
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
                        CanvasKitButton(
                            onClick = { onEvent(Event.OnConfirmUserConflict) },
                            variant = CanvasKitButtonVariant.Ghost
                        ) { _ ->
                            @Suppress("DEPRECATION")
                            Text(
                                text = stringResource(R.string.login_conflict_confirm),
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
private fun LegalFooter(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val termsText = stringResource(R.string.profile_terms_conditions)
    val privacyText = stringResource(R.string.profile_privacy_policy)
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
