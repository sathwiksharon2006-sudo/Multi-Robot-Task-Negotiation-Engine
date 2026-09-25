package com.example.engine

import com.example.model.CellType
import com.example.model.GridPoint
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.WarehouseMap
import kotlin.math.abs
import kotlin.math.sqrt

enum class CollisionType(val description: String) {
    HEAD_ON("Head-On Conflict"),
    REAR_END("Rear-End Overlap"),
    INTERSECTION("Intersection Crossing Conflict"),
    CROSSING_PATHS("Path Trajectory Intersection"),
    SAME_DESTINATION("Contested Destination Cell"),
    NARROW_CORRIDOR("Narrow Corridor Bottleneck"),
    STATION_CONGESTION("Dock / Station Queuing Congestion")
}

data class CollisionRisk(
    val id: String,
    val robotA: Robot,
    val robotB: Robot,
    val collisionType: CollisionType,
    val conflictLocation: GridPoint,
    val timeToCollisionSec: Float,
    val severity: Float, // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis()
)

class CollisionPredictor(
    private val warehouseMap: WarehouseMap,
    private val spatialGrid: SpatialHashGrid,
    var predictionHorizonSec: Float = 5.0f
) {
    fun predictCollisions(robots: List<Robot>): List<CollisionRisk> {
        val detectedRisks = mutableListOf<CollisionRisk>()
        val evaluatedPairs = mutableSetOf<Long>()

        for (i in robots.indices) {
            val r1 = robots[i]
            if (r1.status == RobotStatus.FAILED || r1.status == RobotStatus.CHARGING) continue

            // Query spatial neighbors within 6 units
            val neighbors = spatialGrid.getNearbyRobots(r1.x, r1.y, 6.0f)
            for (r2 in neighbors) {
                if (r1.robotId >= r2.robotId) continue // Avoid duplicate pair check
                if (r2.status == RobotStatus.FAILED || r2.status == RobotStatus.CHARGING) continue

                val pairKey = (r1.robotId.toLong() shl 32) or (r2.robotId.toLong() and 0xFFFFFFFFL)
                if (evaluatedPairs.contains(pairKey)) continue
                evaluatedPairs.add(pairKey)

                val risk = checkPairConflict(r1, r2)
                if (risk != null) {
                    detectedRisks.add(risk)
                }
            }
        }

        return detectedRisks
    }

    private fun checkPairConflict(r1: Robot, r2: Robot): CollisionRisk? {
        val dx = r2.x - r1.x
        val dy = r2.y - r1.y
        val dist = sqrt(dx * dx + dy * dy)

        val r1Vx = r1.targetX - r1.x
        val r1Vy = r1.targetY - r1.y
        val r2Vx = r2.targetX - r2.x
        val r2Vy = r2.targetY - r2.y

        // Check 1: Same destination conflict
        val r1Goal = r1.currentRoute.lastOrNull()
        val r2Goal = r2.currentRoute.lastOrNull()
        if (r1Goal != null && r2Goal != null && r1Goal == r2Goal && dist < 4.0f) {
            return CollisionRisk(
                id = "DEST-${r1.robotId}-${r2.robotId}",
                robotA = r1,
                robotB = r2,
                collisionType = CollisionType.SAME_DESTINATION,
                conflictLocation = r1Goal,
                timeToCollisionSec = dist / ((r1.speed + r2.speed) / 2f).coerceAtLeast(0.1f),
                severity = 0.85f
            )
        }

        // Check 2: Head-on in narrow corridor
        val r1Point = r1.gridPoint
        val r2Point = r2.gridPoint
        val isNarrow = warehouseMap.grid[r1Point.x.coerceIn(0, warehouseMap.width - 1)][r1Point.y.coerceIn(0, warehouseMap.height - 1)] == CellType.NARROW_CORRIDOR ||
                warehouseMap.grid[r2Point.x.coerceIn(0, warehouseMap.width - 1)][r2Point.y.coerceIn(0, warehouseMap.height - 1)] == CellType.NARROW_CORRIDOR

        val dotProduct = (r1Vx * r2Vx + r1Vy * r2Vy)
        val isOpposite = dotProduct < -0.2f

        if (isNarrow && dist < 5.0f && isOpposite) {
            return CollisionRisk(
                id = "NARROW-${r1.robotId}-${r2.robotId}",
                robotA = r1,
                robotB = r2,
                collisionType = CollisionType.NARROW_CORRIDOR,
                conflictLocation = r1Point,
                timeToCollisionSec = dist / (r1.speed + r2.speed).coerceAtLeast(0.1f),
                severity = 0.95f
            )
        }

        // Check 3: Head-on anywhere
        if (dist < 3.0f && isOpposite) {
            return CollisionRisk(
                id = "HEADON-${r1.robotId}-${r2.robotId}",
                robotA = r1,
                robotB = r2,
                collisionType = CollisionType.HEAD_ON,
                conflictLocation = GridPoint((r1.x + r2.x).toInt() / 2, (r1.y + r2.y).toInt() / 2),
                timeToCollisionSec = dist / (r1.speed + r2.speed).coerceAtLeast(0.1f),
                severity = 0.9f
            )
        }

        // Check 4: Intersection / Crossing Trajectory
        // Look ahead 3 route waypoints
        val r1Ahead = r1.currentRoute.drop(r1.routeIndex).take(5)
        val r2Ahead = r2.currentRoute.drop(r2.routeIndex).take(5)

        for (step1 in r1Ahead.indices) {
            val p1 = r1Ahead[step1]
            for (step2 in r2Ahead.indices) {
                val p2 = r2Ahead[step2]
                if (p1 == p2 && abs(step1 - step2) <= 2) {
                    val isIntersection = warehouseMap.grid[p1.x.coerceIn(0, warehouseMap.width - 1)][p1.y.coerceIn(0, warehouseMap.height - 1)] == CellType.INTERSECTION
                    val conflictType = if (isIntersection) CollisionType.INTERSECTION else CollisionType.CROSSING_PATHS
                    val timeEst = (step1 + step2) * 0.8f
                    if (timeEst <= predictionHorizonSec) {
                        return CollisionRisk(
                            id = "CROSS-${r1.robotId}-${r2.robotId}",
                            robotA = r1,
                            robotB = r2,
                            collisionType = conflictType,
                            conflictLocation = p1,
                            timeToCollisionSec = timeEst,
                            severity = if (isIntersection) 0.8f else 0.7f
                        )
                    }
                }
            }
        }

        // Check 5: Rear-end (trailing robot moving faster along same direction)
        if (dist < 1.8f && dotProduct > 0.4f) {
            return CollisionRisk(
                id = "REAREND-${r1.robotId}-${r2.robotId}",
                robotA = r1,
                robotB = r2,
                collisionType = CollisionType.REAR_END,
                conflictLocation = r2Point,
                timeToCollisionSec = dist / abs(r1.speed - r2.speed).coerceAtLeast(0.1f),
                severity = 0.65f
            )
        }

        return null
    }
}
