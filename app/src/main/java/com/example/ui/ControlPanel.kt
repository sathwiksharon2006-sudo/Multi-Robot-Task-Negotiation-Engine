package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FleetSimulator
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.IndustrialSurface
import com.example.ui.theme.IndustrialSurfaceVariant
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RedEmergency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlPanel(
    simulator: FleetSimulator,
    onStartJuryDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IndustrialSurface)
            .border(1.dp, IndustrialBorder)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Star Feature: JURY DEMO MODE (Prominent Hero Button)
        Button(
            onClick = onStartJuryDemo,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EA),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_jury_demo")
        ) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent)
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = "⚡ START JURY DEMO (10-PHASE AUTOMATED SHOWCASE)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Simulation Speed & Engine Controls
        Text(
            text = "SIMULATION ENGINE CONTROLS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { simulator.isRunning = !simulator.isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (simulator.isRunning) AmberWarning else GreenSuccess,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_play_pause")
            ) {
                Icon(
                    if (simulator.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = if (simulator.isRunning) "PAUSE" else "RUN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = {
                    simulator.simulationSpeed = when (simulator.simulationSpeed) {
                        1.0f -> 2.0f
                        2.0f -> 5.0f
                        else -> 1.0f
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndustrialSurfaceVariant,
                    contentColor = CyanAccent
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_speed")
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(
                    text = "${simulator.simulationSpeed.toInt()}x SPEED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = { simulator.reset() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndustrialSurfaceVariant,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_reset")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(
                    text = "RESET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fleet Density Scale Controls
        Text(
            text = "FLEET SCALING",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FleetScaleButton("10 ROBOTS", { simulator.spawnFleet(10) }, Modifier.weight(1f))
            FleetScaleButton("100 ROBOTS", { simulator.spawnFleet(100) }, Modifier.weight(1f))
            FleetScaleButton("500 ROBOTS", { simulator.spawnFleet(500) }, Modifier.weight(1f), isHighlight = true)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Task Generation Controls
        Text(
            text = "MULTI-TASK WORKLOAD GENERATOR",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { simulator.generateDiverseMultiTasks(25) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1), contentColor = Color.White),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_gen_tasks")
            ) {
                Text("+25 MULTI-TASKS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { simulator.generateDiverseMultiTasks(1000) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0), contentColor = CyanAccent),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_flood_tasks")
            ) {
                Text("FLOOD 1,000 TASKS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Work Sharing & Cooperative Load Balancing Controls
        Text(
            text = "COOPERATIVE WORK-SHARING & RESTED WORKFORCE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GreenSuccess,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { simulator.triggerManualWorkSharing() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D25), contentColor = GreenSuccess),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text("🤝 SHARE WORK (RESTED)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { simulator.testBatteryDepletionDocking() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2800), contentColor = AmberWarning),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text("⚡ TEST AUTO-DOCK (<25%)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Autonomous Charging Threshold Configurator
        Text(
            text = "AUTONOMOUS CHARGING THRESHOLD (ROBOT AUTO-DOCKS WHEN BELOW)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val currThreshold = simulator.batteryManager.lowBatteryThresholdPercent
            listOf(15f, 25f, 35f, 50f).forEach { t ->
                val isSelected = (currThreshold == t)
                Button(
                    onClick = { simulator.setLowBatteryThreshold(t) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF00695C) else Color(0xFF131D30),
                        contentColor = if (isSelected) CyanAccent else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanAccent) else null,
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text("<${t.toInt()}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Failure Injection Controls (Industrial Testing Suite)
        Text(
            text = "FAILURE INJECTION & CONFLICT ENGINE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = RedEmergency,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionChip(
                label = "CREATE COLLISION",
                onClick = { simulator.createCollision() },
                color = AmberWarning
            )
            ActionChip(
                label = "CREATE DEADLOCK",
                onClick = { simulator.createDeadlock() },
                color = PurpleNegotiating
            )
            ActionChip(
                label = "FAIL RANDOM ROBOT",
                onClick = { simulator.failRandomRobot() },
                color = RedEmergency
            )
            ActionChip(
                label = "DRAIN BATTERY (5%)",
                onClick = { simulator.drainBattery(1) },
                color = AmberWarning
            )
            ActionChip(
                label = if (simulator.isCorridorBlocked) "CLEAR CORRIDOR" else "BLOCK CORRIDOR",
                onClick = { simulator.blockCorridor() },
                color = if (simulator.isCorridorBlocked) GreenSuccess else RedEmergency
            )
            ActionChip(
                label = "DISCONNECT ROBOT",
                onClick = { simulator.disconnectRobot(1) },
                color = Color(0xFF9E9E9E)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Decentralized Controller Resilience
        Text(
            text = "CENTRAL CONTROLLER RESILIENCE TEST",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { simulator.killCentralController() },
                enabled = simulator.isCentralControllerOnline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedEmergency,
                    disabledContainerColor = Color(0xFF261217),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_kill_controller")
            ) {
                Text("KILL CONTROLLER", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { simulator.restoreCentralController() },
                enabled = !simulator.isCentralControllerOnline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenSuccess,
                    disabledContainerColor = Color(0xFF0F2618),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_restore_controller")
            ) {
                Text("RESTORE CONTROLLER", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FleetScaleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHighlight) Color(0xFF00695C) else IndustrialSurfaceVariant,
            contentColor = if (isHighlight) GreenSuccess else Color.White
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(34.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
