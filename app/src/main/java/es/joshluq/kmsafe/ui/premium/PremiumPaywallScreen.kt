package es.joshluq.kmsafe.ui.premium

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.util.safeClick

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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.premium_paywall_title),
                    style = CanvasKitTheme.typography.headingLarge,
                    color = CanvasKitTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.premium_paywall_subtitle),
                    style = CanvasKitTheme.typography.bodyLarge,
                    color = CanvasKitTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                BenefitItem(
                    icon = Icons.Default.CloudDone,
                    title = stringResource(R.string.premium_benefit_sync_title),
                    description = stringResource(R.string.premium_benefit_sync_desc)
                )

                BenefitItem(
                    icon = Icons.Default.DirectionsCar,
                    title = stringResource(R.string.premium_benefit_fleet_title),
                    description = stringResource(R.string.premium_benefit_fleet_desc)
                )

                BenefitItem(
                    icon = Icons.Default.Assessment,
                    title = stringResource(R.string.premium_benefit_reports_title),
                    description = stringResource(R.string.premium_benefit_reports_desc)
                )

                BenefitItem(
                    icon = Icons.Default.Timeline,
                    title = stringResource(R.string.premium_benefit_roadmap_title),
                    description = stringResource(R.string.premium_benefit_roadmap_desc)
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(40.dp))

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
                    modifier = Modifier.fillMaxWidth()
                ) { contentColor ->
                    Text(
                        text = if (state.isLoading) stringResource(R.string.common_processing) else stringResource(R.string.premium_upgrade_button),
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                CanvasKitButton(
                    variant = CanvasKitButtonVariant.Ghost,
                    onClick = safeClick { onEvent(Event.OnDismissClicked) },
                    enabled = !state.isLoading,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.premium_continue_free),
                        style = CanvasKitTheme.typography.labelLarge,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CanvasKitTheme.colors.brandAccent,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = CanvasKitTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.textPrimary
            )
            Text(
                text = description,
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.textSecondary
            )
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
