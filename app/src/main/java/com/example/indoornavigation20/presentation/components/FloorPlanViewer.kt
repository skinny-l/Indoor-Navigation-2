package com.example.indoornavigation20.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlin.math.min
import kotlin.math.sqrt
import com.example.indoornavigation20.R
import com.example.indoornavigation20.domain.model.*
import com.example.indoornavigation20.navigation.NavigationPath
import com.example.indoornavigation20.navigation.NavigationStep

// Coordinate transformation utility for consistent SVG-to-Canvas mapping
object CoordinateTransformer {
    data class TransformParams(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float
    )

    // SVG dimensions from the actual SVG viewBox
    private const val SVG_WIDTH = 1165.1f
    private const val SVG_HEIGHT = 760.8f

    fun calculateTransformParams(
        canvasWidth: Float,
        canvasHeight: Float
    ): TransformParams {
        // Use uniform scaling to preserve aspect ratio
        val uniformScale = min(canvasWidth / SVG_WIDTH, canvasHeight / SVG_HEIGHT)
        val scaledWidth = SVG_WIDTH * uniformScale
        val scaledHeight = SVG_HEIGHT * uniformScale

        return TransformParams(
            scale = uniformScale,
            offsetX = (canvasWidth - scaledWidth) / 2f,
            offsetY = (canvasHeight - scaledHeight) / 2f
        )
    }

    fun transformPoint(
        x: Float,
        y: Float,
        params: TransformParams
    ): Offset {
        return Offset(
            x = (x * params.scale) + params.offsetX,
            y = (y * params.scale) + params.offsetY
        )
    }

    // Inverse transformation for touch events
    fun inverseTransformPoint(
        x: Float,
        y: Float,
        params: TransformParams
    ): Offset {
        return Offset(
            x = (x - params.offsetX) / params.scale,
            y = (y - params.offsetY) / params.scale
        )
    }
}

@Composable
fun FloorPlanViewer(
    floorPlan: FloorPlan?,
    currentPosition: Position?,
    selectedPOI: PointOfInterest?,
    pointsOfInterest: List<PointOfInterest> = emptyList(),
    navigationPath: NavigationPath? = null,
    onPOIClick: (PointOfInterest) -> Unit = {},
    onPositionClick: (Position) -> Unit = {},
    isAdminMode: Boolean = false,
    onAddPOI: ((Float, Float) -> Unit)? = null,
    onMovePOI: ((PointOfInterest, Float, Float) -> Unit)? = null,
    // New node-related parameters
    userNodes: List<NavNode> = emptyList(),
    selectedNode: NavNode? = null,
    onNodeClick: (NavNode) -> Unit = {},
    onAddNode: ((Float, Float) -> Unit)? = null,
    onMoveNode: ((NavNode, Float, Float) -> Unit)? = null,
    isNodePlacementMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var selectedPOIForMove by remember { mutableStateOf<PointOfInterest?>(null) }
    var selectedNodeForMove by remember { mutableStateOf<NavNode?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var longPressedItem by remember { mutableStateOf<Any?>(null) }

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
                    painter = painterResource(id = R.drawable.plain_svg), // Updated to new XML name
                    contentDescription = "Floor Plan",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )

                // Overlay POIs, nodes and labels on top of the SVG
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isAdminMode, isNodePlacementMode) {
                            if (isAdminMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val transformParams =
                                            CoordinateTransformer.calculateTransformParams(
                                                size.width.toFloat(),
                                                size.height.toFloat()
                                            )

                                        // Find what item we're starting to drag
                                        val tappedPOI = pointsOfInterest.find { poi ->
                                            val poiPos = CoordinateTransformer.transformPoint(
                                                poi.position.x,
                                                poi.position.y,
                                                transformParams
                                            )
                                            val distance = sqrt(
                                                (offset.x - poiPos.x) * (offset.x - poiPos.x) +
                                                        (offset.y - poiPos.y) * (offset.y - poiPos.y)
                                            )
                                            distance < 40f // Larger hit area
                                        }

                                        val tappedNode = userNodes.find { node ->
                                            val nodePos = CoordinateTransformer.transformPoint(
                                                node.position.x,
                                                node.position.y,
                                                transformParams
                                            )
                                            val distance = sqrt(
                                                (offset.x - nodePos.x) * (offset.x - nodePos.x) +
                                                        (offset.y - nodePos.y) * (offset.y - nodePos.y)
                                            )
                                            distance < 40f // Larger hit area
                                        }

                                        when {
                                            tappedPOI != null && !isNodePlacementMode -> {
                                                selectedPOIForMove = tappedPOI
                                                isDragging = true
                                                longPressedItem = tappedPOI
                                            }

                                            tappedNode != null && isNodePlacementMode -> {
                                                selectedNodeForMove = tappedNode
                                                isDragging = true
                                                longPressedItem = tappedNode
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (isDragging) {
                                            dragOffset += dragAmount

                                            val transformParams =
                                                CoordinateTransformer.calculateTransformParams(
                                                    size.width.toFloat(),
                                                    size.height.toFloat()
                                                )

                                            // Update POI position in real-time
                                            selectedPOIForMove?.let { poi ->
                                                val currentPos =
                                                    CoordinateTransformer.transformPoint(
                                                        poi.position.x,
                                                        poi.position.y,
                                                        transformParams
                                                    )
                                                val newScreenPos = currentPos + dragAmount
                                                val newSvgPos =
                                                    CoordinateTransformer.inverseTransformPoint(
                                                        newScreenPos.x,
                                                        newScreenPos.y,
                                                        transformParams
                                                    )
                                                onMovePOI?.invoke(poi, newSvgPos.x, newSvgPos.y)
                                            }

                                            // Update Node position in real-time
                                            selectedNodeForMove?.let { node ->
                                                val currentPos =
                                                    CoordinateTransformer.transformPoint(
                                                        node.position.x,
                                                        node.position.y,
                                                        transformParams
                                                    )
                                                val newScreenPos = currentPos + dragAmount
                                                val newSvgPos =
                                                    CoordinateTransformer.inverseTransformPoint(
                                                        newScreenPos.x,
                                                        newScreenPos.y,
                                                        transformParams
                                                    )
                                                onMoveNode?.invoke(node, newSvgPos.x, newSvgPos.y)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        selectedPOIForMove = null
                                        selectedNodeForMove = null
                                        dragOffset = Offset.Zero
                                        longPressedItem = null
                                    }
                                )
                            } else {
                                // Non-admin tap handling
                                detectTapGestures(
                                    onTap = { offset ->
                                        val transformParams =
                                            CoordinateTransformer.calculateTransformParams(
                                                size.width.toFloat(),
                                                size.height.toFloat()
                                            )

                                        // Check for POI tap
                                        val tappedPOI = pointsOfInterest.find { poi ->
                                            val poiPos = CoordinateTransformer.transformPoint(
                                                poi.position.x,
                                                poi.position.y,
                                                transformParams
                                            )
                                            val distance = sqrt(
                                                (offset.x - poiPos.x) * (offset.x - poiPos.x) +
                                                        (offset.y - poiPos.y) * (offset.y - poiPos.y)
                                            )
                                            distance < 40f
                                        }

                                        // Check for Node tap
                                        val tappedNode = userNodes.find { node ->
                                            val nodePos = CoordinateTransformer.transformPoint(
                                                node.position.x,
                                                node.position.y,
                                                transformParams
                                            )
                                            val distance = sqrt(
                                                (offset.x - nodePos.x) * (offset.x - nodePos.x) +
                                                        (offset.y - nodePos.y) * (offset.y - nodePos.y)
                                            )
                                            distance < 40f
                                        }

                                        when {
                                            tappedPOI != null -> onPOIClick(tappedPOI)
                                            tappedNode != null -> onNodeClick(tappedNode)
                                            else -> {
                                                val svgCoords =
                                                    CoordinateTransformer.inverseTransformPoint(
                                                        offset.x,
                                                        offset.y,
                                                        transformParams
                                                    )
                                                onPositionClick(
                                                    Position(
                                                        svgCoords.x,
                                                        svgCoords.y,
                                                        1
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(isAdminMode, isNodePlacementMode) {
                            if (isAdminMode) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val transformParams =
                                            CoordinateTransformer.calculateTransformParams(
                                                size.width.toFloat(),
                                                size.height.toFloat()
                                            )

                                        // Check for existing items first
                                        val tappedPOI = pointsOfInterest.find { poi ->
                                            val poiPos = CoordinateTransformer.transformPoint(
                                                poi.position.x,
                                                poi.position.y,
                                                transformParams
                                            )
                                            val distance = sqrt(
                                                (offset.x - poiPos.x) * (offset.x - poiPos.x) +
                                                        (offset.y - poiPos.y) * (offset.y - poiPos.y)
                                            )
                                            distance < 40f
                                        }

                                        val tappedNode = userNodes.find { node ->
                                            val nodePos = CoordinateTransformer.transformPoint(
                                                node.position.x,
                                                node.position.y,
                                                transformParams
                                            )
                                            val distance = sqrt(
                                                (offset.x - nodePos.x) * (offset.x - nodePos.x) +
                                                        (offset.y - nodePos.y) * (offset.y - nodePos.y)
                                            )
                                            distance < 40f
                                        }

                                        when {
                                            tappedPOI != null && !isNodePlacementMode -> onPOIClick(
                                                tappedPOI
                                            )

                                            tappedNode != null && isNodePlacementMode -> onNodeClick(
                                                tappedNode
                                            )

                                            tappedNode != null && !isNodePlacementMode -> onNodeClick(
                                                tappedNode
                                            )

                                            else -> {
                                                // Add new item
                                                val svgCoords =
                                                    CoordinateTransformer.inverseTransformPoint(
                                                        offset.x,
                                                        offset.y,
                                                        transformParams
                                                    )
                                                if (isNodePlacementMode) {
                                                    onAddNode?.invoke(svgCoords.x, svgCoords.y)
                                                } else {
                                                    onAddPOI?.invoke(svgCoords.x, svgCoords.y)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    if (floorPlan != null) {
                        // Calculate transform parameters once for consistent coordinate transformation
                        val transformParams =
                            CoordinateTransformer.calculateTransformParams(size.width, size.height)

                        // Draw navigation path
                        navigationPath?.let { path ->
                            drawNavigationPath(path, floorPlan)
                        }

                        // Draw all navigation nodes for debugging
                        /*
                        floorPlan.nodes?.forEach { node ->
                            if (node.position.floor == floorPlan.floorNumber) { // Ensure node is on current floor
                                val transformedPosition = CoordinateTransformer.transformPoint(
                                    node.position.x,
                                    node.position.y,
                                    transformParams
                                )
                                drawCircle(
                                    color = Color.Magenta,
                                    radius = 4f * transformParams.scale,
                                    center = transformedPosition
                                )
                            }
                        }

                        // Draw all connections between nodes for debugging 
                        floorPlan.nodes?.forEach { node ->
                            if (node.position.floor == floorPlan.floorNumber) {
                                val nodePosition = CoordinateTransformer.transformPoint(
                                    node.position.x,
                                    node.position.y,
                                    transformParams
                                )
                                node.connections.forEach { connectionId ->
                                    val connectedNode =
                                        floorPlan.nodes.find { it.id == connectionId }
                                    if (connectedNode != null && connectedNode.position.floor == floorPlan.floorNumber) {
                                        val connectedPosition =
                                            CoordinateTransformer.transformPoint(
                                                connectedNode.position.x,
                                                connectedNode.position.y,
                                                transformParams
                                            )
                                        drawLine(
                                            color = Color.Cyan.copy(alpha = 0.5f),
                                            start = nodePosition,
                                            end = connectedPosition,
                                            strokeWidth = 1.5f * transformParams.scale
                                        )
                                    }
                                }
                            }
                        }
                        */

                        // Draw POIs - positioned relative to the SVG
                        pointsOfInterest.forEach { poi ->
                            if (poi.position.floor == floorPlan.floorNumber) {
                                drawPOI(poi, poi == selectedPOI)
                            }
                        }

                        // Draw user-added navigation nodes
                        userNodes.forEach { node ->
                            if (node.position.floor == floorPlan.floorNumber) {
                                drawNavNode(node, node == selectedNode)
                            }
                        }

                        // Draw current position
                        currentPosition?.let { position ->
                            if (position.floor == floorPlan.floorNumber) {
                                drawCurrentPosition(position)
                            }
                        }

                        // Overlay room labels on the SVG
                        // Temporarily disabled - user will add POIs manually first
                        // drawRoomLabels()
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

            // Selected node info
            selectedNode?.let { node ->
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
                            text = "Node ${node.id}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Floor ${node.position.floor} • ${node.type.name}",
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
    // Use the new coordinate transformer
    val transformParams = CoordinateTransformer.calculateTransformParams(size.width, size.height)

    drawContext.canvas.nativeCanvas.apply {
        // Standardized font for ALL labels
        val standardLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f * transformParams.scale // Apply scale to text size
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(1f, 0.5f, 0.5f, android.graphics.Color.WHITE)
        }

        // Helper to apply scaling to text size and coordinates
        fun drawScaledText(
            text: String,
            svgX: Float,
            svgY: Float,
            paint: android.graphics.Paint,
            maxWidthPixels: Float = 0f
        ) {
            val transformed = CoordinateTransformer.transformPoint(svgX, svgY, transformParams)

            val lines = if (maxWidthPixels > 0 && paint.measureText(text) > maxWidthPixels) {
                // Simple split, for more complex scenarios, a proper text wrapping algo would be needed
                val words = text.split(" ")
                val firstLine = words.take(words.size / 2).joinToString(" ")
                val secondLine = words.drop(words.size / 2).joinToString(" ")
                listOf(firstLine, secondLine)
            } else {
                listOf(text)
            }

            var currentY = transformed.y
            if (lines.size > 1) {
                currentY -= (lines.size - 1) * paint.textSize / 2f // Adjust Y for multi-line centering
            }

            lines.forEachIndexed { index, line ->
                drawText(
                    line,
                    transformed.x,
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
        val rightWingRectWidth = (1457.4f - 1256f) * transformParams.scale
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

        // Pejabot Pengurusan Pentadbiran FSKM (Top part of left block)
        // Max width for this label to allow wrapping, approx width of its containing SVG box (617.2 - 401.1)
        val adminFskmRectWidth = (617.2f - 401.1f) * transformParams.scale
        drawScaledText(
            "Pejabot Pengurusan Pentadbiran FSKM",
            509.15f,
            257f,
            standardLabelPaint,
            adminFskmRectWidth
        )

        // Tandas (L) - Left, below the above admin block's unlabelled middle section
        drawScaledText("Tandas (L)", 509.15f, 470.45f, standardLabelPaint)

        // Pejabot Pengurusan Akademik (Top-center block)
        // Max width for this label to allow wrapping, approx width of its containing SVG box (772.2 - 629.7)
        val adminAkademikRectWidth = (772.2f - 629.7f) * transformParams.scale
        drawScaledText(
            "Pejabot Pengurusan Akademik",
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
    // Use the new coordinate transformer
    val transformParams = CoordinateTransformer.calculateTransformParams(size.width, size.height)
    val position =
        CoordinateTransformer.transformPoint(poi.position.x, poi.position.y, transformParams)

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

    val baseRadius = 12f * transformParams.scale
    val selectedRadius = 18f * transformParams.scale

    // Enhanced visual feedback for selected POI
    if (isSelected) {
        // Pulsing animation effect
        val pulseRadius =
            selectedRadius + (kotlin.math.sin(System.currentTimeMillis() / 300.0) * 3).toFloat()

        // Multiple rings for better visibility
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = pulseRadius + 15f,
            center = position
        )
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = pulseRadius + 8f,
            center = position
        )
        drawCircle(
            color = color.copy(alpha = 0.6f),
            radius = pulseRadius,
            center = position,
            style = Stroke(width = 2f * transformParams.scale)
        )

        // Drag handle indicator
        drawCircle(
            color = Color.White,
            radius = 6f * transformParams.scale,
            center = Offset(
                position.x + selectedRadius + 8f,
                position.y - selectedRadius - 8f
            )
        )
        drawCircle(
            color = color,
            radius = 4f * transformParams.scale,
            center = Offset(
                position.x + selectedRadius + 8f,
                position.y - selectedRadius - 8f
            )
        )

        // Selection indicator lines
        val lineLength = 25f * transformParams.scale
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(position.x - lineLength, position.y),
            end = Offset(position.x + lineLength, position.y),
            strokeWidth = 2f * transformParams.scale
        )
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(position.x, position.y - lineLength),
            end = Offset(position.x, position.y + lineLength),
            strokeWidth = 2f * transformParams.scale
        )
    }

    // Main POI marker with shadow
    val shadowOffset = 2f * transformParams.scale
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = baseRadius,
        center = Offset(position.x + shadowOffset, position.y + shadowOffset)
    )

    // Main circle
    drawCircle(
        color = color,
        radius = baseRadius,
        center = position
    )

    // Inner circle
    drawCircle(
        color = Color.White,
        radius = baseRadius - 3f * transformParams.scale,
        center = position
    )

    // Draw category icon
    val iconText = when (poi.category) {
        POICategory.CLASSROOM -> "🎓"
        POICategory.OFFICE -> "🏢"
        POICategory.CAFETERIA -> "🍽️"
        POICategory.RESTROOM -> "🚻"
        POICategory.ELEVATOR -> "🛗"
        POICategory.LOBBY -> "🏛️"
        POICategory.ENTRANCE -> "🚪"
        else -> "📍"
    }

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.BLACK
            textSize = baseRadius * 0.8f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // For emoji support, we'll use a simple text instead
        val simpleIcon = when (poi.category) {
            POICategory.CLASSROOM -> "TH"
            POICategory.OFFICE -> "O"
            POICategory.CAFETERIA -> "C"
            POICategory.RESTROOM -> "T"
            POICategory.ELEVATOR -> "L"
            POICategory.LOBBY -> "P"
            POICategory.ENTRANCE -> "E"
            else -> "?"
        }

        drawText(
            simpleIcon,
            position.x,
            position.y + baseRadius * 0.3f,
            paint
        )
    }

    // Draw POI name for selected items with better styling
    if (isSelected) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.WHITE
                textSize = 14f * transformParams.scale
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
            }

            // Background for text
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(poi.name, 0, poi.name.length, textBounds)
            val textWidth = textBounds.width()
            val textHeight = textBounds.height()

            val backgroundPaint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.BLACK
                alpha = 180
            }

            drawRoundRect(
                position.x - textWidth / 2f - 8f,
                position.y + selectedRadius + 15f - textHeight / 2f - 4f,
                position.x + textWidth / 2f + 8f,
                position.y + selectedRadius + 15f + textHeight / 2f + 4f,
                8f, 8f,
                backgroundPaint
            )

            drawText(
                poi.name,
                position.x,
                position.y + selectedRadius + 15f,
                paint
            )
        }
    }
}

private fun DrawScope.drawNavNode(node: NavNode, isSelected: Boolean) {
    // Use the new coordinate transformer
    val transformParams = CoordinateTransformer.calculateTransformParams(size.width, size.height)
    val position =
        CoordinateTransformer.transformPoint(node.position.x, node.position.y, transformParams)

    val color = when (node.type) {
        NodeType.WALKWAY -> Color(0xFF2196F3) // Blue for walkway
        NodeType.DOOR -> Color(0xFF4CAF50) // Green for door
        NodeType.ELEVATOR -> Color(0xFF9C27B0) // Purple for elevators
        NodeType.STAIRS -> Color(0xFFFF9800) // Amber for stairs
        NodeType.OBSTACLE -> Color(0xFFFF5722) // Red for obstacles
    }

    val baseRadius = 10f * transformParams.scale
    val selectedRadius = 16f * transformParams.scale

    // Enhanced visual feedback for selected node
    if (isSelected) {
        // Animated pulsing effect
        val pulseRadius =
            selectedRadius + (kotlin.math.sin(System.currentTimeMillis() / 200.0) * 2).toFloat()

        // Multiple rings with different opacities
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = pulseRadius + 20f,
            center = position
        )
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = pulseRadius + 10f,
            center = position
        )
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = pulseRadius,
            center = position,
            style = Stroke(width = 2.5f * transformParams.scale)
        )

        // Connection indicators (small dots around the node)
        val connectionRadius = selectedRadius + 25f
        val connectionCount = node.connections.size.coerceAtMost(8)
        for (i in 0 until connectionCount) {
            val angle = (i * 360f / connectionCount) * kotlin.math.PI / 180f
            val connectionPos = Offset(
                position.x + (connectionRadius * kotlin.math.cos(angle)).toFloat(),
                position.y + (connectionRadius * kotlin.math.sin(angle)).toFloat()
            )
            drawCircle(
                color = color.copy(alpha = 0.7f),
                radius = 3f * transformParams.scale,
                center = connectionPos
            )
        }

        // Drag handle with enhanced visibility
        val handleOffset = selectedRadius + 12f
        drawCircle(
            color = Color.White,
            radius = 7f * transformParams.scale,
            center = Offset(
                position.x + handleOffset,
                position.y - handleOffset
            )
        )
        drawCircle(
            color = color,
            radius = 5f * transformParams.scale,
            center = Offset(
                position.x + handleOffset,
                position.y - handleOffset
            )
        )

        // Cross-hair for precise positioning
        val crossSize = 20f * transformParams.scale
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = Offset(position.x - crossSize, position.y),
            end = Offset(position.x + crossSize, position.y),
            strokeWidth = 1.5f * transformParams.scale
        )
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = Offset(position.x, position.y - crossSize),
            end = Offset(position.x, position.y + crossSize),
            strokeWidth = 1.5f * transformParams.scale
        )
    }

    // Main node marker with enhanced styling
    val shadowOffset = 1.5f * transformParams.scale
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = baseRadius,
        center = Offset(position.x + shadowOffset, position.y + shadowOffset)
    )

    // Outer ring based on node type
    val ringColor = when (node.type) {
        NodeType.WALKWAY -> color.copy(alpha = 0.8f)
        NodeType.DOOR -> Color(0xFF81C784) // Lighter green
        NodeType.ELEVATOR -> Color(0xFFBA68C8) // Lighter purple
        NodeType.STAIRS -> Color(0xFFFFB74D) // Lighter amber
        NodeType.OBSTACLE -> Color(0xFFFF8A65) // Lighter red
    }

    drawCircle(
        color = ringColor,
        radius = baseRadius + 2f * transformParams.scale,
        center = position
    )

    // Main circle
    drawCircle(
        color = color,
        radius = baseRadius,
        center = position
    )

    // Inner circle for contrast
    drawCircle(
        color = Color.White,
        radius = baseRadius - 2.5f * transformParams.scale,
        center = position
    )

    // Draw type-specific icon with better styling
    drawContext.canvas.nativeCanvas.apply {
        val iconText = when (node.type) {
            NodeType.WALKWAY -> "W"
            NodeType.DOOR -> "D"
            NodeType.ELEVATOR -> "E"
            NodeType.STAIRS -> "S"
            NodeType.OBSTACLE -> "X"
        }

        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.BLACK
            textSize = baseRadius * 0.9f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(1f, 0f, 0f, android.graphics.Color.WHITE)
        }

        drawText(
            iconText,
            position.x,
            position.y + baseRadius * 0.3f,
            paint
        )
    }

    // Draw node ID for selected nodes with enhanced styling
    if (isSelected) {
        drawContext.canvas.nativeCanvas.apply {
            val nodeText = "Node ${node.id.takeLast(4)}" // Show last 4 chars of ID
            val connectionText = "${node.connections.size} connections"

            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.WHITE
                textSize = 12f * transformParams.scale
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(2f, 0f, 0f, android.graphics.Color.BLACK)
            }

            // Background for text
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(nodeText, 0, nodeText.length, textBounds)
            val textWidth = kotlin.math.max(
                textBounds.width().toFloat(),
                paint.measureText(connectionText)
            ).toInt()
            val textHeight = textBounds.height()

            val backgroundPaint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.BLACK
                alpha = 200
            }

            val bgTop = position.y + selectedRadius + 20f - textHeight - 8f
            val bgBottom = position.y + selectedRadius + 20f + textHeight + 8f

            drawRoundRect(
                position.x - textWidth / 2f - 10f,
                bgTop,
                position.x + textWidth / 2f + 10f,
                bgBottom,
                6f, 6f,
                backgroundPaint
            )

            // Draw node info
            drawText(
                nodeText,
                position.x,
                position.y + selectedRadius + 20f,
                paint
            )

            // Draw connection count
            paint.textSize = 10f * transformParams.scale
            drawText(
                connectionText,
                position.x,
                position.y + selectedRadius + 35f,
                paint
            )
        }
    }
}

private fun DrawScope.drawCurrentPosition(position: Position) {
    val transformParams = CoordinateTransformer.calculateTransformParams(size.width, size.height)
    val center = CoordinateTransformer.transformPoint(position.x, position.y, transformParams)

    // Draw accuracy circle
    drawCircle(
        color = Color(0x404285F4),
        radius = (position.accuracy.coerceAtLeast(20f)) * transformParams.scale,
        center = center
    )

    // Draw position dot with border
    drawCircle(
        color = Color.White,
        radius = 8f * transformParams.scale,
        center = center
    )

    drawCircle(
        color = Color(0xFF4285F4),
        radius = 6f * transformParams.scale,
        center = center
    )

    drawCircle(
        color = Color.White,
        radius = 2f * transformParams.scale,
        center = center
    )
}

private fun DrawScope.drawNavigationPath(path: NavigationPath, floorPlan: FloorPlan) {
    val transformParams = CoordinateTransformer.calculateTransformParams(size.width, size.height)
    val pathColor = Color(0xFF4285F4)

    // Draw path lines
    path.steps.zipWithNext { current, next ->
        when {
            current is NavigationStep.Move && next is NavigationStep.Move -> {
                val start = CoordinateTransformer.transformPoint(
                    current.position.x,
                    current.position.y,
                    transformParams
                )
                val end = CoordinateTransformer.transformPoint(
                    next.position.x,
                    next.position.y,
                    transformParams
                )

                drawLine(
                    color = pathColor,
                    start = start,
                    end = end,
                    strokeWidth = 3f * transformParams.scale,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            8f * transformParams.scale,
                            4f * transformParams.scale
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
                val waypoint = CoordinateTransformer.transformPoint(
                    step.position.x,
                    step.position.y,
                    transformParams
                )

                drawCircle(
                    color = pathColor,
                    radius = 2f * transformParams.scale,
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
