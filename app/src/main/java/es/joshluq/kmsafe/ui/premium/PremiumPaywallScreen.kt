package es.joshluq.kmsafe.ui.premium

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.core.ui.util.safeClick

@Composable
fun PremiumPaywallRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
    onLaunchBilling: () -> Unit,
    viewModel: PremiumPaywallViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateToDashboard -> onNavigateToDashboard()
                Effect.NavigateBack -> onNavigateBack()
                Effect.LaunchBillingFlow -> onLaunchBilling()
            }
        }
    }

    PremiumPaywallScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    CanvasKitLoadingScaffold(
        isLoading = false,
        containerColor = CanvasKitTheme.colors.backgroundPrimary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CanvasKitTheme.colors.brandPrimary.copy(alpha = 0.1f),
                                CanvasKitTheme.colors.backgroundPrimary
                            )
                        )
                    )
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(52.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.premium_paywall_title),
                        style = CanvasKitTheme.typography.displayMedium,
                        color = CanvasKitTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = stringResource(R.string.premium_paywall_subtitle),
                        style = CanvasKitTheme.typography.bodyLarge,
                        color = CanvasKitTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Benefits Section with Cards
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BenefitCard(
                        icon = Icons.Default.AutoMode,
                        title = stringResource(R.string.premium_benefit_autotracking_title),
                        description = stringResource(R.string.premium_benefit_autotracking_desc),
                    )
                    BenefitCard(
                        icon = Icons.Default.Map,
                        title = stringResource(R.string.premium_benefit_routes_title),
                        description = stringResource(R.string.premium_benefit_routes_desc),
                    )
                    BenefitCard(
                        icon = Icons.Default.CloudDone,
                        title = stringResource(R.string.premium_benefit_sync_title),
                        description = stringResource(R.string.premium_benefit_sync_desc)
                    )
                    BenefitCard(
                        icon = Icons.Default.DirectionsCar,
                        title = stringResource(R.string.premium_benefit_fleet_title),
                        description = stringResource(R.string.premium_benefit_fleet_desc)
                    )
                    BenefitCard(
                        icon = Icons.Default.Assessment,
                        title = stringResource(R.string.premium_benefit_reports_title),
                        description = stringResource(R.string.premium_benefit_reports_desc)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // High-value highlight
                Text(
                    text = stringResource(R.string.premium_paywall_save_money_highlight),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.brandAccent,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CanvasKitTheme.colors.brandAccent.copy(alpha = 0.05f),
                            shape = CircleShape
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                )

                // Large spacer at the bottom to avoid content being covered by the sticky buttons
                Spacer(modifier = Modifier.height(200.dp))
            }

            // Sticky Footer with CTA Buttons
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                CanvasKitTheme.colors.backgroundPrimary.copy(alpha = 0.95f),
                                CanvasKitTheme.colors.backgroundPrimary
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .navigationBarsPadding()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isMigrating) {
                        Text(
                            text = stringResource(R.string.premium_paywall_migrating_msg),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    CanvasKitButton(
                        onClick = safeClick { onEvent(Event.OnUpgradeClicked) },
                        loading = state.isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("premium_buy_now_button")
                    ) { contentColor ->
                        Text(
                            text = if (state.isLoading) {
                                stringResource(
                                    R.string.common_processing
                                )
                            } else {
                                stringResource(R.string.premium_upgrade_button)
                            },
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }

                    CanvasKitButton(
                        variant = CanvasKitButtonVariant.Ghost,
                        onClick = safeClick { onEvent(Event.OnDismissClicked) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.premium_continue_free),
                            style = CanvasKitTheme.typography.labelLarge,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Error Banner
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
private fun BenefitCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                }
                Text(
                    text = description,
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun PremiumPaywallScreenPreview() {
    CanvasKitTheme {
        PremiumPaywallScreen(
            state = State(),
            onEvent = {}
        )
    }
}
