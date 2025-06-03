package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Building(
    val id: String,
    val name: String,
    val coordinates: BuildingCoordinates,
    val dimensions: BuildingDimensions,
    val address: String = "",
    val description: String = "",
    val floors: List<Int> = emptyList()
)

@Serializable
data class BuildingCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)

@Serializable
data class BuildingDimensions(
    val width: Float,
    val height: Float,
    val length: Float
)

@Serializable
data class CoordinateSystem(
    val originLatitude: Double,
    val originLongitude: Double,
    val metersPerPixelX: Float,
    val metersPerPixelY: Float,
    val rotation: Float = 0.0f // rotation in degrees
)