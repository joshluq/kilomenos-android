package es.joshluq.kmsafe.feature.history

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.chips.CanvasKitChip
import es.joshluq.canvaskit.components.chips.CanvasKitChipVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitAccordion
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.R as CoreR
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.core.monetization.components.AdMobBanner
import es.joshluq.kmsafe.domain.model.RecordWithIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryRoute(
    onNavigateToDetail: (String) -> Unit
) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryEffect.NavigateToDetail -> onNavigateToDetail(effect.recordId)
            }
        }
    }

    HistoryScreen(
        state = state.value,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: HistoryState,
    onEvent: (HistoryEvent) -> Unit
) {
    CanvasKitLoadingScaffold(
        isLoading = state.isLoading && state.allRecords.isEmpty(),
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(CoreR.string.dashboard_item_history),
                        style = CanvasKitTheme.typography.headingMedium,
                    )
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary,
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!state.isPremium) {
                    AdMobBanner(
                        adUnitId = state.adUnitId
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))

                Box(modifier = Modifier.weight(1f)) {
                    val content = @Composable {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = CanvasKitTheme.spacing.screenHorizontal, vertical = 8.dp)
                        ) {
                            item {
                                SummaryHeader(
                                    totalKms = state.totalKms,
                                    totalRecordsCount = state.totalRecordsCount
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            item {
                                HistoryFilters(
                                    query = state.searchQuery,
                                    onQueryChange = { onEvent(HistoryEvent.OnSearchQueryChanged(it)) },
                                    currentMode = state.groupingMode,
                                    onModeChange = { onEvent(HistoryEvent.OnGroupingModeChanged(it)) }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            if (state.filteredGroups.isEmpty() && !state.isLoading) {
                                item { HistoryEmptyState() }
                            } else {
                                state.filteredGroups.forEach { (title, records) ->
                                    val isExpanded = state.expandedGroups.contains(title)
                                    val totalGroupKms = records.sumOf { it.record.odometerValue }

                                    item(key = "header_$title") {
                                        CanvasKitAccordion(
                                            expanded = isExpanded,
                                            onExpandedChange = {
                                                onEvent(HistoryEvent.OnToggleGroupExpansion(title))
                                            },
                                            headline = {
                                                GroupHeader(title = title)
                                            },
                                            trailingContent = {
                                                Text(
                                                    text = stringResource(CoreR.string.common_km_suffix, totalGroupKms),
                                                    style = CanvasKitTheme.typography.labelLarge,
                                                    color = CanvasKitTheme.colors.brandAccent,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                            },
                                            content = {}
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    if (isExpanded) {
                                        items(records, key = { it.record.id }) { item ->
                                            HistoryItem(
                                                item = item,
                                                onClick = { onEvent(HistoryEvent.OnRecordClicked(item)) }
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isPremium) {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { onEvent(HistoryEvent.OnRefresh) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            content()
                        }
                    } else {
                        content()
                    }
                }
            }

            // Toast-style Banner (Error)
            CanvasKitBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                variant = CanvasKitAlertVariant.Error,
                message = { Text(state.error?.asString() ?: "") },
                visible = state.error != null,
                onDismiss = { onEvent(HistoryEvent.OnDismissError) }
            )
        }
    }
}

@Composable
private fun SummaryHeader(totalKms: Double, totalRecordsCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = CanvasKitTheme.spacing.md)
    ) {
        Text(
            text = stringResource(R.string.history_total_kms_label),
            style = CanvasKitTheme.typography.bodyLarge,
            color = CanvasKitTheme.colors.textSecondary,
            letterSpacing = 0.5.sp
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = CanvasKitTheme.spacing.xxs)
        ) {
            Text(
                text = NumberFormatter.formatDistance(totalKms),
                style = CanvasKitTheme.typography.displayLarge,
                color = CanvasKitTheme.colors.brandAccent,
            )
            Text(
                text = " " + stringResource(CoreR.string.onboarding_km_suffix).lowercase(),
                style = CanvasKitTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.brandAccent,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }
        Text(
            text = stringResource(R.string.history_records_count_label, totalRecordsCount),
            style = CanvasKitTheme.typography.bodyMedium,
            color = CanvasKitTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HistoryEmptyState() {
    CanvasKitStateView(
        title = stringResource(R.string.history_empty_state_title),
        description = stringResource(R.string.history_empty_state_description),
        icon = {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = CanvasKitTheme.colors.brandAccent
            )
        }
    )
}

@Composable
private fun HistoryFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    currentMode: HistoryGroupingMode,
    onModeChange: (HistoryGroupingMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CanvasKitTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.history_search_label),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = CanvasKitTheme.colors.brandAccent
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GroupingChip(
                label = stringResource(R.string.history_label_day),
                icon = Icons.Default.Today,
                selected = currentMode == HistoryGroupingMode.DAY,
                onClick = { onModeChange(HistoryGroupingMode.DAY) }
            )
            GroupingChip(
                label = stringResource(R.string.history_label_month),
                icon = Icons.Default.CalendarToday,
                selected = currentMode == HistoryGroupingMode.MONTH,
                onClick = { onModeChange(HistoryGroupingMode.MONTH) }
            )
            GroupingChip(
                label = stringResource(R.string.history_label_year),
                icon = Icons.Default.DateRange,
                selected = currentMode == HistoryGroupingMode.YEAR,
                onClick = { onModeChange(HistoryGroupingMode.YEAR) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupingChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    CanvasKitChip(
        selected = selected,
        variant = CanvasKitChipVariant.Outlined,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun GroupHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = CanvasKitTheme.typography.bodyLarge,
            color = CanvasKitTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoryItem(
    item: RecordWithIndicator,
    onClick: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).apply {
            // Check if the record is likely a "Day" record (normalized to UTC 00:00)
            // or if it has a specific time. For now, we keep it simple but consistent.
            timeZone = if (item.record.isInitialRecord) {
                TimeZone.getTimeZone("UTC")
            } else {
                TimeZone.getDefault()
            }
        }
    }
    val dateStr = formatter.format(Date(item.record.timestamp))
    val indicatorColor = if (item.isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.success

    CanvasKitCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = dateStr,
                        style = CanvasKitTheme.typography.labelLarge,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                    Text(
                        text = stringResource(CoreR.string.common_km_suffix, item.record.odometerValue),
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                    if (item.record.label != null) {
                        Text(
                            text = item.record.label!!,
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.record.hasRoute) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowOutward,
                    contentDescription = stringResource(R.string.history_detail_action),
                    tint = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun HistoryScreenPreview() {
    CanvasKitTheme {
        HistoryScreen(
            state = HistoryState(
                totalKms = 1500.51,
                totalRecordsCount = 12,
                filteredGroups = mapOf(
                    "OCTUBRE 2023" to listOf(
                        RecordWithIndicator(
                            OdometerRecord(
                                "2",
                                "1",
                                System.currentTimeMillis(),
                                1500.45,
                                false,
                                label = "Viaje Trabajo"
                            ),
                            false
                        )
                    )
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun HistoryScreenEmptyPreview() {
    CanvasKitTheme {
        HistoryScreen(
            state = HistoryState(
                totalKms = 0.0,
                totalRecordsCount = 0,
                filteredGroups = emptyMap()
            ),
            onEvent = {}
        )
    }
}
