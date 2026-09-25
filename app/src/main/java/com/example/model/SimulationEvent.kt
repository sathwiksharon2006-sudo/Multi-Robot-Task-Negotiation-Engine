package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EventLevel {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

enum class EventCategory {
    ALL,
    TASK,
    NEGOTIATION,
    COLLISION,
    DEADLOCK,
    BATTERY,
    FAILURE,
    CONTROLLER
}

data class SimulationEvent(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: EventCategory,
    val level: EventLevel,
    val title: String,
    val description: String,
    val robotId: Int? = null,
    val partnerRobotId: Int? = null,
    val taskId: String? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
