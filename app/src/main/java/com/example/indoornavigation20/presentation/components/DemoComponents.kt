package com.example.indoornavigation20.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.indoornavigation20.domain.model.*

@Composable
fun SimpleFloorPlanDemo(
    currentPosition: Position?,
    pointsOfInterest: List<PointOfInterest>,
    onPOIClick: (PointOfInterest) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Simple floor plan visualization
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawSimpleFloorPlan()

                // Draw POIs
                pointsOfInterest.forEach { poi ->
                    drawPOI(poi, size)
                }

                // Draw current position
                currentPosition?.let { position ->
                    drawCurrentPosition(position, size)
                }
            }

            // POI List overlay
            if (pointsOfInterest.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pointsOfInterest.forEach { poi ->
                        POIChip(
                            poi = poi,
                            onClick = { onPOIClick(poi) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun POIChip(
    poi: PointOfInterest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (poi.category) {
                POICategory.RESTROOM -> MaterialTheme.colorScheme.secondaryContainer
                POICategory.ELEVATOR -> MaterialTheme.colorScheme.primaryContainer
                POICategory.STAIRS -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (poi.category) {
                            POICategory.RESTROOM -> Color(0xFF9C27B0)
                            POICategory.ELEVATOR -> Color(0xFF2196F3)
                            POICategory.STAIRS -> Color(0xFFFF9800)
                            POICategory.ENTRANCE -> Color(0xFF4CAF50)
                            POICategory.EXIT -> Color(0xFFF44336)       // Corrected: Using EXIT
                            // POICategory.EMERGENCY_EXIT -> Color(0xFFF44336) // Old line that was causing error
                            else -> Color(0xFF607D8B)
                        }
                    )
            )
            Text(
                text = poi.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

private fun DrawScope.drawSimpleFloorPlan() {
    val roomColor = Color(0xFFE0E0E0)
    val wallColor = Color(0xFF424242)
    val hallwayColor = Color(0xFFF5F5F5)

    // Draw rooms
    drawRect(
        color = roomColor,
        topLeft = Offset(50f, 50f),
        size = androidx.compose.ui.geometry.Size(200f, 150f)
    )

    drawRect(
        color = roomColor,
        topLeft = Offset(300f, 50f),
        size = androidx.compose.ui.geometry.Size(200f, 150f)
    )

    drawRect(
        color = roomColor,
        topLeft = Offset(50f, 250f),
        size = androidx.compose.ui.geometry.Size(200f, 150f)
    )

    drawRect(
        color = roomColor,
        topLeft = Offset(300f, 250f),
        size = androidx.compose.ui.geometry.Size(200f, 150f)
    )

    // Draw hallway
    drawRect(
        color = hallwayColor,
        topLeft = Offset(250f, 50f),
        size = androidx.compose.ui.geometry.Size(50f, 350f)
    )

    // Draw walls (simplified)
    drawLine(
        color = wallColor,
        start = Offset(250f, 50f),
        end = Offset(250f, 400f),
        strokeWidth = 4f
    )

    drawLine(
        color = wallColor,
        start = Offset(300f, 50f),
        end = Offset(300f, 400f),
        strokeWidth = 4f
    )
}

private fun DrawScope.drawPOI(poi: PointOfInterest, canvasSize: androidx.compose.ui.geometry.Size) {
    val position = Offset(
        x = poi.position.x * canvasSize.width / 1000f,
        y = poi.position.y * canvasSize.height / 800f
    )

    val color = when (poi.category) {
        POICategory.RESTROOM -> Color(0xFF9C27B0)
        POICategory.ELEVATOR -> Color(0xFF2196F3)
        POICategory.STAIRS -> Color(0xFFFF9800)
        POICategory.ENTRANCE -> Color(0xFF4CAF50)
        POICategory.EXIT -> Color(0xFFF44336)       // Corrected: Using EXIT
        // POICategory.EMERGENCY_EXIT -> Color(0xFFF44336) // Old line that was causing error
        else -> Color(0xFF607D8B)
    }

    // Draw POI marker
    drawCircle(
        color = color,
        radius = 15f,
        center = position
    )

    drawCircle(
        color = Color.White,
        radius = 10f,
        center = position
    )
}

private fun DrawScope.drawCurrentPosition(
    position: Position,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val center = Offset(
        x = position.x * canvasSize.width / 1000f,
        y = position.y * canvasSize.height / 800f
    )

    // Draw accuracy circle
    drawCircle(
        color = Color(0x404285F4),
        radius = position.accuracy * canvasSize.width / 100f,
        center = center
    )

    // Draw position dot
    drawCircle(
        color = Color(0xFF4285F4),
        radius = 12f,
        center = center
    )

    drawCircle(
        color = Color.White,
        radius = 6f,
        center = center
    )
}
