package es.joshluq.kmsafe.feature.fleet.detail

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonSize
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.components.ContractMetricCard
import es.joshluq.kmsafe.core.ui.components.VehiclePhotoSelector
import es.joshluq.kmsafe.core.ui.util.DateUtils
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.feature.fleet.R
import es.joshluq.kmsafe.core.ui.R as CoreR

@Composable
fun VehicleDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val viewModel: VehicleDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateBack -> onNavigateBack()
                is Effect.NavigateToEdit -> onNavigateToEdit(effect.vehicleId)
            }
        }
    }

    VehicleDetailScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val bluetoothPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            androidx.compose.ui.platform.LocalContext.current,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val showBluetoothWarning = state.renting?.bluetoothDeviceAddress != null && !bluetoothPermissionGranted

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.vehicle_detail_title),
                        style = CanvasKitTheme.typography.headingMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { onEvent(Event.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.acc_back),
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = safeClick { onEvent(Event.OnEditClicked) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(CoreR.string.history_edit_action))
                    }
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            state.renting?.let { contract ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
                ) {
                    VehiclePhotoSelector(
                        imageUrl = contract.vehicleImageUrl,
                        isReadOnly = true,
                        selectedUri = null,
                        onClick = { },
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = contract.vehicleName,
                        style = CanvasKitTheme.typography.headingLarge,
                        color = CanvasKitTheme.colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Section: Contract
                    SectionHeader(stringResource(R.string.vehicle_detail_contract_section))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContractMetricCard(
                            icon = Icons.Default.Event,
                            label = stringResource(R.string.onboarding_start_date_label),
                            value = DateUtils.formatDate(contract.startDate),
                            modifier = Modifier.weight(1f)
                        )
                        ContractMetricCard(
                            icon = Icons.Default.Timelapse,
                            label = stringResource(R.string.onboarding_duration_months_label),
                            value = "${contract.durationMonths} ${stringResource(R.string.onboarding_months_suffix)}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section: Odometer
                    SectionHeader(stringResource(R.string.vehicle_detail_odometer_section))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContractMetricCard(
                            icon = Icons.Default.Speed,
                            label = stringResource(R.string.onboarding_total_kms_label),
                            value = "${contract.totalKms} km",
                            modifier = Modifier.weight(1f)
                        )
                        ContractMetricCard(
                            icon = Icons.Default.DirectionsCar,
                            label = stringResource(R.string.onboarding_start_odometer_label),
                            value = "${contract.startOdometer} km",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section: Smart
                    SectionHeader(stringResource(R.string.vehicle_detail_smart_section))
                    ContractMetricCard(
                        icon = Icons.Default.Bluetooth,
                        label = stringResource(R.string.onboarding_bluetooth_label),
                        value = contract.bluetoothDeviceName ?: contract.bluetoothDeviceAddress ?: stringResource(R.string.vehicle_detail_no_bluetooth),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showBluetoothWarning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CanvasKitBanner(
                            variant = CanvasKitAlertVariant.Warning,
                            message = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.vehicle_detail_bt_permission_warning_title),
                                        style = CanvasKitTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.vehicle_detail_bt_permission_warning_desc),
                                        style = CanvasKitTheme.typography.labelSmall
                                    )
                                }
                            },
                            action = {
                                CanvasKitButton(
                                    text = stringResource(R.string.vehicle_detail_bt_permission_warning_action),
                                    onClick = safeClick {
                                        @Suppress("KotlinConstantConditions")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                        }
                                    },
                                    variant = CanvasKitButtonVariant.Primary,
                                    size = CanvasKitButtonSize.Small
                                )
                            },
                            visible = true,
                            onDismiss = null, // Non-dismissible until granted
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section: Advanced
                    SectionHeader(stringResource(R.string.vehicle_detail_advanced_section))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContractMetricCard(
                            icon = Icons.Default.Euro,
                            label = stringResource(R.string.setup_wizard_step_advanced_price_label),
                            value = contract.excessDistancePrice?.let { "$it €/km" } ?: stringResource(R.string.vehicle_detail_no_advanced_data),
                            modifier = Modifier.weight(1f)
                        )
                        ContractMetricCard(
                            icon = Icons.Default.Shield,
                            label = stringResource(R.string.setup_wizard_step_advanced_margin_label),
                            value = if (contract.courtesyMarginKms > 0) {
                                "${contract.courtesyMarginKms} km"
                            } else {
                                stringResource(
                                    R.string.vehicle_detail_no_advanced_data
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Delete Action
                    CanvasKitButton(
                        text = stringResource(CoreR.string.history_delete_action),
                        icon = Icons.Default.Delete,
                        onClick = safeClick {
                            val isSelected = contract.isSelected
                            if (isSelected) {
                                onEvent(Event.OnDeleteClicked)
                            } else {
                                showDeleteDialog = true
                            }
                        },
                        variant = CanvasKitButtonVariant.Ghost,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(32.dp))
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

            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        showDeleteDialog = false
                        onEvent(Event.OnConfirmDelete)
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = CanvasKitTheme.typography.labelSmall,
        color = CanvasKitTheme.colors.textSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CanvasKitConfirmDialog(
        title = stringResource(R.string.vehicles_delete_confirmation_title),
        message = stringResource(R.string.vehicle_detail_delete_msg),
        confirmText = stringResource(R.string.vehicles_delete_confirm),
        cancelText = stringResource(CoreR.string.profile_logout_cancel),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        isDestructive = true,
        icon = Icons.Default.Delete
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun VehicleDetailScreenPreview() {
    CanvasKitTheme {
        VehicleDetailScreen(
            state = State(
                isLoading = false,
                renting = RentingContract(
                    id = "1",
                    vehicleName = "Tesla Model 3",
                    startDate = System.currentTimeMillis() - 3888000000L,
                    durationMonths = 48,
                    totalKms = 60000.0,
                    startOdometer = 0.0,
                    currentOdometer = 1200.0,
                    isSelected = true,
                    bluetoothDeviceName = "My Tesla",
                    excessDistancePrice = 0.05,
                    courtesyMarginKms = 500.0
                )
            ),
            onEvent = {}
        )
    }
}
