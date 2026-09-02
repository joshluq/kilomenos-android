package es.joshluq.kmsafe.feature.fleet.components

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.fleet.R

/**
 * Content for the Bluetooth Device Picker.
 * Displays a list of paired devices. Validates permission before accessing Bluetooth API.
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

    val pairedDevices = remember(hasPermission) {
        if (hasPermission) {
            getPairedDevices(context)
        } else {
            emptyList()
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
        } else if (pairedDevices.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_bluetooth_empty_list),
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.textSecondary
            )
        } else {
            @SuppressLint("MissingPermission")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pairedDevices) { device ->
                    BluetoothDeviceItem(
                        name = device.name ?: stringResource(R.string.onboarding_bluetooth_unknown_device),
                        address = device.address,
                        onClick = {
                            onDeviceSelected(device.name, device.address)
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
    onClick: () -> Unit
) {
    CanvasKitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = safeClick(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = CanvasKitTheme.colors.brandAccent
            )
            Column {
                Text(
                    text = name,
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = address,
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getPairedDevices(context: Context): List<BluetoothDevice> {
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    return adapter?.bondedDevices?.toList() ?: emptyList()
}
