package es.joshluq.kmsafe.ui.renting.setup

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePicker
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePickerDialog
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.ui.renting.components.*
import es.joshluq.kmsafe.ui.util.safeClick
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SetupWizardRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCropper: (String) -> Unit,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: SetupWizardViewModel = hiltViewModel()
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
                Effect.NavigateToDashboard -> onNavigateToDashboard()
                is Effect.NavigateToCropper -> onNavigateToCropper(effect.uri)
            }
        }
    }

    SetupWizardScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    CanvasKitLoadingScaffold(
        isLoading = false, // Wizard manages its own loading state for the save button
        topBar = {
            CanvasKitTopBar(
                title = {
                    RentingStepProgressBar(
                        currentStep = state.currentStep.index,
                        totalSteps = SetupStep.totalSteps,
                        modifier = Modifier.width(120.dp)
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
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "StepTransition"
            ) { step ->
                when (step) {
                    SetupStep.VEHICLE_IDENTITY -> VehicleIdentityStep(state, onEvent)
                    SetupStep.CONTRACT_TIMEFRAME -> ContractTimeframeStep(state, onEvent)
                    SetupStep.MILEAGE_BUDGET -> MileageBudgetStep(state, onEvent)
                    SetupStep.SMART_ACTIVATION -> SmartActivationStep(state, onEvent)
                    SetupStep.ADVANCED_PROTECTION -> AdvancedProtectionStep(state, onEvent)
                }
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

            // Date Picker Dialog
            if (state.showDatePicker) {
                CanvasKitDatePickerDialog(
                    onDismissRequest = { onEvent(Event.OnToggleDatePicker) },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Date(millis)
                                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                onEvent(Event.OnStartDateChanged(formatter.format(date)))
                            }
                            onEvent(Event.OnToggleDatePicker)
                        }) {
                            Text(stringResource(R.string.onboarding_date_picker_confirm), color = CanvasKitTheme.colors.brandAccent)
                        }
                    }
                ) {
                    CanvasKitDatePicker(state = datePickerState)
                }
            }

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
private fun VehicleIdentityStep(state: State, onEvent: (Event) -> Unit) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onEvent(Event.OnOriginalImageSelected(uri)) }

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_identity_title),
        description = stringResource(R.string.setup_wizard_step_identity_desc),
        primaryActionLabel = stringResource(R.string.setup_wizard_action_next),
        onPrimaryActionClick = { onEvent(Event.OnNextClicked) }
    ) {
        VehiclePhotoSelector(
            imageUrl = state.vehicleImageUrl,
            selectedUri = state.selectedImageUri,
            onClick = { photoPickerLauncher.launch("image/*") },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        RentingTextField(
            label = stringResource(R.string.onboarding_vehicle_name_label),
            value = state.vehicleName,
            onValueChange = { onEvent(Event.OnVehicleNameChanged(it)) },
            errorMessage = state.vehicleNameError?.asString(),
            placeholder = stringResource(R.string.onboarding_vehicle_name_placeholder),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
    }
}

@Composable
private fun ContractTimeframeStep(state: State, onEvent: (Event) -> Unit) {
    val focusManager = LocalFocusManager.current

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_timeframe_title),
        description = stringResource(R.string.setup_wizard_step_timeframe_desc),
        primaryActionLabel = stringResource(R.string.setup_wizard_action_next),
        onPrimaryActionClick = { onEvent(Event.OnNextClicked) }
    ) {
        RentingDisplayField(
            label = stringResource(R.string.onboarding_start_date_label),
            value = state.startDate,
            placeholder = stringResource(R.string.onboarding_start_date_placeholder),
            errorMessage = state.startDateError?.asString(),
            onClick = { onEvent(Event.OnToggleDatePicker) },
            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CanvasKitTheme.colors.brandAccent) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RentingTextField(
            label = stringResource(R.string.onboarding_duration_months_label),
            value = state.durationMonths,
            onValueChange = { onEvent(Event.OnDurationMonthsChanged(it)) },
            errorMessage = state.durationMonthsError?.asString(),
            trailingIcon = { Text(stringResource(R.string.onboarding_months_suffix), color = CanvasKitTheme.colors.brandAccent) },
            placeholder = stringResource(R.string.onboarding_duration_months_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

@Composable
private fun MileageBudgetStep(state: State, onEvent: (Event) -> Unit) {
    val focusManager = LocalFocusManager.current

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_mileage_title),
        description = stringResource(R.string.setup_wizard_step_mileage_desc),
        primaryActionLabel = stringResource(R.string.setup_wizard_action_next),
        onPrimaryActionClick = { onEvent(Event.OnNextClicked) }
    ) {
        RentingTextField(
            label = stringResource(R.string.onboarding_total_kms_label),
            value = state.totalKms,
            onValueChange = { onEvent(Event.OnTotalKmsChanged(it)) },
            errorMessage = state.totalKmsError?.asString(),
            trailingIcon = { Text(stringResource(R.string.onboarding_km_suffix), color = CanvasKitTheme.colors.brandAccent) },
            placeholder = stringResource(R.string.onboarding_total_kms_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RentingTextField(
            label = stringResource(R.string.onboarding_start_odometer_label),
            value = state.startOdometer,
            onValueChange = { onEvent(Event.OnStartOdometerChanged(it)) },
            errorMessage = state.startOdometerError?.asString(),
            trailingIcon = { Text(stringResource(R.string.onboarding_km_suffix), color = CanvasKitTheme.colors.brandAccent) },
            placeholder = stringResource(R.string.onboarding_start_odometer_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SmartActivationStep(state: State, onEvent: (Event) -> Unit) {
    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
    } else null

    val isPremium = state.subscriptionLevel == SubscriptionLevel.PREMIUM

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_bluetooth_title),
        description = if (isPremium) stringResource(R.string.setup_wizard_step_bluetooth_desc_premium) 
                      else stringResource(R.string.setup_wizard_step_bluetooth_desc_free),
        primaryActionLabel = stringResource(R.string.setup_wizard_action_next),
        onPrimaryActionClick = { onEvent(Event.OnNextClicked) },
        secondaryAction = {
            TextButton(
                onClick = { onEvent(Event.OnSkipStepClicked) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.setup_wizard_action_skip), color = CanvasKitTheme.colors.textSecondary)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.bluetoothDeviceAddress == null) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.2f),
                        modifier = Modifier.size(100.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                CanvasKitButton(
                    variant = CanvasKitButtonVariant.Secondary,
                    onClick = {
                        if (bluetoothPermissionState?.status?.isGranted != false) {
                            onEvent(Event.OnToggleBluetoothPicker)
                        } else {
                            bluetoothPermissionState.launchPermissionRequest()
                        }
                    }
                ) {
                    Text(stringResource(R.string.setup_wizard_step_bluetooth_action), color = CanvasKitTheme.colors.textPrimary)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.success,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress ?: "",
                    style = CanvasKitTheme.typography.headingMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.setup_wizard_step_bluetooth_linked),
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.success
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { onEvent(Event.OnToggleBluetoothPicker) }) {
                    Text(stringResource(R.string.onboarding_change_photo), color = CanvasKitTheme.colors.brandAccent)
                }
            }
        }
    }
}

@Composable
private fun AdvancedProtectionStep(state: State, onEvent: (Event) -> Unit) {
    val focusManager = LocalFocusManager.current

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_advanced_title),
        description = stringResource(R.string.setup_wizard_step_advanced_desc),
        primaryActionLabel = stringResource(R.string.setup_wizard_action_finish),
        onPrimaryActionClick = { onEvent(Event.OnNextClicked) },
        primaryActionLoading = state.isLoading,
        secondaryAction = {
            TextButton(
                onClick = { onEvent(Event.OnSkipStepClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.setup_wizard_action_skip), color = CanvasKitTheme.colors.textSecondary)
            }
        }
    ) {
        RentingTextField(
            label = stringResource(R.string.setup_wizard_step_advanced_price_label),
            value = state.excessDistancePrice,
            onValueChange = { onEvent(Event.OnExcessDistancePriceChanged(it)) },
            placeholder = stringResource(R.string.setup_wizard_step_advanced_price_placeholder),
            trailingIcon = { Text("€/km", color = CanvasKitTheme.colors.brandAccent) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RentingTextField(
            label = stringResource(R.string.setup_wizard_step_advanced_margin_label),
            value = state.courtesyMarginKms,
            onValueChange = { onEvent(Event.OnCourtesyMarginKmsChanged(it)) },
            placeholder = stringResource(R.string.setup_wizard_step_advanced_margin_placeholder),
            trailingIcon = { Text("km", color = CanvasKitTheme.colors.brandAccent) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SetupWizardScreenPreview() {
    CanvasKitTheme {
        SetupWizardScreen(
            state = State(
                currentStep = SetupStep.VEHICLE_IDENTITY,
                vehicleName = "Tesla Model 3"
            ),
            onEvent = {}
        )
    }
}
