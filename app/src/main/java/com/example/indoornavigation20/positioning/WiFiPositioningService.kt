package com.example.indoornavigation20.positioning

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import com.example.indoornavigation20.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

class WiFiPositioningService(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _wifiScanResults = MutableStateFlow<List<WiFiAccessPoint>>(emptyList())
    val wifiScanResults: StateFlow<List<WiFiAccessPoint>> = _wifiScanResults

    private val _wifiPosition = MutableStateFlow<Position?>(null)
    val wifiPosition: StateFlow<Position?> = _wifiPosition

    // Known WiFi access points with measured positions
    private val knownAccessPoints = mutableMapOf<String, WiFiAccessPoint>()

    // Discovered public WiFi access points
    private val discoveredAPs = mutableMapOf<String, WiFiAccessPoint>()

    fun addKnownAccessPoint(accessPoint: WiFiAccessPoint) {
        knownAccessPoints[accessPoint.bssid] = accessPoint
    }

    fun startWiFiScanning(): Boolean {
        if (!hasWiFiPermissions() || !wifiManager.isWifiEnabled) {
            return false
        }

        return try {
            wifiManager.startScan()
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun scanWiFiAccessPoints(): List<WiFiAccessPoint> {
        if (!hasWiFiPermissions() || !wifiManager.isWifiEnabled) {
            return emptyList()
        }

        return try {
            val scanResults = wifiManager.scanResults
            val wifiAPs = scanResults.mapNotNull { result ->
                processWiFiScanResult(result)
            }

            _wifiScanResults.value = wifiAPs

            // Calculate position if we have enough known APs
            if (wifiAPs.count { knownAccessPoints.containsKey(it.bssid) } >= 3) {
                calculateWiFiPosition(wifiAPs)
            }

            wifiAPs
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun processWiFiScanResult(scanResult: ScanResult): WiFiAccessPoint? {
        val bssid = scanResult.BSSID
        val ssid = scanResult.SSID
        val rssi = scanResult.level
        val frequency = scanResult.frequency

        // Check if this is a known access point
        val knownAP = knownAccessPoints[bssid]
        if (knownAP != null) {
            return knownAP.copy(
                rssi = rssi,
                lastSeen = System.currentTimeMillis(),
                distance = calculateWiFiDistance(rssi, frequency)
            )
        }

        // Create or update discovered AP
        val discoveredAP = discoveredAPs.getOrPut(bssid) {
            WiFiAccessPoint(
                bssid = bssid,
                ssid = ssid,
                position = estimateAPPosition(bssid, rssi),
                rssi = rssi,
                frequency = frequency,
                confidence = 0.2f, // Low confidence for unknown APs
                lastSeen = System.currentTimeMillis()
            )
        }

        // Update with current scan data
        return discoveredAP.copy(
            rssi = rssi,
            lastSeen = System.currentTimeMillis(),
            distance = calculateWiFiDistance(rssi, frequency)
        )
    }

    private fun calculateWiFiDistance(rssi: Int, frequency: Int): Double {
        // WiFi distance calculation using free space path loss model
        // More complex than BLE due to frequency variations
        val fspl = 27.55 - (20 * kotlin.math.log10(frequency.toDouble())) + kotlin.math.abs(rssi)
        return 10.0.pow(fspl / 20.0)
    }

    private fun estimateAPPosition(bssid: String, rssi: Int): Position {
        // Estimate position using hash-based consistent positioning
        val hash = bssid.hashCode()
        val x = ((hash and 0xFF) * 3).toFloat() // 0-765 range
        val y = (((hash shr 8) and 0xFF) * 2.5f) // 0-640 range
        return Position(x = x, y = y, floor = 1)
    }

    private fun calculateWiFiPosition(accessPoints: List<WiFiAccessPoint>) {
        val knownAPs = accessPoints.filter { knownAccessPoints.containsKey(it.bssid) }
        if (knownAPs.size < 3) return

        try {
            // Weighted centroid calculation
            var weightedX = 0.0
            var weightedY = 0.0
            var totalWeight = 0.0

            knownAPs.forEach { ap ->
                val distance = ap.distance ?: 1.0
                val weight = ap.confidence / (distance + 0.1)

                weightedX += ap.position.x * weight
                weightedY += ap.position.y * weight
                totalWeight += weight
            }

            if (totalWeight > 0) {
                val position = Position(
                    x = (weightedX / totalWeight).toFloat(),
                    y = (weightedY / totalWeight).toFloat(),
                    floor = knownAPs.first().position.floor,
                    accuracy = calculateWiFiAccuracy(knownAPs)
                )

                _wifiPosition.value = position
            }
        } catch (e: Exception) {
            // Handle calculation errors
        }
    }

    private fun calculateWiFiAccuracy(accessPoints: List<WiFiAccessPoint>): Float {
        // WiFi positioning accuracy depends on AP count and signal quality
        val rssiVariance = accessPoints.map { it.rssi }.let { rssiList ->
            val mean = rssiList.average()
            rssiList.map { (it - mean) * (it - mean) }.average()
        }

        val baseAccuracy = when (accessPoints.size) {
            in 5..Int.MAX_VALUE -> 2.0f
            4 -> 3.0f
            3 -> 5.0f
            else -> 8.0f
        }

        return (baseAccuracy + rssiVariance / 100).toFloat().coerceAtLeast(1.0f)
    }

    private fun hasWiFiPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
