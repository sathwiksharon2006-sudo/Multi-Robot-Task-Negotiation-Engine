package com.example

import com.example.engine.CollisionPredictor
import com.example.engine.CollisionType
import com.example.engine.DeadlockCycle
import com.example.engine.DeadlockEngine
import com.example.engine.FleetSimulator
import com.example.engine.NegotiationEngine
import com.example.engine.PathPlanner
import com.example.engine.ResolutionAction
import com.example.engine.SpatialHashGrid
import com.example.engine.TaskAllocator
import com.example.model.Capability
import com.example.model.GridPoint
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.RobotType
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.model.TaskType
import com.example.model.WarehouseMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationEngineTest {

    @Test
    fun testSpatialHashGridScalability() {
        val grid = SpatialHashGrid(cellSize = 3.0f)
        for (i in 1..500) {
            val r = Robot(
                robotId = i,
                robotType = RobotType.GENERAL_TRANSPORT,
                x = (i % 50).toFloat(),
                y = (i / 10).toFloat()
            )
            grid.insert(r)
        }
        val neighbors = grid.getNearbyRobots(10f, 10f, 5.0f)
        assertTrue(neighbors.isNotEmpty())
    }

    @Test
    fun testTaskAllocationMultiAttributeScoring() {
        val allocator = TaskAllocator()
        val r1 = Robot(robotId = 1, robotType = RobotType.GENERAL_TRANSPORT, x = 2f, y = 2f, battery = 90f)
        val r2 = Robot(robotId = 2, robotType = RobotType.GENERAL_TRANSPORT, x = 40f, y = 30f, battery = 20f)
        val task = Task(
            taskId = "T-TEST",
            taskType = TaskType.LOGISTICS_TRANSPORT,
            pickupLocation = GridPoint(3, 3),
            deliveryLocation = GridPoint(20, 20),
            priority = TaskPriority.HIGH,
            requiredCapability = Capability.TRANSPORT
        )

        val result = allocator.allocateTask(task, listOf(r1, r2), isCentralControllerOnline = true)
        assertNotNull(result.winningRobot)
        assertEquals(1, result.winningRobot?.robotId)
        assertEquals(TaskStatus.ASSIGNED, task.status)
    }

    @Test
    fun testDecentralizedP2PGossipAllocationWhenControllerOffline() {
        val allocator = TaskAllocator()
        val r1 = Robot(robotId = 1, robotType = RobotType.GENERAL_TRANSPORT, x = 5f, y = 5f, battery = 85f)
        val task = Task(
            taskId = "T-DECENTRALIZED",
            taskType = TaskType.LOGISTICS_TRANSPORT,
            pickupLocation = GridPoint(6, 6),
            deliveryLocation = GridPoint(20, 20),
            priority = TaskPriority.NORMAL,
            requiredCapability = Capability.TRANSPORT
        )

        val result = allocator.allocateTask(task, listOf(r1), isCentralControllerOnline = false)
        assertTrue(result.isPeerToPeer)
        assertEquals(1, result.winningRobot?.robotId)
    }

    @Test
    fun testRightOfWayStarvationPrevention() {
        val map = WarehouseMap(60, 40)
        val planner = PathPlanner(map)
        val engine = NegotiationEngine(map, planner)

        val rUrgent = Robot(robotId = 1, robotType = RobotType.EXPRESS_HAULER, x = 10f, y = 10f, taskPriority = TaskPriority.HIGH)
        val rWaiting = Robot(robotId = 2, robotType = RobotType.GENERAL_TRANSPORT, x = 11f, y = 10f, taskPriority = TaskPriority.LOW, waitingTime = 25f)

        val scoreUrgent = engine.calculateRightOfWayScore(rUrgent)
        val scoreWaiting = engine.calculateRightOfWayScore(rWaiting)

        assertTrue("Starvation prevention failed: $scoreWaiting <= $scoreUrgent", scoreWaiting > scoreUrgent)
    }

    @Test
    fun testDeadlockCycleDetectionAndResolution() {
        val map = WarehouseMap(60, 40)
        val planner = PathPlanner(map)
        val deadlockEngine = DeadlockEngine(map, planner)

        val r1 = Robot(robotId = 1, robotType = RobotType.GENERAL_TRANSPORT, x = 20f, y = 20f)
        val r2 = Robot(robotId = 2, robotType = RobotType.GENERAL_TRANSPORT, x = 21f, y = 20f)
        val r3 = Robot(robotId = 3, robotType = RobotType.GENERAL_TRANSPORT, x = 21f, y = 21f)

        deadlockEngine.clearGraph()
        deadlockEngine.addDependency(1, 2)
        deadlockEngine.addDependency(2, 3)
        deadlockEngine.addDependency(3, 1)

        val robotsById = mapOf(1 to r1, 2 to r2, 3 to r3)
        val cycles = deadlockEngine.detectDeadlocks(robotsById)

        assertEquals(1, cycles.size)
        val recoveryPlan = deadlockEngine.recoverDeadlock(cycles[0])
        assertNotNull(recoveryPlan.targetRobot)
        assertNotNull(recoveryPlan.recoveryAction)
    }

    @Test
    fun testMultiRoleFleetSimulatorIntegration() {
        val sim = FleetSimulator(WarehouseMap(60, 40))
        sim.spawnFleet(12)
        sim.generateTasks(8)
        sim.tick(0.1f)

        val metrics = sim.metrics.value
        assertEquals(12, metrics.totalRobots)
        assertTrue(metrics.totalTasks >= 8)
        assertTrue(metrics.fleetAvgBatteryPercent > 0f)
    }

    @Test
    fun testAutonomousBatteryDockingThreshold() {
        val map = WarehouseMap(60, 40)
        val planner = PathPlanner(map)
        val batteryManager = com.example.engine.BatteryManager(map, planner, lowBatteryThresholdPercent = 25f)

        val rLow = Robot(
            robotId = 10,
            robotType = RobotType.CLEANER_AMR,
            x = 10f,
            y = 10f,
            battery = 22f, // Below 25% threshold
            currentTaskId = "T-CLEAN-1"
        )

        val event = batteryManager.updateRobotBattery(rLow, isMoving = false, dtSec = 0.1f)
        assertNotNull(event)
        assertTrue(event?.requiresTaskReassignment == true)
        assertEquals("T-CLEAN-1", event?.taskToReassign)
        assertEquals(RobotStatus.LOW_BATTERY, rLow.status)
        assertNotNull(rLow.assignedChargingDock)
    }
}
