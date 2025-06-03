package com.example.indoornavigation20.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.indoornavigation20.R
import com.example.indoornavigation20.domain.model.*
import com.example.indoornavigation20.navigation.NavigationPath
import com.example.indoornavigation20.navigation.NavigationStep

@Composable
fun FloorPlanViewer(
    floorPlan: FloorPlan?,
    currentPosition: Position?,
    selectedPOI: PointOfInterest?,
    pointsOfInterest: List<PointOfInterest> = emptyList(),
    navigationPath: NavigationPath? = null,
    onPOIClick: (PointOfInterest) -> Unit = {},
    onPositionClick: (Position) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                // Display the actual SVG floor plan
                Image(
                    painter = painterResource(id = R.drawable.ground_floor),
                    contentDescription = "Floor Plan",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )

                // Overlay POIs and labels on top of the SVG
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (floorPlan != null) {
                        // Draw navigation path
                        navigationPath?.let { path ->
                            drawNavigationPath(path, floorPlan)
                        }

                        // Draw POIs - positioned relative to the SVG
                        pointsOfInterest.forEach { poi ->
                            if (poi.position.floor == floorPlan.floorNumber) {
                                drawPOI(poi, poi == selectedPOI)
                            }
                        }

                        // Draw current position
                        currentPosition?.let { position ->
                            if (position.floor == floorPlan.floorNumber) {
                                drawCurrentPosition(position)
                            }
                        }

                        // Overlay room labels on the SVG
                        drawRoomLabels()
                    }
                }
            }

            // Floor plan info overlay
            floorPlan?.let { plan ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Floor ${plan.floorNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${plan.rooms.size} rooms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Zoom controls
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            scale = (scale * 1.2f).coerceAtMost(3f)
                        }
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(
                        onClick = {
                            scale = (scale / 1.2f).coerceAtLeast(0.5f)
                        }
                    ) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) {
                        Text("⌂", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Selected POI info
            selectedPOI?.let { poi ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .widthIn(max = 300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = poi.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = poi.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Floor ${poi.position.floor} • ${poi.category.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// This function overlays labels on the actual SVG floor plan
private fun DrawScope.drawRoomLabels() {
    // Calculate positioning relative to the SVG coordinates
    val scaleX = size.width / 1550.9f
    val scaleY = size.height / 835.2f
    val uniformScale = minOf(scaleX, scaleY)

    val offsetX = (size.width - 1550.9f * uniformScale) / 2f
    val offsetY = (size.height - 835.2f * uniformScale) / 2f

    drawContext.canvas.nativeCanvas.apply {
        // Standardized font for ALL labels
        val standardLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f // Base size, scaled at drawText
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(1f, 0.5f, 0.5f, android.graphics.Color.WHITE)
        }

        // Helper to apply scaling to text size and coordinates
        // Adjusted for potential multi-line text
        fun drawScaledText(
            text: String,
            svgX: Float,
            svgY: Float,
            paint: android.graphics.Paint,
            maxWidthPixels: Float = 0f
        ) {
            val transformedX = offsetX + svgX * uniformScale
            val transformedY = offsetY + svgY * uniformScale
            paint.textSize = 14f * uniformScale // Apply scale to text size here

            val lines = if (maxWidthPixels > 0 && paint.measureText(text) > maxWidthPixels) {
                // Simple split, for more complex scenarios, a proper text wrapping algo would be needed
                val words = text.split(" ")
                val firstLine = words.take(words.size / 2).joinToString(" ")
                val secondLine = words.drop(words.size / 2).joinToString(" ")
                listOf(firstLine, secondLine)
            } else {
                listOf(text)
            }

            var currentY = transformedY
            if (lines.size > 1) {
                currentY -= (lines.size - 1) * paint.textSize / 2f // Adjust Y for multi-line centering
            }

            lines.forEachIndexed { index, line ->
                drawText(
                    line,
                    transformedX,
                    currentY + (index * paint.textSize * 1.2f) + paint.textSize / 3f,
                    paint
                )
            }
        }

        // Theater Hall labels - Centered in their SVG rectangles
        drawScaledText("TH 1", 480.15f, 779.25f, standardLabelPaint)
        drawScaledText("TH 2", 641.35f, 779.25f, standardLabelPaint)
        drawScaledText("TH 3", 807.65f, 779.25f, standardLabelPaint)

        // Right wing rectangles - Centered in their SVG rectangles
        // Max width for Unit Cawangan Zon 4 to allow wrapping
        val rightWingRectWidth = (1457.4f - 1256f) * uniformScale
        drawScaledText(
            "Unit Cawangan Zon 4",
            1356.7f,
            131.75f,
            standardLabelPaint,
            rightWingRectWidth
        )
        drawScaledText("TH 5", 1356.7f, 222.5f, standardLabelPaint)
        drawScaledText("TH 4", 1356.7f, 317.75f, standardLabelPaint)

        // Cafe - Centered in its SVG rectangle (1339.1, 473.7) to (1549.7, 558.2)
        drawScaledText("Cafe", (1339.1f + 1549.7f) / 2f, (473.7f + 558.2f) / 2f, standardLabelPaint)

        // Pejabat Pengurusan Pentadbiran FSKM (Top part of left block)
        // Max width for this label to allow wrapping, approx width of its containing SVG box (617.2 - 401.1)
        val adminFskmRectWidth = (617.2f - 401.1f) * uniformScale
        drawScaledText(
            "Pejabat Pengurusan Pentadbiran FSKM",
            509.15f,
            257f,
            standardLabelPaint,
            adminFskmRectWidth
        )

        // Tandas (L) - Left, below the above admin block's unlabelled middle section
        drawScaledText("Tandas (L)", 509.15f, 470.45f, standardLabelPaint)

        // Pejabat Pengurusan Akademik (Top-center block)
        // Max width for this label to allow wrapping, approx width of its containing SVG box (772.2 - 629.7)
        val adminAkademikRectWidth = (772.2f - 629.7f) * uniformScale
        drawScaledText(
            "Pejabat Pengurusan Akademik",
            700f,
            113.5f,
            standardLabelPaint,
            adminAkademikRectWidth
        )

        // Tandas (P) - Top-center block, right of Akademik
        drawScaledText("Tandas (P)", 855.65f, 113.5f, standardLabelPaint)

        // Lif - Centered in its specific SVG shape (the rotated square in the walkway)
        // From SVG: M659.2,268.8l28.8,-28.8 43.5,43.5 -28.8,28.8 -43.5,-43.5z
        // Vertices approx: (659.2, 268.8), (688, 240), (731.5, 283.5), (702.7, 312.3)
        // Center approx: (659.2+731.5)/2 = 695.35, (240+312.3)/2 = 276.15
        drawScaledText("Lif", 695.35f, 276.15f, standardLabelPaint)

        // Laman Najib - Centered in its SVG area
        drawScaledText("Laman Najib", 800f, 450f, standardLabelPaint)
    }
}

private fun DrawScope.drawPOI(poi: PointOfInterest, isSelected: Boolean) {
    // Calculate position relative to the SVG coordinates
    val scaleX = size.width / 1550.9f
    val scaleY = size.height / 835.2f
    val uniformScale = minOf(scaleX, scaleY)

    val offsetX = (size.width - 1550.9f * uniformScale) / 2f
    val offsetY = (size.height - 835.2f * uniformScale) / 2f

    val position = Offset(
        offsetX + poi.position.x * uniformScale,
        offsetY + poi.position.y * uniformScale
    )

    val color = when (poi.category) {
        POICategory.CLASSROOM -> Color(0xFF2196F3) // Blue for theaters
        POICategory.OFFICE -> Color(0xFF4CAF50) // Green for offices
        POICategory.CAFETERIA -> Color(0xFFFF5722) // Orange for cafe
        POICategory.RESTROOM -> Color(0xFF9C27B0) // Purple for restrooms
        POICategory.ELEVATOR -> Color(0xFFFF9800) // Amber for elevator
        POICategory.LOBBY -> Color(0xFF607D8B) // Blue grey for lobby/courtyard
        POICategory.ENTRANCE -> Color(0xFF4CAF50) // Green for entrance
        else -> Color(0xFF757575) // Grey for others
    }

    val radius = if (isSelected) 10f * uniformScale else 6f * uniformScale

    // Draw outer ring if selected
    if (isSelected) {
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = 15f * uniformScale,
            center = position
        )
        drawCircle(
            color = color,
            radius = 12f * uniformScale,
            center = position,
            style = Stroke(width = 2f * uniformScale)
        )
    }

    // Draw POI marker
    drawCircle(
        color = color,
        radius = radius,
        center = position
    )

    drawCircle(
        color = Color.White,
        radius = radius - 2f * uniformScale,
        center = position
    )

    // Draw small icon or text indicator
    if (radius >= 4f * uniformScale) {
        val iconText = when (poi.category) {
            POICategory.CLASSROOM -> "TH"
            POICategory.OFFICE -> "O"
            POICategory.CAFETERIA -> "C"
            POICategory.RESTROOM -> "T"
            POICategory.ELEVATOR -> "L"
            POICategory.LOBBY -> "P"
            else -> "?"
        }

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.BLACK
                textSize = radius * 0.8f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawText(
                iconText,
                position.x,
                position.y + radius * 0.2f,
                paint
            )
        }
    }
}

private fun DrawScope.drawCurrentPosition(position: Position) {
    val scaleX = size.width / 1550.9f
    val scaleY = size.height / 835.2f
    val uniformScale = minOf(scaleX, scaleY)

    val offsetX = (size.width - 1550.9f * uniformScale) / 2f
    val offsetY = (size.height - 835.2f * uniformScale) / 2f

    val center = Offset(
        offsetX + position.x * uniformScale,
        offsetY + position.y * uniformScale
    )

    // Draw accuracy circle
    drawCircle(
        color = Color(0x404285F4),
        radius = (position.accuracy.coerceAtLeast(20f)) * uniformScale,
        center = center
    )

    // Draw position dot with border
    drawCircle(
        color = Color.White,
        radius = 8f * uniformScale,
        center = center
    )

    drawCircle(
        color = Color(0xFF4285F4),
        radius = 6f * uniformScale,
        center = center
    )

    drawCircle(
        color = Color.White,
        radius = 2f * uniformScale,
        center = center
    )
}

private fun DrawScope.drawNavigationPath(path: NavigationPath, floorPlan: FloorPlan) {
    val scaleX = size.width / 1550.9f
    val scaleY = size.height / 835.2f
    val uniformScale = minOf(scaleX, scaleY)

    val offsetX = (size.width - 1550.9f * uniformScale) / 2f
    val offsetY = (size.height - 835.2f * uniformScale) / 2f

    val pathColor = Color(0xFF4285F4)

    // Draw path lines
    path.steps.zipWithNext { current, next ->
        when {
            current is NavigationStep.Move && next is NavigationStep.Move -> {
                val start = Offset(
                    offsetX + current.position.x * uniformScale,
                    offsetY + current.position.y * uniformScale
                )
                val end = Offset(
                    offsetX + next.position.x * uniformScale,
                    offsetY + next.position.y * uniformScale
                )

                drawLine(
                    color = pathColor,
                    start = start,
                    end = end,
                    strokeWidth = 3f * uniformScale,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            8f * uniformScale,
                            4f * uniformScale
                        )
                    )
                )
            }
        }
    }

    // Draw path waypoints
    path.steps.forEach { step ->
        when (step) {
            is NavigationStep.Move -> {
                val waypoint = Offset(
                    offsetX + step.position.x * uniformScale,
                    offsetY + step.position.y * uniformScale
                )

                drawCircle(
                    color = pathColor,
                    radius = 2f * uniformScale,
                    center = waypoint
                )
            }
            is NavigationStep.Turn,
            is NavigationStep.FloorChange -> {
                // These don't have visual representation on the floor plan
            }
        }
    }
}
