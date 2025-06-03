package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Beacon(
    val id: String,
    val name: String? = null,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val macAddress: String,
    val position: Position,
    val txPower: Int = -59,
    val pathLossExponent: Double = 2.0
)

@Serializable
data class BeaconMeasurement(
    val beacon: Beacon,
    val rssi: Int,
    val timestamp: Long,
    val distance: Double? = null
)
