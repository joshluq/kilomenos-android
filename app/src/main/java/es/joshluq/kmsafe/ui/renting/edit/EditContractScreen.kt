package es.joshluq.kmsafe.ui.renting.edit

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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.ui.renting.components.BluetoothDevicePicker
import es.joshluq.kmsafe.ui.renting.components.RentingDisplayField
import es.joshluq.kmsafe.ui.renting.components.RentingTextField
import es.joshluq.kmsafe.ui.renting.components.VehiclePhotoSelector
import es.joshluq.kmsafe.ui.util.safeClick

@Composable
fun EditContractRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCropper: (String) -> Unit
) {
    val viewModel: EditContractViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

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
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        text = stringResource(R.string.history_edit_title),
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
                            contentDescription = stringResource(R.string.acc_back),
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
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                VehiclePhotoSelector(
                    imageUrl = state.vehicleImageUrl,
                    selectedUri = state.selectedImageUri,
                    onClick = safeClick { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.padding(top = 24.dp)
                )

                // Group 1: General Details
                EditSectionCard(title = stringResource(R.string.onboarding_vehicle_name_label)) {
                    RentingTextField(
                        label = "",
                        value = state.vehicleName,
                        onValueChange = { onEvent(Event.OnVehicleNameChanged(it)) },
                        errorMessage = state.vehicleNameError?.asString(),
                        placeholder = stringResource(R.string.onboarding_vehicle_name_placeholder),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }

                // Group 2: Contract Adjustments
                EditSectionCard(title = stringResource(R.string.vehicle_detail_contract_section)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RentingTextField(
                            label = stringResource(R.string.onboarding_duration_months_label),
                            value = state.durationMonths,
                            onValueChange = { onEvent(Event.OnDurationMonthsChanged(it)) },
                            errorMessage = state.durationMonthsError?.asString(),
                            trailingIcon = {
                                Text(
                                    stringResource(R.string.onboarding_months_suffix),
                                    color = CanvasKitTheme.colors.brandAccent
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        RentingTextField(
                            label = stringResource(R.string.onboarding_total_kms_label),
                            value = state.totalKms,
                            onValueChange = { onEvent(Event.OnTotalKmsChanged(it)) },
                            errorMessage = state.totalKmsError?.asString(),
                            trailingIcon = {
                                Text(
                                    stringResource(R.string.onboarding_km_suffix),
                                    color = CanvasKitTheme.colors.brandAccent
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RentingDisplayField(
                            label = stringResource(R.string.onboarding_bluetooth_label),
                            value = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress ?: "",
                            placeholder = stringResource(R.string.onboarding_bluetooth_placeholder),
                            onClick = safeClick {
                                keyboardController?.hide()
                                if (bluetoothPermissionState?.status?.isGranted != false) {
                                    onEvent(Event.OnToggleBluetoothPicker)
                                } else {
                                    bluetoothPermissionState.launchPermissionRequest()
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = CanvasKitTheme.colors.brandAccent
                                )
                            }
                        )

                        RentingTextField(
                            label = stringResource(R.string.setup_wizard_step_advanced_price_label),
                            value = state.excessDistancePrice,
                            onValueChange = { onEvent(Event.OnExcessDistancePriceChanged(it)) },
                            placeholder = stringResource(R.string.setup_wizard_step_advanced_price_placeholder),
                            trailingIcon = { Text("€/km", color = CanvasKitTheme.colors.brandAccent) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        RentingTextField(
                            label = stringResource(R.string.setup_wizard_step_advanced_margin_label),
                            value = state.courtesyMarginKms,
                            onValueChange = { onEvent(Event.OnCourtesyMarginKmsChanged(it)) },
                            placeholder = stringResource(R.string.setup_wizard_step_advanced_margin_placeholder),
                            trailingIcon = { Text("km", color = CanvasKitTheme.colors.brandAccent) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                CanvasKitButton(
                    onClick = safeClick {
                        keyboardController?.hide()
                        onEvent(Event.OnSaveClicked)
                    },
                    loading = state.isSaving,
                    enabled = !state.isSaving && state.isDirty,
                    modifier = Modifier.fillMaxWidth()
                ) { contentColor ->
                    Text(
                        text = stringResource(R.string.history_edit_save),
                        color = contentColor,
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                BluetoothDevicePicker(
                    onDismiss = { onEvent(Event.OnToggleBluetoothPicker) },
                    onDeviceSelected = { name, address ->
                        onEvent(Event.OnBluetoothDeviceSelected(name, address))
                    },
                    sheetState = bottomSheetState
                )
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
                    totalKms = 60000,
                    startOdometer = 0,
                    currentOdometer = 1200,
                    isSelected = true,
                    bluetoothDeviceName = "My Tesla",
                    excessDistancePrice = 0.05,
                    courtesyMarginKms = 500
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
