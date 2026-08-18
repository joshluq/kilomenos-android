package es.joshluq.kmsafe.ui.profile.vehicles

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitSkeleton
import es.joshluq.canvaskit.components.feedback.CanvasKitStateView
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.ui.util.safeClick
import kotlinx.coroutines.flow.Flow

@Composable
fun VehicleListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToVehicleDetails: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    viewModel: VehicleListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    VehicleListScreen(
        state = state,
        effects = viewModel.effects,
        onEvent = viewModel::sendEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToVehicleDetails = onNavigateToVehicleDetails,
        onNavigateToAddVehicle = onNavigateToAddVehicle,
        onNavigateToPremiumPaywall = onNavigateToPremiumPaywall
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    state: State,
    effects: Flow<Effect>? = null,
    onEvent: (Event) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToVehicleDetails: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit
) {
    LaunchedEffect(effects) {
        effects?.collect { effect ->
            when (effect) {
                Effect.NavigateBack -> onNavigateBack()
                Effect.NavigateToAddVehicle -> onNavigateToAddVehicle()
                Effect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
                is Effect.NavigateToVehicleDetails -> onNavigateToVehicleDetails(effect.id)
            }
        }
    }

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.vehicles_title),
                        style = CanvasKitTheme.typography.headingMedium,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = safeClick { onEvent(Event.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.acc_back),
                            tint = CanvasKitTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = safeClick { onEvent(Event.OnAddVehicleClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.acc_add_vehicle),
                            tint = CanvasKitTheme.colors.brandAccent
                        )
                    }
                }
            )
        },
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.vehicles.isEmpty() && !state.isLoading) {
                    CanvasKitStateView(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(R.string.vehicles_empty_title),
                        description = stringResource(R.string.vehicles_empty_description),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CarRental,
                                contentDescription = null,
                                modifier = Modifier.size(CanvasKitTheme.spacing.xxxl),
                                tint = CanvasKitTheme.colors.brandAccent
                            )
                        },
                        action = {
                            CanvasKitButton(
                                text = stringResource(R.string.vehicles_add_button),
                                onClick = safeClick { onNavigateToAddVehicle() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(CanvasKitTheme.spacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)
                    ) {
                        items(state.vehicles, key = { it.id }) { vehicle ->
                            VehicleItem(
                                vehicle = vehicle,
                                onClick = safeClick { onEvent(Event.OnVehicleSelected(vehicle.id)) },
                                onDetails = safeClick { onEvent(Event.OnVehicleDetailsClicked(vehicle.id)) },
                                onDelete = safeClick { onEvent(Event.OnDeleteVehicleClicked(vehicle)) }
                            )
                        }
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
                onDismiss = { onEvent(Event.OnDismissError) }
            )
        }

        if (state.vehicleToDelete != null) {
            CanvasKitConfirmDialog(
                title = stringResource(R.string.vehicles_delete_confirmation_title),
                message = stringResource(R.string.vehicles_delete_confirmation_message),
                confirmText = stringResource(R.string.vehicles_delete_confirm),
                cancelText = stringResource(R.string.vehicles_delete_cancel),
                onConfirm = { onEvent(Event.OnDeleteConfirmed) },
                onDismissRequest = { onEvent(Event.OnDeleteCancelled) },
                isDestructive = true,
                icon = Icons.Default.Delete
            )
        }

        if (state.showPremiumLimit) {
            CanvasKitConfirmDialog(
                title = stringResource(R.string.premium_limit_vehicle_title),
                message = stringResource(R.string.premium_limit_vehicle_message),
                confirmText = stringResource(R.string.premium_upgrade_confirm),
                cancelText = stringResource(R.string.premium_upgrade_cancel),
                onConfirm = { onEvent(Event.OnUpgradeClicked) },
                onDismissRequest = { onEvent(Event.OnDismissPremiumLimit) },
                icon = Icons.Default.Info
            )
        }
    }
}

@Composable
private fun VehicleItem(
    vehicle: RentingContract,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    CanvasKitCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        selected = vehicle.isSelected
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (vehicle.vehicleImageUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vehicle.vehicleImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
                                tint = CanvasKitTheme.colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vehicle.vehicleName,
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.vehicle_list_item_summary, vehicle.totalKms, vehicle.durationMonths),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }

            Row {
                IconButton(onClick = onDetails) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.acc_vehicle_info),
                        tint = CanvasKitTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.acc_delete_vehicle),
                        tint = CanvasKitTheme.colors.error
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun VehicleListScreenEmptyPreview() {
    CanvasKitTheme {
        VehicleListScreen(
            state = State(vehicles = emptyList(), isLoading = false),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToVehicleDetails = {},
            onNavigateToAddVehicle = {},
            onNavigateToPremiumPaywall = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun VehicleListScreenLoadingPreview() {
    CanvasKitTheme {
        VehicleListScreen(
            state = State(isLoading = true),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToVehicleDetails = {},
            onNavigateToAddVehicle = {},
            onNavigateToPremiumPaywall = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun VehicleListScreenPreview() {
    CanvasKitTheme {
        VehicleListScreen(
            state = State(
                vehicles = listOf(
                    RentingContract(
                        id = "1",
                        vehicleName = "Tesla Model 3 Tesla Model 3 Tesla Model 3 Tesla Model 3",
                        totalKms = 30000,
                        durationMonths = 36,
                        isSelected = true,
                        startDate = 0,
                        startOdometer = 0,
                        currentOdometer = 0,
                        vehicleImageUrl = "https://www.carlogos.org/car-logos/tesla-logo.png"
                    ),
                    RentingContract(
                        id = "2",
                        vehicleName = "Ford Focus",
                        totalKms = 45000,
                        durationMonths = 48,
                        isSelected = false,
                        startDate = 0,
                        startOdometer = 0,
                        currentOdometer = 0,
                        vehicleImageUrl = "https://www.carlogos.org/car-logos/ford-logo.png"
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToVehicleDetails = {},
            onNavigateToAddVehicle = {},
            onNavigateToPremiumPaywall = {}
        )
    }
}
