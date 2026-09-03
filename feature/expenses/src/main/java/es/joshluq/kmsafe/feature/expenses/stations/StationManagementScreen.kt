package es.joshluq.kmsafe.feature.expenses.stations

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.inputs.CanvasKitSwitch
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.feature.expenses.R

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StationManagementScreen(
    state: StationManagementState,
    onEvent: (StationManagementEvent) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val handleCaptureLocation = {
        if (locationPermissionState.status.isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                location?.let {
                    onEvent(StationManagementEvent.OnLocationCaptured(it.latitude, it.longitude))
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading && state.stations.isEmpty(),
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.stations_management_title),
                        style = CanvasKitTheme.typography.headingMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = safeClick { onEvent(StationManagementEvent.OnAddStationClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.stations_action_add),
                            tint = CanvasKitTheme.colors.brandAccent
                        )
                    }
                },
                centeredTitle = true
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { paddingValues ->
        val content = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CanvasKitTheme.spacing.md)
            ) {
                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                // Search Bar
                CanvasKitTextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(StationManagementEvent.OnSearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.stations_search_placeholder),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.md))

                if (state.filteredStations.isEmpty() && !state.isLoading) {
                    CanvasKitStateView(
                        title = stringResource(R.string.stations_empty_title),
                        description = stringResource(R.string.stations_empty_desc),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = CanvasKitTheme.colors.brandAccent
                            )
                        },
                        action = {
                            CanvasKitButton(
                                text = stringResource(R.string.stations_action_add),
                                onClick = safeClick { onEvent(StationManagementEvent.OnAddStationClicked) },
                                variant = CanvasKitButtonVariant.Primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
                    ) {
                        items(state.filteredStations, key = { it.id }) { station ->
                            StationItemCard(
                                station = station,
                                onClick = { onEvent(StationManagementEvent.OnViewDetail(station.id)) },
                                onToggleFavorite = { onEvent(StationManagementEvent.OnToggleFavorite(station.id, it)) },
                                onEdit = { onEvent(StationManagementEvent.OnEditStation(station)) },
                                onDelete = { onEvent(StationManagementEvent.OnDeleteStation(station.id)) }
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
                    onRefresh = { onEvent(StationManagementEvent.OnRefresh) },
                    modifier = Modifier.fillMaxSize(),
                    content = { content() }
                )
            } else {
                content()
            }

            // Edit/Add Bottom Sheet
            if (state.isEditSheetOpen) {
                EditStationBottomSheet(
                    station = state.selectedStation,
                    isSaving = state.isSaving,
                    isLocationCaptured = state.currentLat != null,
                    onLocationToggle = { enabled ->
                        if (enabled) handleCaptureLocation()
                        else onEvent(StationManagementEvent.OnLocationCaptured(0.0, 0.0))
                    },
                    onDismiss = { onEvent(StationManagementEvent.OnDismissEdit) },
                    onSave = { name, brand ->
                        onEvent(StationManagementEvent.OnSaveStation(state.selectedStation?.id, name, brand))
                    }
                )
            }

            // Banner Notifications
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(CanvasKitTheme.spacing.md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.error != null) {
                    CanvasKitBanner(
                        variant = CanvasKitAlertVariant.Error,
                        message = { Text(state.error.asString()) },
                        visible = true,
                        onDismiss = { onEvent(StationManagementEvent.OnDismissError) }
                    )
                }
            }

            if (state.showDeleteConfirmation) {
                CanvasKitConfirmDialog(
                    title = stringResource(R.string.stations_delete_confirmation_title),
                    message = stringResource(R.string.stations_delete_confirmation_message),
                    confirmText = stringResource(R.string.stations_action_delete),
                    cancelText = stringResource(R.string.stations_action_cancel),
                    onConfirm = { onEvent(StationManagementEvent.OnConfirmDeleteStation) },
                    onDismissRequest = { onEvent(StationManagementEvent.OnCancelDeleteStation) },
                    isDestructive = true,
                    icon = Icons.Default.Delete
                )
            }
        }
    }
}

@Composable
private fun StationItemCard(
    station: ServiceStation,
    onClick: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onToggleFavorite(!station.isFavorite) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (station.isFavorite) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = station.brand,
                    style = CanvasKitTheme.typography.labelLarge,
                    color = CanvasKitTheme.colors.brandAccent
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = safeClick { onEdit() }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.stations_action_edit), tint = CanvasKitTheme.colors.textSecondary)
                }
                IconButton(onClick = safeClick { onDelete() }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.stations_action_delete), tint = CanvasKitTheme.colors.textSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStationBottomSheet(
    station: ServiceStation?,
    isSaving: Boolean,
    isLocationCaptured: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, brand: String) -> Unit
) {
    var name by remember { mutableStateOf(station?.name ?: "") }
    var brand by remember { mutableStateOf(station?.brand ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CanvasKitTheme.spacing.lg)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
        ) {
            Text(
                text = if (station == null) {
                    stringResource(R.string.stations_add_title)
                } else {
                    stringResource(R.string.stations_edit_title)
                },
                style = CanvasKitTheme.typography.headingMedium,
                fontWeight = FontWeight.Bold
            )

            CanvasKitTextField(
                label = stringResource(R.string.stations_field_name),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.stations_field_name_placeholder),
                modifier = Modifier.fillMaxWidth()
            )

            CanvasKitTextField(
                label = stringResource(R.string.stations_field_brand),
                value = brand,
                onValueChange = { brand = it },
                placeholder = stringResource(R.string.stations_field_brand_placeholder),
                modifier = Modifier.fillMaxWidth()
            )

            // Location Capture Toggle
            CanvasKitCard(
                modifier = Modifier.fillMaxWidth(),
                variant = es.joshluq.canvaskit.components.cards.CanvasKitCardVariant.Outlined
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CanvasKitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isLocationCaptured) CanvasKitTheme.colors.success else CanvasKitTheme.colors.textSecondary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.stations_location_toggle),
                                style = CanvasKitTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CanvasKitTheme.colors.textPrimary
                            )
                            Text(
                                text = if (isLocationCaptured) 
                                    stringResource(R.string.expenses_location_captured) 
                                else 
                                    stringResource(R.string.stations_location_toggle_desc),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = if (isLocationCaptured) CanvasKitTheme.colors.success else CanvasKitTheme.colors.textSecondary
                            )
                        }
                    }
                    CanvasKitSwitch(
                        checked = isLocationCaptured,
                        onCheckedChange = onLocationToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.sm))

            CanvasKitButton(
                text = stringResource(R.string.stations_action_save),
                onClick = safeClick { onSave(name, brand) },
                enabled = !isSaving && name.isNotBlank() && brand.isNotBlank(),
                loading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
