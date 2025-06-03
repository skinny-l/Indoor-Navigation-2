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

class BluetoothPositioningService(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val _scanResults = MutableStateFlow<List<BeaconMeasurement>>(emptyList())
    val scanResults: StateFlow<List<BeaconMeasurement>> = _scanResults

    private val _currentPosition = MutableStateFlow<Position?>(null)
    val currentPosition: StateFlow<Position?> = _currentPosition

    private val knownBeacons = mutableMapOf<String, Beacon>()
    private val recentMeasurements = mutableListOf<BeaconMeasurement>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            // Handle scan failure
        }
    }

    fun addKnownBeacon(beacon: Beacon) {
        knownBeacons[beacon.macAddress] = beacon
    }

    fun startScanning(): Boolean {
        if (!isBluetoothEnabled()) {
            return false
        }

        if (!hasRequiredPermissions()) {
            return false
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            bluetoothLeScanner?.startScan(null, settings, scanCallback)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun stopScanning() {
        if (hasRequiredPermissions()) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: SecurityException) {
                // Handle permission error
            }
        }
    }

    private fun processScanResult(scanResult: ScanResult) {
        val macAddress = scanResult.device.address
        val rssi = scanResult.rssi
        val deviceName = try {
            if (hasRequiredPermissions()) {
                scanResult.device.name
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }

        // Check if this is a known beacon
        val beacon = knownBeacons[macAddress] ?: createUnknownBeacon(macAddress, deviceName)

        val distance = calculateDistance(rssi, beacon.txPower, beacon.pathLossExponent)

        val measurement = BeaconMeasurement(
            beacon = beacon,
            rssi = rssi,
            timestamp = System.currentTimeMillis(),
            distance = distance
        )

        // Add to recent measurements
        recentMeasurements.removeAll { it.beacon.macAddress == macAddress }
        recentMeasurements.add(measurement)

        // Keep only recent measurements (last 10 seconds)
        val currentTime = System.currentTimeMillis()
        recentMeasurements.removeAll { currentTime - it.timestamp > 10000 }

        // Update scan results
        _scanResults.value = recentMeasurements.toList()

        // Try to calculate position if we have enough measurements
        if (recentMeasurements.size >= 3) {
            calculatePosition()
        }
    }

    private fun createUnknownBeacon(macAddress: String, name: String?): Beacon {
        // Create a placeholder beacon for unknown devices
        return Beacon(
            id = "unknown_$macAddress",
            name = name,
            uuid = "00000000-0000-0000-0000-000000000000",
            major = 0,
            minor = 0,
            macAddress = macAddress,
            position = Position(x = 0f, y = 0f, floor = 1) // Unknown position
        )
    }

    private fun calculateDistance(rssi: Int, txPower: Int, pathLossExponent: Double): Double {
        if (rssi == 0) return -1.0

        val ratio = (txPower - rssi) / (10.0 * pathLossExponent)
        return 10.0.pow(ratio)
    }

    private fun calculatePosition() {
        val validMeasurements = recentMeasurements.filter {
            it.beacon.position.x != 0f || it.beacon.position.y != 0f
        }

        if (validMeasurements.size < 3) return

        try {
            // Simple centroid calculation with distance weighting
            var weightedX = 0.0
            var weightedY = 0.0
            var totalWeight = 0.0

            validMeasurements.forEach { measurement ->
                val distance = measurement.distance ?: 1.0
                val weight = 1.0 / (distance + 0.1) // Avoid division by zero

                weightedX += measurement.beacon.position.x * weight
                weightedY += measurement.beacon.position.y * weight
                totalWeight += weight
            }

            if (totalWeight > 0) {
                val position = Position(
                    x = (weightedX / totalWeight).toFloat(),
                    y = (weightedY / totalWeight).toFloat(),
                    floor = validMeasurements.first().beacon.position.floor,
                    accuracy = calculateAccuracy(validMeasurements)
                )

                _currentPosition.value = position
            }
        } catch (e: Exception) {
            // Handle calculation errors
        }
    }

    private fun calculateAccuracy(measurements: List<BeaconMeasurement>): Float {
        if (measurements.isEmpty()) return 100f

        val rssiVariance = measurements.map { it.rssi }.let { rssiList ->
            val mean = rssiList.average()
            rssiList.map { (it - mean) * (it - mean) }.average()
        }

        return (rssiVariance / measurements.size).toFloat().coerceIn(0.5f, 50f)
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
