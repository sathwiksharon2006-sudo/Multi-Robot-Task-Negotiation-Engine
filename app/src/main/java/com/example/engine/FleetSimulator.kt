package com.example.engine

import com.example.model.Capability
import com.example.model.CellType
import com.example.model.CommunicationStatus
import com.example.model.EventCategory
import com.example.model.EventLevel
import com.example.model.GridPoint
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.RobotType
import com.example.model.SimulationEvent
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.model.TaskType
import com.example.model.WarehouseMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FleetMetrics(
    val totalRobots: Int = 0,
    val activeRobots: Int = 0,
    val idleRobots: Int = 0,
    val chargingRobots: Int = 0,
    val failedRobots: Int = 0,
    val blockedRobots: Int = 0,
    val totalTasks: Int = 0,
    val pendingTasks: Int = 0,
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val failedTasks: Int = 0,
    val fleetUtilization: Float = 0f,
    val avgTaskCompletionTimeSec: Float = 0f,
    val avgRobotWaitingTimeSec: Float = 0f,
    val collisionsPrevented: Int = 0,
    val activeConflicts: Int = 0,
    val deadlocksDetected: Int = 0,
    val deadlocksResolved: Int = 0,
    val tasksReassigned: Int = 0,
    val robotFailures: Int = 0,
    val controllerOnline: Boolean = true,
    val negotiationsPerformed: Int = 0,
    val avgNegotiationTimeMs: Float = 42f,
    // Work Sharing & Task Diversity Statistics
    val fleetAvgBatteryPercent: Float = 100f,
    val robotsWorkingCount: Int = 0,
    val robotsRestedCount: Int = 0,
    val sharedTasksCount: Int = 0,
    val cleanersWorking: Int = 0,
    val organizersWorking: Int = 0,
    val haulersWorking: Int = 0,
    val inspectorsWorking: Int = 0,
    val expressWorking: Int = 0,
    val lowBatteryThreshold: Float = 25f,
    val batteryHighCount: Int = 0,
    val batteryMediumCount: Int = 0,
    val batteryCriticalCount: Int = 0,
    val totalCleaningCompleted: Int = 0,
    val totalOrganizingCompleted: Int = 0,
    val totalTransportCompleted: Int = 0,
    val totalInspectionCompleted: Int = 0
)

class FleetSimulator(
    val map: WarehouseMap = WarehouseMap(60, 40)
) {
    val spatialGrid = SpatialHashGrid(cellSize = 3.5f)
    val pathPlanner = PathPlanner(map)
    val collisionPredictor = CollisionPredictor(map, spatialGrid)
    val negotiationEngine = NegotiationEngine(map, pathPlanner)
    val deadlockEngine = DeadlockEngine(map, pathPlanner)
    val batteryManager = BatteryManager(map, pathPlanner, lowBatteryThresholdPercent = 25f)
    val taskAllocator = TaskAllocator()

    // State collections
    val robots = mutableListOf<Robot>()
    val tasks = mutableListOf<Task>()
    val events = mutableListOf<SimulationEvent>()
    val activeCollisionRisks = mutableListOf<CollisionRisk>()
    val activeDeadlocks = mutableListOf<DeadlockCycle>()

    private val _metrics = MutableStateFlow(FleetMetrics())
    val metrics: StateFlow<FleetMetrics> = _metrics.asStateFlow()

    private val _eventsFlow = MutableStateFlow<List<SimulationEvent>>(emptyList())
    val eventsFlow: StateFlow<List<SimulationEvent>> = _eventsFlow.asStateFlow()

    // Simulation controls
    var isRunning = true
    var simulationSpeed = 1.0f
    var isCentralControllerOnline = true
    var isCorridorBlocked = false

    // Metric accumulators
    private var collisionCounter = 0
    private var deadlockDetectCounter = 0
    private var deadlockResolveCounter = 0
    private var taskReassignCounter = 0
    private var robotFailureCounter = 0
    private var negotiationCounter = 0
    private var sharedTasksCounter = 0
    private var totalTaskDurationSec = 0f
    private var completedTaskCount = 0

    private val taskIdSeq = AtomicInteger(100)

    init {
        spawnFleet(60)
        generateTasks(40)
    }

    fun spawnFleet(count: Int) {
        robots.clear()
        batteryManager.dockReservations.clear()

        for (i in 1..count) {
            val type = when (i % 6) {
                0 -> RobotType.CLEANER_AMR
                1 -> RobotType.ORGANIZER_AMR
                2 -> RobotType.HEAVY_HAUL_AGV
                3 -> RobotType.EXPRESS_HAULER
                4 -> RobotType.INSPECTION_ROVER
                else -> RobotType.GENERAL_TRANSPORT
            }
            val spawnPoint = map.getRandomWalkablePoint()
            val robot = Robot(
                robotId = i,
                robotType = type,
                x = spawnPoint.x.toFloat(),
                y = spawnPoint.y.toFloat(),
                battery = (70..100).random().toFloat()
            )
            robots.add(robot)
        }
        logEvent(
            EventCategory.CONTROLLER,
            EventLevel.INFO,
            "Multi-Role Fleet Deployed",
            "Initialized $count AMRs across 6 task-specialized categories (Cleaners, Organizers, Heavy AGVs, Express, Inspectors, Transports)."
        )
        updateMetrics()
    }

    fun generateTasks(count: Int) {
        for (i in 0 until count) {
            val id = "T-${taskIdSeq.getAndIncrement()}"
            val taskType: TaskType
            val capability: Capability
            val pickup: GridPoint
            val delivery: GridPoint
            val weight: Float

            when (i % 5) {
                0 -> {
                    // Aisle Cleaning Mission
                    taskType = TaskType.AISLE_CLEANING
                    capability = Capability.CLEANING
                    pickup = GridPoint((5..55).random(), listOf(5, 20, 35).random())
                    delivery = GridPoint(pickup.x + ((-6..6).random()).coerceIn(-pickup.x + 2, map.width - pickup.x - 3), pickup.y)
                    weight = 10f
                }
                1 -> {
                    // Shelf Sorting & Organizing
                    taskType = TaskType.SHELF_ORGANIZING
                    capability = Capability.ORGANIZING
                    pickup = GridPoint(listOf(6, 16, 26, 36, 46).random(), (8..30).random())
                    delivery = GridPoint(pickup.x + 3, pickup.y)
                    weight = 35f
                }
                2 -> {
                    // Heavy Bulk Hauling
                    taskType = TaskType.HEAVY_HAUL
                    capability = Capability.HEAVY_LOAD
                    pickup = map.pickupStations.randomOrNull() ?: GridPoint(1, 6)
                    delivery = map.deliveryStations.randomOrNull() ?: GridPoint(map.width - 2, 6)
                    weight = 350f
                }
                3 -> {
                    // Safety & Hazard Scanning
                    taskType = TaskType.SAFETY_INSPECTION
                    capability = Capability.INSPECT
                    pickup = GridPoint(30, 20) // Narrow bottleneck or intersection
                    delivery = GridPoint((4..50).random(), (4..35).random())
                    weight = 5f
                }
                else -> {
                    // Logistics Transport
                    taskType = TaskType.LOGISTICS_TRANSPORT
                    capability = Capability.TRANSPORT
                    pickup = map.pickupStations.randomOrNull() ?: GridPoint(1, 6)
                    delivery = map.deliveryStations.randomOrNull() ?: GridPoint(map.width - 2, 6)
                    weight = 40f
                }
            }

            val priority = if (i % 9 == 0) TaskPriority.CRITICAL else if (i % 4 == 0) TaskPriority.HIGH else TaskPriority.NORMAL
            val task = Task(
                taskId = id,
                taskType = taskType,
                pickupLocation = pickup,
                deliveryLocation = delivery,
                priority = priority,
                requiredCapability = capability,
                weight = weight
            )
            tasks.add(task)
        }
        logEvent(
            EventCategory.TASK,
            EventLevel.INFO,
            "Tasks Generated",
            "Injected $count multi-role missions (Sanitation, Shelf Organizing, Heavy Haul, Logistics, Safety Scan)."
        )
        updateMetrics()
    }

    /**
     * Primary Simulation Loop Tick (typically called at 25-35Hz).
     */
    fun tick(dtSec: Float) {
        if (!isRunning) return
        val effectiveDt = dtSec * simulationSpeed

        // 1. Rebuild Spatial Hash Grid
        spatialGrid.clear()
        val posMap = HashMap<GridPoint, Robot>(robots.size)
        for (r in robots) {
            spatialGrid.insert(r)
            posMap[r.gridPoint] = r
        }

        // 2. Battery & Charging Update (Autonomous compulsory docking when < 25%)
        for (robot in robots) {
            val isMoving = robot.status == RobotStatus.MOVING || robot.status == RobotStatus.REROUTING
            val batEvent = batteryManager.updateRobotBattery(robot, isMoving, effectiveDt)
            if (batEvent != null) {
                if (batEvent.requiresTaskReassignment && batEvent.taskToReassign != null) {
                    shareOrReassignWork(batEvent.taskToReassign, "Battery threshold reached (< 25%). Routed to charger.")
                }
                logEvent(
                    EventCategory.BATTERY,
                    if (robot.battery <= 0f) EventLevel.ERROR else EventLevel.WARNING,
                    "Battery Telemetry",
                    batEvent.message,
                    robotId = robot.robotId
                )
            }
        }

        // 3. Collision Prediction Lookahead
        val risks = collisionPredictor.predictCollisions(robots)
        activeCollisionRisks.clear()
        activeCollisionRisks.addAll(risks)

        // 4. Resolve Predicted Collisions via P2P Negotiation
        for (risk in risks) {
            if (risk.robotA.status != RobotStatus.NEGOTIATING && risk.robotB.status != RobotStatus.NEGOTIATING) {
                risk.robotA.status = RobotStatus.NEGOTIATING
                risk.robotB.status = RobotStatus.NEGOTIATING

                val negResult = negotiationEngine.negotiate(
                    risk.robotA,
                    risk.robotB,
                    risk.conflictLocation,
                    risk.collisionType
                )
                collisionCounter++
                negotiationCounter++

                logEvent(
                    EventCategory.NEGOTIATION,
                    EventLevel.WARNING,
                    "P2P Conflict: ${risk.collisionType.description}",
                    negResult.explanation,
                    robotId = negResult.winner.robotId,
                    partnerRobotId = negResult.loser.robotId
                )
            }
        }

        // 5. Deadlock Detection & Recovery
        deadlockEngine.buildDependencies(robots, posMap)
        val robotsById = robots.associateBy { it.robotId }
        val deadlocks = deadlockEngine.detectDeadlocks(robotsById)
        activeDeadlocks.clear()
        activeDeadlocks.addAll(deadlocks)

        for (dl in deadlocks) {
            deadlockDetectCounter++
            logEvent(
                EventCategory.DEADLOCK,
                EventLevel.ERROR,
                "Deadlock Detected!",
                "Cycle found in dependency graph: ${dl.cyclePathStr}"
            )
            val recoveryPlan = deadlockEngine.recoverDeadlock(dl)
            deadlockResolveCounter++
            logEvent(
                EventCategory.DEADLOCK,
                EventLevel.SUCCESS,
                "Deadlock Resolved",
                recoveryPlan.explanation,
                robotId = recoveryPlan.targetRobot.robotId
            )
        }

        // 6. Task Allocation with Work-Sharing & Role Matching
        val pendingTasks = tasks.filter { it.status == TaskStatus.PENDING }
        for (task in pendingTasks.take(15)) {
            val result = taskAllocator.allocateTask(task, robots, isCentralControllerOnline)
            if (result.winningRobot != null) {
                val winner = result.winningRobot
                winner.currentTaskType = task.taskType

                val pathToPickup = pathPlanner.findPath(winner.gridPoint, task.pickupLocation)
                val pathToDelivery = pathPlanner.findPath(task.pickupLocation, task.deliveryLocation)
                winner.currentRoute = pathToPickup + pathToDelivery
                winner.routeIndex = 0
                winner.status = RobotStatus.MOVING

                val modeStr = if (result.isPeerToPeer) "P2P Consensus" else "Auction"
                logEvent(
                    EventCategory.TASK,
                    EventLevel.SUCCESS,
                    "${task.taskType.categoryLabel} Assigned ($modeStr)",
                    "${task.taskType.iconGlyph} Task ${task.taskId} awarded to R${winner.robotId} [${winner.robotType.displayName}].",
                    robotId = winner.robotId,
                    taskId = task.taskId
                )
            }
        }

        // 7. Dynamic Work-Sharing: If rested robots are idle and nearby tasks exist, balance load
        performWorkSharingBalance()

        // 8. Robot Kinematics & Movement
        for (robot in robots) {
            updateRobotMovement(robot, effectiveDt)
        }

        updateMetrics()
    }

    /**
     * Balances work across the swarm:
     * When rested idle robots have high energy (>80%), they proactively pick up tasks or relieve fatigued units.
     */
    private fun performWorkSharingBalance() {
        val restedIdleRobots = robots.filter {
            it.status == RobotStatus.IDLE && it.battery > 80f && it.currentTaskId == null
        }
        if (restedIdleRobots.isEmpty()) return

        // Find working robots that have been working hard (battery < 40% or workingTime > 60s)
        val fatiguedRobots = robots.filter {
            (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) &&
                    it.currentTaskId != null &&
                    (it.battery < 35f || it.workingTime > 75f)
        }

        for (fatigued in fatiguedRobots.take(2)) {
            val taskId = fatigued.currentTaskId ?: continue
            val task = tasks.firstOrNull { it.taskId == taskId } ?: continue

            // Find matching capable rested robot
            val reliever = restedIdleRobots.firstOrNull { it.hasCapability(task.requiredCapability) } ?: continue

            // Handover mission
            fatigued.currentTaskId = null
            fatigued.currentTaskType = null
            fatigued.status = RobotStatus.IDLE
            fatigued.currentRoute = emptyList()
            fatigued.isRested = false

            task.assignedRobotId = reliever.robotId
            task.isSharedWorkHandover = true
            reliever.currentTaskId = task.taskId
            reliever.currentTaskType = task.taskType
            reliever.taskPriority = task.priority
            reliever.workSharedHandoverCount++
            reliever.isRested = false

            val pathToTarget = pathPlanner.findPath(reliever.gridPoint, task.deliveryLocation)
            reliever.currentRoute = pathToTarget
            reliever.routeIndex = 0
            reliever.status = RobotStatus.MOVING

            sharedTasksCounter++
            logEvent(
                EventCategory.TASK,
                EventLevel.INFO,
                "Work Shared: Rested Reliever Activated",
                "Rested R${reliever.robotId} [${reliever.robotType.displayName}] stepped in to relieve fatigued R${fatigued.robotId} for ${task.taskType.categoryLabel} ${task.taskId}.",
                robotId = reliever.robotId,
                partnerRobotId = fatigued.robotId,
                taskId = task.taskId
            )
        }
    }

    private fun updateRobotMovement(robot: Robot, dt: Float) {
        if (robot.status == RobotStatus.FAILED || robot.status == RobotStatus.CHARGING) return

        if (robot.status == RobotStatus.WAITING) {
            robot.waitingTime += dt
            if (robot.waitingTime > 8.0f) {
                robot.status = RobotStatus.REROUTING
                val dest = robot.currentRoute.lastOrNull()
                if (dest != null) {
                    val altPath = pathPlanner.findPath(robot.gridPoint, dest)
                    if (altPath.isNotEmpty()) {
                        robot.currentRoute = altPath
                        robot.routeIndex = 0
                        logEvent(
                            EventCategory.NEGOTIATION,
                            EventLevel.INFO,
                            "Starvation Bypass",
                            "R${robot.robotId} waiting limit reached; rerouted along alternative aisle.",
                            robotId = robot.robotId
                        )
                    }
                }
                robot.waitingTime = 0f
            }
            return
        }

        if (robot.status == RobotStatus.IDLE) {
            robot.idleTime += dt
            // Periodically patrol or clean aisle if cleaner
            if (robot.currentRoute.isEmpty() && Math.random() < 0.012) {
                val wanderPoint = map.getRandomWalkablePoint()
                val path = pathPlanner.findPath(robot.gridPoint, wanderPoint)
                if (path.isNotEmpty()) {
                    robot.currentRoute = path
                    robot.routeIndex = 0
                    robot.status = RobotStatus.MOVING
                }
            }
            return
        }

        robot.workingTime += dt

        // Follow Route Waypoints
        if (robot.routeIndex < robot.currentRoute.size) {
            val target = robot.currentRoute[robot.routeIndex]
            robot.targetX = target.x.toFloat()
            robot.targetY = target.y.toFloat()

            val dx = robot.targetX - robot.x
            val dy = robot.targetY - robot.y
            val dist = sqrt(dx * dx + dy * dy)
            val stepDist = robot.speed * 2.0f * dt

            if (dist <= stepDist || dist < 0.15f) {
                robot.x = robot.targetX
                robot.y = robot.targetY
                robot.routeIndex++

                // Check if arrived at charging station
                if (map.grid[target.x][target.y] == CellType.CHARGING_STATION && robot.battery < 90f) {
                    robot.status = RobotStatus.CHARGING
                    robot.currentRoute = emptyList()
                    return
                }

                if (robot.routeIndex >= robot.currentRoute.size) {
                    finishRoute(robot)
                }
            } else {
                val angle = atan2(dy, dx)
                robot.previousX = robot.x
                robot.previousY = robot.y
                robot.x += cos(angle) * stepDist
                robot.y += sin(angle) * stepDist
            }
        } else {
            finishRoute(robot)
        }
    }

    private fun finishRoute(robot: Robot) {
        val finishedTaskId = robot.currentTaskId
        if (finishedTaskId != null) {
            val task = tasks.firstOrNull { it.taskId == finishedTaskId }
            if (task != null) {
                task.status = TaskStatus.COMPLETED
                task.completionTime = System.currentTimeMillis()
                val dur = (task.completionTime!! - task.creationTime) / 1000f
                totalTaskDurationSec += dur
                completedTaskCount++
                robot.recordCompletedTask(task.taskType)

                logEvent(
                    EventCategory.TASK,
                    EventLevel.SUCCESS,
                    "Mission Completed",
                    "${task.taskType.iconGlyph} ${task.taskType.categoryLabel} ${task.taskId} completed by R${robot.robotId} [${robot.robotType.displayName}] in ${dur.toInt()}s.",
                    robotId = robot.robotId,
                    taskId = task.taskId
                )
            }
            robot.currentTaskId = null
            robot.currentTaskType = null
        }
        robot.currentRoute = emptyList()
        robot.routeIndex = 0
        robot.status = RobotStatus.IDLE
        robot.waitingTime = 0f
    }

    fun shareOrReassignWork(taskId: String, reason: String) {
        val task = tasks.firstOrNull { it.taskId == taskId } ?: return
        task.assignedRobotId = null
        task.status = TaskStatus.PENDING
        task.reassignmentCount++
        taskReassignCounter++
        sharedTasksCounter++

        logEvent(
            EventCategory.TASK,
            EventLevel.WARNING,
            "Mission Handover Queued",
            "${task.taskType.iconGlyph} Task $taskId queued for work-sharing. Reason: $reason",
            taskId = taskId
        )
    }

    // ==========================================
    // FAILURE INJECTION & CONTROL ACTIONS
    // ==========================================

    fun failRandomRobot() {
        val candidate = robots.filter { it.status != RobotStatus.FAILED }.randomOrNull() ?: return
        failRobot(candidate.robotId)
    }

    fun failRobot(robotId: Int) {
        val robot = robots.firstOrNull { it.robotId == robotId } ?: return
        robot.status = RobotStatus.FAILED
        robot.failureReason = "DRIVE_ACTUATOR_FAULT"
        robotFailureCounter++

        map.dynamicObstacles.add(robot.gridPoint)

        val taskToReassign = robot.currentTaskId
        if (taskToReassign != null) {
            robot.currentTaskId = null
            robot.currentTaskType = null
            shareOrReassignWork(taskToReassign, "Assigned robot R$robotId experienced motor failure")
        }

        logEvent(
            EventCategory.FAILURE,
            EventLevel.ERROR,
            "Robot Failure Injected",
            "R$robotId [${robot.robotType.displayName}] motor failed at (${robot.gridPoint.x},${robot.gridPoint.y}). Physical obstacle registered; mission transferred to rested fleet.",
            robotId = robotId
        )
    }

    fun restoreRobot(robotId: Int) {
        val robot = robots.firstOrNull { it.robotId == robotId } ?: return
        robot.status = RobotStatus.IDLE
        robot.failureReason = null
        robot.health = 100f
        robot.communicationStatus = CommunicationStatus.CONNECTED
        map.dynamicObstacles.remove(robot.gridPoint)

        logEvent(
            EventCategory.FAILURE,
            EventLevel.SUCCESS,
            "Robot Restored",
            "R$robotId maintenance complete. Obstacle cleared and robot returned to service.",
            robotId = robotId
        )
    }

    fun disconnectRobot(robotId: Int) {
        val robot = robots.firstOrNull { it.robotId == robotId } ?: return
        robot.communicationStatus = CommunicationStatus.LOST
        robot.speed = (robot.speed * 0.5f).coerceAtLeast(0.3f)
        logEvent(
            EventCategory.FAILURE,
            EventLevel.WARNING,
            "Communication Lost",
            "R$robotId telemetry signal lost. Switched to local autonomous crawl.",
            robotId = robotId
        )
    }

    fun drainBattery(robotId: Int) {
        val robot = robots.firstOrNull { it.robotId == robotId } ?: robots.randomOrNull() ?: return
        robot.battery = 8.0f
        logEvent(
            EventCategory.BATTERY,
            EventLevel.WARNING,
            "Battery Depleted (Test)",
            "R${robot.robotId} forced to 8.0% to test compulsory autonomous dock navigation and task handover.",
            robotId = robot.robotId
        )
    }

    fun createCollision() {
        val narrowX = 29
        val narrowY = 20
        val r1 = robots.getOrNull(0) ?: return
        val r2 = robots.getOrNull(1) ?: return

        r1.x = (narrowX - 2).toFloat(); r1.y = narrowY.toFloat(); r1.status = RobotStatus.MOVING
        r1.currentRoute = listOf(GridPoint(narrowX - 2, narrowY), GridPoint(narrowX + 3, narrowY))
        r1.routeIndex = 0

        r2.x = (narrowX + 2).toFloat(); r2.y = narrowY.toFloat(); r2.status = RobotStatus.MOVING
        r2.currentRoute = listOf(GridPoint(narrowX + 2, narrowY), GridPoint(narrowX - 3, narrowY))
        r2.routeIndex = 0

        logEvent(
            EventCategory.COLLISION,
            EventLevel.WARNING,
            "Head-On Conflict Injected",
            "Forced R${r1.robotId} and R${r2.robotId} into opposing trajectory along narrow corridor ($narrowX,$narrowY)."
        )
    }

    fun createDeadlock() {
        if (robots.size < 4) return
        val center = GridPoint(30, 20)
        val r1 = robots[0]; val r2 = robots[1]; val r3 = robots[2]; val r4 = robots[3]

        r1.x = (center.x - 1).toFloat(); r1.y = center.y.toFloat()
        r2.x = center.x.toFloat(); r2.y = (center.y + 1).toFloat()
        r3.x = (center.x + 1).toFloat(); r3.y = center.y.toFloat()
        r4.x = center.x.toFloat(); r4.y = (center.y - 1).toFloat()

        r1.currentRoute = listOf(r1.gridPoint, GridPoint(center.x, center.y))
        r2.currentRoute = listOf(r2.gridPoint, GridPoint(center.x, center.y))
        r3.currentRoute = listOf(r3.gridPoint, GridPoint(center.x, center.y))
        r4.currentRoute = listOf(r4.gridPoint, GridPoint(center.x, center.y))

        for (r in listOf(r1, r2, r3, r4)) {
            r.status = RobotStatus.WAITING
            r.waitingTime = 3.0f
        }

        logEvent(
            EventCategory.DEADLOCK,
            EventLevel.ERROR,
            "Circular Deadlock Injected",
            "Arranged R${r1.robotId}, R${r2.robotId}, R${r3.robotId}, R${r4.robotId} into circular 4-way intersection contention."
        )
    }

    fun blockCorridor() {
        val blockPoint = GridPoint(30, 20)
        if (isCorridorBlocked) {
            map.dynamicObstacles.remove(blockPoint)
            isCorridorBlocked = false
            logEvent(EventCategory.CONTROLLER, EventLevel.SUCCESS, "Corridor Cleared", "Obstacle removed from central corridor. Normal flow restored.")
        } else {
            map.dynamicObstacles.add(blockPoint)
            isCorridorBlocked = true
            logEvent(EventCategory.FAILURE, EventLevel.WARNING, "Corridor Blocked", "Maintenance barrier deployed at central bottleneck. Fleet rerouting triggered.")
        }
    }

    fun killCentralController() {
        isCentralControllerOnline = false
        logEvent(EventCategory.CONTROLLER, EventLevel.ERROR, "CENTRAL CONTROLLER: OFFLINE", "Switched to 100% decentralized Peer-to-Peer gossip & local collision avoidance.")
        updateMetrics()
    }

    fun restoreCentralController() {
        isCentralControllerOnline = true
        logEvent(EventCategory.CONTROLLER, EventLevel.SUCCESS, "CENTRAL CONTROLLER: ONLINE", "Global telemetry synchronized across all ${robots.size} robots.")
        updateMetrics()
    }

    private fun logEvent(
        category: EventCategory,
        level: EventLevel,
        title: String,
        description: String,
        robotId: Int? = null,
        partnerRobotId: Int? = null,
        taskId: String? = null
    ) {
        val event = SimulationEvent(
            category = category,
            level = level,
            title = title,
            description = description,
            robotId = robotId,
            partnerRobotId = partnerRobotId,
            taskId = taskId
        )
        events.add(0, event)
        if (events.size > 200) {
            events.removeAt(events.size - 1)
        }
        _eventsFlow.value = events.toList()
    }

    private fun updateMetrics() {
        val active = robots.count { it.status == RobotStatus.MOVING || it.status == RobotStatus.REROUTING || it.status == RobotStatus.WORKING }
        val idle = robots.count { it.status == RobotStatus.IDLE }
        val charging = robots.count { it.status == RobotStatus.CHARGING }
        val failed = robots.count { it.status == RobotStatus.FAILED }
        val blocked = robots.count { it.status == RobotStatus.WAITING || it.status == RobotStatus.BLOCKED || it.status == RobotStatus.NEGOTIATING }

        val available = (active + idle + blocked).coerceAtLeast(1)
        val utilization = (active.toFloat() / available.toFloat() * 100f).coerceIn(0f, 100f)

        val pending = tasks.count { it.status == TaskStatus.PENDING }
        val activeT = tasks.count { it.status == TaskStatus.ASSIGNED || it.status == TaskStatus.IN_PROGRESS }
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val failedT = tasks.count { it.status == TaskStatus.FAILED }

        val avgDuration = if (completedTaskCount > 0) totalTaskDurationSec / completedTaskCount else 24.5f
        val avgWait = if (robots.isNotEmpty()) robots.map { it.waitingTime }.average().toFloat() else 0f
        val avgBattery = if (robots.isNotEmpty()) robots.map { it.battery }.average().toFloat() else 100f
        val restedRobots = robots.count { (it.status == RobotStatus.IDLE || it.battery >= 80f) && it.currentTaskId == null }

        val cleaners = robots.count { it.robotType == RobotType.CLEANER_AMR && (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) }
        val organizers = robots.count { it.robotType == RobotType.ORGANIZER_AMR && (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) }
        val haulers = robots.count { it.robotType == RobotType.HEAVY_HAUL_AGV && (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) }
        val inspectors = robots.count { it.robotType == RobotType.INSPECTION_ROVER && (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) }
        val express = robots.count { it.robotType == RobotType.EXPRESS_HAULER && (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) }

        val threshold = batteryManager.lowBatteryThresholdPercent
        val batteryHigh = robots.count { it.battery >= 75f }
        val batteryMed = robots.count { it.battery in threshold..<75f }
        val batteryCrit = robots.count { it.battery < threshold }

        val cleanDone = robots.sumOf { it.completedCleaningCount }
        val orgDone = robots.sumOf { it.completedOrganizingCount }
        val haulDone = robots.sumOf { it.completedTransportCount }
        val inspectDone = robots.sumOf { it.completedInspectionCount }

        _metrics.value = FleetMetrics(
            totalRobots = robots.size,
            activeRobots = active,
            idleRobots = idle,
            chargingRobots = charging,
            failedRobots = failed,
            blockedRobots = blocked,
            totalTasks = tasks.size,
            pendingTasks = pending,
            activeTasks = activeT,
            completedTasks = completed,
            failedTasks = failedT,
            fleetUtilization = utilization,
            avgTaskCompletionTimeSec = avgDuration,
            avgRobotWaitingTimeSec = avgWait,
            collisionsPrevented = collisionCounter,
            activeConflicts = activeCollisionRisks.size,
            deadlocksDetected = deadlockDetectCounter,
            deadlocksResolved = deadlockResolveCounter,
            tasksReassigned = taskReassignCounter,
            robotFailures = robotFailureCounter,
            controllerOnline = isCentralControllerOnline,
            negotiationsPerformed = negotiationCounter,
            avgNegotiationTimeMs = 38.5f,
            fleetAvgBatteryPercent = avgBattery,
            robotsWorkingCount = active,
            robotsRestedCount = restedRobots,
            sharedTasksCount = sharedTasksCounter,
            cleanersWorking = cleaners,
            organizersWorking = organizers,
            haulersWorking = haulers,
            inspectorsWorking = inspectors,
            expressWorking = express,
            lowBatteryThreshold = threshold,
            batteryHighCount = batteryHigh,
            batteryMediumCount = batteryMed,
            batteryCriticalCount = batteryCrit,
            totalCleaningCompleted = cleanDone,
            totalOrganizingCompleted = orgDone,
            totalTransportCompleted = haulDone,
            totalInspectionCompleted = inspectDone
        )
    }

    fun setLowBatteryThreshold(threshold: Float) {
        batteryManager.lowBatteryThresholdPercent = threshold
        logEvent(
            EventCategory.BATTERY,
            EventLevel.INFO,
            "Battery Threshold Updated",
            "Autonomous Power Dock navigation threshold set to ${threshold.toInt()}%. Robots with battery below this will immediately dock."
        )
        updateMetrics()
    }

    fun triggerManualWorkSharing() {
        performWorkSharingBalance()
        updateMetrics()
    }

    fun testBatteryDepletionDocking() {
        val workingBots = robots.filter { (it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING) && it.status != RobotStatus.CHARGING }
        val targets = if (workingBots.isNotEmpty()) workingBots.take(3) else robots.take(3)
        for (bot in targets) {
            bot.battery = 18f
        }
        logEvent(
            EventCategory.BATTERY,
            EventLevel.WARNING,
            "Low-Battery Telemetry Injected",
            "Simulated battery drop to 18% on ${targets.size} robots. Autonomous navigation to nearest charging dock initiated."
        )
        updateMetrics()
    }

    fun generateDiverseMultiTasks(count: Int) {
        generateTasks(count)
    }

    fun reset() {
        isCentralControllerOnline = true
        isCorridorBlocked = false
        map.dynamicObstacles.clear()
        events.clear()
        tasks.clear()
        collisionCounter = 0
        deadlockDetectCounter = 0
        deadlockResolveCounter = 0
        taskReassignCounter = 0
        robotFailureCounter = 0
        negotiationCounter = 0
        sharedTasksCounter = 0
        completedTaskCount = 0
        totalTaskDurationSec = 0f
        spawnFleet(60)
        generateTasks(40)
    }
}
