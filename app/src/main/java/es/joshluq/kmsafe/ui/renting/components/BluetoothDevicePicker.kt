package es.joshluq.kmsafe.ui.renting.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.sheets.CanvasKitBottomSheet
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R

/**
 * BottomSheet to pick a bonded Bluetooth device.
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDevicePicker(
    onDismiss: () -> Unit,
    onDeviceSelected: (name: String, address: String) -> Unit,
    sheetState: SheetState
) {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = remember { 
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter 
    }
    val bondedDevices = remember {
        bluetoothAdapter?.bondedDevices?.map { it.name to it.address } ?: emptyList()
    }

    CanvasKitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_bluetooth_picker_title),
                style = CanvasKitTheme.typography.headingMedium,
                color = CanvasKitTheme.colors.textPrimary
            )

            if (bondedDevices.isEmpty()) {
                Text(
                    text = "No bonded devices found", // TODO: Move to strings
                    style = CanvasKitTheme.typography.bodyMedium,
                    color = CanvasKitTheme.colors.textSecondary
                )
            } else {
                bondedDevices.forEach { (name, address) ->
                    CanvasKitCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDeviceSelected(name, address) }
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
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
