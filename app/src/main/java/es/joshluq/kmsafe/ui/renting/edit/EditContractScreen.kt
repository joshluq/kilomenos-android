package es.joshluq.kmsafe.ui.renting.edit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.renting.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EditContractRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCropper: (String) -> Unit,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: EditContractViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val croppedUri by backStackEntry.savedStateHandle.getStateFlow<String?>("cropped_uri", null).collectAsStateWithLifecycle()
    
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
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
    } else null

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
                    IconButton(onClick = { 
                        keyboardController?.hide()
                        onEvent(Event.OnBackClicked) 
                    }) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Image Picker
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    VehiclePhotoSelector(
                        imageUrl = state.vehicleImageUrl,
                        selectedUri = state.selectedImageUri,
                        onClick = { photoPickerLauncher.launch("image/*") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                RentingTextField(
                    label = stringResource(R.string.onboarding_vehicle_name_label),
                    value = state.vehicleName,
                    onValueChange = { onEvent(Event.OnVehicleNameChanged(it)) },
                    errorMessage = state.vehicleNameError?.asString(),
                    placeholder = stringResource(R.string.onboarding_vehicle_name_placeholder),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // Locked Start Date
                RentingDisplayField(
                    label = stringResource(R.string.onboarding_start_date_label),
                    value = state.renting?.let { formatDate(it.startDate) } ?: "",
                    trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CanvasKitTheme.colors.textSecondary, modifier = Modifier.size(20.dp)) }
                )

                RentingTextField(
                    label = stringResource(R.string.onboarding_duration_months_label),
                    value = state.durationMonths,
                    onValueChange = { onEvent(Event.OnDurationMonthsChanged(it)) },
                    errorMessage = state.durationMonthsError?.asString(),
                    trailingIcon = { Text(stringResource(R.string.onboarding_months_suffix), color = CanvasKitTheme.colors.brandAccent) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )

                RentingTextField(
                    label = stringResource(R.string.onboarding_total_kms_label),
                    value = state.totalKms,
                    onValueChange = { onEvent(Event.OnTotalKmsChanged(it)) },
                    errorMessage = state.totalKmsError?.asString(),
                    trailingIcon = { Text(stringResource(R.string.onboarding_km_suffix), color = CanvasKitTheme.colors.brandAccent) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )

                // Locked Odometer Initial
                RentingDisplayField(
                    label = stringResource(R.string.onboarding_start_odometer_label),
                    value = "${state.renting?.startOdometer ?: 0} km",
                    trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CanvasKitTheme.colors.textSecondary, modifier = Modifier.size(20.dp)) }
                )

                // Smart Linking
                RentingDisplayField(
                    label = stringResource(R.string.onboarding_bluetooth_label),
                    value = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress ?: "",
                    placeholder = stringResource(R.string.onboarding_bluetooth_placeholder),
                    onClick = {
                        keyboardController?.hide()
                        if (bluetoothPermissionState?.status?.isGranted != false) {
                            onEvent(Event.OnToggleBluetoothPicker)
                        } else {
                            bluetoothPermissionState.launchPermissionRequest()
                        }
                    },
                    trailingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CanvasKitTheme.colors.brandAccent) }
                )

                // Advanced Protection (Price & Margin)
                RentingTextField(
                    label = stringResource(R.string.setup_wizard_step_advanced_price_label),
                    value = state.excessDistancePrice,
                    onValueChange = { onEvent(Event.OnExcessDistancePriceChanged(it)) },
                    placeholder = stringResource(R.string.setup_wizard_step_advanced_price_placeholder),
                    trailingIcon = { Text("€/km", color = CanvasKitTheme.colors.brandAccent) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                )

                RentingTextField(
                    label = stringResource(R.string.setup_wizard_step_advanced_margin_label),
                    value = state.courtesyMarginKms,
                    onValueChange = { onEvent(Event.OnCourtesyMarginKmsChanged(it)) },
                    placeholder = stringResource(R.string.setup_wizard_step_advanced_margin_placeholder),
                    trailingIcon = { Text("km", color = CanvasKitTheme.colors.brandAccent) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(32.dp))

                CanvasKitButton(
                    onClick = { 
                        keyboardController?.hide()
                        onEvent(Event.OnSaveClicked) 
                    },
                    loading = state.isSaving,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { contentColor ->
                    Text(stringResource(R.string.history_edit_save), color = contentColor, style = CanvasKitTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(64.dp))
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
