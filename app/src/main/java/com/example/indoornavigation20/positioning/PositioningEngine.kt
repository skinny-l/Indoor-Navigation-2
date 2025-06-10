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

    private val _positioningMode = MutableStateFlow<PositioningMode>(PositioningMode.INDOOR_ONLY)
    val positioningMode: StateFlow<PositioningMode> = _positioningMode

    private val _positioningStatus = MutableStateFlow<PositioningStatus>(PositioningStatus.IDLE)
    val positioningStatusPublic: StateFlow<PositioningStatus> = _positioningStatus

    // Signal strength indicator for UI
    private val _signalStrength = MutableStateFlow<SignalStrength>(SignalStrength.UNAVAILABLE)
    val signalStrength: StateFlow<SignalStrength> = _signalStrength

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

    // Reference to building detector for integration
    private var buildingDetector: BuildingDetector? = null

    init {
        context?.let {
            hybridBLEService = HybridPositioningService(it)
            wifiService = WiFiPositioningService(it)
            useRealServices = true

            // Observe BLE positioning and used beacons
            CoroutineScope(Dispatchers.Main).launch {
                hybridBLEService?.currentPosition?.collect { blePosition ->
                    updateIndoorPosition(blePosition)
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                hybridBLEService?.positioningStatus?.collect { status ->
                    _positioningStatus.value = status
                    updateSignalStrength(status)
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                hybridBLEService?.beaconsUsedInLastPositioning?.collect { beacons ->
                    _beaconsUsedInLastPositioningInternal.value = beacons
                    updateSignalStrengthFromBeacons(beacons)
                }
            }
        }
    }

    fun startPositioning() {
        if (useRealServices && hybridBLEService != null) {
            _positioningMode.value = PositioningMode.INDOOR_ONLY

            val bleScanStarted = hybridBLEService!!.startScanning()

            if (bleScanStarted) {
                CoroutineScope(Dispatchers.Main).launch {
                    hybridBLEService!!.currentPosition.collect { blePosition ->
                        updateIndoorPosition(blePosition)
                    }
                }
            } else {
                _positioningStatus.value = PositioningStatus.ERROR
                _signalStrength.value = SignalStrength.UNAVAILABLE
            }
        } else {
            _positioningStatus.value = PositioningStatus.ERROR
            _signalStrength.value = SignalStrength.UNAVAILABLE
        }
    }

    private fun updateIndoorPosition(blePosition: Position?) {
        val status =
            _positioningStatus.value // Use the engine's status, which is updated by HybridPositioningService
        // Count how many of the beacons used in the last calculation were actual "known" beacons
        val knownBeaconsUsedCount = _beaconsUsedInLastPositioningInternal.value.count {
            hybridBLEService?.isBeaconKnown(it.beacon.macAddress) == true
        }

        // A position is only valid if it's from a POSITIONED state, used at least one known beacon, and meets accuracy criteria
        if (blePosition != null &&
            status == PositioningStatus.POSITIONED &&
            knownBeaconsUsedCount > 0 && // THIS IS THE NEW CRITICAL CHECK
            isValidIndoorPosition(blePosition) &&
            (buildingDetector?.isInsideBuilding?.value != false)
        ) {

            _currentPosition.value = blePosition
            println("✅ Valid indoor position accepted: (${blePosition.x}, ${blePosition.y}) using $knownBeaconsUsedCount known beacon(s)")
        } else {
            if (_currentPosition.value != null) { // Log only when it becomes null
                println("❌ Indoor position rejected or lost. Status: $status, Known Beacons Used: $knownBeaconsUsedCount, BLE Pos: $blePosition, Accuracy: ${blePosition?.accuracy}")
            }
            _currentPosition.value = null // Set to null if conditions aren't met
        }
    }

    private fun isValidIndoorPosition(position: Position): Boolean {
        // Only show position if we have high confidence (accuracy <= 3m and good beacon count)
        // This ensures we're likely inside the building with good beacon coverage
        val knownBeaconsUsedCount = _beaconsUsedInLastPositioningInternal.value.count {
            hybridBLEService?.isBeaconKnown(it.beacon.macAddress) == true
        }

        // Stricter requirements: need good accuracy AND multiple known beacons
        return position.accuracy <= 3.0f && knownBeaconsUsedCount >= 2
    }

    private fun updateSignalStrength(status: PositioningStatus) {
        _signalStrength.value = when (status) {
            PositioningStatus.POSITIONED -> {
                val currentPos = _currentPosition.value
                when {
                    currentPos?.accuracy != null && currentPos.accuracy <= 2.0f -> SignalStrength.EXCELLENT
                    currentPos?.accuracy != null && currentPos.accuracy <= 4.0f -> SignalStrength.GOOD
                    currentPos?.accuracy != null && currentPos.accuracy <= 6.0f -> SignalStrength.FAIR
                    else -> SignalStrength.POOR
                }
            }
            PositioningStatus.SCANNING -> SignalStrength.SEARCHING
            PositioningStatus.INSUFFICIENT_SIGNALS -> SignalStrength.POOR
            PositioningStatus.ERROR -> SignalStrength.UNAVAILABLE
            PositioningStatus.IDLE -> SignalStrength.UNAVAILABLE
        }
    }

    private fun updateSignalStrengthFromBeacons(beacons: List<BeaconMeasurement>) {
        if (beacons.isEmpty()) {
            _signalStrength.value = SignalStrength.UNAVAILABLE
            return
        }

        val knownBeaconsCount =
            beacons.count { hybridBLEService?.isBeaconKnown(it.beacon.macAddress) == true }
        val averageRssi = beacons.map { it.rssi }.average()

        _signalStrength.value = when {
            knownBeaconsCount >= 4 && averageRssi >= -50 -> SignalStrength.EXCELLENT
            knownBeaconsCount >= 3 && averageRssi >= -60 -> SignalStrength.GOOD
            knownBeaconsCount >= 2 && averageRssi >= -70 -> SignalStrength.FAIR
            knownBeaconsCount >= 1 -> SignalStrength.POOR
            else -> SignalStrength.UNAVAILABLE
        }
    }

    fun stopPositioning() {
        hybridBLEService?.stopScanning()
        _currentPosition.value = null
        _signalStrength.value = SignalStrength.UNAVAILABLE
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

    /**
     * Set building detector for position validation
     */
    fun setBuildingDetector(detector: BuildingDetector) {
        buildingDetector = detector
        println("🔗 PositioningEngine: Building detector connected")
    }
}

enum class PositioningMode {
    INDOOR_ONLY,    // Real positioning inside building only
    HYBRID,         // Real positioning with beacons + public BLE
    BEACONS_ONLY,   // Only known beacons (fallback)
    WIFI_ONLY,      // Only WiFi positioning
    HYBRID_WIFI     // Real positioning with BLE beacons + Public BLE + WiFi APs
}

enum class SignalStrength {
    EXCELLENT,      // 4+ beacons, very strong signal
    GOOD,           // 3+ beacons, good signal
    FAIR,           // 2+ beacons, fair signal
    POOR,           // 1 beacon or weak signal
    SEARCHING,      // Scanning for beacons
    UNAVAILABLE     // No beacons detected or outside building
}
