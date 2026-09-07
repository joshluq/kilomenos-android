package es.joshluq.kmsafe.feature.fleet.setup

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.chips.CanvasKitChip
import es.joshluq.canvaskit.components.chips.CanvasKitChipVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePickerField
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.components.VehiclePhotoSelector
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.toTextProvider
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.feature.fleet.R
import es.joshluq.kmsafe.feature.fleet.components.BluetoothDevicePicker
import es.joshluq.kmsafe.feature.fleet.components.RentingStepLayout
import es.joshluq.kmsafe.feature.fleet.components.RentingStepProgressBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import es.joshluq.kmsafe.core.ui.R as CoreR

@Composable
fun SetupWizardRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCropper: (String) -> Unit,
    onNavigateToPremiumPaywall: () -> Unit = {},
    backStackEntry: NavBackStackEntry
) {
    val viewModel: SetupWizardViewModel = hiltViewModel()
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
                Effect.NavigateToDashboard -> onNavigateToDashboard()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
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
    val keyboardController = LocalSoftwareKeyboardController.current

    CanvasKitLoadingScaffold(
        isLoading = false,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
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
                    .padding(CanvasKitTheme.spacing.md),
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
                    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        BluetoothDevicePicker(
                            onDeviceSelected = { name, address ->
                                onEvent(Event.OnBluetoothDeviceSelected(name, address))
                            }
                        )
                    }
                }
            }

            if (state.showPremiumLimit) {
                CanvasKitConfirmDialog(
                    title = stringResource(R.string.premium_limit_vehicle_title),
                    message = stringResource(R.string.premium_limit_vehicle_message),
                    confirmText = stringResource(CoreR.string.premium_upgrade_confirm),
                    cancelText = stringResource(R.string.premium_upgrade_cancel),
                    onConfirm = { onEvent(Event.OnUpgradeClicked) },
                    onDismissRequest = { onEvent(Event.OnDismissPremiumLimit) },
                    icon = Icons.Default.Info
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
        onPrimaryActionClick = safeClick { onEvent(Event.OnNextClicked) }
    ) {
        VehiclePhotoSelector(
            imageUrl = state.vehicleImageUrl,
            selectedUri = state.selectedImageUri,
            onClick = safeClick { photoPickerLauncher.launch("image/*") },
            modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.lg)
        )

        CanvasKitTextField(
            label = stringResource(R.string.onboarding_vehicle_name_label),
            value = state.vehicleName,
            onValueChange = { onEvent(Event.OnVehicleNameChanged(it)) },
            errorText = state.vehicleNameError?.asString(),
            isError = state.vehicleNameError != null,
            placeholder = stringResource(R.string.onboarding_vehicle_name_placeholder),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.lg).testTag("setup_vehicle_name_input")
        )

        Text(
            text = stringResource(R.string.onboarding_fuel_type_label),
            style = CanvasKitTheme.typography.labelLarge,
            color = CanvasKitTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.sm)
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

@Composable
private fun ContractTimeframeStep(state: State, onEvent: (Event) -> Unit) {
    val focusManager = LocalFocusManager.current

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_timeframe_title),
        description = stringResource(R.string.setup_wizard_step_timeframe_desc),
        primaryActionLabel = stringResource(R.string.setup_wizard_action_next),
        onPrimaryActionClick = safeClick { onEvent(Event.OnNextClicked) }
    ) {
        val selectedDateMillis = remember(state.startDate) {
            runCatching {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(state.startDate)?.time
            }.getOrNull()
        }

        CanvasKitDatePickerField(
            label = stringResource(R.string.onboarding_start_date_label),
            selectedDateMillis = selectedDateMillis,
            onDateSelected = { millis ->
                if (millis != null) {
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    onEvent(Event.OnStartDateChanged(formatter.format(Date(millis))))
                } else {
                    onEvent(Event.OnStartDateChanged(""))
                }
            },
            placeholder = stringResource(R.string.onboarding_start_date_placeholder),
            errorText = state.startDateError?.asString(),
            isError = state.startDateError != null,
            modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.md)
        )

        CanvasKitTextField(
            label = stringResource(R.string.onboarding_duration_months_label),
            value = state.durationMonths,
            onValueChange = { newValue ->
                onEvent(Event.OnDurationMonthsChanged(newValue.filter { it.isDigit() }))
            },
            errorText = state.durationMonthsError?.asString(),
            isError = state.durationMonthsError != null,
            suffix = stringResource(R.string.onboarding_months_suffix),
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
        onPrimaryActionClick = safeClick { onEvent(Event.OnNextClicked) }
    ) {
        CanvasKitTextField(
            label = stringResource(R.string.onboarding_total_kms_label),
            value = state.totalKms,
            onValueChange = { newValue ->
                onEvent(Event.OnTotalKmsChanged(newValue.filter { it.isDigit() || it == '.' || it == ',' }))
            },
            errorText = state.totalKmsError?.asString(),
            isError = state.totalKmsError != null,
            suffix = stringResource(CoreR.string.onboarding_km_suffix),
            placeholder = stringResource(R.string.onboarding_total_kms_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.md)
        )

        Spacer(modifier = Modifier.height(12.dp))

        CanvasKitTextField(
            label = stringResource(R.string.onboarding_start_odometer_label),
            value = state.startOdometer,
            onValueChange = { newValue ->
                onEvent(Event.OnStartOdometerChanged(newValue.filter { it.isDigit() || it == '.' || it == ',' }))
            },
            errorText = state.startOdometerError?.asString(),
            isError = state.startOdometerError != null,
            suffix = stringResource(CoreR.string.onboarding_km_suffix),
            placeholder = stringResource(R.string.onboarding_start_odometer_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            helperText = stringResource(R.string.onboarding_start_odometer_support)
        )
        Spacer(modifier = Modifier.height(12.dp))

        CanvasKitTextField(
            label = stringResource(R.string.onboarding_current_odometer_label),
            value = state.currentOdometer,
            onValueChange = { newValue ->
                onEvent(Event.OnCurrentOdometerChanged(newValue.filter { it.isDigit() || it == '.' || it == ',' }))
            },
            errorText = state.currentOdometerError?.asString(),
            isError = state.currentOdometerError != null,
            suffix = stringResource(CoreR.string.onboarding_km_suffix),
            placeholder = stringResource(R.string.onboarding_current_odometer_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            helperText = stringResource(R.string.onboarding_current_odometer_support)
        )

        if (state.showMileageWarning) {
            Spacer(modifier = Modifier.height(16.dp))
            CanvasKitBanner(
                variant = CanvasKitAlertVariant.Warning,
                message = { Text(state.mileageWarningMessage?.asString() ?: "") },
                visible = true,
                onDismiss = null // Handled by input change in VM
            )
        }
    }
}

@Composable
private fun SmartActivationStep(state: State, onEvent: (Event) -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onEvent(Event.OnToggleBluetoothPicker)
        }
    }

    val isPremium = state.subscriptionLevel == SubscriptionLevel.PREMIUM

    RentingStepLayout(
        title = stringResource(R.string.setup_wizard_step_bluetooth_title),
        description = if (isPremium) {
            stringResource(R.string.setup_wizard_step_bluetooth_desc_premium)
        } else {
            stringResource(R.string.setup_wizard_step_bluetooth_desc_free)
        },
        primaryActionLabel = stringResource(R.string.setup_wizard_action_next),
        onPrimaryActionClick = safeClick { onEvent(Event.OnNextClicked) },
        primaryActionEnabled = !state.isLoading,
        secondaryAction = {
            TextButton(
                onClick = safeClick { onEvent(Event.OnSkipStepClicked) },
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
                    onClick = safeClick {
                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (hasPermission) {
                            onEvent(Event.OnToggleBluetoothPicker)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.setup_wizard_step_bluetooth_action),
                        color = CanvasKitTheme.colors.textPrimary
                    )
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
                    text = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress,
                    style = CanvasKitTheme.typography.headingMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.setup_wizard_step_bluetooth_linked),
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.success
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = safeClick { onEvent(Event.OnToggleBluetoothPicker) }) {
                    Text(
                        stringResource(R.string.onboarding_change_bluetooth),
                        color = CanvasKitTheme.colors.brandAccent
                    )
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
        onPrimaryActionClick = safeClick { onEvent(Event.OnNextClicked) },
        primaryActionEnabled = !state.isLoading,
        primaryActionLoading = state.isLoading,
        secondaryAction = {
            CanvasKitButton(
                variant = CanvasKitButtonVariant.Ghost,
                onClick = safeClick { onEvent(Event.OnSkipStepClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { contentColor ->
                Text(stringResource(R.string.setup_wizard_action_skip), color = contentColor)
            }
        }
    ) {
        CanvasKitTextField(
            label = stringResource(R.string.setup_wizard_step_advanced_price_label),
            value = state.excessDistancePrice,
            onValueChange = { newValue ->
                onEvent(Event.OnExcessDistancePriceChanged(newValue.filter { it.isDigit() || it == '.' || it == ',' }))
            },
            errorText = state.excessDistancePriceError?.asString(),
            isError = state.excessDistancePriceError != null,
            placeholder = stringResource(R.string.setup_wizard_step_advanced_price_placeholder),
            suffix = "€/km",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = CanvasKitTheme.spacing.md)
        )

        CanvasKitTextField(
            label = stringResource(R.string.setup_wizard_step_advanced_margin_label),
            value = state.courtesyMarginKms,
            onValueChange = { newValue ->
                onEvent(Event.OnCourtesyMarginKmsChanged(newValue.filter { it.isDigit() || it == '.' || it == ',' }))
            },
            errorText = state.courtesyMarginError?.asString(),
            isError = state.courtesyMarginError != null,
            placeholder = stringResource(R.string.setup_wizard_step_advanced_margin_placeholder),
            suffix = "km",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        if (state.showMileageWarning) {
            Spacer(modifier = Modifier.height(16.dp))
            CanvasKitBanner(
                variant = CanvasKitAlertVariant.Warning,
                message = { Text(state.mileageWarningMessage?.asString() ?: "") },
                visible = true,
                onDismiss = null // Handled by input change in VM
            )
        }
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
