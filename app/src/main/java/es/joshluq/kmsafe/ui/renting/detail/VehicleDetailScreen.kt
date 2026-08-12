package es.joshluq.kmsafe.ui.renting.detail

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TextButton
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
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialogContent
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.ui.renting.components.ContractMetricCard
import es.joshluq.kmsafe.ui.renting.components.VehiclePhotoSelector
import es.joshluq.kmsafe.ui.util.safeClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                            contentDescription = stringResource(R.string.acc_back),
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = safeClick { onEvent(Event.OnEditClicked) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.history_edit_action))
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
                        .padding(horizontal = 24.dp)
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
                            value = formatDate(contract.startDate),
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
                    TextButton(
                        onClick = safeClick { showDeleteDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = CanvasKitTheme.colors.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.history_delete_action), color = CanvasKitTheme.colors.error)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

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
    CanvasKitDialog(onDismissRequest = onDismiss) {
        CanvasKitDialogContent(
            title = { Text(stringResource(R.string.vehicles_delete_confirmation_title), fontWeight = FontWeight.Bold) },
            content = { Text(stringResource(R.string.vehicle_detail_delete_msg)) },
            buttons = {
                TextButton(onClick = safeClick(onClick = onDismiss)) {
                    Text(stringResource(R.string.profile_logout_cancel), color = CanvasKitTheme.colors.textSecondary)
                }
                CanvasKitButton(
                    onClick = safeClick { onConfirm() },
                    variant = CanvasKitButtonVariant.Ghost
                ) {
                    Text(stringResource(R.string.vehicles_delete_confirm), color = CanvasKitTheme.colors.error)
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
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
                    totalKms = 60000,
                    startOdometer = 0,
                    currentOdometer = 1200,
                    isSelected = true,
                    bluetoothDeviceName = "My Tesla",
                    excessDistancePrice = 0.05,
                    courtesyMarginKms = 500
                )
            ),
            onEvent = {}
        )
    }
}
