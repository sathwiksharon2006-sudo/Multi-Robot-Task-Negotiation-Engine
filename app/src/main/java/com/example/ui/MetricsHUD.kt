package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FleetMetrics
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.IndustrialSurface
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RedEmergency

/**
 * Compact System Status Header (approximately 48dp height).
 * Replaces the large top status area so the 3D warehouse can start near the top
 * and occupy 65-75% of the viewport.
 */
@Composable
fun CompactSystemHeader(
    metrics: FleetMetrics,
    isDemoActive: Boolean,
    onControllerClick: () -> Unit,
    onEnergyClick: () -> Unit,
    onRobotsClick: () -> Unit = {},
    onConflictsClick: () -> Unit = {},
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Header_Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(IndustrialSurface)
            .border(BorderStroke(1.dp, IndustrialBorder))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LEFT: System Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "MULTI-ROBOT ENGINE",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.4.sp
            )
        }

        // CENTER/RIGHT: Compact Badges (Controller, Fleet Energy, Robots, Conflicts, Demo)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            // 1. Controller Status Pill (Compact Badge, <=135dp)
            val isOnline = metrics.controllerOnline
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .background(
                        color = if (isOnline) GreenSuccess.copy(alpha = 0.12f) else RedEmergency.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isOnline) GreenSuccess.copy(alpha = 0.6f) else RedEmergency.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onControllerClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = (if (isOnline) GreenSuccess else RedEmergency).copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (isOnline) "● ONLINE" else "● OFFLINE",
                        color = if (isOnline) GreenSuccess else RedEmergency,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 2. Fleet Energy Compact Indicator
            val avgBat = metrics.fleetAvgBatteryPercent.toInt()
            val batColor = if (avgBat > 60) GreenSuccess else if (avgBat > 30) AmberWarning else RedEmergency
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .background(
                        color = Color(0xFF0F1A2A),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = batColor.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onEnergyClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔋 $avgBat%",
                    color = batColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 3. Robot Fleet Count
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .background(
                        color = Color(0xFF0F1A2A),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = CyanAccent.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onRobotsClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖 ${metrics.totalRobots}",
                    color = CyanAccent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 4. Conflicts Badge
            val hasConflicts = metrics.activeConflicts > 0
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .background(
                        color = if (hasConflicts) AmberWarning.copy(alpha = 0.15f) else Color(0xFF0F1A2A),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (hasConflicts) AmberWarning.copy(alpha = 0.7f) else Color(0xFF334155),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onConflictsClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠ ${metrics.activeConflicts}",
                    color = if (hasConflicts) AmberWarning else Color(0xFF94A3B8),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 5. Small [ DEMO ] Button
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .background(
                        color = if (isDemoActive) PurpleNegotiating.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDemoActive) PurpleNegotiating else CyanAccent.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onDemoClick() }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDemoActive) "■ STOP" else "▶ DEMO",
                    color = if (isDemoActive) PurpleNegotiating else CyanAccent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Fleet Stats & Telemetry Panel (Moved to bottom section under the 3D warehouse).
 * Preserves all detailed metrics, work-sharing statistics, and multi-tasking telemetry.
 */
@Composable
fun FleetStatsBottomPanel(
    metrics: FleetMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(IndustrialSurface, Color(0xFF070C16))
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        colors = listOf(CyanAccent.copy(alpha = 0.4f), PurpleNegotiating.copy(alpha = 0.4f))
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Summary & Multi-Task Status Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FLEET STATS • ${metrics.sharedTasksCount} SHARED • <${metrics.lowBatteryThreshold.toInt()}% AUTO-DOCK",
                fontSize = 8.5.sp,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "🧹 ${metrics.totalCleaningCompleted} • 📦 ${metrics.totalOrganizingCompleted} • 🚚 ${metrics.totalTransportCompleted}",
                fontSize = 8.5.sp,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontally Scrollable Cards for Complete Fleet Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Work Sharing & Rested Reliever Card
            MetricCard(
                title = "WORK SHARED",
                value = "${metrics.sharedTasksCount}",
                subtitle = "${metrics.robotsRestedCount} RESTED READY",
                accentColor = GreenSuccess
            )

            // Robots Active / Working
            MetricCard(
                title = "WORKING AMRs",
                value = "${metrics.robotsWorkingCount} / ${metrics.totalRobots}",
                subtitle = "${metrics.chargingRobots} CHARGING",
                accentColor = CyanAccent
            )

            // Category: Facility Cleaners
            MetricCard(
                title = "🧹 CLEANERS",
                value = "${metrics.cleanersWorking}",
                subtitle = "SWEEPING AISLES",
                accentColor = GreenSuccess
            )

            // Category: Shelf Organizers
            MetricCard(
                title = "📦 ORGANIZERS",
                value = "${metrics.organizersWorking}",
                subtitle = "BIN & SHELF SORT",
                accentColor = ElectricGold
            )

            // Category: Heavy Haulers
            MetricCard(
                title = "🏗️ HEAVY AGVs",
                value = "${metrics.haulersWorking}",
                subtitle = "PALLET TRANSFER",
                accentColor = RedEmergency
            )

            // Category: Express Couriers
            MetricCard(
                title = "⚡ EXPRESS",
                value = "${metrics.expressWorking}",
                subtitle = "RAPID TRANSIT",
                accentColor = CyanAccent
            )

            // Category: Safety Inspectors
            MetricCard(
                title = "🔍 INSPECTORS",
                value = "${metrics.inspectorsWorking}",
                subtitle = "HAZARD SCANNING",
                accentColor = PurpleNegotiating
            )

            // Collision Averted
            MetricCard(
                title = "3D AVERTED",
                value = "${metrics.collisionsPrevented}",
                subtitle = "${metrics.activeConflicts} RISKS",
                accentColor = ElectricGold
            )

            // Deadlocks Recovered
            MetricCard(
                title = "DEADLOCKS",
                value = "${metrics.deadlocksDetected}",
                subtitle = "${metrics.deadlocksResolved} BROKEN",
                accentColor = if (metrics.deadlocksDetected > 0) RedEmergency else Color(0xFF90A4AE)
            )
        }
    }
}

/**
 * Backwards compatibility wrapper for MetricsHUD
 */
@Composable
fun MetricsHUD(
    metrics: FleetMetrics,
    modifier: Modifier = Modifier
) {
    FleetStatsBottomPanel(metrics = metrics, modifier = modifier)
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF131D32), Color(0xFF090E1A))
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.5f), Color(0xFF1E2D45)))
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 8.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.3.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subtitle,
            fontSize = 7.5.sp,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
        )
    }
}
