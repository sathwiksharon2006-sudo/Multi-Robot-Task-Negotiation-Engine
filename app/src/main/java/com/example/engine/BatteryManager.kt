package com.example.engine

import com.example.model.GridPoint
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.WarehouseMap

data class BatteryEvent(
    val robot: Robot,
    val requiresTaskReassignment: Boolean,
    val taskToReassign: String?,
    val assignedDock: GridPoint?,
    val message: String
)

class BatteryManager(
    private val map: WarehouseMap,
    private val pathPlanner: PathPlanner,
    var lowBatteryThresholdPercent: Float = 25.0f
) {
    // Dock reservations: GridPoint -> RobotId
    val dockReservations = HashMap<GridPoint, Int>()

    fun updateRobotBattery(robot: Robot, isMoving: Boolean, dtSec: Float): BatteryEvent? {
        if (robot.status == RobotStatus.FAILED) return null

        if (robot.status == RobotStatus.CHARGING) {
            // Charging speed: +5.0% per second
            robot.battery = (robot.battery + 5.0f * dtSec).coerceAtMost(100f)
            if (robot.battery >= 96f) {
                // Fully charged! Release dock and mark rested
                robot.assignedChargingDock?.let { dockReservations.remove(it) }
                robot.assignedChargingDock = null
                robot.status = RobotStatus.IDLE
                robot.workingTime = 0f // Reset fatigue
                return BatteryEvent(
                    robot = robot,
                    requiresTaskReassignment = false,
                    taskToReassign = null,
                    assignedDock = null,
                    message = "R${robot.robotId} [${robot.robotType.displayName}] fully replenished to 100%. Rested and re-entering swarm circulation."
                )
            }
            return null
        }

        // Consumption calculation based on chassis type, payload, and movement
        val loadFactor = 1.0f + (robot.currentLoad / robot.loadCapacity.coerceAtLeast(1f)) * 0.4f
        val movementFactor = if (isMoving) 1.0f else 0.12f
        val drain = robot.robotType.powerConsumptionRate * loadFactor * movementFactor * dtSec * 1.8f
        robot.battery = (robot.battery - drain).coerceAtLeast(0f)

        // 1. Critical Depletion
        if (robot.battery <= 0f) {
            robot.status = RobotStatus.FAILED
            robot.failureReason = "BATTERY_EXHAUSTED"
            return BatteryEvent(
                robot = robot,
                requiresTaskReassignment = robot.currentTaskId != null,
                taskToReassign = robot.currentTaskId,
                assignedDock = null,
                message = "EMERGENCY: R${robot.robotId} battery fully drained to 0%. Stranded at (${robot.gridPoint.x},${robot.gridPoint.y})."
            )
        }

        // 2. Battery dropped below threshold -> Immediate compulsory routing to nearby charging port
        if (robot.battery < lowBatteryThresholdPercent &&
            robot.status != RobotStatus.CHARGING &&
            robot.assignedChargingDock == null
        ) {
            robot.status = RobotStatus.LOW_BATTERY
            val orphanedTaskId = robot.currentTaskId
            robot.currentTaskId = null
            robot.currentTaskType = null

            val targetDock = findAvailableChargingDock(robot.gridPoint)
            if (targetDock != null) {
                dockReservations[targetDock] = robot.robotId
                robot.assignedChargingDock = targetDock
                val pathToDock = pathPlanner.findPath(robot.gridPoint, targetDock)
                if (pathToDock.isNotEmpty()) {
                    robot.currentRoute = pathToDock
                    robot.routeIndex = 0
                }
            }

            return BatteryEvent(
                robot = robot,
                requiresTaskReassignment = orphanedTaskId != null,
                taskToReassign = orphanedTaskId,
                assignedDock = targetDock,
                message = "AUTONOMOUS DOCKING: R${robot.robotId} battery at ${robot.battery.toInt()}% (< ${lowBatteryThresholdPercent.toInt()}% threshold). Work handed over; navigating to nearest Power Dock."
            )
        }

        return null
    }

    fun findAvailableChargingDock(from: GridPoint): GridPoint? {
        val freeDocks = map.chargingStations.filter { !dockReservations.containsKey(it) }
        return freeDocks.minByOrNull { it.distanceTo(from) }
    }

    fun releaseDock(dock: GridPoint) {
        dockReservations.remove(dock)
    }
}
