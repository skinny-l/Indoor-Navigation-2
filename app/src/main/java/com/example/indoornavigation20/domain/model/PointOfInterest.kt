package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PointOfInterest(
    val id: String,
    val name: String,
    val description: String,
    val category: POICategory,
    val position: Position,
    val keywords: List<String> = emptyList(),
    val accessibility: AccessibilityInfo? = null
)

@Serializable
enum class POICategory {
    CLASSROOM, OFFICE, RESTROOM, ELEVATOR, STAIRS,
    ENTRANCE, EXIT, EMERGENCY_EXIT, CAFETERIA, LIBRARY, LABORATORY, LOBBY
}

@Serializable
data class AccessibilityInfo(
    val wheelchairAccessible: Boolean,
    val audioDescription: String? = null,
    val brailleSignage: Boolean = false
)
