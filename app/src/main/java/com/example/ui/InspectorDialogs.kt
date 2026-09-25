package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.Task
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.IndustrialSurface
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RedEmergency

@Composable
fun RobotDetailDialog(
    robot: Robot,
    onDismiss: () -> Unit,
    onFailRobot: (Int) -> Unit,
    onRestoreRobot: (Int) -> Unit,
    onDrainBattery: (Int) -> Unit,
    onDisconnect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IndustrialSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ROBOT R${robot.robotId} TELEMETRY",
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = robot.status.name,
                    color = when (robot.status) {
                        RobotStatus.FAILED -> RedEmergency
                        RobotStatus.CHARGING -> Color(0xFF7C4DFF)
                        RobotStatus.MOVING -> CyanAccent
                        RobotStatus.WAITING -> AmberWarning
                        RobotStatus.NEGOTIATING -> PurpleNegotiating
                        else -> GreenSuccess
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B101D), RoundedCornerShape(6.dp))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("Category", "${robot.robotType.categoryName} (${robot.robotType.displayName})")
                DetailRow("Primary Role", robot.robotType.primaryRole)
                DetailRow("Position", "(${String.format("%.1f", robot.x)}, ${String.format("%.1f", robot.y)})")
                DetailRow("Battery Level", "${robot.battery.toInt()}% [Auto-Docks at <25%]")
                DetailRow("Health & Condition", "${robot.health.toInt()}%")
                DetailRow("Comm Status", robot.communicationStatus.name)
                DetailRow("Payload", "${robot.currentLoad.toInt()} kg / Max ${robot.loadCapacity.toInt()} kg")
                DetailRow("Speed", "${String.format("%.2f", robot.speed)} m/s")
                DetailRow("Active Mission", robot.currentTaskId?.let { "${robot.currentTaskType?.categoryLabel ?: "Task"} ($it)" } ?: "IDLE / RESTED")
                DetailRow("Multi-Task Record", robot.getMultiTaskSummary(), CyanAccent)
                DetailRow("Work Shared (Relieved)", "${robot.workSharedHandoverCount} missions handed over", GreenSuccess)
                DetailRow("Rested Status", if (robot.isRested) "⚡ RESTED & READY" else "ACTIVE ON DUTY", if (robot.isRested) GreenSuccess else AmberWarning)
                DetailRow("Missions Completed", "${robot.tasksCompletedCount}")
                DetailRow("Capabilities", robot.capabilities.joinToString(", ") { it.name })
                robot.assignedChargingDock?.let {
                    DetailRow("Assigned Power Dock", "Bay at (${it.x}, ${it.y})", ElectricGold)
                }
                DetailRow("Waiting Time", "${String.format("%.1f", robot.waitingTime)}s")
                robot.failureReason?.let { DetailRow("Failure Reason", it, RedEmergency) }
                robot.negotiationState?.let {
                    DetailRow("P2P State", "Conflict with R${it.partnerRobotId} - ${it.resolutionAction}", PurpleNegotiating)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FIELD MAINTENANCE ACTIONS",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (robot.status == RobotStatus.FAILED) {
                        Button(
                            onClick = { onRestoreRobot(robot.robotId) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("RESTORE", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Button(
                            onClick = { onFailRobot(robot.robotId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedEmergency),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("FAIL MOTOR", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = { onDrainBattery(robot.robotId) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("DRAIN BATTERY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = CyanAccent, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun TaskDetailDialog(
    task: Task,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IndustrialSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TASK ${task.taskId}",
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = task.status.name,
                    color = when (task.status) {
                        com.example.model.TaskStatus.COMPLETED -> GreenSuccess
                        com.example.model.TaskStatus.IN_PROGRESS -> CyanAccent
                        com.example.model.TaskStatus.PENDING -> AmberWarning
                        com.example.model.TaskStatus.FAILED -> RedEmergency
                        else -> PurpleNegotiating
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B101D), RoundedCornerShape(6.dp))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("Type", task.taskType.name)
                DetailRow("Priority", task.priority.name)
                DetailRow("Required Capability", task.requiredCapability.name)
                DetailRow("Pickup Station", "(${task.pickupLocation.x}, ${task.pickupLocation.y})")
                DetailRow("Delivery Dock", "(${task.deliveryLocation.x}, ${task.deliveryLocation.y})")
                DetailRow("Cargo Weight", "${task.weight.toInt()} kg")
                DetailRow("Assigned Robot", if (task.assignedRobotId != null) "R${task.assignedRobotId}" else "UNASSIGNED")
                DetailRow("Reassignment Count", "${task.reassignmentCount}")
                DetailRow("Submitted Bids", "${task.bids.size} bids recorded")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = CyanAccent, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CentralControllerDialog(
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onToggleController: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IndustrialSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = if (isOnline) "● CONTROLLER ONLINE" else "● CONTROLLER OFFLINE",
                    color = if (isOnline) GreenSuccess else RedEmergency,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isOnline) "OPTIONAL" else "DECENTRALIZED",
                    color = if (isOnline) CyanAccent else AmberWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B101D), RoundedCornerShape(6.dp))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isOnline) {
                        "Global coordination service is available.\nRobots retain local decision-making and can continue existing missions if the controller fails."
                    } else {
                        "Central controller unavailable.\nExisting robot missions continue using local/peer-to-peer coordination."
                    },
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = IndustrialBorder)
                Spacer(modifier = Modifier.height(4.dp))

                DetailRow(
                    "Architecture",
                    if (isOnline) "Decentralized Swarm + Global Sync" else "100% P2P Gossip Active",
                    if (isOnline) CyanAccent else AmberWarning
                )
                DetailRow(
                    "Mission Status",
                    "Continuous execution uninterrupted",
                    GreenSuccess
                )
                DetailRow(
                    "P2P Conflict Protocol",
                    "Distributed spatial priority locks",
                    PurpleNegotiating
                )

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        onToggleController()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnline) RedEmergency else GreenSuccess,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(
                        text = if (isOnline) "KILL CONTROLLER (TEST RESILIENCE)" else "RESTORE CENTRAL CONTROLLER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = CyanAccent, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun FleetEnergyDialog(
    avgBatteryPercent: Float,
    healthyCount: Int,
    lowCount: Int,
    criticalCount: Int,
    chargingCount: Int,
    autoDockThreshold: Float,
    onDismiss: () -> Unit
) {
    val avgBat = avgBatteryPercent.toInt()
    val batColor = if (avgBat > 60) GreenSuccess else if (avgBat > 30) AmberWarning else RedEmergency

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IndustrialSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "FLEET ENERGY",
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "🔋 $avgBat% avg",
                    color = batColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B101D), RoundedCornerShape(6.dp))
                    .border(1.dp, IndustrialBorder, RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Fleet Energy represents the current overall energy condition of the simulated robot fleet.",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = IndustrialBorder)
                Spacer(modifier = Modifier.height(4.dp))

                DetailRow("Average Fleet Level", "$avgBat%", batColor)
                DetailRow("Healthy (>75%)", "$healthyCount robots", GreenSuccess)
                DetailRow("Low Operating", "$lowCount robots", AmberWarning)
                DetailRow("Critical (<${autoDockThreshold.toInt()}%)", "$criticalCount robots", RedEmergency)
                DetailRow("Charging in Docks", "$chargingCount robots", ElectricGold)
                DetailRow("Auto-Dock Policy", "<${autoDockThreshold.toInt()}% Auto-Navigation", CyanAccent)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = CyanAccent, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
