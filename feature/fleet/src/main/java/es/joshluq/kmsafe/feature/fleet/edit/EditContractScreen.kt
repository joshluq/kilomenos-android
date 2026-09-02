package es.joshluq.kmsafe.feature.fleet.edit

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.chips.CanvasKitChip
import es.joshluq.canvaskit.components.chips.CanvasKitChipVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.components.VehiclePhotoSelector
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.safeClickable
import es.joshluq.kmsafe.core.ui.util.toTextProvider
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.feature.fleet.R
import es.joshluq.kmsafe.feature.fleet.components.BluetoothDevicePicker
import es.joshluq.kmsafe.core.ui.R as CoreR

@Composable
fun EditContractRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCropper: (String) -> Unit,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: EditContractViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val croppedUri by backStackEntry.savedStateHandle.getStateFlow<String?>(
        "cropped_uri",
        null
    ).collectAsStateWithLifecycle()

    LaunchedEffect(croppedUri) {
        croppedUri?.let { uri ->
            viewModel.sendEvent(Event.OnImageSelected(uri.toUri()))
            backStackEntry.savedStateHandle.remove<String>("cropped_uri")
        }
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                Effect.NavigateBack -> onNavigateBack()
                is Effect.NavigateToCropper -> onNavigateToCropper(effect.uri)
            }
        }
    }

    EditContractScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditContractScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onEvent(Event.OnOriginalImageSelected(uri)) }

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(CoreR.string.history_edit_title),
                        style = CanvasKitTheme.typography.headingMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = safeClick {
                            keyboardController?.hide()
                            onEvent(Event.OnBackClicked)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.acc_back),
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val focusManager = LocalFocusManager.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.lg)
            ) {
                VehiclePhotoSelector(
                    imageUrl = state.vehicleImageUrl,
                    selectedUri = state.selectedImageUri,
                    onClick = safeClick { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.padding(top = 24.dp)
                )

                // Group 1: General Details
                EditSectionCard(title = stringResource(R.string.onboarding_vehicle_name_label)) {
                    Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)) {
                        CanvasKitTextField(
                            label = stringResource(R.string.onboarding_vehicle_name_label),
                            value = state.vehicleName,
                            onValueChange = { onEvent(Event.OnVehicleNameChanged(it)) },
                            errorText = state.vehicleNameError?.asString(),
                            isError = state.vehicleNameError != null,
                            placeholder = stringResource(R.string.onboarding_vehicle_name_placeholder),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Text(
                            text = stringResource(R.string.onboarding_fuel_type_label),
                            style = CanvasKitTheme.typography.labelLarge,
                            color = CanvasKitTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = CanvasKitTheme.spacing.sm)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val fuelOptions = FuelType.entries.toTypedArray()
                            fuelOptions.forEach { fuelType ->
                                CanvasKitChip(
                                    selected = state.fuelType == fuelType,
                                    variant = CanvasKitChipVariant.Outlined,
                                    onClick = safeClick { onEvent(Event.OnFuelTypeChanged(fuelType)) },
                                    label = {
                                        Text(
                                            text = fuelType.toTextProvider().asString(),
                                            style = CanvasKitTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Group 2: Contract Adjustments
                EditSectionCard(title = stringResource(R.string.vehicle_detail_contract_section)) {
                    Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)) {
                        CanvasKitTextField(
                            label = stringResource(R.string.onboarding_duration_months_label),
                            value = state.durationMonths,
                            onValueChange = { onEvent(Event.OnDurationMonthsChanged(it)) },
                            errorText = state.durationMonthsError?.asString(),
                            isError = state.durationMonthsError != null,
                            suffix = stringResource(R.string.onboarding_months_suffix),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        CanvasKitTextField(
                            label = stringResource(R.string.onboarding_total_kms_label),
                            value = state.totalKms,
                            onValueChange = { onEvent(Event.OnTotalKmsChanged(it)) },
                            errorText = state.totalKmsError?.asString(),
                            isError = state.totalKmsError != null,
                            suffix = stringResource(CoreR.string.onboarding_km_suffix),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }
                }

                // Group 3: Intelligence & Protection
                EditSectionCard(
                    title = stringResource(R.string.vehicle_detail_smart_section),
                    icon = Icons.Default.Security
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CanvasKitTextField(
                                label = stringResource(R.string.onboarding_bluetooth_label),
                                value = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = stringResource(R.string.onboarding_bluetooth_placeholder),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = CanvasKitTheme.colors.brandAccent
                                    )
                                }
                            )
                            // Transparent overlay to capture clicks on the entire field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .safeClickable {
                                        keyboardController?.hide()
                                        if (bluetoothPermissionState?.status?.isGranted != false) {
                                            onEvent(Event.OnToggleBluetoothPicker)
                                        } else {
                                            bluetoothPermissionState.launchPermissionRequest()
                                        }
                                    }
                            )
                        }

                        CanvasKitTextField(
                            label = stringResource(R.string.setup_wizard_step_advanced_price_label),
                            value = state.excessDistancePrice,
                            onValueChange = { onEvent(Event.OnExcessDistancePriceChanged(it)) },
                            placeholder = stringResource(R.string.setup_wizard_step_advanced_price_placeholder),
                            suffix = "€/km",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        CanvasKitTextField(
                            label = stringResource(R.string.setup_wizard_step_advanced_margin_label),
                            value = state.courtesyMarginKms,
                            onValueChange = { onEvent(Event.OnCourtesyMarginKmsChanged(it)) },
                            placeholder = stringResource(R.string.setup_wizard_step_advanced_margin_placeholder),
                            suffix = "km",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xs))

                CanvasKitButton(
                    text = stringResource(CoreR.string.history_edit_save),
                    onClick = safeClick {
                        keyboardController?.hide()
                        onEvent(Event.OnSaveClicked)
                    },
                    loading = state.isSaving,
                    enabled = !state.isSaving && state.isDirty,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(48.dp))
            }

            // Error Banner
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(Event.OnDismissError) }
            )

            if (state.showBluetoothPicker) {
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { onEvent(Event.OnToggleBluetoothPicker) },
                    sheetState = bottomSheetState,
                    containerColor = CanvasKitTheme.colors.backgroundPrimary
                ) {
                    BluetoothDevicePicker(
                        onDeviceSelected = { name, address ->
                            onEvent(Event.OnBluetoothDeviceSelected(name ?: address, address))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    icon: ImageVector? = null,
    backgroundColor: Color = CanvasKitTheme.colors.backgroundPrimary,
    content: @Composable ColumnScope.() -> Unit
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title.uppercase(),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun EditContractScreenPreview() {
    CanvasKitTheme {
        EditContractScreen(
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
                ),
                vehicleName = "Tesla Model 3",
                durationMonths = "48",
                totalKms = "60000",
                excessDistancePrice = "0.05",
                courtesyMarginKms = "500",
                isDirty = false
            ),
            onEvent = {}
        )
    }
}
