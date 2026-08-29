package es.joshluq.kmsafe.ui.overview

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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
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
import es.joshluq.canvaskit.components.feedback.CanvasKitSkeleton
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePickerField
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingStrategy
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.components.sheets.CanvasKitBottomSheet
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.data.location.LocationTrackingService
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.ui.common.components.AdMobBanner
import es.joshluq.kmsafe.core.ui.components.BrandingLogo
import es.joshluq.kmsafe.ui.overview.components.TrackingCard
import es.joshluq.kmsafe.ui.overview.model.MonthlyUsageUiModel
import es.joshluq.kmsafe.ui.util.DateUtils
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.safeClickable
import kotlin.math.absoluteValue
import es.joshluq.kmsafe.core.ui.util.NumberFormatter

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
                Effect.StartTrackingService -> {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
                Effect.StopTrackingService -> {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_STOP
                    }
                    context.startService(intent)
                }
                Effect.OpenAppSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }

                Effect.NavigateToWelcomeDiscovery -> onNavigateToWelcomeDiscovery()
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
        topBar = { OverviewTopBar(state, onEvent) },
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
                val isActivityGranted = activityRecognitionState?.status?.isGranted ?: true
                val isBackgroundGranted = backgroundLocationState?.status?.isGranted ?: true
                val isNotificationsGranted = notificationsPermissionState?.status?.isGranted ?: true

                if (state.hasRenting) {
                    RentingState(
                        state = state,
                        onEvent = onEvent,
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

                // BUSINESS RULE: If auto-tracking is enabled but Critical Permissions are missing -> Navigate to Permissions Screen
                LaunchedEffect(
                    state.autoTrackingEnabled,
                    isActivityGranted,
                    isBackgroundGranted,
                    isNotificationsGranted
                ) {
                    if (state.autoTrackingEnabled && (!isActivityGranted || !isBackgroundGranted || !isNotificationsGranted)) {
                        onEvent(Event.OnRequestPermissionsRationale)
                    }
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

            // Projection Banner (Warning/Info)
            state.projection?.let { projection ->
                val variant = if (projection.isOverLimit) CanvasKitAlertVariant.Warning else CanvasKitAlertVariant.Success
                CanvasKitBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 0.dp)
                        .safeClickable { onEvent(Event.OnProjectionBannerClicked) },
                    variant = variant,
                    visible = state.showProjectionBanner,
                    onDismiss = { onEvent(Event.OnDismissProjectionBanner) },
                    message = {
                        Text(
                            text = if (projection.isOverLimit) {
                                stringResource(
                                    R.string.projection_card_status_over,
                                    NumberFormatter.formatDistance(projection.expectedFinalBalance.absoluteValue)
                                )
                            } else {
                                stringResource(R.string.projection_card_status_safe, NumberFormatter.formatDistance(projection.expectedFinalBalance))
                            },
                            style = CanvasKitTheme.typography.bodyMedium
                        )
                    }
                )
            }

            // Bluetooth Suggestion Banner (Premium only)
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (state.showProjectionBanner) 64.dp else 0.dp)
                    .safeClickable {
                        state.renting?.let { onEvent(Event.OnEditContractClicked(it.id)) }
                    },
                variant = CanvasKitAlertVariant.Info,
                visible = state.showBluetoothSuggestionBanner,
                onDismiss = { onEvent(Event.OnDismissBluetoothSuggestionBanner) },
                message = {
                    Text(
                        text = stringResource(R.string.onboarding_bluetooth_suggestion_banner),
                        style = CanvasKitTheme.typography.bodyMedium
                    )
                }
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
    onStartTracking: () -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
    ) {
        if (state.isPremium == false) {
            AdMobBanner(
                adUnitId = BuildConfig.ADMOB_BANNER_ID
            )
        }
        Spacer(modifier = Modifier.height(2.dp))

        TrackingCard(
            isTracking = state.isTracking,
            distanceMeters = state.trackedDistance,
            onStart = onStartTracking,
            onStop = {
                keyboardController?.hide()
                onEvent(Event.OnStopTrackingClicked)
            },
            onConfirm = {
                keyboardController?.hide()
                onEvent(Event.OnConfirmTrackedTripClicked)
            },
            onCancel = {
                keyboardController?.hide()
                onEvent(Event.OnCancelTrackedTripClicked)
            }
        )

        MainBalanceCard(
            vehicleName = state.renting?.vehicleName ?: "",
            balance = state.balance,
            totalKms = state.totalKmsDriven,
            imageUrl = state.renting?.vehicleImageUrl,
            onEditClick = {
                keyboardController?.hide()
                state.renting?.let { onEvent(Event.OnEditContractClicked(it.id)) }
            },
            onCardClick = {
                state.renting?.let { onNavigateToVehicleDetail(it.id) }
            }
        )

        TheoreticalLimitsSection(
            dailyLimit = state.dailyLimit,
            monthlyLimit = state.monthlyLimit
        )

        ChartsSection(state)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MainBalanceCard(
    vehicleName: String,
    balance: Double,
    totalKms: Double,
    imageUrl: String? = null,
    onEditClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val isPositive = balance >= 0
    val balanceColor = if (isPositive) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.error

    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated,
        onClick = onCardClick,
        header = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(imageUrl)
                            .build(),
                        contentDescription = stringResource(R.string.acc_vehicle_info),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CanvasKitSkeleton(
                                modifier = Modifier.fillMaxSize()
                            )
                        },
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

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.acc_edit_contract),
                        tint = if (imageUrl != null) Color.White else CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (imageUrl != null) Color.Black.copy(alpha = 0.3f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    )
                }

                if (imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.65f))
                    )
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    text = vehicleName.uppercase(),
                    style = CanvasKitTheme.typography.bodyLarge,
                    color = CanvasKitTheme.colors.brandAccent,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        footer = {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = CanvasKitTheme.colors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overview_total_kms_driven),
                    style = CanvasKitTheme.typography.bodyLarge,
                    color = CanvasKitTheme.colors.textSecondary
                )
                Text(
                    text = stringResource(R.string.common_km_suffix, NumberFormatter.formatDistance(totalKms)),
                    style = CanvasKitTheme.typography.headingMedium,
                    color = CanvasKitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = CanvasKitTheme.colors.textSecondary,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = stringResource(R.string.overview_kilometer_balance),
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.textSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        if (isPositive) R.string.common_km_positive_suffix else R.string.common_km_negative_suffix,
                        balance.absoluteValue.toInt()
                    ),
                    style = CanvasKitTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    color = balanceColor,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun TheoreticalLimitsSection(
    dailyLimit: Double,
    monthlyLimit: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.overview_ideal_limits_title),
            style = CanvasKitTheme.typography.bodyMedium,
            color = CanvasKitTheme.colors.textSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                label = stringResource(R.string.overview_daily_limit),
                value = stringResource(R.string.common_km_suffix, dailyLimit.toInt()),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.overview_monthly_limit),
                value = stringResource(R.string.common_km_suffix, monthlyLimit.toInt()),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChartsSection(state: State) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp
        ) { page ->
            if (page == 0) {
                ComparisonChart(state.timePercentage, state.kmsPercentage, state.differencePercentage)
            } else {
                MonthlyBarChart(monthlyUsage = state.monthlyUsage)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(2) { iteration ->
                val color = if (pagerState.currentPage == iteration) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.borderSubtle
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    CanvasKitCard(
        modifier = modifier,
        variant = CanvasKitCardVariant.Outlined,
        header = {
            Text(
                text = label,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
        }
    ) {
        Text(
            text = value,
            style = CanvasKitTheme.typography.headingMedium,
            fontWeight = FontWeight.Bold,
            color = CanvasKitTheme.colors.textPrimary
        )
    }
}

@Composable
fun MonthlyBarChart(monthlyUsage: List<MonthlyUsageUiModel>) {
    var showInfo by remember { mutableStateOf(false) }

    CanvasKitCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
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
                IconButton(onClick = { showInfo = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.acc_chart_help),
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
                                    R.string.common_ratio_label,
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

@Composable
private fun ComparisonChart(timeP: Float, kmsP: Float, diff: Float) {
    var showInfo by remember { mutableStateOf(false) }

    CanvasKitCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.overview_usage_comparison_title),
                    style = CanvasKitTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                IconButton(onClick = { showInfo = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.acc_chart_help),
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(timeP, kmsP)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.common_percentage_format, diff.absoluteValue),
                        style = CanvasKitTheme.typography.headingLarge,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        stringResource(R.string.overview_difference_label),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(
                    color = CanvasKitTheme.colors.brandAccent,
                    label = stringResource(R.string.overview_chart_legend_kms)
                )
                Spacer(modifier = Modifier.width(16.dp))
                ChartLegendItem(
                    color = CanvasKitTheme.colors.textSecondary,
                    label = stringResource(R.string.overview_chart_legend_time)
                )
            }
        }
    }

    if (showInfo) {
        ChartInfoDialog(
            content = stringResource(R.string.overview_chart_comparison_info),
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
private fun DonutChart(timeP: Float, kmsP: Float) {
    val backgroundGray = CanvasKitTheme.colors.borderSubtle
    val gray = CanvasKitTheme.colors.textSecondary
    val accent = CanvasKitTheme.colors.brandAccent
    Canvas(Modifier.size(160.dp)) {
        val sW = 16.dp.toPx()
        drawArc(backgroundGray, -90f, 360f, false, style = Stroke(sW * 1.5f, cap = StrokeCap.Round))
        drawArc(gray, -90f, 360f * timeP, false, style = Stroke(sW, cap = StrokeCap.Round))
        drawArc(
            accent,
            -90f,
            360f * kmsP,
            false,
            style = Stroke(sW),
            size = size.copy(width = size.width - 40f, height = size.height - 40f),
            topLeft = Offset(20f, 20f)
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
                    label = stringResource(R.string.overview_current_odometer_label),
                    value = state.newOdometerValue,
                    onValueChange = { onEvent(Event.OnNewOdometerChanged(it)) },
                    placeholder = stringResource(R.string.overview_current_odometer_placeholder),
                    suffix = stringResource(R.string.onboarding_km_suffix),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                CanvasKitTextField(
                    label = stringResource(R.string.overview_record_label_label),
                    value = state.newRecordLabel,
                    onValueChange = { onEvent(Event.OnNewLabelChanged(it)) },
                    placeholder = stringResource(R.string.overview_record_label_placeholder),
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
            label = stringResource(R.string.overview_record_date_label),
            selectedDateMillis = state.newRecordDate,
            onDateSelected = { millis ->
                millis?.let { onEvent(Event.OnNewRecordDateChanged(it)) }
            },
            placeholder = stringResource(R.string.onboarding_start_date_placeholder),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTopBar(state: State, onEvent: (Event) -> Unit) {
    CanvasKitTopBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.safeClickable(
                    enabled = state.availableVehicles.size > 1,
                    onClick = { onEvent(Event.OnToggleVehicleSwitcher) }
                )
            ) {
                BrandingLogo(logoSize = 34.dp)

                if (state.availableVehicles.size > 1) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.acc_open_menu),
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                DropdownMenu(
                    expanded = state.showVehicleSwitcher,
                    onDismissRequest = { onEvent(Event.OnToggleVehicleSwitcher) },
                    modifier = Modifier.background(CanvasKitTheme.colors.backgroundPrimary)
                ) {
                    state.availableVehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = vehicle.vehicleName,
                                    style = CanvasKitTheme.typography.bodyLarge,
                                    color = if (vehicle.isSelected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textPrimary,
                                    fontWeight = if (vehicle.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = safeClick { onEvent(Event.OnSwitchVehicleClicked(vehicle.id)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CarRental,
                                    contentDescription = null,
                                    tint = if (vehicle.isSelected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary
                                )
                            }
                        )
                    }
                }
            }
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
                    contentDescription = stringResource(R.string.acc_upgrade_premium),
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
                        contentDescription = stringResource(R.string.acc_sync_pending),
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
        confirmText = stringResource(R.string.history_close_button),
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
                adUnitId = BuildConfig.ADMOB_BANNER_ID
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
                            stringResource(R.string.profile_welcome_guide_option),
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
