package com.example.indoornavigation20.positioning

import android.content.Context
import com.example.indoornavigation20.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow

class PositioningEngine(private val context: Context? = null) {
    private val _currentPosition = MutableStateFlow<Position?>(null)
    val currentPosition: StateFlow<Position?> = _currentPosition

    private val _positioningMode = MutableStateFlow<PositioningMode>(PositioningMode.MOCK)
    val positioningMode: StateFlow<PositioningMode> = _positioningMode

    private val _positioningStatus = MutableStateFlow<PositioningStatus>(PositioningStatus.IDLE)
    val positioningStatusPublic: StateFlow<PositioningStatus> =
        _positioningStatus // Renamed for clarity if needed, or use as is

    // Internal MutableStateFlow for beacons used in positioning
    private val _beaconsUsedInLastPositioningInternal =
        MutableStateFlow<List<BeaconMeasurement>>(emptyList())

    // Publicly exposed StateFlow for UI collection
    val beaconsUsedInLastPositioning: StateFlow<List<BeaconMeasurement>> =
        _beaconsUsedInLastPositioningInternal


    // Known beacons with precise positions
    private var hybridBLEService: HybridPositioningService? = null
    private var wifiService: WiFiPositioningService? = null
    private var useRealServices = false

    init {
        context?.let {
            hybridBLEService = HybridPositioningService(it)
            wifiService = WiFiPositioningService(it)
            useRealServices = true

            // Observe BLE positioning and used beacons
            CoroutineScope(Dispatchers.Main).launch {
                hybridBLEService?.currentPosition?.collect { blePosition ->
                    updateFusedPosition(blePosition, wifiService?.wifiPosition?.value)
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                hybridBLEService?.positioningStatus?.collect { status ->
                    _positioningStatus.value = status
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                hybridBLEService?.beaconsUsedInLastPositioning?.collect { beacons ->
                    _beaconsUsedInLastPositioningInternal.value = beacons
                }
            }

            // Observe WiFi positioning
        }
    }

    fun startPositioning() {
        if (useRealServices && hybridBLEService != null) {
            // Always try BLE first - it should work if there are any Bluetooth devices around
            val bleScanStarted = hybridBLEService!!.startScanning()

            if (bleScanStarted) {
                _positioningMode.value = PositioningMode.HYBRID

                // Observe BLE positioning
                CoroutineScope(Dispatchers.Main).launch {
                    hybridBLEService!!.currentPosition.collect { blePosition ->
                        if (blePosition != null) {
                            // We got a real BLE position, use it
                            _currentPosition.value = blePosition
                        } else {
                            // No BLE position yet, but keep scanning - don't fall back to mock immediately
                            // Only show mock if we haven't gotten any real position after some time
                            kotlinx.coroutines.delay(10000) // Wait 10 seconds
                            if (_currentPosition.value == null) {
                                startMockPositioning() // Fallback after waiting
                            }
                        }
                    }
                }

                // Also try WiFi positioning if available
                wifiService?.let { wifi ->
                    val wifiScanStarted = wifi.startWiFiScanning()
                    if (wifiScanStarted) {
                        _positioningMode.value = PositioningMode.HYBRID_WIFI

                        CoroutineScope(Dispatchers.Main).launch {
                            wifi.wifiPosition.collect { wifiPosition ->
                                val blePos = hybridBLEService?.currentPosition?.value
                                updateFusedPosition(blePos, wifiPosition)
                            }
                        }

                        // Regularly trigger WiFi scan for updates
                        CoroutineScope(Dispatchers.Main).launch {
                            while (true) {
                                kotlinx.coroutines.delay(5000) // Scan WiFi every 5 seconds
                                wifi.scanWiFiAccessPoints()
                            }
                        }
                    }
                }
            } else {
                // BLE scanning failed to start, fall back to mock
                startMockPositioning()
            }
        } else {
            startMockPositioning()
        }
    }

    private fun updateFusedPosition(blePos: Position?, wifiPos: Position?) {
        val finalPosition = when {
            blePos != null && wifiPos != null -> {
                // Fuse BLE and WiFi positions (simple averaging for now)
                // More sophisticated fusion (Kalman filter) could be added here
                Position(
                    x = (blePos.x + wifiPos.x) / 2,
                    y = (blePos.y + wifiPos.y) / 2,
                    floor = blePos.floor, // Assume same floor or implement floor fusion
                    accuracy = minOf(blePos.accuracy, wifiPos.accuracy) / 1.5f, // Enhanced accuracy
                    timestamp = System.currentTimeMillis()
                )
            }

            blePos != null -> blePos
            wifiPos != null -> wifiPos
            else -> null
        }

        _currentPosition.value = finalPosition
    }

    private fun startMockPositioning() {
        _positioningMode.value = PositioningMode.MOCK
        _currentPosition.value = Position(x = 250f, y = 300f, floor = 1, accuracy = 2.5f)
    }

    fun stopPositioning() {
        hybridBLEService?.stopScanning()
        // WiFi scanning stops automatically, or manage explicitly if needed
    }

    fun addKnownBeacon(beacon: Beacon) {
        hybridBLEService?.addKnownBeacon(beacon)
    }

    fun addKnownWiFiAccessPoint(ap: WiFiAccessPoint) {
        wifiService?.addKnownAccessPoint(ap)
    }

    fun getPositioningStatus(): PositioningStatus? {
        return hybridBLEService?.positioningStatus?.value
    }

    fun getBLEScanResults(): StateFlow<List<BeaconMeasurement>>? {
        return hybridBLEService?.scanResults
    }

    fun getWiFiScanResults(): StateFlow<List<WiFiAccessPoint>>? {
        return wifiService?.wifiScanResults
    }

    fun isUsingRealServices(): Boolean = useRealServices

    // Expose Positioning Status as a Flow
    fun getPositioningStatusFlow(): StateFlow<PositioningStatus> {
        // Directly return the engine's status flow, which is updated from HybridPositioningService
        return positioningStatusPublic
    }

    // Setup your 4 known beacons and WiFi APs - call this after starting positioning
    fun setupKnownBeacons() {
        // Add your 4 actual beacons with real UUIDs
        val yourBeacons = listOf(
            Beacon(
                id = "beacon_1",
                name = "My Beacon 1",
                uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825", // Actual UUID from your beacons
                major = 1,
                minor = 1,
                macAddress = "AUTO_DETECT_1", // Will be auto-detected when scanning
                position = Position(x = 100f, y = 100f, floor = 1), // Placeholder - will estimate
                txPower = -59,
                pathLossExponent = 2.0
            ),
            Beacon(
                id = "beacon_2",
                name = "My Beacon 2",
                uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825", // Same UUID, different minor
                major = 1,
                minor = 2,
                macAddress = "AUTO_DETECT_2", // Will be auto-detected when scanning
                position = Position(x = 400f, y = 100f, floor = 1), // Placeholder - will estimate
                txPower = -59,
                pathLossExponent = 2.0
            ),
            Beacon(
                id = "beacon_3",
                name = "My Beacon 3",
                uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825", // Same UUID, different minor
                major = 1,
                minor = 3,
                macAddress = "AUTO_DETECT_3", // Will be auto-detected when scanning
                position = Position(x = 100f, y = 400f, floor = 1), // Placeholder - will estimate
                txPower = -59,
                pathLossExponent = 2.0
            ),
            Beacon(
                id = "beacon_4",
                name = "My Beacon 4",
                uuid = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825", // Same UUID, different minor
                major = 1,
                minor = 4,
                macAddress = "AUTO_DETECT_4", // Will be auto-detected when scanning
                position = Position(x = 400f, y = 400f, floor = 1), // Placeholder - will estimate
                txPower = -59,
                pathLossExponent = 2.0
            )
        )

        yourBeacons.forEach { beacon ->
            addKnownBeacon(beacon)
        }

        // Example: Add known WiFi APs (you'll need to survey your building)
        val knownAPs = listOf(
            WiFiAccessPoint(
                bssid = "YOUR_OFFICE_WIFI_BSSID_1",
                ssid = "OfficeNet",
                position = Position(x = 50f, y = 50f, floor = 1),
                rssi = -50,
                frequency = 2412,
                lastSeen = 0L
            ),
            WiFiAccessPoint(
                bssid = "YOUR_OFFICE_WIFI_BSSID_2",
                ssid = "GuestNet",
                position = Position(x = 350f, y = 250f, floor = 1),
                rssi = -60,
                frequency = 5220,
                lastSeen = 0L
            )
        )
        knownAPs.forEach { addKnownWiFiAccessPoint(it) }
    }

    fun isBeaconKnown(beaconMacAddress: String): Boolean {
        return hybridBLEService?.isBeaconKnown(beaconMacAddress) ?: false
    }
}

enum class PositioningMode {
    MOCK,           // Demo positioning with fake data
    HYBRID,         // Real positioning with beacons + public BLE
    BEACONS_ONLY,   // Only known beacons (fallback)
    WIFI_ONLY,      // Only WiFi positioning
    HYBRID_WIFI     // Real positioning with BLE beacons + Public BLE + WiFi APs
}
