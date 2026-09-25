package com.example.engine

import com.example.model.CellType
import com.example.model.GridPoint
import com.example.model.WarehouseMap
import java.util.PriorityQueue
import kotlin.math.abs

class PathPlanner(
    private val map: WarehouseMap
) {
    private data class Node(
        val point: GridPoint,
        val gCost: Float,
        val hCost: Float,
        val parent: Node? = null
    ) : Comparable<Node> {
        val fCost: Float get() = gCost + hCost
        override fun compareTo(other: Node): Int = fCost.compareTo(other.fCost)
    }

    private val neighborsOffsets = arrayOf(
        GridPoint(1, 0),
        GridPoint(-1, 0),
        GridPoint(0, 1),
        GridPoint(0, -1)
    )

    fun findPath(
        start: GridPoint,
        goal: GridPoint,
        penalizedPoints: Set<GridPoint> = emptySet(),
        avoidTemporaryBlocked: Boolean = true
    ): List<GridPoint> {
        if (start == goal) return listOf(start)
        if (!map.isWalkable(goal.x, goal.y)) return emptyList()

        val openSet = PriorityQueue<Node>()
        val closedSet = HashSet<GridPoint>()
        val gCosts = HashMap<GridPoint, Float>()

        val startNode = Node(start, 0f, heuristic(start, goal))
        openSet.add(startNode)
        gCosts[start] = 0f

        var iterations = 0
        val maxIterations = 2500

        while (openSet.isNotEmpty() && iterations < maxIterations) {
            iterations++
            val current = openSet.poll() ?: break

            if (current.point == goal) {
                return reconstructPath(current)
            }

            closedSet.add(current.point)

            for (offset in neighborsOffsets) {
                val nextPoint = GridPoint(current.point.x + offset.x, current.point.y + offset.y)

                if (closedSet.contains(nextPoint)) continue
                if (!map.isWalkable(nextPoint.x, nextPoint.y)) continue

                // Check extra penalty if cell is temporarily congested or blocked by another robot
                var moveCost = 1.0f
                if (penalizedPoints.contains(nextPoint)) {
                    moveCost += 25.0f
                }
                if (map.grid[nextPoint.x][nextPoint.y] == CellType.NARROW_CORRIDOR) {
                    moveCost += 0.5f // Slight penalty to prefer wide aisles unless necessary
                }

                val tentativeGCost = current.gCost + moveCost
                val currentGCost = gCosts[nextPoint] ?: Float.MAX_VALUE

                if (tentativeGCost < currentGCost) {
                    gCosts[nextPoint] = tentativeGCost
                    val nextNode = Node(nextPoint, tentativeGCost, heuristic(nextPoint, goal), current)
                    openSet.add(nextNode)
                }
            }
        }

        return emptyList() // No path found
    }

    private fun heuristic(a: GridPoint, b: GridPoint): Float {
        return (abs(a.x - b.x) + abs(a.y - b.y)).toFloat()
    }

    private fun reconstructPath(node: Node): List<GridPoint> {
        val path = mutableListOf<GridPoint>()
        var curr: Node? = node
        while (curr != null) {
            path.add(curr.point)
            curr = curr.parent
        }
        path.reverse()
        return path
    }
}
