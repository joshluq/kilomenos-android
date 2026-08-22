package es.joshluq.kmsafe.ui.history.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.feedback.CanvasKitAlertVariant
import es.joshluq.canvaskit.components.feedback.CanvasKitBanner
import es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialog
import es.joshluq.canvaskit.components.feedback.CanvasKitDialogContent
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.layout.CanvasKitLoadingScaffold
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.safeClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun RecordDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit
) {
    val viewModel: RecordDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecordDetailEffect.NavigateBack -> onNavigateBack()
                RecordDetailEffect.NavigateToPremiumPaywall -> onNavigateToPremiumPaywall()
            }
        }
    }

    RecordDetailScreen(
        state = state,
        onEvent = viewModel::sendEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    state: RecordDetailState,
    onEvent: (RecordDetailEvent) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    CanvasKitLoadingScaffold(
        isLoading = state.isLoading,
        topBar = {
            CanvasKitTopBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_detail_action),
                        style = CanvasKitTheme.typography.headingMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = safeClick {
                            keyboardController?.hide()
                            onEvent(RecordDetailEvent.OnBackClicked)
                        }
                    ) {
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
        containerColor = CanvasKitTheme.colors.backgroundSecondary
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
            ) {
                state.record?.let { record ->
                    Spacer(modifier = Modifier.height(24.dp))

                    // Hero Data
                    HeroMileageCard(record.odometerValue)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Details Section
                    DetailSection(record)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Consumption Section (Premium)
                    if (state.isPremium && state.consumptionL100km != null) {
                        ConsumptionCard(state.consumptionL100km)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Route Map Section
                    RouteMapCard(
                        isPremium = state.isPremium,
                        hasRoute = record.hasRoute,
                        encodedPolyline = state.route?.encodedPolyline,
                        isLoading = state.isRouteLoading,
                        onUpgradeClicked = { onEvent(RecordDetailEvent.OnPremiumUpgradeClicked) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons
                    ActionButtons(onEvent)

                    Spacer(modifier = Modifier.height(32.dp))
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
                onDismiss = { onEvent(RecordDetailEvent.OnDismissError) }
            )

            if (state.showDeleteConfirmation) {
                DeleteConfirmationDialog(onEvent)
            }

            if (state.showEditDialog) {
                EditRecordDialog(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun RouteMapCard(
    isPremium: Boolean,
    hasRoute: Boolean,
    encodedPolyline: String?,
    isLoading: Boolean,
    onUpgradeClicked: () -> Unit
) {
    CanvasKitCard {
        Column {
            Text(
                text = stringResource(R.string.history_detail_route_title),
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(CanvasKitTheme.shapes.medium)
                    .background(CanvasKitTheme.colors.backgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    !isPremium -> {
                        TeaserOverlay(
                            text = stringResource(R.string.history_detail_route_premium_teaser),
                            onClick = onUpgradeClicked
                        )
                    }
                    !hasRoute || encodedPolyline.isNullOrBlank() -> {
                        Text(
                            text = stringResource(R.string.history_detail_no_route),
                            style = CanvasKitTheme.typography.bodyLarge,
                            color = CanvasKitTheme.colors.textSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        RouteMap(encodedPolyline)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteMap(encodedPolyline: String) {
    val points = remember(encodedPolyline) { PolyUtil.decode(encodedPolyline) }
    val cameraPositionState = rememberCameraPositionState()

    if (points.isNotEmpty()) {
        LaunchedEffect(points) {
            val boundsBuilder = LatLngBounds.builder()
            points.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            // Padding increased to 150 to show more references around the route
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        }

    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = false
        )
    ) {
        Polyline(
            points = points,
            color = CanvasKitTheme.colors.brandAccent,
            width = 8f,
            startCap = RoundCap(),
            endCap = RoundCap(),
            jointType = JointType.ROUND
        )
    }
}

@Composable
private fun TeaserOverlay(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeClickable(onClick = onClick)
            .background(CanvasKitTheme.colors.textPrimary.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CanvasKitTheme.colors.textSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.premium_upgrade_confirm),
                style = CanvasKitTheme.typography.labelLarge,
                color = CanvasKitTheme.colors.brandAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeroMileageCard(kms: Double) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.overview_current_odometer_label).uppercase(),
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary,
                letterSpacing = 1.sp
            )
            Text(
                text = stringResource(R.string.common_km_suffix, kms),
                style = CanvasKitTheme.typography.displayLarge,
                color = CanvasKitTheme.colors.brandAccent,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun DetailSection(record: OdometerRecord) {
    val dateFormatter = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    CanvasKitCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailRow(
                icon = Icons.Default.CalendarToday,
                label = stringResource(R.string.overview_record_date_label),
                value = dateFormatter.format(Date(record.timestamp))
            )

            DetailRow(
                icon = Icons.Default.AccessTime,
                label = stringResource(R.string.history_detail_time_label),
                value = timeFormatter.format(Date(record.timestamp))
            )

            if (record.label != null) {
                DetailRow(
                    icon = Icons.AutoMirrored.Default.Label,
                    label = stringResource(R.string.overview_record_label_label),
                    value = record.label!!
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = CanvasKitTheme.colors.brandAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
            Text(
                text = value,
                style = CanvasKitTheme.typography.bodyLarge,
                color = CanvasKitTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConsumptionCard(l100km: Double) {
    val cardColor = CanvasKitTheme.colors.brandPrimary.copy(alpha = 0.05f)
    CanvasKitCard {
        Box(modifier = Modifier.background(cardColor)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.history_detail_consumption_title),
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.brandAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.common_fuel_consumption_format, l100km),
                        style = CanvasKitTheme.typography.headingLarge,
                        color = CanvasKitTheme.colors.textPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = CanvasKitTheme.colors.brandAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(onEvent: (RecordDetailEvent) -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.md)) {
        CanvasKitButton(
            text = stringResource(R.string.history_edit_action),
            icon = Icons.Default.Edit,
            onClick = safeClick {
                keyboardController?.hide()
                focusManager.clearFocus()
                onEvent(RecordDetailEvent.OnEditClicked)
            },
            modifier = Modifier.fillMaxWidth()
        )

        CanvasKitButton(
            text = stringResource(R.string.history_delete_action),
            icon = Icons.Default.Delete,
            onClick = safeClick {
                keyboardController?.hide()
                focusManager.clearFocus()
                onEvent(RecordDetailEvent.OnDeleteClicked)
            },
            variant = CanvasKitButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(onEvent: (RecordDetailEvent) -> Unit) {
    CanvasKitConfirmDialog(
        title = stringResource(R.string.history_delete_confirmation_title),
        message = stringResource(R.string.history_delete_confirmation_message),
        confirmText = stringResource(R.string.history_delete_action),
        cancelText = stringResource(R.string.profile_logout_cancel),
        onConfirm = { onEvent(RecordDetailEvent.OnConfirmDelete) },
        onDismissRequest = { onEvent(RecordDetailEvent.OnCancelDelete) },
        isDestructive = true,
        icon = Icons.Default.Delete
    )
}

@Composable
fun EditRecordDialog(
    state: RecordDetailState,
    onEvent: (RecordDetailEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    CanvasKitDialog(
        onDismissRequest = { onEvent(RecordDetailEvent.OnDismissEdit) }
    ) {
        CanvasKitDialogContent(
            title = {
                Text(
                    text = stringResource(R.string.history_edit_title),
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CanvasKitTextField(
                        label = stringResource(R.string.overview_current_odometer_label),
                        value = state.editingOdometerValue,
                        onValueChange = { onEvent(RecordDetailEvent.OnEditingOdometerChanged(it)) },
                        placeholder = stringResource(R.string.overview_current_odometer_placeholder),
                        suffix = stringResource(R.string.onboarding_km_suffix),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )

                    CanvasKitTextField(
                        label = stringResource(R.string.overview_record_label_label),
                        value = state.editingLabel,
                        onValueChange = { onEvent(RecordDetailEvent.OnEditingLabelChanged(it)) },
                        placeholder = stringResource(R.string.overview_record_label_placeholder),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (state.isPremium) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    )

                    if (state.isPremium) {
                        CanvasKitTextField(
                            label = stringResource(R.string.overview_record_fuel_label),
                            value = state.editingFuel,
                            onValueChange = { onEvent(RecordDetailEvent.OnEditingFuelChanged(it)) },
                            placeholder = stringResource(R.string.overview_record_fuel_placeholder),
                            suffix = "L",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            })
                        )
                    }
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvasKitButton(
                        text = stringResource(R.string.history_close_button),
                        variant = CanvasKitButtonVariant.Secondary,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onEvent(RecordDetailEvent.OnDismissEdit)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CanvasKitButton(
                        text = stringResource(R.string.history_edit_save),
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onEvent(RecordDetailEvent.OnUpdateRecordClicked)
                        },
                        enabled = !state.isEditing && state.editingOdometerValue.isNotBlank(),
                        loading = state.isEditing,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun RecordDetailScreenPreview() {
    CanvasKitTheme {
        RecordDetailScreen(
            state = RecordDetailState(
                isLoading = false,
                isPremium = false,
                consumptionL100km = 5.4,
                record = OdometerRecord(
                    id = "1",
                    contractId = "1",
                    timestamp = System.currentTimeMillis(),
                    odometerValue = 12000.0,
                    isInitialRecord = false,
                    label = "Viaje al trabajo",
                    fuelAmount = 15.0
                )
            ),
            onEvent = {}
        )
    }
}
