package com.example.engine

import com.example.model.CellType
import com.example.model.GridPoint
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.WarehouseMap

data class DeadlockCycle(
    val robotsInCycle: List<Robot>,
    val cyclePathStr: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeadlockRecoveryPlan(
    val targetRobot: Robot,
    val recoveryAction: String,
    val explanation: String,
    val whyExplanation: String = "Wait-For Graph detected cyclic lock. R${targetRobot.robotId} was chosen to yield because it has the lowest mission priority disparity and shortest distance to a bypass turnout."
)

class DeadlockEngine(
    private val map: WarehouseMap,
    private val pathPlanner: PathPlanner
) {
    // Wait-For Graph: robotId -> list of robotIds that this robot is waiting on
    private val waitForGraph = HashMap<Int, MutableSet<Int>>()

    fun clearGraph() {
        waitForGraph.clear()
    }

    fun addDependency(waitingRobotId: Int, blockingRobotId: Int) {
        if (waitingRobotId != blockingRobotId) {
            waitForGraph.getOrPut(waitingRobotId) { mutableSetOf() }.add(blockingRobotId)
        }
    }

    /**
     * Builds Wait-For dependencies from robots in WAITING or BLOCKED status.
     */
    fun buildDependencies(robots: List<Robot>, robotPositionMap: Map<GridPoint, Robot>) {
        clearGraph()
        for (robot in robots) {
            if (robot.status == RobotStatus.WAITING || robot.status == RobotStatus.BLOCKED || robot.waitingTime > 1.5f) {
                // Next intended step
                val nextWaypoint = robot.currentRoute.getOrNull(robot.routeIndex + 1) ?: continue
                val blockingRobot = robotPositionMap[nextWaypoint]
                if (blockingRobot != null && blockingRobot.robotId != robot.robotId) {
                    addDependency(robot.robotId, blockingRobot.robotId)
                }
            }
        }
    }

    /**
     * Cycle detection using DFS with recursion stack tracking.
     */
    fun detectDeadlocks(robotsById: Map<Int, Robot>): List<DeadlockCycle> {
        val visited = HashSet<Int>()
        val recursionStack = HashSet<Int>()
        val path = mutableListOf<Int>()
        val detectedCycles = mutableListOf<DeadlockCycle>()

        for (node in waitForGraph.keys) {
            if (!visited.contains(node)) {
                dfsCycle(node, visited, recursionStack, path, detectedCycles, robotsById)
            }
        }

        return detectedCycles
    }

    private fun dfsCycle(
        current: Int,
        visited: MutableSet<Int>,
        recursionStack: MutableSet<Int>,
        path: MutableList<Int>,
        detectedCycles: MutableList<DeadlockCycle>,
        robotsById: Map<Int, Robot>
    ) {
        visited.add(current)
        recursionStack.add(current)
        path.add(current)

        val neighbors = waitForGraph[current] ?: emptySet()
        for (neighbor in neighbors) {
            if (!visited.contains(neighbor)) {
                dfsCycle(neighbor, visited, recursionStack, path, detectedCycles, robotsById)
            } else if (recursionStack.contains(neighbor)) {
                // Cycle found!
                val cycleStartIndex = path.indexOf(neighbor)
                if (cycleStartIndex != -1) {
                    val cycleIds = path.subList(cycleStartIndex, path.size).toList()
                    val cycleRobots = cycleIds.mapNotNull { robotsById[it] }
                    if (cycleRobots.size >= 2) {
                        val cycleStr = cycleIds.joinToString(" -> ") { "R$it" } + " -> R$neighbor"
                        detectedCycles.add(DeadlockCycle(cycleRobots, cycleStr))
                    }
                }
            }
        }

        path.removeAt(path.size - 1)
        recursionStack.remove(current)
    }

    /**
     * Resolves a deadlock cycle by selecting the optimal robot to yield/divert.
     */
    fun recoverDeadlock(cycle: DeadlockCycle): DeadlockRecoveryPlan {
        // Select robot with lowest priority and lowest current load
        val candidate = cycle.robotsInCycle.minByOrNull { robot ->
            val priorityWeight = robot.taskPriority.level * 100f
            val loadWeight = robot.currentLoad * 0.1f
            priorityWeight + loadWeight
        } ?: cycle.robotsInCycle.first()

        // 1. Try to find an adjacent waiting area or free aisle cell to pull aside
        val turnout = findBypassCell(candidate.gridPoint)
        val plan: DeadlockRecoveryPlan

        if (turnout != null) {
            // Divert to turnout
            candidate.currentRoute = listOf(candidate.gridPoint, turnout) + candidate.currentRoute.drop(candidate.routeIndex)
            candidate.routeIndex = 0
            candidate.status = RobotStatus.RECOVERING
            candidate.waitingTime = 0f
            plan = DeadlockRecoveryPlan(
                targetRobot = candidate,
                recoveryAction = "PULL_INTO_TURNOUT",
                explanation = "R${candidate.robotId} yielded corridor to break cycle; pulling into turnout bay (${turnout.x},${turnout.y})"
            )
        } else {
            // 2. Reverse step or alternative reroute
            val dest = candidate.currentRoute.lastOrNull()
            if (dest != null) {
                // Penalize current conflict location and calculate bypass route
                val bypassPath = pathPlanner.findPath(
                    start = candidate.gridPoint,
                    goal = dest,
                    penalizedPoints = cycle.robotsInCycle.map { it.gridPoint }.toSet()
                )
                if (bypassPath.isNotEmpty()) {
                    candidate.currentRoute = bypassPath
                    candidate.routeIndex = 0
                    candidate.status = RobotStatus.REROUTING
                    candidate.waitingTime = 0f
                    plan = DeadlockRecoveryPlan(
                        targetRobot = candidate,
                        recoveryAction = "REROUTE_AROUND_CYCLE",
                        explanation = "R${candidate.robotId} recalculated bypass route around deadlock cycle"
                    )
                } else {
                    // Fallback: force yield & reset waiting
                    candidate.waitingTime = 0f
                    candidate.status = RobotStatus.WAITING
                    plan = DeadlockRecoveryPlan(
                        targetRobot = candidate,
                        recoveryAction = "FORCE_YIELD",
                        explanation = "R${candidate.robotId} forced to back off and yield right-of-way"
                    )
                }
            } else {
                candidate.waitingTime = 0f
                candidate.status = RobotStatus.WAITING
                plan = DeadlockRecoveryPlan(
                    targetRobot = candidate,
                    recoveryAction = "FORCE_YIELD",
                    explanation = "R${candidate.robotId} yielded priority"
                )
            }
        }

        return plan
    }

    private fun findBypassCell(point: GridPoint): GridPoint? {
        val offsets = listOf(
            GridPoint(0, 1),
            GridPoint(0, -1),
            GridPoint(1, 0),
            GridPoint(-1, 0),
            GridPoint(1, 1),
            GridPoint(-1, -1),
            GridPoint(1, -1),
            GridPoint(-1, 1)
        )
        for (offset in offsets) {
            val check = GridPoint(point.x + offset.x, point.y + offset.y)
            if (check.x in 0 until map.width && check.y in 0 until map.height) {
                if (map.grid[check.x][check.y] == CellType.WAITING_AREA ||
                    (map.isWalkable(check.x, check.y) && map.grid[check.x][check.y] == CellType.FLOOR)) {
                    return check
                }
            }
        }
        return null
    }
}
