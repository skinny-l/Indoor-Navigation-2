package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indoornavigation20.domain.model.BeaconMeasurement
import com.example.indoornavigation20.domain.model.WiFiAccessPoint
import com.example.indoornavigation20.positioning.PositioningEngine
import com.example.indoornavigation20.positioning.PositioningMode
import com.example.indoornavigation20.positioning.PositioningStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrilaterationTestScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val positioningEngine = remember { PositioningEngine(context) }

    val currentPosition by positioningEngine.currentPosition.collectAsState()
    val positioningMode by positioningEngine.positioningMode.collectAsState()
    val positioningStatus by positioningEngine.positioningStatusPublic.collectAsState()
    val bleScanResults by (positioningEngine.getBLEScanResults()
        ?: MutableStateFlow(emptyList<BeaconMeasurement>())).collectAsState()
    val wifiScanResults by (positioningEngine.getWiFiScanResults()
        ?: MutableStateFlow<List<WiFiAccessPoint>>(emptyList())).collectAsState()

    // Collect the list of beacons used for positioning
    val beaconsUsedInPositioning by positioningEngine.beaconsUsedInLastPositioning.collectAsState()

    LaunchedEffect(Unit) {
        positioningEngine.startPositioning()
        positioningEngine.setupKnownBeacons() // Ensure your 4 beacons are configured
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Positioning Status") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Positioning Test Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            StatusCard(
                mode = positioningMode,
                status = positioningStatus
            )

            Spacer(modifier = Modifier.height(16.dp))

            PositionCard(currentPosition)

            Spacer(modifier = Modifier.height(16.dp))

            // Display Beacons Used for Positioning
            BeaconsUsedCard(beaconsUsedInPositioning)

            Spacer(modifier = Modifier.height(16.dp))

            BLEScanResultsCard(bleScanResults)

            Spacer(modifier = Modifier.height(16.dp))

            WiFiScanResultsCard(wifiScanResults)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    positioningEngine.stopPositioning()
                    positioningEngine.startPositioning()
                    positioningEngine.setupKnownBeacons()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restart Positioning & Setup Beacons")
            }
        }
    }
}

@Composable
private fun StatusCard(mode: PositioningMode, status: PositioningStatus?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mode: ${mode.name}", fontWeight = FontWeight.Bold)
            Text("Status: ${status?.name ?: "LOADING..."}")
        }
    }
}

@Composable
private fun PositionCard(position: com.example.indoornavigation20.domain.model.Position?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Current Fused Position", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (position != null) {
                Text(
                    "X: ${String.format("%.2f", position.x)}, Y: ${
                        String.format(
                            "%.2f",
                            position.y
                        )
                    }"
                )
                Text("Floor: ${position.floor}")
                Text("Accuracy: ${String.format("%.2f", position.accuracy)}m")
                Text("Timestamp: ${position.timestamp}")
            } else {
                Text("No position data available.")
            }
        }
    }
}

@Composable
private fun BLEScanResultsCard(results: List<BeaconMeasurement>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "BLE Scan Results (${results.size} detected)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text("No BLE devices detected.")
            } else {
                results.take(5).forEach {
                    Text(
                        "Name: ${it.beacon.name ?: "Unknown"}, ID: ${it.beacon.id}, MAC: ${it.beacon.macAddress}, RSSI: ${it.rssi}, Dist: ${
                            String.format(
                                "%.2f",
                                it.distance ?: 0.0
                            )
                        }m",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WiFiScanResultsCard(results: List<WiFiAccessPoint>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "WiFi Scan Results (${results.size} detected)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text("No WiFi APs detected.")
            } else {
                results.take(5).forEach { resultItem: WiFiAccessPoint ->
                    Text(
                        "SSID: ${resultItem.ssid}, BSSID: ${resultItem.bssid}, RSSI: ${resultItem.rssi}, Dist: ${
                            resultItem.distance?.let { String.format("%.2f", it) } ?: "N/A"
                        }m",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BeaconsUsedCard(results: List<BeaconMeasurement>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Beacons Used in Last Positioning (${results.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text("No beacons currently used for positioning.")
            } else {
                results.forEach { // Show all beacons used
                    Text(
                        "Name: ${it.beacon.name ?: "Unknown"}, ID: ${it.beacon.id}, MAC: ${it.beacon.macAddress}, RSSI: ${it.rssi}, Dist: ${
                            String.format(
                                "%.2f",
                                it.distance ?: 0.0
                            )
                        }m",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
