package com.example.indoornavigation20.presentation.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Permissions Section
            StatusSection(
                title = "Permissions",
                items = listOf(
                    StatusItem(
                        "Location (Fine)",
                        hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION),
                        "Required for indoor positioning"
                    ),
                    StatusItem(
                        "Location (Coarse)",
                        hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION),
                        "Required for rough positioning"
                    ),
                    StatusItem(
                        "Bluetooth",
                        hasPermission(context, Manifest.permission.BLUETOOTH),
                        "Required for BLE beacon scanning"
                    ),
                    StatusItem(
                        "Bluetooth Admin",
                        hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN),
                        "Required to manage Bluetooth"
                    ),
                    StatusItem(
                        "Bluetooth Scan",
                        hasPermission(context, Manifest.permission.BLUETOOTH_SCAN),
                        "Android 12+ BLE scanning"
                    ),
                    StatusItem(
                        "Bluetooth Connect",
                        hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT),
                        "Android 12+ BLE connections"
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware Section
            StatusSection(
                title = "Hardware",
                items = listOf(
                    StatusItem(
                        "Bluetooth Adapter",
                        isBluetoothAvailable(context),
                        "BLE hardware availability"
                    ),
                    StatusItem(
                        "Bluetooth Enabled",
                        isBluetoothEnabled(context),
                        "Bluetooth is turned on"
                    ),
                    StatusItem(
                        "Location Services",
                        true, // Assume available
                        "GPS/Network location"
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features Section
            StatusSection(
                title = "Features",
                items = listOf(
                    StatusItem(
                        "Floor Plan Display",
                        true,
                        "Interactive floor maps"
                    ),
                    StatusItem(
                        "POI Search",
                        true,
                        "Find points of interest"
                    ),
                    StatusItem(
                        "Mock Positioning",
                        true,
                        "Simulated indoor location"
                    ),
                    StatusItem(
                        "Real BLE Positioning",
                        isBluetoothEnabled(context) && hasPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ),
                        "Actual beacon-based positioning"
                    ),
                    StatusItem(
                        "Navigation Routing",
                        true,
                        "A* pathfinding algorithm"
                    ),
                    StatusItem(
                        "Multi-floor Support",
                        true,
                        "Navigate between floors"
                    )
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "What's Working:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("✅ Complete UI with Material 3 design")
                    Text("✅ Interactive floor plan visualization")
                    Text("✅ Real-time POI search and filtering")
                    Text("✅ Navigation between app screens")
                    Text("✅ Settings and preferences")
                    Text("✅ Mock indoor positioning")
                    Text("✅ Pathfinding and route calculation")
                    Text("✅ Permission handling and requests")

                    if (isBluetoothEnabled(context) && hasPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    ) {
                        Text("✅ Real Bluetooth BLE scanning")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSection(
    title: String,
    items: List<StatusItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            items.forEach { item ->
                StatusItemRow(item)
                if (item != items.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusItemRow(item: StatusItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = if (item.isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (item.isAvailable) "Available" else "Not Available",
            tint = if (item.isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

private data class StatusItem(
    val name: String,
    val isAvailable: Boolean,
    val description: String
)

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun isBluetoothAvailable(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return bluetoothManager?.adapter != null
}

private fun isBluetoothEnabled(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return bluetoothManager?.adapter?.isEnabled == true
}