package com.example.engine

import com.example.model.CellType
import com.example.model.GridPoint
import com.example.model.NegotiationState
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.TaskPriority
import com.example.model.WarehouseMap
import kotlin.math.min

enum class ResolutionAction {
    YIELD_AND_WAIT,
    PULL_INTO_TURNOUT,
    REVERSE_STEP,
    REROUTE_ALTERNATIVE
}

data class NegotiationResult(
    val winner: Robot,
    val loser: Robot,
    val winnerScore: Float,
    val loserScore: Float,
    val action: ResolutionAction,
    val conflictLocation: GridPoint,
    val explanation: String,
    val whyExplanation: String
)

class NegotiationEngine(
    private val map: WarehouseMap,
    private val pathPlanner: PathPlanner
) {
    /**
     * Calculates dynamic right-of-way score.
     * Incorporates starvation prevention via waiting time escalation.
     */
    fun calculateRightOfWayScore(robot: Robot): Float {
        var score = 100f

        // 1. Task Priority bonus
        when (robot.taskPriority) {
            TaskPriority.CRITICAL -> score += 300f
            TaskPriority.HIGH -> score += 150f
            TaskPriority.NORMAL -> score += 50f
            TaskPriority.LOW -> score += 10f
        }

        // 2. Battery Urgency (robots low on power cannot afford to wait)
        if (robot.battery < 15f) {
            score += 250f
        } else if (robot.battery < 30f) {
            score += 100f
        }

        // 3. Starvation Prevention: Waiting time escalation (+15 points per second waited)
        // This guarantees that even a low priority robot will eventually overtake a stream of high priority robots
        val starvationBonus = min(robot.waitingTime * 15f, 400f)
        score += starvationBonus

        // 4. Heavy load inertia bonus (harder to stop/reverse heavy AGVs)
        if (robot.currentLoad > 100f) {
            score += 40f
        }

        // 5. Route progress / remaining distance: close to destination gets right-of-way to clear space
        val remainingSteps = robot.currentRoute.size - robot.routeIndex
        if (remainingSteps in 1..4) {
            score += 60f
        }

        // 6. Comm loss safe crawl penalty (communication lost robots must be cautious)
        if (robot.communicationStatus != com.example.model.CommunicationStatus.CONNECTED) {
            score -= 80f
        }

        return score
    }

    fun negotiate(
        robotA: Robot,
        robotB: Robot,
        conflictLocation: GridPoint,
        collisionType: CollisionType
    ): NegotiationResult {
        val scoreA = calculateRightOfWayScore(robotA)
        val scoreB = calculateRightOfWayScore(robotB)

        val winner: Robot
        val loser: Robot
        val winnerScore: Float
        val loserScore: Float

        if (scoreA >= scoreB) {
            winner = robotA
            loser = robotB
            winnerScore = scoreA
            loserScore = scoreB
        } else {
            winner = robotB
            loser = robotA
            winnerScore = scoreB
            loserScore = scoreA
        }

        // Determine best resolution action for the yielding robot
        val action: ResolutionAction
        val explanation: String

        // Check if there is an adjacent waiting turnout
        val nearbyTurnout = findAdjacentTurnout(loser.gridPoint)
        val isNarrow = map.grid[loser.gridPoint.x.coerceIn(0, map.width - 1)][loser.gridPoint.y.coerceIn(0, map.height - 1)] == CellType.NARROW_CORRIDOR

        if (isNarrow && collisionType == CollisionType.HEAD_ON) {
            if (nearbyTurnout != null) {
                action = ResolutionAction.PULL_INTO_TURNOUT
                explanation = "R${loser.robotId} pulling into turnout bay at (${nearbyTurnout.x},${nearbyTurnout.y})"
                divertToTurnout(loser, nearbyTurnout)
            } else {
                action = ResolutionAction.REROUTE_ALTERNATIVE
                explanation = "R${loser.robotId} rerouting around bottleneck corridor"
                rerouteRobot(loser, conflictLocation)
            }
        } else if (collisionType == CollisionType.REAR_END) {
            action = ResolutionAction.YIELD_AND_WAIT
            explanation = "R${loser.robotId} matching speed & maintaining headway distance"
            loser.speed = (winner.speed * 0.7f).coerceAtLeast(0.3f)
            loser.status = RobotStatus.WAITING
        } else {
            // General intersection / crossing yield
            action = ResolutionAction.YIELD_AND_WAIT
            val reason = if (loser.waitingTime > 5f) "starvation guard active" else "task priority disparity"
            explanation = "R${winner.robotId} granted RoW (Score ${winnerScore.toInt()} vs ${loserScore.toInt()}, $reason); R${loser.robotId} yields"
            loser.status = RobotStatus.WAITING
        }

        // Set negotiation states on both robots
        winner.negotiationState = NegotiationState(
            partnerRobotId = loser.robotId,
            resourceLocation = conflictLocation,
            conflictReason = collisionType.description,
            priorityScore = winnerScore,
            isWinner = true,
            resolutionAction = "GRANTED_ROW"
        )

        loser.negotiationState = NegotiationState(
            partnerRobotId = winner.robotId,
            resourceLocation = conflictLocation,
            conflictReason = collisionType.description,
            priorityScore = loserScore,
            isWinner = false,
            resolutionAction = action.name
        )

        val whyExplanation = "• Priority Advantage: R${winner.robotId} (${winner.taskPriority.name}) vs R${loser.robotId} (${loser.taskPriority.name})\n" +
                "• Energy Factor: R${winner.robotId} (${winner.battery.toInt()}%) vs R${loser.robotId} (${loser.battery.toInt()}%)\n" +
                "• Starvation Guard: R${loser.robotId} waited ${String.format("%.1f", loser.waitingTime)}s\n" +
                "• Safe Action: R${loser.robotId} performs ${action.name} to guarantee collision-free crossing."

        return NegotiationResult(
            winner = winner,
            loser = loser,
            winnerScore = winnerScore,
            loserScore = loserScore,
            action = action,
            conflictLocation = conflictLocation,
            explanation = explanation,
            whyExplanation = whyExplanation
        )
    }

    private fun findAdjacentTurnout(point: GridPoint): GridPoint? {
        val candidates = listOf(
            GridPoint(point.x + 1, point.y),
            GridPoint(point.x - 1, point.y),
            GridPoint(point.x, point.y + 1),
            GridPoint(point.x, point.y - 1),
            GridPoint(point.x + 1, point.y + 1),
            GridPoint(point.x - 1, point.y - 1)
        )
        return candidates.firstOrNull { p ->
            p.x in 0 until map.width && p.y in 0 until map.height &&
                    (map.grid[p.x][p.y] == CellType.WAITING_AREA || map.waitingAreas.contains(p))
        }
    }

    private fun divertToTurnout(robot: Robot, turnout: GridPoint) {
        val divertPath = listOf(robot.gridPoint, turnout)
        robot.currentRoute = divertPath + robot.currentRoute.drop(robot.routeIndex)
        robot.routeIndex = 0
        robot.status = RobotStatus.WAITING
    }

    private fun rerouteRobot(robot: Robot, blockedPoint: GridPoint) {
        val dest = robot.currentRoute.lastOrNull() ?: return
        val newPath = pathPlanner.findPath(
            start = robot.gridPoint,
            goal = dest,
            penalizedPoints = setOf(blockedPoint)
        )
        if (newPath.isNotEmpty()) {
            robot.currentRoute = newPath
            robot.routeIndex = 0
            robot.status = RobotStatus.REROUTING
        } else {
            robot.status = RobotStatus.WAITING
        }
    }
}
