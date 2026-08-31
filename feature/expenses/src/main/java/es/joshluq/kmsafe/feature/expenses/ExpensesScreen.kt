package es.joshluq.kmsafe.feature.expenses

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.components.chips.CanvasKitChip
import es.joshluq.canvaskit.components.chips.CanvasKitChipVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.toTextProvider
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.domain.model.EnergyCategory
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.feature.expenses.components.AddExpenseBottomSheet
import es.joshluq.kmsafe.feature.expenses.components.StationVolatilityCard
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    state: ExpensesState,
    onEvent: (ExpensesEvent) -> Unit,
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val listState = rememberLazyListState()

    // Auto-scroll to top when a new expense is added
    LaunchedEffect(state.expenses.size) {
        if (state.expenses.isNotEmpty() && !state.isLoading) {
            listState.animateScrollToItem(0)
        }
    }

    // Logic to capture location when permission is granted and sheet is open
    // This handles the case where the user grants permission while the sheet is already visible.
    LaunchedEffect(locationPermissionState.status.isGranted, state.isAddExpenseSheetOpen) {
        if (locationPermissionState.status.isGranted && state.isAddExpenseSheetOpen && state.currentLat == null) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                location?.let {
                    onEvent(ExpensesEvent.OnLocationCaptured(it.latitude, it.longitude))
                }
            }
        }
    }

    val handleOpenAddExpense = {
        if (locationPermissionState.status.isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                location?.let {
                    onEvent(ExpensesEvent.OnLocationCaptured(it.latitude, it.longitude))
                }
                onEvent(ExpensesEvent.OnOpenAddExpense)
            }
        } else {
            locationPermissionState.launchPermissionRequest()
            // We open it anyway, location is a nice-to-have but shouldn't block the UI
            onEvent(ExpensesEvent.OnOpenAddExpense)
        }
    }

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading && state.expenses.isEmpty(),
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.expenses_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = safeClick { onEvent(ExpensesEvent.OnManageStationsClicked) }) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = stringResource(R.string.stations_management_title),
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                centeredTitle = true
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = safeClick { handleOpenAddExpense() },
                containerColor = CanvasKitTheme.colors.brandAccent,
                contentColor = CanvasKitTheme.colors.onBrandAccent,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.expenses_action_add)
                )
            }
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        val content = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CanvasKitTheme.spacing.md)
            ) {
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

                KpiHeaderSection(state = state, onEvent = onEvent)

                if (state.vehicleFuelType.category == EnergyCategory.HYBRID) {
                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                    FilterSection(
                        currentFilter = state.filterMode,
                        onFilterSelected = { onEvent(ExpensesEvent.OnFilterChanged(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                state.selectedVolatility?.let { volatility ->
                    StationVolatilityCard(
                        volatility = volatility,
                        onDismiss = { onEvent(ExpensesEvent.OnDismissVolatility) },
                        onViewDetail = { onEvent(ExpensesEvent.OnViewStationDetail(it)) }
                    )
                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))
                }

                if (state.filteredExpenses.isEmpty() && !state.isLoading) {
                    CanvasKitStateView(
                        title = stringResource(R.string.expenses_empty_title),
                        description = stringResource(R.string.expenses_empty_description),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = CanvasKitTheme.colors.brandAccent
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
                    ) {
                        items(state.filteredExpenses, key = { it.id }) { expense ->
                            ExpenseItemCard(
                                expense = expense,
                                onClick = { 
                                    expense.stationId?.let { 
                                        onEvent(ExpensesEvent.OnViewStationVolatility(it, expense.fuelType)) 
                                    } 
                                },
                                onDelete = safeClick { onEvent(ExpensesEvent.OnDeleteExpense(expense.id)) }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isPremium) {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { onEvent(ExpensesEvent.OnRefresh) },
                    modifier = Modifier.fillMaxSize(),
                    content = { content() }
                )
            } else {
                content()
            }

            // Add Expense Bottom Sheet
            if (state.isAddExpenseSheetOpen) {
                AddExpenseBottomSheet(
                    currentOdometer = state.currentOdometer,
                    vehicleFuelType = state.vehicleFuelType,
                    lastUsedFuelType = state.lastUsedFuelType,
                    lastUnitPrice = state.lastUnitPrice,
                    lastGasolinePrice = state.lastGasolinePrice,
                    lastElectricPrice = state.lastElectricPrice,
                    initialStationId = state.initialStationId,
                    isSaving = state.isSaving,
                    stations = state.stations,
                    isLocationCaptured = state.currentLat != null,
                    onDismiss = { onEvent(ExpensesEvent.OnDismissAddExpense) },
                    onSave = { fuelType, unitPrice, volume, total, stationId, stationName, odo, isFull, notes, _ ->
                        onEvent(
                            ExpensesEvent.OnSaveExpense(
                                fuelType = fuelType,
                                unitPrice = unitPrice,
                                volumeQuantity = volume,
                                totalCost = total,
                                stationId = stationId,
                                stationName = stationName,
                                odometerAtExpense = odo,
                                isFullTank = isFull,
                                notes = notes,
                                lastRefuelTimestamp = state.lastRefuelTimestamp
                            )
                        )
                    }
                )
            }

            // Consumption banner
            state.consumptionBannerData?.let { data ->
                val avg = data.averageConsumption
                val isBetter = avg != null && data.consumptionPer100km < avg
                val bannerVariant = if (isBetter) CanvasKitAlertVariant.Success else CanvasKitAlertVariant.Info
                val subtitleText = when {
                    avg != null && isBetter ->
                        stringResource(R.string.expenses_banner_better_than_avg, avg)
                    avg != null ->
                        stringResource(R.string.expenses_banner_worse_than_avg, avg)
                    else -> null
                }
                CanvasKitBanner(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(CanvasKitTheme.spacing.md)
                        .navigationBarsPadding(),
                    variant = bannerVariant,
                    visible = true,
                    onDismiss = { onEvent(ExpensesEvent.OnDismissConsumptionBanner) },
                    message = {
                        Column {
                            Text(
                                text = stringResource(
                                    R.string.expenses_banner_consumption,
                                    data.consumptionPer100km,
                                    data.unit
                                ),
                                style = CanvasKitTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            subtitleText?.let {
                                Text(
                                    text = it,
                                    style = CanvasKitTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                )
            }

            // Toast-style Banner (Error & Success)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(CanvasKitTheme.spacing.md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasKitBanner(
                    variant = CanvasKitAlertVariant.Error,
                    message = { Text(state.error?.asString() ?: "") },
                    visible = state.error != null,
                    onDismiss = { onEvent(ExpensesEvent.OnDismissError) }
                )
            }

            if (state.showDeleteConfirmation) {
                CanvasKitConfirmDialog(
                    title = stringResource(R.string.expenses_delete_confirmation_title),
                    message = stringResource(R.string.expenses_delete_confirmation_message),
                    confirmText = stringResource(R.string.expenses_action_delete),
                    cancelText = stringResource(R.string.expenses_action_cancel),
                    onConfirm = { onEvent(ExpensesEvent.OnConfirmDeleteExpense) },
                    onDismissRequest = { onEvent(ExpensesEvent.OnCancelDeleteExpense) },
                    isDestructive = true,
                    icon = Icons.Default.Delete
                )
            }
        }
    }
}

@Composable
private fun KpiHeaderSection(
    state: ExpensesState,
    onEvent: (ExpensesEvent) -> Unit
) {
    CanvasKitCard(modifier = Modifier.fillMaxWidth(), variant = CanvasKitCardVariant.Elevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.expenses_kpi_month_spent),
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = NumberFormatter.formatCurrency(state.currentMonthTotalCost),
                    style = CanvasKitTheme.typography.headingLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.brandAccent
                )
                Text(
                    text = stringResource(R.string.expenses_kpi_total_spent, state.allTimeTotalCost),
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }

            state.kmSinceLastFullRefuel?.let { kms ->
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(onClick = safeClick { onEvent(ExpensesEvent.OnToggleTripsVisibility) })
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.expenses_kpi_km_since_refuel, kms),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (state.showTripsSinceLastRefuel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (state.showTripsSinceLastRefuel) {
                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))
                    TripsSinceRefuelList(state.recordsSinceLastRefuel)
                }
            }

            if (state.electrificationSavingsEuros > 0.0) {
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.success
                    )
                    Text(
                        text = stringResource(R.string.expenses_kpi_ev_savings, state.electrificationSavingsEuros),
                        style = CanvasKitTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CanvasKitTheme.colors.success
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    currentFilter: ExpenseFilterMode,
    onFilterSelected: (ExpenseFilterMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CanvasKitChip(
            selected = currentFilter == ExpenseFilterMode.ALL,
            variant = CanvasKitChipVariant.Outlined,
            onClick = { onFilterSelected(ExpenseFilterMode.ALL) },
            label = { Text(stringResource(R.string.expenses_filter_all)) }
        )
        CanvasKitChip(
            selected = currentFilter == ExpenseFilterMode.COMBUSTION,
            variant = CanvasKitChipVariant.Outlined,
            onClick = { onFilterSelected(ExpenseFilterMode.COMBUSTION) },
            label = { Text(stringResource(R.string.expenses_filter_combustion)) }
        )
        CanvasKitChip(
            selected = currentFilter == ExpenseFilterMode.ELECTRIC,
            variant = CanvasKitChipVariant.Outlined,
            onClick = { onFilterSelected(ExpenseFilterMode.ELECTRIC) },
            label = { Text(stringResource(R.string.expenses_filter_electric)) }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    expense: FuelExpense,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isElectric = expense.fuelType.category == EnergyCategory.ELECTRIC
    val unit = expense.fuelType.unitOfMeasure
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", locale)

    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isElectric) Icons.Default.ElectricBolt else Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = expense.stationName ?: expense.fuelType.toTextProvider().asString(),
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CanvasKitTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateFormatter.format(Date(expense.timestamp)),
                            style = CanvasKitTheme.typography.labelLarge,
                            color = CanvasKitTheme.colors.textSecondary
                        )

                        Text(
                            text = "${String.format(locale, "%.2f", expense.volumeQuantity)} $unit" +
                                        " -> ${String.format(locale, "%.3f", expense.unitPrice)} €/$unit",
                            style = CanvasKitTheme.typography.bodyMedium,
                            color = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Absolute.Right
                ) {
                    Text(
                        text = NumberFormatter.formatCurrency(expense.totalCost),
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.expenses_action_delete),
                            tint = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Consumption badges
            val consumption = expense.consumptionPer100km
            val kmSince = expense.kmSinceLastRefuel

            if (consumption != null || kmSince != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (consumption != null && expense.isFullTank) {
                        ConsumptionBadge(
                            text = stringResource(R.string.expenses_consumption_badge, consumption, unit)
                        )
                    }
                    if (kmSince != null) {
                        ConsumptionBadge(
                            text = stringResource(R.string.expenses_km_since_refuel, kmSince),
                            isSecondary = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripsSinceRefuelList(records: List<OdometerRecord>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CanvasKitTheme.colors.backgroundSecondary, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.expenses_trips_since_refuel_title).uppercase(),
            style = CanvasKitTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CanvasKitTheme.colors.brandAccent
        )

        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.expenses_no_trips_yet),
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
        } else {
            records.forEach { record ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.label ?: stringResource(R.string.expenses_trip_generic_label),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.expenses_trip_km_format, record.odometerValue),
                        style = CanvasKitTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsumptionBadge(
    text: String,
    isSecondary: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSecondary) {
            CanvasKitTheme.colors.backgroundSecondary
        } else {
            CanvasKitTheme.colors.brandAccent.copy(alpha = 0.12f)
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = CanvasKitTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSecondary) {
                CanvasKitTheme.colors.textSecondary
            } else {
                CanvasKitTheme.colors.brandAccent
            }
        )
    }
}

private class ExpensesStateProvider : androidx.compose.ui.tooling.preview.PreviewParameterProvider<ExpensesState> {
    override val values: Sequence<ExpensesState> = sequenceOf(
        ExpensesState(
            expenses = listOf(
                FuelExpense(
                    id = "1",
                    vehicleId = "v1",
                    stationName = "Repsol Castellana",
                    timestamp = System.currentTimeMillis(),
                    fuelType = FuelType.GASOLINE_95,
                    unitPrice = 1.659,
                    volumeQuantity = 45.0,
                    totalCost = 74.65,
                    isFullTank = true,
                    consumptionPer100km = 6.4,
                    kmSinceLastRefuel = 703.0
                ),
                FuelExpense(
                    id = "2",
                    vehicleId = "v1",
                    stationName = "Tesla Supercharger",
                    timestamp = System.currentTimeMillis() - 86400000 * 2,
                    fuelType = FuelType.ELECTRIC_KWH,
                    unitPrice = 0.35,
                    volumeQuantity = 52.0,
                    totalCost = 18.20,
                    isFullTank = true,
                    consumptionPer100km = 18.5,
                    kmSinceLastRefuel = 280.0
                )
            ).reversed(),
            filteredExpenses = listOf(
                FuelExpense(
                    id = "1",
                    vehicleId = "v1",
                    stationName = "Repsol Castellana",
                    timestamp = System.currentTimeMillis(),
                    fuelType = FuelType.GASOLINE_95,
                    unitPrice = 1.659,
                    volumeQuantity = 45.0,
                    totalCost = 74.65,
                    isFullTank = true,
                    consumptionPer100km = 6.4,
                    kmSinceLastRefuel = 703.0
                ),
                FuelExpense(
                    id = "2",
                    vehicleId = "v1",
                    stationName = "Tesla Supercharger",
                    timestamp = System.currentTimeMillis() - 86400000 * 2,
                    fuelType = FuelType.ELECTRIC_KWH,
                    unitPrice = 0.35,
                    volumeQuantity = 52.0,
                    totalCost = 18.20,
                    isFullTank = true,
                    consumptionPer100km = 18.5,
                    kmSinceLastRefuel = 280.0
                )
            ),
            currentMonthTotalCost = 92.85,
            allTimeTotalCost = 1250.40,
            electrificationSavingsEuros = 45.20
        ),
        ExpensesState(
            expenses = emptyList(),
            filteredExpenses = emptyList(),
            isLoading = false
        )
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpensesScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(ExpensesStateProvider::class) state: ExpensesState
) {
    CanvasKitTheme {
        ExpensesScreen(
            state = state,
            onEvent = {}
        )
    }
}
