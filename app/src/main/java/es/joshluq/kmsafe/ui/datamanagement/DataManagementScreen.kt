package es.joshluq.kmsafe.ui.datamanagement

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.usecase.ExportDataUseCase
import es.joshluq.kmsafe.core.ui.R as CoreR
import es.joshluq.kmsafe.feature.profile.R as ProfileR

@Composable
fun DataManagementRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit
) {
    val viewModel: DataManagementViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                viewModel.sendEvent(Event.OnImportClicked(content))
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.state.value.lastExportedContent?.let { content ->
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(content.toByteArray())
                }
            }
        }
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is Effect.CreateFile -> {
                    exportLauncher.launch(effect.filename)
                }
                Effect.LaunchImportPicker -> {
                    importLauncher.launch("application/json")
                }
                Effect.NavigateBack -> onNavigateBack()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
            }
        }
    }

    DataManagementScreen(
        state = state.value,
        onEvent = viewModel::sendEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    state: State,
    onEvent: (Event) -> Unit,
    onNavigateBack: () -> Unit
) {
    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(ProfileR.string.profile_data_management_title),
                        style = CanvasKitTheme.typography.headingMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                centeredTitle = true
            )
        },
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
                    .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // JSON Backup
                CanvasKitCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEvent(Event.OnExportClicked(ExportDataUseCase.Format.JSON)) }
                ) {
                    DataOptionContent(
                        title = stringResource(R.string.profile_data_management_export_json_title),
                        description = stringResource(R.string.profile_export_json_desc),
                        icon = Icons.Default.SdCard,
                        isLocked = !state.isPremium
                    )
                }

                // CSV Export
                CanvasKitCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEvent(Event.OnExportClicked(ExportDataUseCase.Format.CSV)) }
                ) {
                    DataOptionContent(
                        title = stringResource(R.string.profile_data_management_export_csv_title),
                        description = stringResource(R.string.profile_export_csv_desc),
                        icon = Icons.Default.Description,
                        isLocked = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import
                CanvasKitCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEvent(Event.OnImportRequested) }
                ) {
                    DataOptionContent(
                        title = stringResource(R.string.profile_import_data_option),
                        description = stringResource(R.string.profile_import_json_desc),
                        icon = Icons.Default.SdCard,
                        isLocked = !state.isPremium
                    )
                }
            }

            // Toast-style Banner (Error & Success)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasKitBanner(
                    variant = CanvasKitAlertVariant.Error,
                    message = { Text(state.error?.asString() ?: "") },
                    visible = state.error != null,
                    onDismiss = { onEvent(Event.OnDismissError) }
                )

                CanvasKitBanner(
                    variant = CanvasKitAlertVariant.Success,
                    message = { Text(state.successMessage?.asString() ?: "") },
                    visible = state.successMessage != null,
                    onDismiss = { onEvent(Event.OnDismissError) }
                )
            }
        }

        if (state.showPremiumLimit) {
            CanvasKitConfirmDialog(
                title = stringResource(R.string.premium_limit_data_title),
                message = stringResource(R.string.premium_limit_data_message),
                confirmText = stringResource(CoreR.string.premium_upgrade_confirm),
                cancelText = stringResource(R.string.premium_upgrade_cancel),
                onConfirm = { onEvent(Event.OnUpgradeClicked) },
                onDismissRequest = { onEvent(Event.OnDismissPremiumLimit) },
                icon = Icons.Default.Lock
            )
        }
    }
}

@Composable
private fun DataOptionContent(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLocked: Boolean = false
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
            }
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(CoreR.string.acc_locked),
                    tint = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = CanvasKitTheme.typography.bodyMedium,
            color = CanvasKitTheme.colors.textSecondary
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun DataManagementScreenPreview() {
    CanvasKitTheme {
        DataManagementScreen(
            state = State(isLoading = false, lastExportedContent = null),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
