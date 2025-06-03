package com.example.indoornavigation20.domain.model

// WiFi Access Point data class
data class WiFiAccessPoint(
    val bssid: String, // MAC address of AP
    val ssid: String, // Network name
    val position: Position, // Assumes Position is in the same package or imported
    val rssi: Int,
    val frequency: Int,
    val confidence: Float = 1.0f,
    val lastSeen: Long,
    val distance: Double? = null
)