package com.example.indoornavigation20.positioning

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.indoornavigation20.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * Detects whether the user is inside or outside the building
 * Uses GPS, beacon signals, and WiFi to make the determination
 */
class BuildingDetector(private val context: Context) {

    private val _isInsideBuilding = MutableStateFlow(false)
    val isInsideBuilding: StateFlow<Boolean> = _isInsideBuilding

    private val _detectionMethod = MutableStateFlow("Unknown")
    val detectionMethod: StateFlow<String> = _detectionMethod

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var lastGpsLocation: Location? = null
    private var lastIndoorPosition: Position? = null

    // Building boundaries (you should adjust these for your actual building)
    private val buildingBounds = BuildingBounds(
        northLat = 40.7831, // Replace with your building's actual coordinates
        southLat = 40.7821,
        eastLng = -73.9712,
        westLng = -73.9722
    )

    fun startDetection() {
        startGpsTracking()
    }

    fun stopDetection() {
        stopGpsTracking()
    }

    @Suppress("MissingPermission")
    private fun startGpsTracking() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000L, // 10 seconds
                10f,    // 10 meters
                gpsLocationListener
            )
        } catch (e: SecurityException) {
            println("⚠️ GPS permission not granted")
        }
    }

    private fun stopGpsTracking() {
        locationManager.removeUpdates(gpsLocationListener)
    }

    private val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastGpsLocation = location
            evaluateBuildingStatus()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun updateIndoorPosition(position: Position?) {
        lastIndoorPosition = position
        evaluateBuildingStatus()
    }

    fun updateBeaconSignals(beaconCount: Int, averageRssi: Double) {
        // Strong beacon signals indicate user is inside
        if (beaconCount >= 2 && averageRssi > -70) {
            _isInsideBuilding.value = true
            _detectionMethod.value = "Strong beacon signals ($beaconCount beacons)"
            return
        }

        evaluateBuildingStatus()
    }

    private fun evaluateBuildingStatus() {
        val gps = lastGpsLocation
        val indoor = lastIndoorPosition

        // Priority 1: If we have recent accurate indoor positioning, user is inside
        if (indoor != null && indoor.accuracy <= 5.0f &&
            (System.currentTimeMillis() - indoor.timestamp) < 30000
        ) {
            _isInsideBuilding.value = true
            _detectionMethod.value = "Indoor positioning (accuracy: ${indoor.accuracy}m)"
            return
        }

        // Priority 2: Check GPS location against building bounds
        if (gps != null) {
            val isInBounds = isLocationInBuilding(gps)
            val accuracy = gps.accuracy

            when {
                isInBounds && accuracy <= 10f -> {
                    // Inside building bounds with good GPS accuracy
                    _isInsideBuilding.value = true
                    _detectionMethod.value = "GPS inside building (accuracy: ${accuracy}m)"
                }

                !isInBounds && accuracy <= 15f -> {
                    // Outside building bounds with good GPS accuracy
                    _isInsideBuilding.value = false
                    _detectionMethod.value = "GPS outside building (accuracy: ${accuracy}m)"
                }

                isInBounds && accuracy > 10f -> {
                    // In building bounds but poor GPS (likely inside due to signal attenuation)
                    _isInsideBuilding.value = true
                    _detectionMethod.value = "Poor GPS in building area (likely inside)"
                }

                else -> {
                    // Default to outside if we can't determine
                    _isInsideBuilding.value = false
                    _detectionMethod.value = "GPS unclear (accuracy: ${accuracy}m)"
                }
            }
            return
        }

        // Priority 3: If no indoor position for a while, likely outside
        if (indoor == null || (System.currentTimeMillis() - indoor.timestamp) > 60000) {
            _isInsideBuilding.value = false
            _detectionMethod.value = "No indoor signals detected"
        }
    }

    private fun isLocationInBuilding(location: Location): Boolean {
        return location.latitude <= buildingBounds.northLat &&
                location.latitude >= buildingBounds.southLat &&
                location.longitude <= buildingBounds.eastLng &&
                location.longitude >= buildingBounds.westLng
    }

    /**
     * For testing: manually set building status
     */
    fun setTestingMode(isInside: Boolean, reason: String = "Manual testing") {
        _isInsideBuilding.value = isInside
        _detectionMethod.value = reason
        println("🧪 Building detector: User manually set to ${if (isInside) "INSIDE" else "OUTSIDE"} - $reason")
    }

    /**
     * Get current building status for debugging
     */
    fun getCurrentStatus(): Pair<Boolean, String> {
        return Pair(_isInsideBuilding.value, _detectionMethod.value)
    }
}

data class BuildingBounds(
    val northLat: Double,
    val southLat: Double,
    val eastLng: Double,
    val westLng: Double
)
