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
        println("🗺️ Available nodes: ${floorPlan.nodes.size}, Walls: ${floorPlan.walls.size}")

        // Print all available navigation nodes for debugging
        println("🗺️ Navigation nodes:")
        floorPlan.nodes.forEach { node ->
            println("   - ${node.id}: (${node.position.x}, ${node.position.y}) connections: ${node.connections}")
        }

        // FORCE node-based routing when user has placed nodes
        if (floorPlan.nodes.isNotEmpty()) {
            println("🗺️ User has placed ${floorPlan.nodes.size} nodes - forcing node-based routing")

            // Filter walkable nodes only
            val walkableNodes =
                floorPlan.nodes.filter { it.isWalkable && it.type != NodeType.OBSTACLE }
            println("🗺️ Walkable nodes: ${walkableNodes.size}")

            // Always try to snap to nodes when they exist
            val startNode = findNearestWalkableNodeIfClose(start, walkableNodes, maxDistance = 200f)
            val goalNode = findNearestWalkableNodeIfClose(goal, walkableNodes, maxDistance = 200f)

            println("🗺️ Start node search: looking for node near (${start.x}, ${start.y})")
            walkableNodes.forEach { node ->
                val dist = distance(start, node.position)
                println("   - ${node.id} at (${node.position.x}, ${node.position.y}) distance: ${dist}")
            }

            println("🗺️ Goal node search: looking for node near (${goal.x}, ${goal.y})")
            walkableNodes.forEach { node ->
                val dist = distance(goal, node.position)
                println("   - ${node.id} at (${node.position.x}, ${node.position.y}) distance: ${dist}")
            }

            // If we found nodes, use them for routing
            if (startNode != null && goalNode != null) {
                val nodePath =
                    findPathThroughNodes(startNode, goalNode, walkableNodes, floorPlan.walls)
                if (nodePath.isNotEmpty()) {
                    println("✅ Path found through ${nodePath.size} nodes: ${nodePath.map { it.id }}")
                    return buildWalkableCorridorPath(start, goal, nodePath, floorPlan.walls)
                }
                println("❌ No node path from ${startNode.id} to ${goalNode.id}")
            }

            // Enhanced fallback with wall-aware pathfinding
            return findPathWithObstacleAvoidance(start, goal, walkableNodes, floorPlan.walls)
        }

        // No nodes available - use basic obstacle avoidance
        println("⚠️ No navigation nodes found, attempting basic obstacle avoidance")
        return findPathWithBasicObstacleAvoidance(start, goal, floorPlan.walls)
    }

    private fun isPathWalkable(start: Position, end: Position, walls: List<Wall>): Boolean {
        if (walls.isEmpty()) return true

        for (wall in walls) {
            if (lineIntersectsWall(start, end, wall)) {
                return false
            }
        }
        return true
    }

    private fun lineIntersectsWall(start: Position, end: Position, wall: Wall): Boolean {
        // Check if line segment from start to end intersects with the wall
        val x1 = start.x
        val y1 = start.y
        val x2 = end.x
        val y2 = end.y

        val x3 = wall.startPosition.x
        val y3 = wall.startPosition.y
        val x4 = wall.endPosition.x
        val y4 = wall.endPosition.y

        val denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (abs(denominator) < 1e-10) return false // Lines are parallel

        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator
        val u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator

        return t >= 0 && t <= 1 && u >= 0 && u <= 1
    }

    private fun findPathWithBasicObstacleAvoidance(
        start: Position,
        goal: Position,
        walls: List<Wall>
    ): List<Position> {
        // Simple obstacle avoidance by finding intermediate points
        val intermediatePoints = mutableListOf<Position>()

        // Try waypoints around major obstacles
        val midX = (start.x + goal.x) / 2
        val midY = (start.y + goal.y) / 2

        val candidates = listOf(
            Position(midX, start.y, start.floor),
            Position(goal.x, midY, start.floor),
            Position(start.x, midY, start.floor),
            Position(midX, goal.y, start.floor)
        )

        for (candidate in candidates) {
            if (isPathWalkable(start, candidate, walls) && isPathWalkable(candidate, goal, walls)) {
                return listOf(start, candidate, goal)
            }
        }

        // If no simple path found, return direct path as fallback
        println("⚠️ No walkable path found, using direct path as fallback")
        return listOf(start, goal)
    }

    private fun findPathWithObstacleAvoidance(
        start: Position,
        goal: Position,
        walkableNodes: List<NavNode>,
        walls: List<Wall>
    ): List<Position> {
        val pathPoints = mutableListOf<Position>()
        pathPoints.add(start)

        // Find nearest walkable nodes for routing
        val startNode = findNearestWalkableNodeIfClose(start, walkableNodes, maxDistance = 50f)
        val goalNode = findNearestWalkableNodeIfClose(goal, walkableNodes, maxDistance = 50f)

        if (startNode != null && isPathWalkable(start, startNode.position, walls)) {
            pathPoints.add(startNode.position)

            if (goalNode != null && startNode.id != goalNode.id) {
                // Try to find path between nodes
                val nodePath = findPathThroughNodes(startNode, goalNode, walkableNodes, walls)
                if (nodePath.isNotEmpty()) {
                    // Add intermediate nodes (skip first as it's already added)
                    pathPoints.addAll(nodePath.drop(1).map { it.position })
                } else if (isPathWalkable(startNode.position, goalNode.position, walls)) {
                    pathPoints.add(goalNode.position)
                }
            }

            if (goalNode != null && isPathWalkable(pathPoints.last(), goalNode.position, walls)) {
                if (pathPoints.last() != goalNode.position) {
                    pathPoints.add(goalNode.position)
                }
            }
        }

        // Ensure we end at the goal if path is walkable
        if (isPathWalkable(pathPoints.last(), goal, walls)) {
            pathPoints.add(goal)
        } else {
            // Try basic obstacle avoidance for the final segment
            val avoidancePath = findPathWithBasicObstacleAvoidance(pathPoints.last(), goal, walls)
            pathPoints.addAll(avoidancePath.drop(1)) // Skip first point as it's already in pathPoints
        }

        return pathPoints.distinctBy { "${it.x.toInt()},${it.y.toInt()}" }
    }

    private fun findNearestWalkableNodeIfClose(
        position: Position,
        walkableNodes: List<NavNode>,
        maxDistance: Float
    ): NavNode? {
        val nearestNode = walkableNodes
            .filter { it.type == NodeType.WALKWAY || it.type == NodeType.DOOR }
            .minByOrNull { distance(position, it.position) }

        if (nearestNode != null && distance(position, nearestNode.position) <= maxDistance) {
            return nearestNode
        }
        return null
    }

    private fun findPathThroughNodes(
        startNode: NavNode,
        goalNode: NavNode,
        allNodes: List<NavNode>,
        walls: List<Wall>
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

            // Only traverse through explicitly connected walkable nodes
            for (neighborId in current.node.connections) {
                if (neighborId in closedList) continue

                val neighbor = nodeMap[neighborId] ?: continue

                // Skip non-walkable neighbors or obstacles
                if (!neighbor.isWalkable || neighbor.type == NodeType.OBSTACLE) continue

                // Verify the path between nodes is walkable (no wall intersections)
                if (!isPathWalkable(current.node.position, neighbor.position, walls)) continue

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

        return emptyList() // No walkable path found through corridor network
    }

    private fun calculateCorridorDistance(nodeA: NavNode, nodeB: NavNode): Float {
        // Calculate Manhattan distance to prefer corridor-like paths
        val dx = kotlin.math.abs(nodeA.position.x - nodeB.position.x)
        val dy = kotlin.math.abs(nodeA.position.y - nodeB.position.y)
        return dx + dy
    }

    private fun buildWalkableCorridorPath(
        start: Position,
        goal: Position,
        nodePath: List<NavNode>,
        walls: List<Wall>
    ): List<Position> {
        val finalPathPoints = mutableListOf<Position>()

        if (nodePath.isEmpty()) {
            println("⚠️ buildWalkableCorridorPath: nodePath is empty. Falling back to obstacle avoidance.")
            return findPathWithBasicObstacleAvoidance(start, goal, walls)
        }

        // Start with the original start position
        finalPathPoints.add(start)
        println("  ➡️ Path: Added raw start (${start.x}, ${start.y})")

        // Add path to first node if walkable
        val firstNodePos = nodePath.first().position
        if (start.x != firstNodePos.x || start.y != firstNodePos.y) {
            if (isPathWalkable(start, firstNodePos, walls)) {
                finalPathPoints.add(firstNodePos)
                println("  ➡️ Path: Added first A* node (${firstNodePos.x}, ${firstNodePos.y})")
            } else {
                println("  ⚠️ Path to first node blocked, finding alternative")
                val alternativePath = findPathWithBasicObstacleAvoidance(start, firstNodePos, walls)
                finalPathPoints.addAll(alternativePath.drop(1)) // Skip start position
            }
        }

        // Add all node positions from the A* path, ensuring each segment is walkable
        for (i in 1 until nodePath.size) {
            val currentNodePos = nodePath[i].position
            val lastPos = finalPathPoints.last()

            if (isPathWalkable(lastPos, currentNodePos, walls)) {
                finalPathPoints.add(currentNodePos)
                println("  ➡️ Path: Added A* node ${nodePath[i].id} (${currentNodePos.x}, ${currentNodePos.y})")
            } else {
                println("  ⚠️ Path segment blocked, attempting detour")
                // Try to find alternative path to this node
                val detourPath = findPathWithBasicObstacleAvoidance(lastPos, currentNodePos, walls)
                finalPathPoints.addAll(detourPath.drop(1)) // Skip the starting position
            }
        }

        // Add path to final goal if walkable
        val lastNodePos = finalPathPoints.last()
        if (lastNodePos.x != goal.x || lastNodePos.y != goal.y) {
            if (isPathWalkable(lastNodePos, goal, walls)) {
                finalPathPoints.add(goal)
                println("  ➡️ Path: Added raw goal (${goal.x}, ${goal.y})")
            } else {
                println("  ⚠️ Path to goal blocked, finding alternative")
                val finalPath = findPathWithBasicObstacleAvoidance(lastNodePos, goal, walls)
                finalPathPoints.addAll(finalPath.drop(1)) // Skip starting position
            }
        }

        println("  ➡️ Path: Final walkable points: ${finalPathPoints.map { "(${it.x},${it.y})" }}")
        return simplifyWalkablePath(finalPathPoints, walls)
    }

    private fun simplifyWalkablePath(path: List<Position>, walls: List<Wall>): List<Position> {
        if (path.size < 3) return path

        val simplifiedPath = mutableListOf<Position>()
        simplifiedPath.add(path.first())

        var i = 0
        while (i < path.size - 1) {
            var farthestReachable = i + 1

            // Find the farthest point we can reach directly without hitting walls
            for (j in i + 2 until path.size) {
                if (isPathWalkable(path[i], path[j], walls)) {
                    farthestReachable = j
                } else {
                    break
                }
            }

            // Add the farthest reachable point
            if (farthestReachable > i + 1) {
                simplifiedPath.add(path[farthestReachable])
                i = farthestReachable
            } else {
                simplifiedPath.add(path[i + 1])
                i++
            }
        }

        // Ensure we have the final destination
        if (simplifiedPath.last() != path.last()) {
            simplifiedPath.add(path.last())
        }

        println("  ➡️ Path: Simplified walkable path: ${simplifiedPath.map { "(${it.x},${it.y})" }}")
        return simplifiedPath
    }

    private fun findMultiFloorPath(
        start: Position,
        goal: Position,
        floorPlans: Map<Int, FloorPlan>
    ): NavigationPath {
        val steps = mutableListOf<NavigationStep>()

        val startFloorPlan = floorPlans[start.floor]!!
        val transitions = startFloorPlan.nodes.filter {
            (it.type == NodeType.ELEVATOR || it.type == NodeType.STAIRS) && it.isWalkable
        }

        val nearestTransition = transitions.minByOrNull {
            distance(start, it.position)
        } ?: throw IllegalStateException("No walkable floor transition found")

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
                    it.position.y == nearestTransition.position.y &&
                    it.isWalkable
        } ?: throw IllegalStateException("Corresponding walkable transition not found")

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
