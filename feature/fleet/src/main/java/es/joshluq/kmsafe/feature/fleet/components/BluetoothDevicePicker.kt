package es.joshluq.kmsafe.feature.fleet.components

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.BluetoothUtils
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.fleet.R

/**
 * UI representation of a Bluetooth device with its current connection state.
 */
data class BluetoothDeviceItemUiModel(
    val device: BluetoothDevice,
    val isConnected: Boolean = false
)

/**
 * Content for the Bluetooth Device Picker.
 * Displays a list of paired devices, highlighting currently connected devices at the top.
 */
@Composable
fun BluetoothDevicePicker(
    onDeviceSelected: (name: String?, address: String) -> Unit
) {
    val context = LocalContext.current

    // Safety check: Don't call Bluetooth APIs without permission check
    val hasPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val devicesWithState by produceState(
        initialValue = emptyList<BluetoothDeviceItemUiModel>(),
        key1 = hasPermission
    ) {
        if (hasPermission) {
            val bonded = BluetoothUtils.getPairedDevices(context)
            // Immediately show paired devices
            value = bonded.map { BluetoothDeviceItemUiModel(it, isConnected = false) }

            // Query active audio connections asynchronously and prioritize connected devices
            val connectedAddresses = BluetoothUtils.getConnectedAudioDeviceAddresses(context)
            if (connectedAddresses.isNotEmpty()) {
                value = bonded.map { device ->
                    val isConnected = connectedAddresses.contains(
                        BluetoothUtils.normalizeAddress(device.address)
                    )
                    BluetoothDeviceItemUiModel(device, isConnected)
                }.sortedWith(
                    compareByDescending<BluetoothDeviceItemUiModel> { it.isConnected }
                        .thenBy { it.device.name ?: "" }
                )
            }
        } else {
            value = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_bluetooth_picker_title),
            style = CanvasKitTheme.typography.headingMedium,
            color = CanvasKitTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasPermission) {
            Text(
                text = stringResource(R.string.onboarding_bluetooth_permission_denied),
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.error
            )
        } else if (devicesWithState.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_bluetooth_empty_list),
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.textSecondary
            )
        } else {
            @SuppressLint("MissingPermission")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devicesWithState, key = { it.device.address }) { item ->
                    BluetoothDeviceItem(
                        name = item.device.name ?: stringResource(R.string.onboarding_bluetooth_unknown_device),
                        address = item.device.address,
                        isConnected = item.isConnected,
                        onClick = {
                            onDeviceSelected(item.device.name, item.device.address)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothDeviceItem(
    name: String,
    address: String,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = safeClick(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) CanvasKitTheme.colors.brandAccent else CanvasKitTheme.colors.textSecondary
                )
                Column {
                    Text(
                        text = name,
                        style = CanvasKitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CanvasKitTheme.colors.textPrimary
                    )
                    Text(
                        text = address,
                        style = CanvasKitTheme.typography.labelSmall,
                        color = CanvasKitTheme.colors.textSecondary
                    )
                }
            }

            if (isConnected) {
                Surface(
                    color = CanvasKitTheme.colors.brandAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CanvasKitTheme.colors.brandAccent)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_bluetooth_connected_tag),
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.brandAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
