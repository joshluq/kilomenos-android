package es.joshluq.kmsafe.feature.expenses.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CameraAlt
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonSize
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.chips.CanvasKitChip
import es.joshluq.canvaskit.components.chips.CanvasKitChipVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.inputs.CanvasKitSwitch
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.model.EnergyCategory
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ReceiptScanResult
import es.joshluq.kmsafe.core.ui.util.toTextProvider
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.feature.expenses.R
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Refined "Quick Capture" bottom sheet for registering a fuel or EV charging expense.
 *
 * Design principles:
 * - **Minimum friction**: volume field autoFocused, 3 visible inputs max.
 * - **Smart pre-fill**: fuel type derived from vehicle history, price preLoaded from last refuel.
 * - **Inline toggle**: full-tank switch shares the same row as volume for visual compactness.
 * - **Live total**: total cost animates in real time as the user types price × volume.
 * - **Price delta**: shows a colored ↑/↓ indicator vs. the last recorded price.
 * - **No notes field**: removed to reduce friction; can be added in a future edit flow.
 *
 * @param currentOdometer Pre-filled odometer from ExpensesState.
 * @param vehicleFuelType The primary fuel type of the vehicle (determines if hybrid).
 * @param lastUnitPrice Unit price from the most recent expense of the same type.
 * @param lastGasolinePrice Last price for combustion type (for hybrid switching).
 * @param lastElectricPrice Last price for electric type (for hybrid switching).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    currentOdometer: Double = 0.0,
    vehicleFuelType: FuelType = FuelType.GASOLINE_95,
    lastUsedFuelType: FuelType = FuelType.GASOLINE_95,
    lastUnitPrice: Double? = null,
    lastGasolinePrice: Double? = null,
    lastElectricPrice: Double? = null,
    initialStationId: String? = null,
    priceReportMode: Boolean = false,
    isSaving: Boolean = false,
    stations: List<ServiceStation> = emptyList(),
    isLocationCaptured: Boolean = false,
    scannedReceiptResult: ReceiptScanResult? = null,
    isScanningReceipt: Boolean = false,
    onScanReceiptClick: () -> Unit = {},
    onDiscardScan: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        fuelType: FuelType,
        unitPrice: Double,
        volumeQuantity: Double,
        totalCost: Double,
        stationId: String?,
        stationName: String?,
        odometerAtExpense: Double?,
        isFullTank: Boolean,
        notes: String?,
        lastRefuelTimestamp: Long?,
        receiptImagePath: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var volumeText by remember { mutableStateOf("") }
    var unitPriceText by remember {
        mutableStateOf(lastUnitPrice?.let { String.format(Locale.getDefault(), "%.3f", it) } ?: "")
    }
    var isFullTank by remember { mutableStateOf(true) }
    var odometerText by remember {
        mutableStateOf(if (currentOdometer > 0.0) String.format(Locale.getDefault(), "%.2f", currentOdometer) else "")
    }
    var selectedStation by remember { 
        mutableStateOf(stations.find { it.id == initialStationId }) 
    }
    var stationNameInput by remember { mutableStateOf("") }
    
    // Hybrid state logic: 
    // We determine if it's hybrid based on the vehicle's default fuel type, 
    // but we pre-select the category based on what was last used.
    val isHybrid = vehicleFuelType.category == EnergyCategory.HYBRID
    var selectedHybridCategory by remember { 
        mutableStateOf(
            if (lastUsedFuelType.category == EnergyCategory.ELECTRIC) EnergyCategory.ELECTRIC 
            else EnergyCategory.COMBUSTION
        ) 
    }

    val effectiveFuelType = when {
        isHybrid && selectedHybridCategory == EnergyCategory.ELECTRIC -> FuelType.ELECTRIC_KWH
        isHybrid && selectedHybridCategory == EnergyCategory.COMBUSTION -> FuelType.GASOLINE_95
        else -> lastUsedFuelType
    }

    val unit = effectiveFuelType.unitOfMeasure
    val isElectric = effectiveFuelType.category == EnergyCategory.ELECTRIC

    /** Normalizes comma-decimal input (e.g. "1,5" → "1.5") for consistent parsing. */
    fun String.normalizeDecimal() = this.replace(',', '.')

    val unitPrice = unitPriceText.normalizeDecimal().toDoubleOrNull() ?: 0.0
    val volume = volumeText.normalizeDecimal().toDoubleOrNull() ?: 0.0
    val computedTotal = if (unitPrice > 0.0 && volume > 0.0) unitPrice * volume else 0.0

    // Animated total for live feedback as the user types
    val animatedTotal by animateFloatAsState(
        targetValue = computedTotal.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "total_animation"
    )

    // Price delta vs. last refuel (Context-aware for Hybrid switching)
    val effectiveLastPrice = when {
        isHybrid && selectedHybridCategory == EnergyCategory.ELECTRIC -> lastElectricPrice
        isHybrid && selectedHybridCategory == EnergyCategory.COMBUSTION -> lastGasolinePrice
        else -> lastUnitPrice
    }
    val priceDelta = if (effectiveLastPrice != null && unitPrice > 0.0) unitPrice - effectiveLastPrice else null

    LaunchedEffect(Unit) { 
        if (priceReportMode) priceFocusRequester.requestFocus()
        else focusRequester.requestFocus() 
    }

    LaunchedEffect(scannedReceiptResult) {
        if (scannedReceiptResult != null) {
            stationNameInput = scannedReceiptResult.stationName
            if (scannedReceiptResult.pricePerLiter > 0.0) {
                unitPriceText = String.format(Locale.getDefault(), "%.3f", scannedReceiptResult.pricePerLiter)
            }
            if (scannedReceiptResult.liters > 0.0) {
                volumeText = String.format(Locale.getDefault(), "%.2f", scannedReceiptResult.liters)
            }
        }
    }

    val locale = LocalLocale.current.platformLocale
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CanvasKitTheme.spacing.lg)
                .padding(bottom = CanvasKitTheme.spacing.xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            // Header: fuel type badge (read-only) + context
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (priceReportMode) {
                            stringResource(R.string.expenses_sheet_title_price_report)
                        } else if (isHybrid) {
                            stringResource(R.string.expenses_sheet_title_hybrid)
                        } else if (isElectric) {
                            stringResource(R.string.expenses_sheet_title_ev)
                        } else {
                            stringResource(R.string.expenses_sheet_title_fuel)
                        },
                        style = CanvasKitTheme.typography.headingMedium,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                    Text(
                        text = lastUsedFuelType.toTextProvider().asString(),
                        style = CanvasKitTheme.typography.bodyMedium,
                        color = CanvasKitTheme.colors.brandAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isLocationCaptured) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.success,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.expenses_location_captured),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.success
                            )
                        }
                    }
                }
                // Live total counter — the main UX highlight
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.expenses_sheet_total_cost),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        text = NumberFormatter.formatCurrency(animatedTotal.toDouble()),
                        style = CanvasKitTheme.typography.headingLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (computedTotal > 0.0) {
                            CanvasKitTheme.colors.brandAccent
                        } else {
                            CanvasKitTheme.colors.textSecondary
                        }
                    )
                }
            }

            // Layer 3 Zero-Friction: AI Receipt Scanner Action / Banner
            if (!priceReportMode && !isElectric) {
                if (scannedReceiptResult != null) {
                    CanvasKitBanner(
                        message = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.expenses_scan_badge))
                                Text(
                                    text = stringResource(R.string.expenses_scan_discard),
                                    style = CanvasKitTheme.typography.labelSmall,
                                    color = CanvasKitTheme.colors.brandAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable(onClick = safeClick { onDiscardScan() })
                                )
                            }
                        },
                        variant = CanvasKitAlertVariant.Info
                    )

                    if (!scannedReceiptResult.arithmeticCheck.valid) {
                        CanvasKitBanner(
                            message = {
                                Text(
                                    stringResource(
                                        R.string.expenses_scan_discrepancy_alert,
                                        scannedReceiptResult.arithmeticCheck.calculatedAmount,
                                        scannedReceiptResult.totalAmount
                                    )
                                )
                            },
                            variant = CanvasKitAlertVariant.Warning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hybrid Selector
            if (isHybrid) {
                Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)) {
                    Text(
                        text = stringResource(R.string.expenses_sheet_energy_type),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CanvasKitChip(
                            selected = selectedHybridCategory == EnergyCategory.COMBUSTION,
                            label = { Text(stringResource(R.string.expenses_sheet_energy_combustion)) },
                            onClick = safeClick { 
                                selectedHybridCategory = EnergyCategory.COMBUSTION
                                unitPriceText = lastGasolinePrice?.let { String.format(Locale.getDefault(), "%.3f", it) } ?: ""
                            },
                            variant = CanvasKitChipVariant.Outlined
                        )
                        CanvasKitChip(
                            selected = selectedHybridCategory == EnergyCategory.ELECTRIC,
                            label = { Text(stringResource(R.string.expenses_sheet_energy_electric)) },
                            onClick = safeClick { 
                                selectedHybridCategory = EnergyCategory.ELECTRIC 
                                unitPriceText = lastElectricPrice?.let { String.format(Locale.getDefault(), "%.3f", it) } ?: ""
                            },
                            variant = CanvasKitChipVariant.Outlined
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ⓪ Station Selection
            Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)) {
                Text(
                    text = if (isElectric) {
                        stringResource(R.string.expenses_sheet_station_ev)
                    } else {
                        stringResource(R.string.expenses_sheet_station_fuel)
                    },
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )

                if (stations.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        stations.take(4).forEach { station ->
                            val isSelected = selectedStation?.id == station.id
                            CanvasKitChip(
                                selected = isSelected,
                                label = { Text(station.name) },
                                onClick = safeClick { 
                                    selectedStation = if (isSelected) null else station 
                                    if (!isSelected) stationNameInput = ""
                                },
                                variant = CanvasKitChipVariant.Outlined
                            )
                        }
                    }
                }

                if (selectedStation == null) {
                    CanvasKitTextField(
                        value = stationNameInput,
                        onValueChange = { stationNameInput = it },
                        placeholder = if (isElectric) {
                            stringResource(R.string.expenses_sheet_station_ev)
                        } else {
                            stringResource(R.string.expenses_sheet_station_fuel)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    )
                }
            }

            // Layer 3 Zero-Friction: Quick Presets (20€, 30€, 50€, Lleno)
            if (!priceReportMode && !isElectric) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
                ) {
                    listOf(20.0, 30.0, 50.0).forEach { presetAmount ->
                        val presetLabel = when (presetAmount) {
                            20.0 -> stringResource(R.string.expenses_preset_20)
                            30.0 -> stringResource(R.string.expenses_preset_30)
                            else -> stringResource(R.string.expenses_preset_50)
                        }
                        CanvasKitChip(
                            selected = false,
                            label = { Text(presetLabel) },
                            onClick = safeClick {
                                val price = unitPriceText.normalizeDecimal().toDoubleOrNull() ?: 0.0
                                if (price > 0.0) {
                                    val computedVol = presetAmount / price
                                    volumeText = String.format(locale, "%.2f", computedVol)
                                } else {
                                    priceFocusRequester.requestFocus()
                                }
                            },
                            variant = CanvasKitChipVariant.Outlined
                        )
                    }
                    CanvasKitChip(
                        selected = isFullTank,
                        label = { Text(stringResource(R.string.expenses_preset_full_tank)) },
                        onClick = safeClick { isFullTank = !isFullTank },
                        variant = CanvasKitChipVariant.Outlined
                    )
                }
            }

            // ① Volume + Full-tank toggle — same row (Hidden in price report mode)
            if (!priceReportMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanvasKitTextField(
                        value = volumeText,
                        onValueChange = { volumeText = it },
                        label = if (isElectric) {
                            stringResource(R.string.expenses_sheet_volume_ev)
                        } else {
                            stringResource(R.string.expenses_sheet_volume_fuel)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )

                    // Full-tank toggle inline with info tooltip
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.expenses_sheet_full_tank),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = if (isFullTank) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary,
                            fontWeight = if (isFullTank) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val tooltipState = rememberTooltipState()
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                            tooltip = {
                                Text(
                                    text = stringResource(R.string.expenses_sheet_full_tank_hint),
                                    style = CanvasKitTheme.typography.labelSmall
                                )
                            },
                            state = tooltipState
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CanvasKitSwitch(
                                    checked = isFullTank,
                                    onCheckedChange = { isFullTank = it }
                                )
                                IconButton(
                                    onClick = { 
                                        coroutineScope.launch { tooltipState.show() }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = stringResource(R.string.expenses_sheet_full_tank_hint),
                                        tint = CanvasKitTheme.colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
                ) {
                    CanvasKitTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = if (isElectric) {
                            stringResource(R.string.expenses_sheet_price_ev)
                        } else {
                            stringResource(R.string.expenses_sheet_price_fuel)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.focusRequester(priceFocusRequester)
                    )
                    // Price delta vs. last refuel
                    AnimatedVisibility(visible = priceDelta != null) {
                        priceDelta?.let { delta ->
                            val isUp = delta > 0.0
                            val arrow = if (isUp) "↑" else "↓"
                            val color = if (isUp) CanvasKitTheme.colors.error else CanvasKitTheme.colors.success
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) {
                                        append("$arrow ${String.format(locale, "%.3f", kotlin.math.abs(delta))} €/$unit ")
                                    }
                                    withStyle(SpanStyle(color = CanvasKitTheme.colors.textSecondary)) {
                                        append(stringResource(R.string.expenses_sheet_price_delta_label))
                                    }
                                },
                                style = CanvasKitTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (!priceReportMode) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.xs)
                    ) {
                        CanvasKitTextField(

                            value = odometerText,
                            onValueChange = { odometerText = it },
                            label = stringResource(R.string.expenses_sheet_odometer),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        if (currentOdometer > 0.0) {
                            Text(
                                text = stringResource(R.string.expenses_sheet_odometer_hint, currentOdometer),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.brandAccent
                            )
                        }
                    }
                }

            }
            Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

            // Save CTA
            CanvasKitButton(
                text = if (isElectric) {
                    stringResource(R.string.expenses_sheet_action_save_ev)
                } else if (priceReportMode) {
                    stringResource(R.string.expenses_sheet_action_update_price)
                } else {
                    stringResource(R.string.expenses_sheet_action_save)
                },
                enabled = !isSaving && unitPrice > 0.0 && (priceReportMode || volume > 0.0),
                loading = isSaving,
                modifier = Modifier.fillMaxWidth().testTag("expense_save_button"),
                onClick = safeClick {
                    if (unitPrice > 0.0 && (priceReportMode || volume > 0.0)) {
                        onSave(
                            effectiveFuelType,
                            unitPrice,
                            if (priceReportMode) 0.0 else volume,
                            if (priceReportMode) 0.0 else computedTotal,
                            selectedStation?.id,
                            selectedStation?.name ?: stationNameInput.takeIf { it.isNotBlank() },
                            if (priceReportMode) null else odometerText.normalizeDecimal().toDoubleOrNull(),
                            if (priceReportMode) false else isFullTank,
                            null, // notes: removed from this version
                            null, // lastRefuelTimestamp injected via state in ExpensesScreen
                            null  // receiptImagePath injected via screen / state
                        )
                    }
                }
            )
        }
    }
}
