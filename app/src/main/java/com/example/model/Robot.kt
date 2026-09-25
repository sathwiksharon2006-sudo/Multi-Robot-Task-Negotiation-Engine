package com.example.model

enum class RobotType(
    val displayName: String,
    val categoryName: String,
    val baseSpeed: Float,
    val loadCapacityKg: Float,
    val batteryCapacityAh: Float,
    val powerConsumptionRate: Float,
    val primaryRole: String
) {
    CLEANER_AMR(
        displayName = "AeroScrub AMR",
        categoryName = "Facility Cleaner",
        baseSpeed = 0.95f,
        loadCapacityKg = 40f,
        batteryCapacityAh = 110f,
        powerConsumptionRate = 0.045f,
        primaryRole = "Corridor Sanitation & Aisle Sweeping"
    ),
    ORGANIZER_AMR(
        displayName = "SortMaster AMR",
        categoryName = "Shelf Organizer",
        baseSpeed = 0.9f,
        loadCapacityKg = 80f,
        batteryCapacityAh = 100f,
        powerConsumptionRate = 0.05f,
        primaryRole = "Bin Sorting & Shelf Organization"
    ),
    HEAVY_HAUL_AGV(
        displayName = "TitanLift AGV",
        categoryName = "Heavy Hauler",
        baseSpeed = 0.7f,
        loadCapacityKg = 500f,
        batteryCapacityAh = 200f,
        powerConsumptionRate = 0.085f,
        primaryRole = "Bulk Pallet & Heavy Cargo Transport"
    ),
    EXPRESS_HAULER(
        displayName = "VelocitySpeed AMR",
        categoryName = "Express Courier",
        baseSpeed = 1.6f,
        loadCapacityKg = 50f,
        batteryCapacityAh = 85f,
        powerConsumptionRate = 0.065f,
        primaryRole = "Priority Rapid Delivery"
    ),
    INSPECTION_ROVER(
        displayName = "SentinelEye Rover",
        categoryName = "Safety Inspector",
        baseSpeed = 1.25f,
        loadCapacityKg = 25f,
        batteryCapacityAh = 90f,
        powerConsumptionRate = 0.035f,
        primaryRole = "Hazard, Thermal & Structural Scanning"
    ),
    GENERAL_TRANSPORT(
        displayName = "OmniCarrier AMR",
        categoryName = "General Transport",
        baseSpeed = 1.05f,
        loadCapacityKg = 130f,
        batteryCapacityAh = 105f,
        powerConsumptionRate = 0.05f,
        primaryRole = "Standard Goods Transfer"
    )
}

enum class RobotStatus {
    IDLE,
    MOVING,
    WORKING,
    WAITING,
    NEGOTIATING,
    CHARGING,
    LOW_BATTERY,
    FAILED,
    COMMUNICATION_LOST,
    BLOCKED,
    REROUTING,
    RECOVERING,
    MAINTENANCE
}

enum class CommunicationStatus {
    CONNECTED,
    DELAYED,
    LOST
}

enum class Capability {
    TRANSPORT,
    CLEANING,
    ORGANIZING,
    HEAVY_LOAD,
    FAST_HAUL,
    INSPECT
}

data class NegotiationState(
    val partnerRobotId: Int,
    val resourceLocation: GridPoint,
    val conflictReason: String,
    val priorityScore: Float,
    val isWinner: Boolean = false,
    val resolutionAction: String = "NEGOTIATING",
    val timestamp: Long = System.currentTimeMillis()
)

data class Robot(
    val robotId: Int,
    val robotType: RobotType,
    var x: Float,
    var y: Float,
    var previousX: Float = x,
    var previousY: Float = y,
    var targetX: Float = x,
    var targetY: Float = y,
    var speed: Float = robotType.baseSpeed,
    var battery: Float = 100f,
    val batteryCapacity: Float = robotType.batteryCapacityAh,
    var status: RobotStatus = RobotStatus.IDLE,
    var health: Float = 100f,
    var currentTaskId: String? = null,
    var currentTaskType: TaskType? = null,
    var taskPriority: TaskPriority = TaskPriority.NORMAL,
    val capabilities: Set<Capability> = getDefaultCapabilities(robotType),
    var currentRoute: List<GridPoint> = emptyList(),
    var routeIndex: Int = 0,
    val loadCapacity: Float = robotType.loadCapacityKg,
    var currentLoad: Float = 0f,
    var communicationStatus: CommunicationStatus = CommunicationStatus.CONNECTED,
    var negotiationState: NegotiationState? = null,
    var waitingTime: Float = 0f,
    var idleTime: Float = 0f,
    var workingTime: Float = 0f,
    var tasksCompletedCount: Int = 0,
    var failureReason: String? = null,
    var estimatedCompletionTime: Float = 0f,
    var assignedChargingDock: GridPoint? = null,
    // Multi-tasking state & work-sharing telemetry
    val taskHistory: MutableList<TaskType> = mutableListOf(),
    var completedCleaningCount: Int = 0,
    var completedOrganizingCount: Int = 0,
    var completedTransportCount: Int = 0,
    var completedInspectionCount: Int = 0,
    var workSharedHandoverCount: Int = 0,
    var isRested: Boolean = true
) {
    companion object {
        fun getDefaultCapabilities(type: RobotType): Set<Capability> = when (type) {
            // All robots are multi-capable: they can perform their primary specialization + secondary tasks like cleaning, organizing, hauling
            RobotType.CLEANER_AMR -> setOf(Capability.CLEANING, Capability.ORGANIZING, Capability.TRANSPORT)
            RobotType.ORGANIZER_AMR -> setOf(Capability.ORGANIZING, Capability.CLEANING, Capability.TRANSPORT)
            RobotType.HEAVY_HAUL_AGV -> setOf(Capability.HEAVY_LOAD, Capability.TRANSPORT, Capability.ORGANIZING)
            RobotType.EXPRESS_HAULER -> setOf(Capability.FAST_HAUL, Capability.TRANSPORT, Capability.INSPECT)
            RobotType.INSPECTION_ROVER -> setOf(Capability.INSPECT, Capability.CLEANING, Capability.TRANSPORT)
            RobotType.GENERAL_TRANSPORT -> setOf(Capability.TRANSPORT, Capability.ORGANIZING, Capability.CLEANING)
        }
    }

    val gridPoint: GridPoint
        get() = GridPoint(x.toInt().coerceAtLeast(0), y.toInt().coerceAtLeast(0))

    fun isEligibleForTask(batteryThreshold: Float = 25f): Boolean {
        return status != RobotStatus.FAILED &&
                status != RobotStatus.CHARGING &&
                status != RobotStatus.MAINTENANCE &&
                battery > batteryThreshold &&
                currentTaskId == null
    }

    fun hasCapability(cap: Capability): Boolean = capabilities.contains(cap)

    fun recordCompletedTask(type: TaskType) {
        taskHistory.add(0, type)
        if (taskHistory.size > 8) taskHistory.removeAt(taskHistory.size - 1)
        tasksCompletedCount++
        when (type) {
            TaskType.AISLE_CLEANING -> completedCleaningCount++
            TaskType.SHELF_ORGANIZING -> completedOrganizingCount++
            TaskType.LOGISTICS_TRANSPORT, TaskType.HEAVY_HAUL -> completedTransportCount++
            TaskType.SAFETY_INSPECTION -> completedInspectionCount++
            else -> {}
        }
    }

    fun getMultiTaskSummary(): String {
        val parts = mutableListOf<String>()
        if (completedCleaningCount > 0) parts.add("🧹 $completedCleaningCount Cleaned")
        if (completedOrganizingCount > 0) parts.add("📦 $completedOrganizingCount Organised")
        if (completedTransportCount > 0) parts.add("🚚 $completedTransportCount Hauled")
        if (completedInspectionCount > 0) parts.add("🔍 $completedInspectionCount Inspected")
        return if (parts.isEmpty()) "Multi-Skilled Ready" else parts.joinToString(" • ")
    }
}
