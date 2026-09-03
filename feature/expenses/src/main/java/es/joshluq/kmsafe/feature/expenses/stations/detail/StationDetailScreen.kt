package es.joshluq.kmsafe.feature.expenses.stations.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.toTextProvider
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.domain.model.EnergyCategory
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.feature.expenses.R
import es.joshluq.kmsafe.feature.expenses.components.PriceLineChart
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailScreen(
    state: StationDetailState,
    onEvent: (StationDetailEvent) -> Unit,
    onBack: () -> Unit
) {
    val detail = state.detail
    val station = detail?.station

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading && detail == null,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = station?.name ?: "",
                        style = CanvasKitTheme.typography.headingMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    station?.let {
                        IconButton(onClick = { onEvent(StationDetailEvent.OnToggleFavorite(!it.isFavorite)) }) {
                            Icon(
                                imageVector = if (it.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (it.isFavorite) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { paddingValues ->
        val content = @Composable {
            if (detail != null && station != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 1. Map Section (if coordinates exist)
                    if (station.latitude != 0.0 || station.longitude != 0.0) {
                        item {
                            StationMapHeader(station.latitude, station.longitude, station.name)
                        }
                    }

                    // 2. Info Card
                    item {
                        Column(
                            modifier = Modifier.padding(CanvasKitTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
                        ) {
                            StationInfoCard(station.brand, station.address)
                            
                            // 3. Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
                            ) {
                                StatCard(
                                    label = stringResource(R.string.stations_detail_total_spent),
                                    value = NumberFormatter.formatCurrency(detail.totalSpent),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = stringResource(R.string.stations_detail_refuel_count),
                                    value = detail.refuelCount.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 4. Volatility Chart
                    state.volatility?.let { vol ->
                        if (vol.priceHistory.size > 1) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = CanvasKitTheme.spacing.md)) {
                                    Text(
                                        text = stringResource(R.string.expenses_volatility_title, vol.fuelType.toTextProvider().asString()),
                                        style = CanvasKitTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CanvasKitTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))
                                    CanvasKitCard(modifier = Modifier.fillMaxWidth()) {
                                        PriceLineChart(
                                            history = vol.priceHistory,
                                            minPrice = vol.minRecordedPrice,
                                            maxPrice = vol.maxRecordedPrice,
                                            avgPrice = vol.historicalAveragePrice,
                                            modifier = Modifier.padding(CanvasKitTheme.spacing.sm)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. History Header
                    item {
                        Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))
                        Text(
                            text = stringResource(R.string.stations_detail_history_title),
                            style = CanvasKitTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CanvasKitTheme.colors.textPrimary,
                            modifier = Modifier.padding(horizontal = CanvasKitTheme.spacing.md)
                        )
                        Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))
                    }

                    // 6. History List
                    items(detail.expenseHistory, key = { it.id }) { expense ->
                        HistoryItemRow(expense)
                    }
                }
            }
        }

        if (state.isPremium) {
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { onEvent(StationDetailEvent.OnRefresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                content = { content() }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StationMapHeader(lat: Double, lng: Double, name: String) {
    val position = remember(lat, lng) { LatLng(lat, lng) }
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 15f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(CanvasKitTheme.colors.borderSubtle)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        ) {
            Marker(
                state = remember(position) { MarkerState(position = position) },
                title = name
            )
        }
    }
}

@Composable
private fun StationInfoCard(brand: String, address: String) {
    CanvasKitCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent
                )
            }
            Column {
                Text(
                    text = brand,
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                if (address.isNotBlank()) {
                    Text(
                        text = address,
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    CanvasKitCard(modifier = modifier) {
        Column {
            Text(
                text = label,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
            Text(
                text = value,
                style = CanvasKitTheme.typography.headingMedium,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun HistoryItemRow(expense: FuelExpense) {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", locale) }
    val isElectric = expense.fuelType.category == EnergyCategory.ELECTRIC

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CanvasKitTheme.spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isElectric) Icons.Default.ElectricBolt else Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = CanvasKitTheme.colors.brandAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormatter.format(Date(expense.timestamp)),
                    style = CanvasKitTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CanvasKitTheme.colors.textPrimary
                )
                Text(
                    text = "${String.format(locale, "%.2f", expense.volumeQuantity)} ${expense.fuelType.unitOfMeasure}",
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
            Text(
                text = NumberFormatter.formatCurrency(expense.totalCost),
                style = CanvasKitTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.textPrimary
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = CanvasKitTheme.spacing.md),
            thickness = 0.5.dp,
            color = CanvasKitTheme.colors.borderSubtle
        )
    }
}
