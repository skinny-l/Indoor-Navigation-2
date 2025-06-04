package com.example.indoornavigation20.navigation

import com.example.indoornavigation20.domain.model.*
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class PathfindingEngine {

    fun findPath(
        start: Position,
        goal: Position,
        floorPlans: Map<Int, FloorPlan>
    ): NavigationPath {
        return if (start.floor == goal.floor) {
            val floorPlan =
                floorPlans[start.floor] ?: throw IllegalArgumentException("Floor plan not found")
            val path = findSingleFloorPath(start, goal, floorPlan)
            NavigationPath(steps = path.map { NavigationStep.Move(it) })
        } else {
            findMultiFloorPath(start, goal, floorPlans)
        }
    }

    private fun findSingleFloorPath(
        start: Position,
        goal: Position,
        floorPlan: FloorPlan
    ): List<Position> {
        println("🗺️ PathfindingEngine: Finding path from (${start.x}, ${start.y}) to (${goal.x}, ${goal.y})")
        println("🗺️ Available nodes: ${floorPlan.nodes.size}")

        if (floorPlan.nodes.isEmpty()) {
            println("⚠️ No navigation nodes found, using direct path")
            return listOf(start, goal)
        }

        // Attempt to snap to the nearest nodes only if they are very close
        val startNode = findNearestNodeIfClose(start, floorPlan.nodes, maxDistance = 20f)
        val goalNode = findNearestNodeIfClose(goal, floorPlan.nodes, maxDistance = 20f)

        val effectiveStartPos = startNode?.position ?: start
        val effectiveGoalPos = goalNode?.position ?: goal

        println("🗺️ Effective Start: ${startNode?.id ?: "RAW_POS"} at (${effectiveStartPos.x}, ${effectiveStartPos.y})")
        println("🗺️ Effective Goal: ${goalNode?.id ?: "RAW_POS"} at (${effectiveGoalPos.x}, ${effectiveGoalPos.y})")

        // If both start and goal are snapped to nodes, use A* through the node network
        if (startNode != null && goalNode != null) {
            val nodePath = findPathThroughNodes(startNode, goalNode, floorPlan.nodes)
            if (nodePath.isNotEmpty()) {
                println("✅ Path found through ${nodePath.size} nodes: ${nodePath.map { it.id }}")
                return buildCorridorPath(
                    start,
                    goal,
                    nodePath
                ) // Use original start/goal for full path
            }
            println("❌ No node path from ${startNode.id} to ${goalNode.id}, trying direct or partial node path.")
        }

        // Fallback or partial node path logic (e.g., if one end is not near a node)
        // This part might need more sophisticated handling for better routes when not fully on node network
        // For now, if a full node path isn't found, it will effectively create a path that includes
        // start -> nearest_start_node (if_snapped) -> ... -> nearest_goal_node (if_snapped) -> goal
        // If snapping fails for both, it becomes a direct line. 
        // This simplified fallback can lead to the straight lines if node network isn't dense or well-connected.

        val pathPoints = mutableListOf<Position>()
        pathPoints.add(start)
        if (startNode != null && (startNode.position.x != start.x || startNode.position.y != start.y)) {
            pathPoints.add(startNode.position)
        }

        // If we have both start and goal nodes but no path between them, it implies a disconnected graph or isolated nodes.
        // In a real-world scenario, this would indicate an issue with the map data.
        // For now, we are just creating a line between the nodes or to the goal if one node is missing.
        if (startNode != null && goalNode != null && startNode.id != goalNode.id) {
            // This is a simplification; ideally, we'd attempt a partial A* or other strategy.
            println("↔️ Connecting snapped start/goal nodes directly as no full A* path was found.")
        }

        if (goalNode != null && (goalNode.position.x != goal.x || goalNode.position.y != goal.y)) {
            pathPoints.add(goalNode.position)
        }
        pathPoints.add(goal)

        println(
            "🔗 Using simplified path connection. Points: ${
                pathPoints.map {
                    Pair(
                        it.x,
                        it.y
                    )
                }
            }"
        )
        return pathPoints.distinctBy { "${it.x.toInt()},${it.y.toInt()}" }
    }

    private fun findNearestNodeIfClose(
        position: Position,
        nodes: List<NavNode>,
        maxDistance: Float
    ): NavNode? {
        val nearestNode = nodes.filter { it.type == NodeType.WALKWAY || it.type == NodeType.DOOR }
            .minByOrNull { distance(position, it.position) }

        if (nearestNode != null && distance(position, nearestNode.position) <= maxDistance) {
            return nearestNode
        }
        return null
    }

    private fun findPathThroughNodes(
        startNode: NavNode,
        goalNode: NavNode,
        allNodes: List<NavNode>
    ): List<NavNode> {
        val openList = PriorityQueue<AStarNode>(compareBy { it.fCost })
        val closedList = mutableSetOf<String>()
        val nodeMap = allNodes.associateBy { it.id }

        openList.add(
            AStarNode(
                node = startNode,
                gCost = 0f,
                hCost = heuristic(startNode.position, goalNode.position),
                parent = null
            )
        )

        while (openList.isNotEmpty()) {
            val current = openList.poll()
            closedList.add(current.node.id)

            if (current.node.id == goalNode.id) {
                return reconstructPath(current)
            }

            // Only traverse through explicitly connected nodes (no shortcuts)
            for (neighborId in current.node.connections) {
                if (neighborId in closedList) continue

                val neighbor = nodeMap[neighborId] ?: continue

                // Use actual corridor distance, not straight-line distance
                val corridorDistance = calculateCorridorDistance(current.node, neighbor)
                val tentativeGCost = current.gCost + corridorDistance

                val existingNode = openList.find { it.node.id == neighborId }
                if (existingNode == null || tentativeGCost < existingNode.gCost) {
                    openList.remove(existingNode)
                    openList.add(
                        AStarNode(
                            node = neighbor,
                            gCost = tentativeGCost,
                            hCost = heuristic(neighbor.position, goalNode.position),
                            parent = current
                        )
                    )
                }
            }
        }

        return emptyList() // No path found through corridor network
    }

    private fun calculateCorridorDistance(nodeA: NavNode, nodeB: NavNode): Float {
        // Calculate Manhattan distance to prefer corridor-like paths
        val dx = kotlin.math.abs(nodeA.position.x - nodeB.position.x)
        val dy = kotlin.math.abs(nodeA.position.y - nodeB.position.y)
        return dx + dy
    }

    private fun buildCorridorPath(
        start: Position, // Original start position
        goal: Position,   // Original goal position
        nodePath: List<NavNode> // The exact sequence of nodes from A*
    ): List<Position> {
        val finalPathPoints = mutableListOf<Position>()

        if (nodePath.isEmpty()) {
            // This case should ideally not be reached if findPathThroughNodes found a path.
            // If it does, it means A* failed, and we might have a disconnected graph or an issue.
            // Fallback to a direct line, but log this as an error/warning.
            println("⚠️ buildCorridorPath: nodePath is empty. A* failed or graph disconnected. Falling back to direct line.")
            finalPathPoints.add(start)
            finalPathPoints.add(goal)
            return finalPathPoints
        }

        // 1. Start with the original start position.
        finalPathPoints.add(start)
        println("  ➡️ Path: Added raw start (${start.x}, ${start.y})")

        // 2. Add the position of the first node in the A* path IF it's different from the start.
        // This connects the start point to the beginning of the node network segment.
        val firstNodePos = nodePath.first().position
        if (start.x != firstNodePos.x || start.y != firstNodePos.y) {
            finalPathPoints.add(firstNodePos)
            println("  ➡️ Path: Added first A* node (${firstNodePos.x}, ${firstNodePos.y})")
        }

        // 3. Add all intermediate node positions from the A* path.
        // Skip the first node if it was already added (i.e., if it wasn't the same as raw start).
        val startIndexForLoop = if (finalPathPoints.last() == firstNodePos) 1 else 0
        for (i in startIndexForLoop until nodePath.size) {
            val currentNodePos = nodePath[i].position
            // Only add if different from the last point to avoid micro-duplicates from start/first node handling
            if (finalPathPoints.isEmpty() || finalPathPoints.last().x != currentNodePos.x || finalPathPoints.last().y != currentNodePos.y) {
                finalPathPoints.add(currentNodePos)
                println("  ➡️ Path: Added A* node ${nodePath[i].id} (${currentNodePos.x}, ${currentNodePos.y})")
            }
        }

        // 4. Add the position of the last node in the A* path IF it's different from the current last point in our path.
        // This ensures the end of the node network segment is explicitly included.
        val lastNodePos = nodePath.last().position
        if (finalPathPoints.last().x != lastNodePos.x || finalPathPoints.last().y != lastNodePos.y) {
            // This check is a bit redundant if the loop correctly added the last node,
            // but ensures the last A* node is present before connecting to the raw goal.
            finalPathPoints.add(lastNodePos)
            println("  ➡️ Path: Added last A* node (explicit) (${lastNodePos.x}, ${lastNodePos.y})")
        }

        // 5. Finally, add the original goal position, IF it's different from the last node added.
        if (finalPathPoints.last().x != goal.x || finalPathPoints.last().y != goal.y) {
            finalPathPoints.add(goal)
            println("  ➡️ Path: Added raw goal (${goal.x}, ${goal.y})")
        }

        println("  ➡️ Path: Final points before simplify: ${finalPathPoints.map { "(${it.x},${it.y})" }}")
        // Apply a very conservative simplification just to remove truly redundant points.
        return simplifyPathBasic(finalPathPoints)
    }

    // Formerly simplifyPath, now a more basic version.
    // The main goal is to remove only truly redundant consecutive points.
    private fun simplifyPathBasic(path: List<Position>): List<Position> {
        if (path.size < 2) return path
        val simplifiedPath = mutableListOf<Position>()
        simplifiedPath.add(path.first())
        for (i in 1 until path.size) {
            // Only add point if it's different from the last one added.
            // This removes consecutive duplicates but keeps all turns.
            if (path[i].x != simplifiedPath.last().x || path[i].y != simplifiedPath.last().y) {
                simplifiedPath.add(path[i])
            }
        }
        println("  ➡️ Path: Final points after simplifyBasic: ${simplifiedPath.map { "(${it.x},${it.y})" }}")
        return simplifiedPath
    }

    // The old simplifyPath logic, temporarily disabled by not being called.
    private fun simplifyPathAdvanced(path: List<Position>): List<Position> {
        if (path.size < 2) return path
        val simplifiedPath = mutableListOf<Position>()
        simplifiedPath.add(path.first())
        for (i in 1 until path.size - 1) {
            val p1 = simplifiedPath.last()
            val p2 = path[i]
            val p3 = path[i + 1]
            val collinear = (p2.y - p1.y) * (p3.x - p2.x) == (p3.y - p2.y) * (p2.x - p1.x)
            if (!collinear || i == path.size - 2) {
                if (!(p1.x == p2.x && p1.y == p2.y)) {
                    simplifiedPath.add(p2)
                 }
            }
        }
        if (!(simplifiedPath.last().x == path.last().x && simplifiedPath.last().y == path.last().y)) {
            simplifiedPath.add(path.last())
        }

        return simplifiedPath.distinctBy { "${it.x.toInt()}-${it.y.toInt()}" }
    }

    private fun findMultiFloorPath(
        start: Position,
        goal: Position,
        floorPlans: Map<Int, FloorPlan>
    ): NavigationPath {
        val steps = mutableListOf<NavigationStep>()

        val startFloorPlan = floorPlans[start.floor]!!
        val transitions = startFloorPlan.nodes.filter {
            it.type == NodeType.ELEVATOR || it.type == NodeType.STAIRS
        }

        val nearestTransition = transitions.minByOrNull {
            distance(start, it.position)
        } ?: throw IllegalStateException("No floor transition found")

        val pathToTransition =
            findSingleFloorPath(start, nearestTransition.position, startFloorPlan)
        steps.addAll(pathToTransition.map { NavigationStep.Move(it) })

        steps.add(
            NavigationStep.FloorChange(
                from = start.floor,
                to = goal.floor,
                via = if (nearestTransition.type == NodeType.ELEVATOR) "Elevator" else "Stairs"
            )
        )

        val goalFloorPlan = floorPlans[goal.floor]!!
        val goalTransition = goalFloorPlan.nodes.find {
            it.position.x == nearestTransition.position.x &&
                    it.position.y == nearestTransition.position.y
        } ?: throw IllegalStateException("Corresponding transition not found")

        val pathFromTransition = findSingleFloorPath(goalTransition.position, goal, goalFloorPlan)
        steps.addAll(pathFromTransition.map { NavigationStep.Move(it) })

        return NavigationPath(steps)
    }

    private fun distance(a: Position, b: Position): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun heuristic(a: Position, b: Position): Float {
        // Use Manhattan distance for corridor-like pathfinding
        return abs(a.x - b.x) + abs(a.y - b.y)
    }

    private fun reconstructPath(node: AStarNode): List<NavNode> {
        val path = mutableListOf<NavNode>()
        var current: AStarNode? = node
        while (current != null) {
            path.add(0, current.node)
            current = current.parent
        }
        return path
    }

    private data class AStarNode(
        val node: NavNode,
        val gCost: Float,
        val hCost: Float,
        val parent: AStarNode? = null
    ) {
        val fCost: Float get() = gCost + hCost
    }
}

data class NavigationPath(
    val steps: List<NavigationStep>,
    val totalDistance: Float = steps.filterIsInstance<NavigationStep.Move>()
        .windowed(2).sumOf { (a, b) ->
            sqrt((b.position.x - a.position.x).let { it * it } +
                    (b.position.y - a.position.y).let { it * it }).toDouble()
        }.toFloat()
)

sealed class NavigationStep {
    data class Move(val position: Position) : NavigationStep()
    data class FloorChange(val from: Int, val to: Int, val via: String) : NavigationStep()
    data class Turn(val direction: TurnDirection, val angle: Float) : NavigationStep()
}

enum class TurnDirection { LEFT, RIGHT, STRAIGHT }
