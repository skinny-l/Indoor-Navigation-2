package com.example.indoornavigation20.positioning

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.indoornavigation20.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow
import kotlin.math.sqrt

class HybridPositioningService(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val _scanResults = MutableStateFlow<List<BeaconMeasurement>>(emptyList())
    val scanResults: StateFlow<List<BeaconMeasurement>> = _scanResults

    private val _currentPosition = MutableStateFlow<Position?>(null)
    val currentPosition: StateFlow<Position?> = _currentPosition

    private val _positioningStatus = MutableStateFlow<PositioningStatus>(PositioningStatus.IDLE)
    val positioningStatus: StateFlow<PositioningStatus> = _positioningStatus

    private val _beaconsUsedInLastPositioning =
        MutableStateFlow<List<BeaconMeasurement>>(emptyList())
    val beaconsUsedInLastPositioning: StateFlow<List<BeaconMeasurement>> =
        _beaconsUsedInLastPositioning

    // Known beacons with precise positions
    private val knownBeacons = mutableMapOf<String, Beacon>()

    // Public BLE devices with estimated/learned positions
    private val publicDevices = mutableMapOf<String, PublicBLEDevice>()

    // Recent measurements for positioning
    private val recentMeasurements = mutableListOf<BeaconMeasurement>()

    // Device stability tracking for public devices
    private val deviceStabilityTracker = mutableMapOf<String, DeviceStability>()

    fun isBeaconKnown(beaconMacAddress: String): Boolean {
        return knownBeacons.containsKey(beaconMacAddress)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _positioningStatus.value = PositioningStatus.ERROR
        }
    }

    fun addKnownBeacon(beacon: Beacon) {
        knownBeacons[beacon.macAddress] = beacon
        println("📝 Added known beacon: ${beacon.name} with UUID: ${beacon.uuid} and MAC: ${beacon.macAddress}")
    }

    fun startScanning(): Boolean {
        if (!isBluetoothEnabled() || !hasRequiredPermissions()) {
            return false
        }

        _positioningStatus.value = PositioningStatus.SCANNING

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            bluetoothLeScanner?.startScan(null, settings, scanCallback)
            return true
        } catch (e: SecurityException) {
            _positioningStatus.value = PositioningStatus.ERROR
            return false
        }
    }

    fun stopScanning() {
        if (hasRequiredPermissions()) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
                _positioningStatus.value = PositioningStatus.IDLE
            } catch (e: SecurityException) {
                // Handle permission error
            }
        }
    }

    private fun processScanResult(scanResult: ScanResult) {
        val macAddress = scanResult.device.address
        val rssi = scanResult.rssi
        val deviceName = getDeviceName(scanResult)

        // Extract UUID from scan result if available
        val detectedUUID = extractUUIDFromScanResult(scanResult)

        // Debug: Log detected UUIDs to help troubleshoot beacon detection
        if (detectedUUID != null) {
            println("🔍 Detected UUID: $detectedUUID for device: ${deviceName ?: "Unknown"} (MAC: $macAddress, RSSI: $rssi)")
        }

        // Process the device (known beacon or public BLE device)
        val beacon = when {
            // Check if this is one of our known beacons by UUID
            isKnownBeaconByUUID(detectedUUID) -> {
                updateKnownBeaconMacAddress(
                    detectedUUID,
                    macAddress
                ) // This will update the map, and we'll fetch by newMacAddress next
                knownBeacons[macAddress] // Fetch by the (potentially new) MAC address
            }
            // Check if this is one of our known beacons by MAC address
            knownBeacons.containsKey(macAddress) -> {
                println("✅ Found known beacon by MAC: $macAddress")
                knownBeacons[macAddress]
            }
            // Check if this is a stable public device
            isStablePublicDevice(macAddress, rssi) -> {
                getOrCreatePublicDevice(macAddress, deviceName, rssi)
            }
            else -> {
                println("⚠️ No matching beacon found for MAC: $macAddress, creating as public.")
                // If it's not known and not stable yet, but we have a UUID, let's still try to use it as a public device for now
                // This helps in scenarios where beacon positions might be learned over time or are less stable initially
                getOrCreatePublicDevice(macAddress, deviceName, rssi)
            }
        }

        // If beacon is still null here, it means it's an unidentifiable device we can't use.
        if (beacon == null) {
            println("🚫 Beacon could not be identified or created for MAC: $macAddress")
            return
        }

        val distance = calculateDistance(rssi, beacon.txPower, beacon.pathLossExponent)

        val measurement = BeaconMeasurement(
            beacon = beacon,
            rssi = rssi,
            timestamp = System.currentTimeMillis(),
            distance = distance
        )

        // Update measurements
        updateMeasurements(measurement)

        // Try positioning if we have enough stable references
        println("🔍 Checking positioning: ${recentMeasurements.size} measurements (need 3+)")
        if (recentMeasurements.size >= 3) {
            println("🚀 Triggering positioning calculation...")
            calculateHybridPosition()
        } else {
            println("⏳ Not enough measurements for positioning yet")
        }
    }

    private fun extractUUIDFromScanResult(scanResult: ScanResult): String? {
        return try {
            val scanRecord = scanResult.scanRecord
            scanRecord?.let { record ->
                // Look for iBeacon format in manufacturer data
                val manufacturerData = record.manufacturerSpecificData
                println("🔬 Extracting UUID from ${manufacturerData.size()} manufacturer data entries")

                for (i in 0 until manufacturerData.size()) {
                    val companyId = manufacturerData.keyAt(i)
                    val data = manufacturerData.valueAt(i)
                    println("🔬 Company ID: $companyId, Data size: ${data.size}")

                    if (data.size >= 23) { // iBeacon format
                        // Extract UUID from bytes 2-17 (16 bytes)
                        val uuidBytes = data.sliceArray(2..17)
                        val formattedUUID = formatUUIDFromBytes(uuidBytes)
                        println("🔬 Raw UUID bytes: ${uuidBytes.joinToString(" ") { "%02X".format(it) }}")
                        println("🔬 Formatted UUID: $formattedUUID")
                        return formattedUUID
                    }
                }

                // Look for UUID in service UUIDs
                val serviceUuid = record.serviceUuids?.firstOrNull()?.toString()?.uppercase()
                if (serviceUuid != null) {
                    println("🔬 Found service UUID: $serviceUuid")
                    return serviceUuid
                }

                println("🔬 No UUID found in scan record")
                null
            }
        } catch (e: Exception) {
            println("🔬 Error extracting UUID: ${e.message}")
            null
        }
    }

    private fun formatUUIDFromBytes(bytes: ByteArray): String {
        if (bytes.size != 16) return ""

        return bytes.joinToString("") { "%02X".format(it) }.let { raw ->
            // Format as standard UUID: 8-4-4-4-12
            "${raw.substring(0, 8)}-${raw.substring(8, 12)}-${
                raw.substring(
                    12,
                    16
                )
            }-${raw.substring(16, 20)}-${raw.substring(20, 32)}"
        }
    }

    private fun isKnownBeaconByUUID(uuid: String?): Boolean {
        if (uuid == null) return false

        println("🔍 Checking if UUID '$uuid' matches any known beacon:")
        knownBeacons.values.forEach { beacon ->
            println("  - Comparing with: ${beacon.name} UUID: '${beacon.uuid}'")
            if (beacon.uuid.equals(uuid, ignoreCase = true)) {
                println("  ✅ MATCH FOUND!")
                return true
            }
        }
        println("  ❌ No match found")
        return false
    }

    private fun getKnownBeaconByUUID(uuid: String?): Beacon? {
        if (uuid == null) return null
        return knownBeacons.values.find { it.uuid.equals(uuid, ignoreCase = true) }
    }

    private fun updateKnownBeaconMacAddress(uuid: String?, newMacAddress: String): Beacon? {
        if (uuid == null) return null

        val entry = knownBeacons.entries.find {
            it.value.uuid.equals(uuid, ignoreCase = true) &&
                    it.value.macAddress.startsWith("AUTO_DETECT")
        }

        if (entry != null) {
            val oldMacAddress = entry.key
            val beaconToUpdate = entry.value

            knownBeacons.remove(oldMacAddress)
            val updatedBeacon = beaconToUpdate.copy(macAddress = newMacAddress)
            knownBeacons[newMacAddress] = updatedBeacon
            println("✅ Updated known beacon: ${updatedBeacon.name} from MAC $oldMacAddress to $newMacAddress")
            return updatedBeacon
        }
        return null
    }

    private fun isStablePublicDevice(macAddress: String, rssi: Int): Boolean {
        val stability = deviceStabilityTracker.getOrPut(macAddress) {
            DeviceStability(macAddress, mutableListOf(), System.currentTimeMillis())
        }

        stability.rssiReadings.add(rssi)
        stability.lastSeen = System.currentTimeMillis()

        // Keep only recent readings (last 30 seconds)
        val cutoffTime = System.currentTimeMillis() - 30000
        stability.rssiReadings.removeAll { reading ->
            stability.rssiReadings.indexOf(reading) < stability.rssiReadings.size - 10
        }

        // More lenient stability requirements for faster positioning
        // Device is stable if:
        // 1. We have at least 3 readings (reduced from 5)
        // 2. RSSI variance is reasonable (increased threshold)
        // 3. Device has been seen consistently
        if (stability.rssiReadings.size >= 3) {
            val variance = calculateRSSIVariance(stability.rssiReadings)
            // Increased variance threshold to accept more devices
            return variance < 150 || stability.rssiReadings.size >= 6 // Accept if very stable OR have many readings
        }

        return false
    }

    private fun getOrCreatePublicDevice(
        macAddress: String,
        deviceName: String?,
        rssi: Int
    ): Beacon {
        return publicDevices.getOrPut(macAddress) {
            PublicBLEDevice(
                macAddress = macAddress,
                deviceName = deviceName,
                estimatedPosition = estimateDevicePosition(macAddress, rssi),
                confidence = 0.3f, // Lower confidence than known beacons
                lastSeen = System.currentTimeMillis()
            )
        }.toBeacon()
    }

    private fun estimateDevicePosition(macAddress: String, rssi: Int): Position {
        // Strategy 1: If we have some known beacon positions, estimate relative to them
        if (knownBeacons.isNotEmpty() && recentMeasurements.isNotEmpty()) {
            return estimateRelativePosition(macAddress, rssi)
        }

        // Strategy 2: Use hash-based consistent positioning
        return generateConsistentPosition(macAddress)
    }

    private fun estimateRelativePosition(macAddress: String, rssi: Int): Position {
        // Find the strongest known beacon signal
        val knownMeasurements = recentMeasurements.filter {
            knownBeacons.containsKey(it.beacon.macAddress)
        }

        if (knownMeasurements.isNotEmpty()) {
            val strongestKnown = knownMeasurements.maxByOrNull { it.rssi }!!
            val distance = calculateDistance(rssi, -59, 2.0) // Default values

            // Estimate position relative to strongest known beacon
            val angle = macAddress.hashCode() % 360 * (Math.PI / 180) // Consistent angle
            val basePos = strongestKnown.beacon.position

            return Position(
                x = basePos.x + (distance * kotlin.math.cos(angle)).toFloat(),
                y = basePos.y + (distance * kotlin.math.sin(angle)).toFloat(),
                floor = basePos.floor
            )
        }

        return generateConsistentPosition(macAddress)
    }

    private fun generateConsistentPosition(macAddress: String): Position {
        // Generate consistent position based on MAC address hash
        val hash = macAddress.hashCode()
        val x = ((hash and 0xFF) * 4).toFloat() // 0-1020 range
        val y = (((hash shr 8) and 0xFF) * 3).toFloat() // 0-765 range
        val floor = 1 // Default floor

        return Position(x = x, y = y, floor = floor)
    }

    private fun updateMeasurements(measurement: BeaconMeasurement) {
        // Remove old measurement from same device
        recentMeasurements.removeAll { it.beacon.macAddress == measurement.beacon.macAddress }
        recentMeasurements.add(measurement)

        // Keep only recent measurements (last 15 seconds)
        val currentTime = System.currentTimeMillis()
        recentMeasurements.removeAll { currentTime - it.timestamp > 15000 }

        println(
            "🔢 Updated measurements: ${recentMeasurements.size} total (${
                recentMeasurements.count {
                    knownBeacons.containsKey(
                        it.beacon.macAddress
                    )
                }
            } known)"
        )
        recentMeasurements.forEach { m ->
            val type = if (knownBeacons.containsKey(m.beacon.macAddress)) "KNOWN" else "PUBLIC"
            println("  - $type: ${m.beacon.name ?: m.beacon.id} | RSSI: ${m.rssi}")
        }

        // Update scan results
        _scanResults.value = recentMeasurements.toList()
    }

    private fun calculateHybridPosition() {
        val validMeasurements = recentMeasurements.filter {
            it.distance != null && it.distance > 0.1
        }

        if (validMeasurements.size < 3) {
            _positioningStatus.value = PositioningStatus.INSUFFICIENT_SIGNALS
            return
        }

        try {
            // Debug: Show which beacons are being used for positioning
            val knownUsed =
                validMeasurements.count { knownBeacons.containsKey(it.beacon.macAddress) }
            val publicUsed = validMeasurements.size - knownUsed
            println("📍 Positioning with $knownUsed known beacons + $publicUsed public devices:")
            validMeasurements.forEach { measurement ->
                val type =
                    if (knownBeacons.containsKey(measurement.beacon.macAddress)) "KNOWN" else "PUBLIC"
                val confidence = getDeviceConfidence(measurement.beacon.macAddress)
                println(
                    "  - $type: ${measurement.beacon.name ?: measurement.beacon.id} | RSSI: ${measurement.rssi} | Dist: ${
                        String.format(
                            "%.1f",
                            measurement.distance
                        )
                    }m | Conf: ${String.format("%.1f", confidence)}"
                )
            }

            // Weighted positioning based on device confidence
            var weightedX = 0.0
            var weightedY = 0.0
            var totalWeight = 0.0

            validMeasurements.forEach { measurement ->
                val distance = measurement.distance!!
                val confidence = getDeviceConfidence(measurement.beacon.macAddress)
                val weight = confidence / (distance + 0.1) // Distance and confidence weighting

                weightedX += measurement.beacon.position.x * weight
                weightedY += measurement.beacon.position.y * weight
                totalWeight += weight
            }

            if (totalWeight > 0) {
                val accuracy = calculateHybridAccuracy(validMeasurements)
                val position = Position(
                    x = (weightedX / totalWeight).toFloat(),
                    y = (weightedY / totalWeight).toFloat(),
                    floor = validMeasurements.first().beacon.position.floor,
                    accuracy = accuracy
                )

                println(
                    "📍 Calculated position: (${
                        String.format(
                            "%.1f",
                            position.x
                        )
                    }, ${String.format("%.1f", position.y)}) with accuracy: ${
                        String.format(
                            "%.1f",
                            accuracy
                        )
                    }m"
                )

                _currentPosition.value = position
                _positioningStatus.value = PositioningStatus.POSITIONED
                _beaconsUsedInLastPositioning.value = validMeasurements // Update beacons used
            }
        } catch (e: Exception) {
            println("❌ Positioning calculation error: ${e.message}")
            _positioningStatus.value = PositioningStatus.ERROR
        }
    }

    private fun getDeviceConfidence(macAddress: String): Double {
        return when {
            knownBeacons.containsKey(macAddress) -> 1.0 // High confidence for known beacons
            publicDevices.containsKey(macAddress) -> publicDevices[macAddress]!!.confidence.toDouble()
            else -> 0.1 // Very low confidence for unknown devices
        }
    }

    private fun calculateHybridAccuracy(measurements: List<BeaconMeasurement>): Float {
        val knownCount = measurements.count { knownBeacons.containsKey(it.beacon.macAddress) }
        val publicCount = measurements.size - knownCount
        val knownMeasurements =
            measurements.filter { knownBeacons.containsKey(it.beacon.macAddress) }
        val publicMeasurements =
            measurements.filter { !knownBeacons.containsKey(it.beacon.macAddress) }

        // If we have multiple known beacons, calculate accuracy based on signal strength and geometry
        if (knownCount >= 3) {
            // Calculate average distance to known beacons
            val avgDistance = knownMeasurements.mapNotNull { it.distance }.average()

            // Calculate signal strength quality
            val avgRssi = knownMeasurements.map { it.rssi }.average()
            val rssiVariance = knownMeasurements.map { it.rssi }.let { rssiList ->
                val mean = rssiList.average()
                rssiList.map { (it - mean) * (it - mean) }.average()
            }

            // Better accuracy calculation based on proximity and signal quality
            val baseAccuracy = when {
                avgDistance <= 2.0 && avgRssi >= -50 -> 0.5f // Very close to beacons
                avgDistance <= 5.0 && avgRssi >= -60 -> 1.0f // Close to beacons  
                avgDistance <= 10.0 && avgRssi >= -70 -> 1.5f // Moderate distance
                knownCount >= 4 -> 2.0f // 4+ known beacons, good geometry
                knownCount >= 3 -> 2.5f // 3 known beacons
                else -> 3.0f
            }

            // Adjust for signal variance (stable signals = better accuracy)
            val variancePenalty = (rssiVariance / 50.0).toFloat().coerceAtMost(1.0f)

            return (baseAccuracy + variancePenalty).coerceAtLeast(0.3f)
        }

        // Dynamic accuracy calculation for public devices and mixed scenarios
        val allMeasurements = measurements
        val avgRssi = allMeasurements.map { it.rssi }.average()
        val avgDistance = allMeasurements.mapNotNull { it.distance }.average()
        val deviceCount = allMeasurements.size

        // Calculate signal variance for stability assessment
        val rssiVariance = allMeasurements.map { it.rssi }.let { rssiList ->
            val mean = rssiList.average()
            rssiList.map { (it - mean) * (it - mean) }.average()
        }

        // Base accuracy calculation based on signal quality and device count
        val baseAccuracy = when {
            // Strong signals, close devices
            avgRssi >= -45 && avgDistance <= 3.0 -> 1.5f
            avgRssi >= -55 && avgDistance <= 5.0 -> 2.5f
            avgRssi >= -65 && avgDistance <= 8.0 -> 3.5f
            avgRssi >= -75 && avgDistance <= 12.0 -> 5.0f

            // More devices = better accuracy (triangulation)
            deviceCount >= 6 -> 3.0f
            deviceCount >= 5 -> 4.0f
            deviceCount >= 4 -> 5.0f
            deviceCount >= 3 -> 6.0f

            else -> 8.0f // Fallback for poor conditions
        }

        // Improve accuracy with more devices and stable signals
        val deviceCountBonus = when {
            deviceCount >= 6 -> -1.5f
            deviceCount >= 5 -> -1.0f
            deviceCount >= 4 -> -0.5f
            else -> 0f
        }

        // Penalty for unstable signals
        val stabilityPenalty = when {
            rssiVariance > 200 -> 2.0f // Very unstable
            rssiVariance > 100 -> 1.0f // Somewhat unstable
            rssiVariance > 50 -> 0.5f  // Slightly unstable
            else -> 0f                 // Stable
        }

        // Mix of known and public devices gets better accuracy
        val mixBonus = if (knownCount > 0 && publicCount > 0) -1.0f else 0f

        val finalAccuracy = (baseAccuracy + deviceCountBonus + stabilityPenalty + mixBonus)
            .coerceIn(0.8f, 12.0f) // Minimum 0.8m, maximum 12m

        println("🎯 Accuracy calculation: base=$baseAccuracy, devices=$deviceCount (bonus=$deviceCountBonus), variance=$rssiVariance (penalty=$stabilityPenalty), mix=$mixBonus, final=$finalAccuracy")

        return finalAccuracy
    }

    // Helper functions
    private fun getDeviceName(scanResult: ScanResult): String? {
        return try {
            if (hasRequiredPermissions()) {
                scanResult.device.name
            } else null
        } catch (e: SecurityException) {
            null
        }
    }

    private fun calculateDistance(rssi: Int, txPower: Int, pathLossExponent: Double): Double {
        if (rssi == 0) return -1.0
        val ratio = (txPower - rssi) / (10.0 * pathLossExponent)
        return 10.0.pow(ratio)
    }

    private fun calculateRSSIVariance(readings: List<Int>): Double {
        if (readings.size < 2) return 0.0
        val mean = readings.average()
        return readings.map { (it - mean) * (it - mean) }.average()
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

// Data classes for hybrid positioning
data class PublicBLEDevice(
    val macAddress: String,
    val deviceName: String?,
    val estimatedPosition: Position,
    val confidence: Float, // 0.0 to 1.0
    val lastSeen: Long
) {
    fun toBeacon(): Beacon {
        return Beacon(
            id = "public_$macAddress",
            name = deviceName,
            uuid = "00000000-0000-0000-0000-000000000000",
            major = 0,
            minor = 0,
            macAddress = macAddress,
            position = estimatedPosition,
            txPower = -59, // Estimated
            pathLossExponent = 2.0
        )
    }
}

data class DeviceStability(
    val macAddress: String,
    val rssiReadings: MutableList<Int>,
    var lastSeen: Long
)

enum class PositioningStatus {
    IDLE,
    SCANNING,
    POSITIONED,
    INSUFFICIENT_SIGNALS,
    ERROR
}

