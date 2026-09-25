package com.example.engine

import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.Task
import com.example.model.TaskBid
import com.example.model.TaskStatus
import kotlin.math.abs

data class AllocationResult(
    val task: Task,
    val winningRobot: Robot?,
    val bidsSubmitted: List<TaskBid>,
    val winningScore: Float,
    val isPeerToPeer: Boolean,
    val explanation: String
)

class TaskAllocator {

    fun calculateBid(robot: Robot, task: Task): TaskBid {
        val distToPickup = abs(robot.x - task.pickupLocation.x) + abs(robot.y - task.pickupLocation.y)
        val distanceCost = distToPickup * 1.5f

        val batteryRemaining = robot.battery
        val batteryPenalty = if (batteryRemaining < 25f) 500f else (100f - batteryRemaining) * 0.3f

        val workloadCost = if (robot.currentTaskId != null) 300f else 0f
        val capabilityMatch = robot.hasCapability(task.requiredCapability)
        val capabilityBonus = if (capabilityMatch) 100f else -1000f

        val priorityBenefit = task.priority.level * 40f

        val finalScore = distanceCost + workloadCost + batteryPenalty - capabilityBonus - priorityBenefit

        return TaskBid(
            robotId = robot.robotId,
            score = finalScore,
            distanceCost = distanceCost,
            batteryScore = batteryRemaining,
            capabilityMatch = capabilityMatch
        )
    }

    /**
     * Allocates task using either Central Auction or Decentralized P2P Gossip.
     */
    fun allocateTask(
        task: Task,
        robots: List<Robot>,
        isCentralControllerOnline: Boolean
    ): AllocationResult {
        if (task.status != TaskStatus.PENDING && task.status != TaskStatus.NEGOTIATING) {
            return AllocationResult(task, null, emptyList(), Float.MAX_VALUE, false, "Task already claimed or finished")
        }

        task.status = TaskStatus.NEGOTIATING

        // Eligible robots: must be capable, not failed/charging, and have battery > 15%
        val eligibleRobots = robots.filter { robot ->
            robot.hasCapability(task.requiredCapability) &&
                    robot.status != RobotStatus.FAILED &&
                    robot.status != RobotStatus.CHARGING &&
                    robot.status != RobotStatus.LOW_BATTERY &&
                    robot.currentTaskId == null &&
                    robot.battery > 15f
        }

        if (eligibleRobots.isEmpty()) {
            task.status = TaskStatus.PENDING
            return AllocationResult(
                task = task,
                winningRobot = null,
                bidsSubmitted = emptyList(),
                winningScore = Float.MAX_VALUE,
                isPeerToPeer = !isCentralControllerOnline,
                explanation = "NO CAPABLE OR AVAILABLE ROBOT FOR TASK ${task.taskId}"
            )
        }

        // If Central Controller is OFFLINE: simulate peer-to-peer neighborhood auction
        // Only robots within local communication range (~25 units) participate in the gossip round
        val candidateRobots = if (!isCentralControllerOnline) {
            val localGroup = eligibleRobots.filter { r ->
                val d = abs(r.x - task.pickupLocation.x) + abs(r.y - task.pickupLocation.y)
                d <= 35f
            }
            if (localGroup.isNotEmpty()) localGroup else eligibleRobots.take(5)
        } else {
            eligibleRobots
        }

        val bids = candidateRobots.map { calculateBid(it, task) }
        task.bids.clear()
        task.bids.addAll(bids)

        val bestBid = bids.minByOrNull { it.score }
        val winningRobot = candidateRobots.firstOrNull { it.robotId == bestBid?.robotId }

        if (winningRobot != null && bestBid != null) {
            task.assignedRobotId = winningRobot.robotId
            task.status = TaskStatus.ASSIGNED
            winningRobot.currentTaskId = task.taskId
            winningRobot.taskPriority = task.priority

            val modeStr = if (isCentralControllerOnline) "Central Auction" else "P2P Gossip Consensus"
            val explanation = "Task ${task.taskId} allocated to R${winningRobot.robotId} via $modeStr (Bids: ${bids.size}, Score: ${bestBid.score.toInt()})"

            return AllocationResult(
                task = task,
                winningRobot = winningRobot,
                bidsSubmitted = bids,
                winningScore = bestBid.score,
                isPeerToPeer = !isCentralControllerOnline,
                explanation = explanation
            )
        }

        task.status = TaskStatus.PENDING
        return AllocationResult(
            task = task,
            winningRobot = null,
            bidsSubmitted = bids,
            winningScore = Float.MAX_VALUE,
            isPeerToPeer = !isCentralControllerOnline,
            explanation = "Failed to determine winning bid for Task ${task.taskId}"
        )
    }
}
