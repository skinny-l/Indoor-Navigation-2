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
            // If no navigation nodes, return direct path (fallback)
            return listOf(start, goal)
        }

        val startNode = findNearestNode(start, floorPlan.nodes)
        val goalNode = findNearestNode(goal, floorPlan.nodes)

        println("🗺️ Start node: ${startNode.id} at (${startNode.position.x}, ${startNode.position.y})")
        println("🗺️ Goal node: ${goalNode.id} at (${goalNode.position.x}, ${goalNode.position.y})")

        val openList = PriorityQueue<AStarNode>(compareBy { it.fCost })
        val closedList = mutableSetOf<String>()
        val nodeMap = floorPlan.nodes.associateBy { it.id }

        openList.add(
            AStarNode(
                node = startNode,
                gCost = 0f,
                hCost = heuristic(startNode.position, goal),
                parent = null
            )
        )

        while (openList.isNotEmpty()) {
            val current = openList.poll()
            closedList.add(current.node.id)

            if (current.node.id == goalNode.id) {
                val nodePath = reconstructPath(current)
                println("✅ Path found through ${nodePath.size} nodes: ${nodePath.map { it.id }}")
                return buildFullPath(start, goal, nodePath)
            }

            for (neighborId in current.node.connections) {
                if (neighborId in closedList) continue

                val neighbor = nodeMap[neighborId] ?: continue
                val tentativeGCost =
                    current.gCost + distance(current.node.position, neighbor.position)

                val existingNode = openList.find { it.node.id == neighborId }
                if (existingNode == null || tentativeGCost < existingNode.gCost) {
                    openList.remove(existingNode)
                    openList.add(
                        AStarNode(
                            node = neighbor,
                            gCost = tentativeGCost,
                            hCost = heuristic(neighbor.position, goal),
                            parent = current
                        )
                    )
                }
            }
        }

        println("❌ No path found through nodes, using direct path")
        // If no path found through nodes, return direct path
        return listOf(start, goal)
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

    private fun buildFullPath(
        start: Position,
        goal: Position,
        nodePath: List<NavNode>
    ): List<Position> {
        val fullPath = mutableListOf<Position>()

        // Add start position
        fullPath.add(start)

        // Add all node positions
        fullPath.addAll(nodePath.map { it.position })

        // Add goal position if it's different from the last node
        if (nodePath.isEmpty() || distance(nodePath.last().position, goal) > 5f) {
            fullPath.add(goal)
        }

        return fullPath
    }

    private fun findNearestNode(position: Position, nodes: List<NavNode>): NavNode {
        return nodes.minByOrNull { distance(position, it.position) }
            ?: throw IllegalArgumentException("No nodes available")
    }

    private fun distance(a: Position, b: Position): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun heuristic(a: Position, b: Position): Float {
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
