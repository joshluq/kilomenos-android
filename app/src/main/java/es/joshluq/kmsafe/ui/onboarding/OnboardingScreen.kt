package es.joshluq.kmsafe.ui.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitSkeleton
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePicker
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePickerDialog
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.inputs.CanvasKitTextFieldVariant
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.components.sheets.CanvasKitBottomSheet
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.util.safeClick
import es.joshluq.kmsafe.ui.util.safeClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import com.google.accompanist.permissions.PermissionState

@Composable
fun OnboardingRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCropper: (String) -> Unit,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val croppedUri by backStackEntry.savedStateHandle.getStateFlow<String?>("cropped_uri", null).collectAsStateWithLifecycle()
    
    LaunchedEffect(croppedUri) {
        croppedUri?.let { uri ->
            viewModel.sendEvent(Event.OnImageSelected(uri.toUri()))
            backStackEntry.savedStateHandle.remove<String>("cropped_uri")
        }
    }

    OnboardingScreen(
        state = state,
        onEvent = viewModel::sendEvent,
        onNavigateBack = onNavigateBack
    )
    
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is Effect.NavigateBack -> onNavigateBack()
                is Effect.NavigateToCropper -> onNavigateToCropper(effect.uri)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    state: State,
    onEvent: (Event) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val datePickerState = rememberDatePickerState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        onEvent(Event.OnOriginalImageSelected(uri))
    }

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading && state.renting == null,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = when {
                            state.isReadOnly -> stringResource(R.string.onboarding_title_alternative)
                            else -> stringResource(R.string.onboarding_title)
                        },
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { 
                        keyboardController?.hide()
                        onNavigateBack() 
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
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (state.isReadOnly) 24.dp else 16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Image Picker Section
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    VehicleImagePicker(
                        imageUrl = state.vehicleImageUrl,
                        selectedUri = state.selectedImageUri,
                        isReadOnly = state.isReadOnly,
                        onClick = { if (!state.isReadOnly) photoPickerLauncher.launch("image/*") }
                    )
                }

                if (state.isReadOnly) {
                    ReadOnlyContent(state)
                } else {
                    EditableContent(state, onEvent, bluetoothPermissionState)
                }

                Spacer(modifier = Modifier.height(64.dp))
            }

            // Silent Interaction Block Overlay while saving
            if (state.isLoading && state.renting != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { }
                )
            }

            // Toast-style Banner (Error)
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(Event.OnDismissError) }
            )

            // Date Picker Dialog moved inside composition Box to ensure z-index
            if (state.showDatePicker) {
                CanvasKitDatePickerDialog(
                    onDismissRequest = { onEvent(Event.OnToggleDatePicker) },
                    confirmButton = {
                        CanvasKitButton(
                            onClick = safeClick {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = Date(millis)
                                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    onEvent(Event.OnStartDateChanged(formatter.format(date)))
                                    focusManager.clearFocus()
                                }
                                onEvent(Event.OnToggleDatePicker)
                            },
                            variant = CanvasKitButtonVariant.Ghost,
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_date_picker_confirm),
                                color = CanvasKitTheme.colors.brandAccent
                            )
                        }
                    },
                    dismissButton = {
                        CanvasKitButton(
                            variant = CanvasKitButtonVariant.Ghost,
                            onClick = safeClick { onEvent(Event.OnToggleDatePicker) }
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_date_picker_cancel),
                                color = CanvasKitTheme.colors.brandAccent
                            )
                        }
                    }
                ) {
                    CanvasKitDatePicker(state = datePickerState)
                }
            }

            if (state.showBluetoothPicker) {
                BluetoothPickerBottomSheet(
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
private fun ReadOnlyContent(state: State) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_vehicle_name_label),
            value = state.vehicleName
        )

        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_start_date_label),
            value = state.startDate
        )

        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_duration_months_label),
            value = state.durationMonths,
            suffix = stringResource(R.string.onboarding_months_suffix)
        )

        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_total_kms_label),
            value = state.totalKms,
            suffix = stringResource(R.string.onboarding_km_suffix)
        )

        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_start_odometer_label),
            value = state.startOdometer,
            suffix = stringResource(R.string.onboarding_km_suffix)
        )

        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_current_odometer_label),
            value = state.currentOdometer,
            suffix = stringResource(R.string.onboarding_km_suffix)
        )

        if (state.bluetoothDeviceAddress != null) {
            OnboardingDisplayField(
                label = stringResource(R.string.onboarding_bluetooth_label),
                value = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun EditableContent(
    state: State, 
    onEvent: (Event) -> Unit,
    bluetoothPermissionState: PermissionState?
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var waitingForBluetoothPermission by remember { mutableStateOf(false) }

    LaunchedEffect(bluetoothPermissionState?.status?.isGranted) {
        if (bluetoothPermissionState?.status?.isGranted == true && waitingForBluetoothPermission) {
            onEvent(Event.OnToggleBluetoothPicker)
            waitingForBluetoothPermission = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OnboardingTextField(
            label = stringResource(R.string.onboarding_vehicle_name_label),
            value = state.vehicleName,
            onValueChange = { onEvent(Event.OnVehicleNameChanged(it)) },
            errorMessage = state.vehicleNameError?.asString(),
            placeholder = stringResource(R.string.onboarding_vehicle_name_placeholder),
            enabled = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
        )

        if (state.isEditMode) {
            OnboardingDisplayField(
                label = stringResource(R.string.onboarding_start_date_label),
                value = state.startDate,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        } else {
            OnboardingDisplayField(
                label = stringResource(R.string.onboarding_start_date_label),
                value = state.startDate,
                placeholder = stringResource(R.string.onboarding_start_date_placeholder),
                errorMessage = state.startDateError?.asString(),
                onClick = { 
                    keyboardController?.hide()
                    onEvent(Event.OnToggleDatePicker) 
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.onboarding_start_date_label),
                        tint = CanvasKitTheme.colors.brandAccent
                    )
                }
            )
        }

        OnboardingTextField(
            label = stringResource(R.string.onboarding_duration_months_label),
            value = state.durationMonths,
            onValueChange = { onEvent(Event.OnDurationMonthsChanged(it)) },
            errorMessage = state.durationMonthsError?.asString(),
            trailingIcon = {
                Text(
                    text = stringResource(R.string.onboarding_months_suffix),
                    color = CanvasKitTheme.colors.brandAccent
                )
            },
            placeholder = stringResource(R.string.onboarding_duration_months_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            enabled = true
        )

        OnboardingTextField(
            label = stringResource(R.string.onboarding_total_kms_label),
            value = state.totalKms,
            onValueChange = { onEvent(Event.OnTotalKmsChanged(it)) },
            errorMessage = state.totalKmsError?.asString(),
            trailingIcon = {
                Text(
                    text = stringResource(R.string.onboarding_km_suffix),
                    color = CanvasKitTheme.colors.brandAccent
                )
            },
            placeholder = stringResource(R.string.onboarding_total_kms_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            enabled = true
        )

        if (state.isEditMode) {
            OnboardingDisplayField(
                label = stringResource(R.string.onboarding_start_odometer_label),
                value = state.startOdometer,
                suffix = stringResource(R.string.onboarding_km_suffix),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        } else {
            OnboardingTextField(
                label = stringResource(R.string.onboarding_start_odometer_label),
                value = state.startOdometer,
                onValueChange = { onEvent(Event.OnStartOdometerChanged(it)) },
                errorMessage = state.startOdometerError?.asString(),
                trailingIcon = {
                    Text(
                        text = stringResource(R.string.onboarding_km_suffix),
                        color = CanvasKitTheme.colors.brandAccent
                    )
                },
                placeholder = stringResource(R.string.onboarding_start_odometer_placeholder),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                enabled = true
            )
        }

        OnboardingTextField(
            label = stringResource(R.string.onboarding_current_odometer_label),
            value = state.currentOdometer,
            onValueChange = { onEvent(Event.OnCurrentOdometerChanged(it)) },
            errorMessage = state.currentOdometerError?.asString(),
            trailingIcon = {
                Text(
                    text = stringResource(R.string.onboarding_km_suffix),
                    color = CanvasKitTheme.colors.brandAccent
                )
            },
            placeholder = stringResource(R.string.onboarding_current_odometer_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { 
                keyboardController?.hide()
                focusManager.clearFocus()
            }),
            enabled = true
        )

        OnboardingDisplayField(
            label = stringResource(R.string.onboarding_bluetooth_label),
            value = state.bluetoothDeviceName ?: state.bluetoothDeviceAddress ?: "",
            placeholder = stringResource(R.string.onboarding_bluetooth_placeholder),
            onClick = {
                keyboardController?.hide()
                if (bluetoothPermissionState?.status?.isGranted != false) {
                    onEvent(Event.OnToggleBluetoothPicker)
                } else {
                    waitingForBluetoothPermission = true
                    bluetoothPermissionState.launchPermissionRequest()
                }
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        CanvasKitButton(
            onClick = safeClick { 
                keyboardController?.hide()
                focusManager.clearFocus()
                onEvent(Event.OnRegisterClicked) 
            },
            enabled = !state.isLoading,
            loading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { contentColor ->
            Text(
                text = if (state.isLoading) {
                    stringResource(R.string.onboarding_saving_button)
                } else {
                    stringResource(R.string.onboarding_save_button)
                },
                style = CanvasKitTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

@Composable
fun VehicleImagePicker(
    imageUrl: String?,
    selectedUri: Uri?,
    isReadOnly: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(shape)
            .background(CanvasKitTheme.colors.backgroundPrimary)
            .border(1.dp, CanvasKitTheme.colors.borderSubtle, shape)
            .clickable(enabled = !isReadOnly, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            selectedUri != null -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.onboarding_photo_label),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { CanvasKitSkeleton(modifier = Modifier.fillMaxSize()) },
                    error = {
                        Box(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .background(CanvasKitTheme.colors.error.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }
                )
            }
            imageUrl != null -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.onboarding_photo_label),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { CanvasKitSkeleton(modifier = Modifier.fillMaxSize()) },
                    error = {
                        Box(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .background(CanvasKitTheme.colors.error.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.onboarding_photo_label),
                    tint = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingDisplayField(
    label: String,
    value: String,
    placeholder: String? = null,
    suffix: String? = null,
    onClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorMessage: String? = null
) {
    val colors = CanvasKitTheme.colors
    val spacing = CanvasKitTheme.spacing
    val typography = CanvasKitTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row {
            Spacer(modifier = Modifier.width(spacing.lg))
            Text(
                text = label,
                style = typography.labelSmall,
                color = if (errorMessage != null) colors.error else colors.textPrimary
            )
        }
        Box(
            modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = if (errorMessage != null) colors.error else colors.borderSubtle,
                        shape = CanvasKitTheme.shapes.pill
                    )
                    .clip(CanvasKitTheme.shapes.pill)
                    .then(
                        if (onClick != null) {
                            Modifier.safeClickable(
                                interactionSource = interactionSource,
                                indication = null, // To remove the dark flicker as requested
                                onClick = onClick
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        color = colors.textSecondary,
                        style = typography.bodyMedium
                    )
                } else {
                    Text(
                        text = value,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (suffix != null) {
                        Text(
                            text = suffix,
                            style = typography.labelLarge,
                            color = colors.brandAccent
                        )
                    }
                    if (trailingIcon != null) {
                        if (suffix != null) Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.xs))
                        trailingIcon()
                    }
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = typography.labelSmall,
                color = colors.error,
                modifier = Modifier.padding(start = spacing.lg, top = 2.dp)
            )
        }
    }
}

@Composable
fun OnboardingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    val spacing = CanvasKitTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        CanvasKitTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            errorText = errorMessage,
            isError = errorMessage != null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            enabled = enabled,
            variant = CanvasKitTextFieldVariant.Outlined,
            label = label
        )
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothPickerBottomSheet(
    onDismiss: () -> Unit,
    onDeviceSelected: (String, String) -> Unit,
    sheetState: SheetState
) {
    val bluetoothAdapter: BluetoothAdapter? = remember { BluetoothAdapter.getDefaultAdapter() }
    val bondedDevices = remember {
        bluetoothAdapter?.bondedDevices?.map { it.name to it.address } ?: emptyList()
    }

    CanvasKitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_bluetooth_picker_title),
                style = CanvasKitTheme.typography.headingMedium,
                color = CanvasKitTheme.colors.textPrimary
            )

            if (bondedDevices.isEmpty()) {
                Text(
                    text = "No bonded devices found",
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textSecondary
                )
            } else {
                bondedDevices.forEach { (name, address) ->
                    CanvasKitCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDeviceSelected(name, address) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CanvasKitTheme.colors.brandAccent)
                            Column {
                                Text(name, style = CanvasKitTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(address, style = CanvasKitTheme.typography.labelSmall, color = CanvasKitTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun OnboardingScreenReadOnlyPreview() {
    CanvasKitTheme {
        OnboardingScreen(
            state = State.Empty.copy(
                isLoading = false,
                isReadOnly = true,
                vehicleName = "Volkswagen ID.3",
                startDate = "12/10/2023",
                durationMonths = "36",
                totalKms = "45000",
                startOdometer = "0",
                currentOdometer = "1500"
            ),
            onEvent = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun OnboardingScreenLoadingPreview() {
    CanvasKitTheme {
        OnboardingScreen(
            state = State.Empty,
            onEvent = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun OnboardingScreenPreview() {
    CanvasKitTheme {
        OnboardingScreen(
            state = State.Empty.copy(isLoading = false),
            onEvent = {}
        )
    }
}
