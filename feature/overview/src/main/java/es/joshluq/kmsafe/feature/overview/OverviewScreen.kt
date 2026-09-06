package es.joshluq.kmsafe.feature.overview

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePickerField
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingStrategy
import es.joshluq.canvaskit.components.menus.CanvasKitDropdownMenu
import es.joshluq.canvaskit.components.menus.CanvasKitDropdownMenuItem
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.components.sheets.CanvasKitBottomSheet
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.monetization.components.AdMobBanner
import es.joshluq.kmsafe.core.ui.components.BrandingLogo
import es.joshluq.kmsafe.core.ui.util.DateUtils
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.safeClickable
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.feature.overview.components.AeroRunwayPacingBar
import es.joshluq.kmsafe.feature.overview.components.CopilotRadarSection
import es.joshluq.kmsafe.feature.overview.components.FloatingTelemetryPill
import es.joshluq.kmsafe.feature.overview.components.StatusCapsule
import es.joshluq.kmsafe.feature.overview.model.MonthlyUsageUiModel
import es.joshluq.kmsafe.core.ui.R as CoreR

/**
 * Navigation entry point for the Overview screen.
 */
@Composable
fun OverviewRoute(
    onNavigateToOnboarding: (String?, Boolean) -> Unit,
    onNavigateToProjection: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToWelcomeDiscovery: () -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: OverviewViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()

    // Observe navigation results from SavedStateHandle (Coordinator Pattern)
    val permissionResult by backStackEntry.savedStateHandle
        .getStateFlow<Boolean?>("permissions_granted", null)
        .collectAsStateWithLifecycle()

    LaunchedEffect(permissionResult) {
        permissionResult?.let { granted ->
            viewModel.sendEvent(Event.OnPermissionsResult(granted))
            backStackEntry.savedStateHandle["permissions_granted"] = null
        }
    }

    OverviewScreen(
        state = state.value,
        onEvent = viewModel::sendEvent,
        onNavigateToVehicleDetail = onNavigateToVehicleDetail
    )

    val context = LocalContext.current

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is Effect.NavigateToOnboarding -> onNavigateToOnboarding(effect.vehicleId, effect.isEdit)
                Effect.NavigateToProjection -> onNavigateToProjection()
                Effect.NavigateToPermissions -> onNavigateToPermissions()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
                Effect.NavigateToPreferences -> onNavigateToPreferences()
                Effect.OpenAppSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }

                Effect.NavigateToWelcomeDiscovery -> onNavigateToWelcomeDiscovery()
                is Effect.NavigateToVehicleDetail -> onNavigateToVehicleDetail(effect.id)
            }
        }
    }
}

/**
 * Pure visual representation of the Overview screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun OverviewScreen(
    state: State,
    onEvent: (Event) -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val activityRecognitionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACTIVITY_RECOGNITION)
    } else {
        null
    }

    val backgroundLocationState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    val notificationsPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        null
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            onEvent(Event.OnRequestPermissionsRationale)
        }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            onEvent(Event.OnStartTrackingClicked)
        }
    }

    LaunchedEffect(state.showBottomSheet) {
        if (state.showBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    val isDataAvailable = state.renting != null
    val loadingStrategy = if (isDataAvailable) {
        CanvasKitLoadingStrategy.ProgressLine
    } else {
        CanvasKitLoadingStrategy.ReplaceContent
    }

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        loadingStrategy = loadingStrategy,
        topBar = {
            OverviewTopBar(
                state = state,
                onEvent = onEvent
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets(),
        floatingActionButton = {
            if (state.hasRenting) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = safeClick {
                            keyboardController?.hide()
                            onEvent(Event.OnUpdateOdometerClicked)
                        },
                        containerColor = CanvasKitTheme.colors.brandAccent,
                        contentColor = CanvasKitTheme.colors.onBrandAccent,
                        shape = CircleShape,
                        modifier = Modifier.testTag("add_odometer_fab")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.overview_update_odometer_title)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column {
                val isBluetoothGranted = bluetoothPermissionState?.status?.isGranted ?: true
                val hasBluetoothLinked = !state.renting?.bluetoothDeviceAddress.isNullOrBlank()
                // Business Rule: The warning should ONLY appear if the vehicle has already linked
                // Bluetooth but for some reason Bluetooth permission is missing.
                val isBluetoothPermissionMissing = hasBluetoothLinked && !isBluetoothGranted
                val hasBluetoothPermissions = !isBluetoothPermissionMissing

                if (state.hasRenting) {
                    RentingState(
                        state = state,
                        onEvent = onEvent,
                        hasBluetoothPermissions = hasBluetoothPermissions,
                        onRequestBluetoothPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                onEvent(Event.OnRequestPermissionsRationale)
                            }
                        },
                        onStartTracking = {
                            val permissions = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            multiplePermissionsLauncher.launch(permissions.toTypedArray())
                        },
                        onNavigateToVehicleDetail = onNavigateToVehicleDetail
                    )
                } else if (!state.isLoading) {
                    EmptyState(
                        state = state,
                        onRegisterClick = { onEvent(Event.OnRegisterRentingClicked) },
                        onHowItWorksClick = { onEvent(Event.OnWelcomeGuideClicked) }
                    )
                }
            }
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

            // Floating Telemetry Pill (Dynamic Island pattern during active recording)
            FloatingTelemetryPill(
                isVisible = state.isTracking,
                distanceMeters = state.trackedDistance,
                onStopClick = { onEvent(Event.OnStopTrackingClicked) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        if (state.showBottomSheet) {
            CanvasKitBottomSheet(
                onDismissRequest = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onEvent(Event.OnBottomSheetDismissed)
                },
                sheetState = bottomSheetState,
            ) {
                UpdateOdometerContent(state, onEvent)
            }
        }

        if (state.showAutoTrackingPromotion) {
            AutoTrackingPromotionDialog(
                onConfigClicked = {
                    onEvent(Event.OnAutoTrackingPromotionAccepted)
                },
                onDismiss = { onEvent(Event.OnDismissAutoTrackingPromotion) }
            )
        }
    }
}

@Composable
private fun RentingState(
    state: State,
    onEvent: (Event) -> Unit,
    hasBluetoothPermissions: Boolean,
    onRequestBluetoothPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit
) {
    val contract = state.renting
    val daysRemaining = if (contract != null) {
        val endDate = DateUtils.getContractEndDate(contract.startDate, contract.durationMonths)
        ((endDate - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        if (state.statusCapsule != null) {
            StatusCapsule(
                item = state.statusCapsule,
                onClick = { onEvent(Event.OnStatusCapsuleClicked(it)) }
            )
        }

        if (!state.isTracking && state.trackedDistance > 0) {
            TripCompletedCard(
                distanceMeters = state.trackedDistance,
                onConfirm = { onEvent(Event.OnConfirmTrackedTripClicked) },
                onCancel = { onEvent(Event.OnCancelTrackedTripClicked) }
            )
        }

        AeroRunwayPacingBar(
            vehicleName = contract?.vehicleName ?: "",
            timePercentage = state.timePercentage,
            kmsPercentage = state.kmsPercentage,
            differencePercentage = state.differencePercentage,
            balance = state.balance,
            currentOdometer = state.actualKmsDriven,
            daysRemaining = daysRemaining,
            availableVehicles = state.availableVehicles,
            showVehicleSwitcher = state.showVehicleSwitcher,
            onVehicleDetailClick = {
                contract?.let { onNavigateToVehicleDetail(it.id) }
            },
            onToggleVehicleSwitcher = {
                onEvent(Event.OnToggleVehicleSwitcher)
            },
            onSwitchVehicle = { vehicleId ->
                onEvent(Event.OnSwitchVehicleClicked(vehicleId))
            }
        )

        CopilotRadarSection(
            dailyQuotaKm = state.dailyLimit,
            isPremium = state.isPremium ?: false,
            isBluetoothConnected = state.isVehicleBluetoothConnected,
            hasPermissions = hasBluetoothPermissions,
            onStartTripClick = onStartTracking,
            onUpgradeClick = { onEvent(Event.OnPremiumUpgradeClicked) },
            onRequestPermissions = onRequestBluetoothPermission
        )

        if (state.isPremium == false) {
            AdMobBanner(
                adUnitId = state.adUnitId
            )
        }

        MonthlyBarChart(monthlyUsage = state.monthlyUsage)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TripCompletedCard(
    distanceMeters: Double,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val kms = distanceMeters / 1000.0
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overview_trip_completed_title),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.brandAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${"%.2f".format(kms)} km",
                    style = CanvasKitTheme.typography.headingMedium,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
            }

            Text(
                text = stringResource(R.string.overview_trip_completed_question),
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasKitButton(
                    text = stringResource(R.string.tracking_card_cancel_action),
                    variant = CanvasKitButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                    onClick = safeClick { onCancel() }
                )
                CanvasKitButton(
                    text = stringResource(R.string.tracking_card_save_action),
                    variant = CanvasKitButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = safeClick { onConfirm() }
                )
            }
        }
    }
}

@Composable
fun MonthlyBarChart(monthlyUsage: List<MonthlyUsageUiModel>) {
    var showInfo by remember { mutableStateOf(false) }

    CanvasKitCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.overview_monthly_usage_title),
                    style = CanvasKitTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { showInfo = true })
                {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(CoreR.string.acc_chart_help),
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (monthlyUsage.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.overview_no_data_available),
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxOverall = monthlyUsage.maxOfOrNull { maxOf(it.barPercentage, it.budgetPercentage) }?.coerceAtLeast(1f) ?: 1f

                    monthlyUsage.takeLast(6).forEach { usage ->
                        val usageHeightFraction = (usage.barPercentage / maxOverall).coerceIn(0.05f, 1f)
                        val budgetHeightFraction = (usage.budgetPercentage / maxOverall).coerceIn(0.05f, 1f)
                        val barColor = if (usage.limitState == MonthlyUsageUiModel.LimitState.SAFE) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.error

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            Text(
                                text = stringResource(
                                    CoreR.string.common_ratio_label,
                                    usage.kmsText,
                                    usage.budgetedKmsText
                                ),
                                style = CanvasKitTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = CanvasKitTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))

                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(budgetHeightFraction)
                                        .background(
                                            CanvasKitTheme.colors.borderSubtle,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Box(
                                    Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(usageHeightFraction)
                                        .background(
                                            barColor,
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                Box(
                                    Modifier
                                        .width(28.dp)
                                        .height(2.dp)
                                        .align(Alignment.BottomCenter)
                                        .padding(
                                            bottom = (200 * budgetHeightFraction).dp
                                        )
                                        .background(CanvasKitTheme.colors.textSecondary)
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                usage.monthName,
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(
                    color = CanvasKitTheme.colors.brandAccent,
                    label = stringResource(R.string.overview_chart_legend_actual)
                )
                Spacer(modifier = Modifier.width(16.dp))
                ChartLegendItem(
                    color = CanvasKitTheme.colors.borderSubtle,
                    label = stringResource(R.string.overview_chart_legend_budget)
                )
            }
        }
    }

    if (showInfo) {
        ChartInfoDialog(
            content = stringResource(R.string.overview_chart_monthly_info),
            onDismiss = { showInfo = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateOdometerContent(
    state: State,
    onEvent: (Event) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CanvasKitTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.overview_update_odometer_title),
            style = CanvasKitTheme.typography.headingMedium,
            fontWeight = FontWeight.Bold,
            color = CanvasKitTheme.colors.textSecondary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)) {
            Column(modifier = Modifier.weight(1f)) {
                CanvasKitTextField(
                    label = stringResource(CoreR.string.overview_current_odometer_label),
                    value = state.newOdometerValue,
                    onValueChange = { onEvent(Event.OnNewOdometerChanged(it)) },
                    placeholder = stringResource(CoreR.string.overview_current_odometer_placeholder),
                    suffix = stringResource(CoreR.string.onboarding_km_suffix),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                CanvasKitTextField(
                    label = stringResource(CoreR.string.overview_record_label_label),
                    value = state.newRecordLabel,
                    onValueChange = { onEvent(Event.OnNewLabelChanged(it)) },
                    placeholder = stringResource(CoreR.string.overview_record_label_placeholder),
                    enabled = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
            }
        }

        CanvasKitDatePickerField(
            label = stringResource(CoreR.string.overview_record_date_label),
            selectedDateMillis = state.newRecordDate,
            onDateSelected = { millis ->
                millis?.let { onEvent(Event.OnNewRecordDateChanged(it)) }
            },
            placeholder = stringResource(R.string.overview_start_date_placeholder),
            isError = state.newRecordDateError != null,
            errorText = state.newRecordDateError?.asString()
        )

        CanvasKitButton(
            text = stringResource(R.string.overview_save_record_button),
            modifier = Modifier.fillMaxWidth(),
            onClick = safeClick {
                keyboardController?.hide()
                focusManager.clearFocus()
                val preciseTimestamp = DateUtils.mergeDateWithCurrentTime(state.newRecordDate)
                onEvent(Event.OnSaveRecordClicked(preciseTimestamp))
            },
            enabled = !state.isSaving &&
                state.newOdometerValue.isNotBlank() &&
                state.newRecordDateError == null,
            loading = state.isSaving
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OverviewTopBar(
    state: State,
    onEvent: (Event) -> Unit
) {
    CanvasKitTopBar(
        title = {
            BrandingLogo(logoSize = 32.dp, isMinimized = false)
        },
        actions = {
            OverviewTopbarActions(state, onEvent)
        },
        centeredTitle = true
    )
}

@Composable
private fun OverviewTopbarActions(
    state: State,
    onEvent: (Event) -> Unit
) {
    if (state.subscriptionLevel == SubscriptionLevel.TRIAL) {
        Surface(
            color = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = "TRIAL",
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.brandAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }

    when (state.isPremium) {
        false -> {
            IconButton(onClick = safeClick { onEvent(Event.OnPremiumUpgradeClicked) }) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(CoreR.string.acc_upgrade_premium),
                    tint = CanvasKitTheme.colors.brandAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        true -> {
            if (state.isSyncPending) {
                val infiniteTransition = rememberInfiniteTransition(label = "SyncPulsate")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "SyncAlpha"
                )
                IconButton(
                    onClick = { /* No-op, just indicator */ },
                    modifier = Modifier.testTag("sync_pending_indicator")
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncProblem,
                        contentDescription = stringResource(CoreR.string.acc_sync_pending),
                        tint = CanvasKitTheme.colors.brandAccent.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = CanvasKitTheme.typography.labelSmall,
            color = CanvasKitTheme.colors.textSecondary
        )
    }
}

@Composable
private fun ChartInfoDialog(
    content: String,
    onDismiss: () -> Unit
) {
    CanvasKitConfirmDialog(
        title = stringResource(R.string.overview_chart_info_title),
        message = content,
        confirmText = stringResource(CoreR.string.history_close_button),
        onConfirm = onDismiss,
        onDismissRequest = onDismiss
    )
}

@Composable
private fun AutoTrackingPromotionDialog(
    onConfigClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    CanvasKitConfirmDialog(
        title = stringResource(R.string.overview_promotion_autotracking_title),
        message = stringResource(R.string.overview_promotion_autotracking_desc),
        confirmText = stringResource(R.string.overview_promotion_autotracking_confirm),
        cancelText = stringResource(R.string.overview_promotion_autotracking_dismiss),
        onConfirm = onConfigClicked,
        onDismissRequest = onDismiss,
        icon = Icons.Default.AutoAwesome
    )
}

@Composable
private fun EmptyState(
    state: State,
    onRegisterClick: () -> Unit,
    onHowItWorksClick: () -> Unit
) {
    Column {
        if (state.isPremium == false) {
            AdMobBanner(
                adUnitId = state.adUnitId
            )
        }
        CanvasKitStateView(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(R.string.overview_empty_state_title),
            description = stringResource(R.string.overview_empty_state_description),
            icon = {
                Icon(
                    imageVector = Icons.Default.CarRental,
                    contentDescription = null,
                    modifier = Modifier.size(CanvasKitTheme.spacing.xxxl),
                )
            },
            action = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CanvasKitButton(
                        onClick = safeClick { onRegisterClick() },
                        modifier = Modifier.fillMaxWidth().testTag("overview_register_renting_button")
                    ) { contentColor ->
                        Text(
                            stringResource(R.string.overview_register_renting_button),
                            color = contentColor
                        )
                    }
                    CanvasKitButton(
                        onClick = safeClick { onHowItWorksClick() },
                        variant = CanvasKitButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth()
                    ) { _ ->
                        Text(
                            stringResource(R.string.overview_welcome_guide_option),
                            color = CanvasKitTheme.colors.brandAccent,
                            style = CanvasKitTheme.typography.labelLarge
                        )
                    }
                }
            }
        )
    }
}

internal class OverviewStateProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State> = sequenceOf(
        State(
            isLoading = false,
            isPremium = false,
            renting = RentingContract(
                id = "1",
                vehicleName = "Volkswagen ID.3",
                startDate = System.currentTimeMillis() - 3888000000L,
                durationMonths = 36,
                totalKms = 45000.0,
                startOdometer = 0.0,
                currentOdometer = 150.0,
                isSelected = true,
                vehicleImageUrl = "https://www.carlogos.org/car-logos/volkswagen-id-3-logo.png"
            ),
            balance = 150.0, dailyLimit = 50.0, monthlyLimit = 1250.0, totalKmsDriven = 300.0,
            timePercentage = 0.75f, kmsPercentage = 0.60f, differencePercentage = 15.0f,
            monthlyUsage = listOf(
                MonthlyUsageUiModel("Ene", "1200", "1500", 1200f, 1500f, MonthlyUsageUiModel.LimitState.SAFE)
            ),
            showProjectionBanner = false,
            projection = TripProjection(
                projectedTotalKms = 10000.0,
                expectedFinalBalance = 1000000.0,
                isOverLimit = false,
                dailyAverage = 1000.0,
                hasEnoughData = true,
            ),
            availableVehicles = listOf(
                RentingContract(
                    id = "1",
                    vehicleName = "Volkswagen ID.3",
                    startDate = 0,
                    durationMonths = 36,
                    totalKms = 45000.0,
                    startOdometer = 0.0,
                    currentOdometer = 0.0,
                    isSelected = true,
                    vehicleImageUrl = "https://www.carlogos.org/car-logos/volkswagen-id-3-logo.png"
                ),
                RentingContract(
                    id = "2",
                    vehicleName = "Tesla Model 3",
                    startDate = 0,
                    durationMonths = 24,
                    totalKms = 20000.0,
                    startOdometer = 0.0,
                    currentOdometer = 0.0,
                    isSelected = false,
                    vehicleImageUrl = "https://www.carlogos.org/car-logos/tesla-logo.png"
                )
            )
        )
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun OverviewScreenPreview(@PreviewParameter(OverviewStateProvider::class) state: State) {
    CanvasKitTheme {
        OverviewScreen(
            state = state,
            onEvent = {},
            onNavigateToVehicleDetail = {}
        )
    }
}
