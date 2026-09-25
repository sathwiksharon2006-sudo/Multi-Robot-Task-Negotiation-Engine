package com.example.model

enum class TaskType(val categoryLabel: String, val iconGlyph: String) {
    LOGISTICS_TRANSPORT("Transport", "🚚"),
    AISLE_CLEANING("Sanitation & Cleaning", "🧹"),
    SHELF_ORGANIZING("Shelf Sorting & Organizing", "📦"),
    SAFETY_INSPECTION("Safety & Hazard Scan", "🔍"),
    HEAVY_HAUL("Heavy Bulk Hauling", "🏗️"),
    CHARGING("Battery Replenishment", "⚡"),
    EMERGENCY("Critical Hazard Clear", "🚨")
}

enum class TaskPriority(val level: Int, val weightMultiplier: Float) {
    LOW(1, 0.7f),
    NORMAL(2, 1.0f),
    HIGH(3, 1.5f),
    CRITICAL(4, 2.5f)
}

enum class TaskStatus(val displayName: String) {
    CREATED("Created"),
    ANNOUNCED("Announced"),
    PENDING("Queued"),
    NEGOTIATING("Negotiating Bids"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

data class TaskBid(
    val robotId: Int,
    val score: Float,
    val distanceCost: Float,
    val batteryScore: Float,
    val capabilityMatch: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class Task(
    val taskId: String,
    val taskType: TaskType,
    val pickupLocation: GridPoint,
    val deliveryLocation: GridPoint,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val requiredCapability: Capability = Capability.TRANSPORT,
    val weight: Float = 25f,
    val estimatedDuration: Float = 30f,
    val deadline: Long = System.currentTimeMillis() + 180_000L,
    var assignedRobotId: Int? = null,
    var status: TaskStatus = TaskStatus.PENDING,
    val creationTime: Long = System.currentTimeMillis(),
    var completionTime: Long? = null,
    var bids: MutableList<TaskBid> = mutableListOf(),
    var reassignmentCount: Int = 0,
    var isSharedWorkHandover: Boolean = false,
    var batteryConsumed: Float = 0f,
    var distanceTraveled: Float = 0f,
    var whyAssignedExplanation: String = ""
) {
    val pickupName: String
        get() = "Pickup (${pickupLocation.x}, ${pickupLocation.y})"

    val destinationName: String
        get() = when (taskType) {
            TaskType.AISLE_CLEANING -> "Aisle Sanitation (${deliveryLocation.x}, ${deliveryLocation.y})"
            TaskType.SHELF_ORGANIZING -> "Storage Shelf (${deliveryLocation.x}, ${deliveryLocation.y})"
            TaskType.HEAVY_HAUL -> "Heavy Dock (${deliveryLocation.x}, ${deliveryLocation.y})"
            TaskType.SAFETY_INSPECTION -> "Inspection Zone (${deliveryLocation.x}, ${deliveryLocation.y})"
            TaskType.CHARGING -> "Charging Bay (${deliveryLocation.x}, ${deliveryLocation.y})"
            else -> "Delivery Station (${deliveryLocation.x}, ${deliveryLocation.y})"
        }
}
