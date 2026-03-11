package es.joshluq.kmsafe.ui.overview

import android.content.res.Configuration
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialogContent
import es.joshluq.canvaskit.components.feedback.CanvasKitSkeleton
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.inputs.CanvasKitDatePicker
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.components.sheets.CanvasKitBottomSheet
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.data.location.LocationTrackingService
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.ui.onboarding.OnboardingTextField
import es.joshluq.kmsafe.ui.common.components.AdMobBanner
import es.joshluq.kmsafe.ui.common.components.BrandingLogo
import es.joshluq.kmsafe.ui.overview.components.TrackingCard
import es.joshluq.kmsafe.ui.overview.model.MonthlyUsageUiModel
import es.joshluq.kmsafe.ui.util.DateUtils
import es.joshluq.kmsafe.ui.util.safeClick
import es.joshluq.kmsafe.ui.util.safeClickable
import kotlin.math.absoluteValue

/**
 * Navigation entry point for the Overview screen.
 */
@Composable
fun OverviewRoute(
    onNavigateToOnboarding: (String?, Boolean) -> Unit,
    onNavigateToProjection: () -> Unit
) {
    val viewModel: OverviewViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()
    OverviewScreen(
        state = state.value,
        onEvent = viewModel::sendEvent
    )

    val context = LocalContext.current

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is Effect.NavigateToOnboarding -> onNavigateToOnboarding(effect.vehicleId, effect.isEdit)
                Effect.NavigateToProjection -> onNavigateToProjection()
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
            }
        }
    }
}

/**
 * Pure visual representation of the Overview screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: State,
    onEvent: (Event) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
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

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = { OverviewTopBar(state, onEvent) },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets(),
        floatingActionButton = {
            if (state.hasRenting) {
                FloatingActionButton(
                    onClick = safeClick { onEvent(Event.OnUpdateOdometerClicked) },
                    containerColor = CanvasKitTheme.colors.brandAccent,
                    contentColor = CanvasKitTheme.colors.onBrandAccent,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.overview_update_odometer_title)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column {
                when {
                    state.hasRenting -> RentingState(state, onEvent, permissionLauncher)
                    else -> EmptyState(state = state, onRegisterClick = { onEvent(Event.OnRegisterRentingClicked) })
                }
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
                                    projection.expectedFinalBalance.absoluteValue
                                )
                            } else {
                                stringResource(R.string.projection_card_status_safe, projection.expectedFinalBalance)
                            },
                            style = CanvasKitTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }

        if (state.showBottomSheet) {
            CanvasKitBottomSheet(
                onDismissRequest = { onEvent(Event.OnBottomSheetDismissed) },
                sheetState = bottomSheetState,
            ) {
                UpdateOdometerContent(state, onEvent)
            }
        }
    }
}

@Composable
private fun RentingState(
    state: State, 
    onEvent: (Event) -> Unit,
    permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.isPremium) {
            AdMobBanner(
                adUnitId = BuildConfig.ADMOB_BANNER_ID
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        TrackingSection(state, onEvent, permissionLauncher)

        MainBalanceCard(
            vehicleName = state.renting?.vehicleName ?: "",
            balance = state.balance,
            totalKms = state.totalKmsDriven,
            imageUrl = state.renting?.vehicleImageUrl,
            onEditClick = { state.renting?.let { onEvent(Event.OnEditContractClicked(it.id)) } }
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
private fun TrackingSection(
    state: State, 
    onEvent: (Event) -> Unit,
    permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    TrackingCard(
        isTracking = state.isTracking,
        distanceMeters = state.trackedDistance,
        onStart = {
            val permissions = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        },
        onStop = { onEvent(Event.OnStopTrackingClicked) },
        onConfirm = { onEvent(Event.OnConfirmTrackedTripClicked) },
        onCancel = { onEvent(Event.OnCancelTrackedTripClicked) }
    )
}

@Composable
private fun MainBalanceCard(
    vehicleName: String,
    balance: Int,
    totalKms: Int,
    imageUrl: String? = null,
    onEditClick: () -> Unit
) {
    val isPositive = balance >= 0
    val balanceColor = if (isPositive) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.error

    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CanvasKitCardVariant.Elevated,
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
                    text = stringResource(R.string.common_km_suffix, totalKms),
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
                        balance.absoluteValue
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
    dailyLimit: Int,
    monthlyLimit: Int
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
                value = stringResource(R.string.common_km_suffix, dailyLimit),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.overview_monthly_limit),
                value = stringResource(R.string.common_km_suffix, monthlyLimit),
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.overview_no_data_available),
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
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
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.newRecordDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.overview_update_odometer_title),
            style = CanvasKitTheme.typography.headingMedium,
            fontWeight = FontWeight.Bold,
            color = CanvasKitTheme.colors.textSecondary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                OnboardingTextField(
                    label = stringResource(R.string.overview_current_odometer_label),
                    value = state.newOdometerValue,
                    onValueChange = { onEvent(Event.OnNewOdometerChanged(it)) },
                    placeholder = stringResource(R.string.overview_current_odometer_placeholder),
                    trailingIcon = {
                        Text(
                            text = stringResource(R.string.onboarding_km_suffix),
                            color = CanvasKitTheme.colors.brandAccent,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OnboardingTextField(
                    label = stringResource(R.string.overview_record_label_label),
                    value = state.newRecordLabel,
                    onValueChange = { onEvent(Event.OnNewLabelChanged(it)) },
                    placeholder = stringResource(R.string.overview_record_label_placeholder),
                    enabled = true
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.overview_record_date_label),
                style = CanvasKitTheme.typography.labelLarge,
                color = CanvasKitTheme.colors.textSecondary
            )
            CanvasKitDatePicker(state = datePickerState, showModeToggle = false)
        }
        CanvasKitButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = safeClick {
                datePickerState.selectedDateMillis?.let {
                    val preciseTimestamp = DateUtils.mergeDateWithCurrentTime(it)
                    onEvent(Event.OnSaveRecordClicked(preciseTimestamp))
                }
            },
            enabled = !state.isSaving && state.newOdometerValue.isNotBlank(),
            loading = state.isSaving

        ) { contentColor ->
            Text(
                stringResource(R.string.overview_save_record_button),
                color = contentColor
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTopBar(state: State, onEvent: (Event) -> Unit) {
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
            if (state.isSyncPending) {
                IconButton(onClick = { /* No-op, just indicator */ }) {
                    Icon(
                        imageVector = Icons.Default.SyncProblem,
                        contentDescription = stringResource(R.string.acc_sync_pending),
                        tint = CanvasKitTheme.colors.brandAccent.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        centeredTitle = true
    )
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
    CanvasKitDialog(onDismissRequest = onDismiss) {
        CanvasKitDialogContent(
            title = {
                Text(
                    text = stringResource(R.string.overview_chart_info_title),
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            content = {
                Text(
                    text = content,
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textSecondary
                )
            },
            buttons = {
                CanvasKitButton(
                    variant = CanvasKitButtonVariant.Secondary,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { contentColor ->
                    Text(
                        text = stringResource(R.string.history_close_button),
                        color = contentColor
                    )
                }
            }
        )
    }
}

@Composable
private fun EmptyState(state: State, onRegisterClick: () -> Unit) {
    Column {
        if (!state.isPremium) {
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
                CanvasKitButton(
                    onClick = safeClick { onRegisterClick() },
                    modifier = Modifier.fillMaxWidth()
                ) { contentColor ->
                    Text(
                        stringResource(R.string.overview_register_renting_button),
                        color = contentColor
                    )
                }
            }
        )
    }
}

internal class OverviewStateProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State> = sequenceOf(
        State(
            isLoading = false,
            isPremium = true,
            renting = RentingContract(
                id = "1",
                vehicleName = "Volkswagen ID.3",
                startDate = System.currentTimeMillis() - 3888000000L,
                durationMonths = 36,
                totalKms = 45000,
                startOdometer = 0,
                currentOdometer = 150,
                isSelected = true,
                vehicleImageUrl = "https://www.carlogos.org/car-logos/volkswagen-id-3-logo.png"
            ),
            balance = 150, dailyLimit = 50, monthlyLimit = 1250, totalKmsDriven = 300,
            timePercentage = 0.75f, kmsPercentage = 0.60f, differencePercentage = 15.0f,
            monthlyUsage = listOf(
                MonthlyUsageUiModel("Ene", "1200", "1500", 1200f, 1500f, MonthlyUsageUiModel.LimitState.SAFE)
            ),
            showProjectionBanner = false,
            projection = TripProjection(
                projectedTotalKms = 10000,
                expectedFinalBalance = 1000000,
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
                    totalKms = 45000,
                    startOdometer = 0,
                    currentOdometer = 0,
                    isSelected = true,
                    vehicleImageUrl = "https://www.carlogos.org/car-logos/volkswagen-id-3-logo.png"
                ),
                RentingContract(
                    id = "2",
                    vehicleName = "Tesla Model 3",
                    startDate = 0,
                    durationMonths = 24,
                    totalKms = 20000,
                    startOdometer = 0,
                    currentOdometer = 0,
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
    CanvasKitTheme { OverviewScreen(state = state, onEvent = {}) }
}
