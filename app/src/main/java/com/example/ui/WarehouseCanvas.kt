package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CameraFocusTarget
import com.example.engine.CollisionRisk
import com.example.engine.DeadlockCycle
import com.example.model.CellType
import com.example.model.Robot
import com.example.model.RobotStatus
import com.example.model.RobotType
import com.example.model.TaskType
import com.example.model.WarehouseMap
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.ChargingPadNeon
import com.example.ui.theme.CrateColor1
import com.example.ui.theme.CrateColor2
import com.example.ui.theme.CrateColor3
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeliveryPadNeon
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.FloorGridLine
import com.example.ui.theme.FloorTileDark
import com.example.ui.theme.FloorTileLight
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.IndustrialDarkBg
import com.example.ui.theme.NarrowCorridorGlow
import com.example.ui.theme.PickupPadNeon
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RackBeam
import com.example.ui.theme.RackFront
import com.example.ui.theme.RackSide
import com.example.ui.theme.RackTop
import com.example.ui.theme.RedEmergency
import com.example.ui.theme.RobotChassisDark
import com.example.ui.theme.RobotChassisRim
import com.example.ui.theme.RobotHeadlight
import com.example.ui.theme.RobotLiDAR
import com.example.ui.theme.StatusBlockedColor
import com.example.ui.theme.StatusChargingColor
import com.example.ui.theme.StatusCommLostColor
import com.example.ui.theme.StatusFailedColor
import com.example.ui.theme.StatusIdleColor
import com.example.ui.theme.StatusLowBatteryColor
import com.example.ui.theme.StatusMovingColor
import com.example.ui.theme.StatusNegotiatingColor
import com.example.ui.theme.StatusRecoveringColor
import com.example.ui.theme.StatusReroutingColor
import com.example.ui.theme.StatusWaitingColor
import com.example.ui.theme.StatusWorkingColor
import com.example.ui.theme.WallSideLeft
import com.example.ui.theme.WallSideRight
import com.example.ui.theme.WallTop
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class CameraProjection {
    ISOMETRIC_3D,
    TOP_DOWN_3D
}

data class VisualLayers(
    var showRobots: Boolean = true,
    var showRoutes: Boolean = true,
    var showTaskLabels: Boolean = true,
    var showRobotIds: Boolean = true,
    var showChargingStations: Boolean = true,
    var showConflicts: Boolean = true,
    var showNegotiations: Boolean = true,
    var showObstacles: Boolean = true,
    var showWarehouseZones: Boolean = true,
    var showDestinations: Boolean = true
)

@Composable
fun WarehouseCanvas(
    map: WarehouseMap,
    robots: List<Robot>,
    activeCollisionRisks: List<CollisionRisk>,
    activeDeadlocks: List<DeadlockCycle>,
    selectedRobot: Robot?,
    onSelectRobot: (Robot) -> Unit,
    cameraFocusTarget: CameraFocusTarget? = null,
    highlightedRobotIds: Set<Int> = emptySet(),
    modifier: Modifier = Modifier
) {
    var projectionMode by remember { mutableStateOf(CameraProjection.ISOMETRIC_3D) }
    var showDynamicLighting by remember { mutableStateOf(true) }
    var showLiDARAndLights by remember { mutableStateOf(true) }
    var showLayersMenu by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }

    var visualLayers by remember { mutableStateOf(VisualLayers()) }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Enhanced 3D Presentation, Camera Follow & Filter State
    var lockedOnRobotId by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var brightnessLevel by remember { mutableFloatStateOf(1.4f) }
    var lightMode by remember { mutableStateOf("DUAL") } // "CAMERA", "CLUSTERS", "DUAL"

    val textMeasurer = rememberTextMeasurer()

    // Smooth real-time camera tracking when locked on a robot
    val lockedRobot = robots.firstOrNull { it.robotId == lockedOnRobotId }
    LaunchedEffect(lockedOnRobotId, lockedRobot?.x, lockedRobot?.y, scale, projectionMode) {
        if (lockedRobot != null) {
            val baseCellW = (800f / map.width) * scale
            val baseCellH = (600f / map.height) * scale
            val isoStepX = baseCellW * 0.866f
            val isoStepY = baseCellH * 0.5f

            if (projectionMode == CameraProjection.ISOMETRIC_3D) {
                offsetX = -((lockedRobot.x - lockedRobot.y) * isoStepX) * 0.82f
                offsetY = -((lockedRobot.x + lockedRobot.y) * isoStepY) * 0.65f + 140f
            } else {
                offsetX = -(lockedRobot.x * baseCellW) + 400f
                offsetY = -(lockedRobot.y * baseCellH) + 300f
            }
        }
    }

    // Smooth camera transition when cameraFocusTarget changes (from Mission Story or presets)
    LaunchedEffect(cameraFocusTarget) {
        if (cameraFocusTarget != null) {
            val targetScale = cameraFocusTarget.targetZoom
            // Center the targeted grid coordinate
            val gx = cameraFocusTarget.gridX
            val gy = cameraFocusTarget.gridY
            val baseCellW = (800f / map.width) * targetScale
            val baseCellH = (600f / map.height) * targetScale
            val isoStepX = baseCellW * 0.866f
            val isoStepY = baseCellH * 0.5f

            val targetOffsetX = -((gx - gy) * isoStepX) * 0.7f
            val targetOffsetY = -((gx + gy) * isoStepY) * 0.45f

            scale = targetScale
            offsetX = targetOffsetX
            offsetY = targetOffsetY
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Robotics3D_Animations")
    val animAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animAngle"
    )
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialDarkBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(robots, scale, offsetX, offsetY, projectionMode) {
                detectTapGestures { tapOffset ->
                    val clicked = findRobotAtScreenPos(
                        tapOffset = tapOffset,
                        robots = robots,
                        map = map,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        canvasSize = Size(size.width.toFloat(), size.height.toFloat()),
                        mode = projectionMode
                    )
                    if (clicked != null) {
                        onSelectRobot(clicked)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val baseCellW = (canvasW / map.width) * scale
            val baseCellH = (canvasH / map.height) * scale

            val isoStepX = baseCellW * 0.866f
            val isoStepY = baseCellH * 0.5f
            val isoZScale = baseCellH * 0.72f

            val originX = if (projectionMode == CameraProjection.ISOMETRIC_3D) canvasW * 0.5f + offsetX else offsetX
            val originY = if (projectionMode == CameraProjection.ISOMETRIC_3D) canvasH * 0.22f + offsetY else offsetY

            fun project(gx: Float, gy: Float, gz: Float = 0f): Offset {
                return if (projectionMode == CameraProjection.ISOMETRIC_3D) {
                    val sx = (gx - gy) * isoStepX + originX
                    val sy = (gx + gy) * isoStepY - gz * isoZScale + originY
                    Offset(sx, sy)
                } else {
                    val sx = gx * baseCellW + originX
                    val sy = gy * baseCellH + originY
                    Offset(sx, sy)
                }
            }

            // ==========================================
            // 1. FLOOR GRID TILES & FUNCTIONAL ZONES
            // ==========================================
            for (gx in 0 until map.width) {
                for (gy in 0 until map.height) {
                    val p0 = project(gx.toFloat(), gy.toFloat(), 0f)
                    val p1 = project((gx + 1).toFloat(), gy.toFloat(), 0f)
                    val p2 = project((gx + 1).toFloat(), (gy + 1).toFloat(), 0f)
                    val p3 = project(gx.toFloat(), (gy + 1).toFloat(), 0f)

                    val minX = minOf(p0.x, p1.x, p2.x, p3.x)
                    val maxX = maxOf(p0.x, p1.x, p2.x, p3.x)
                    val minY = minOf(p0.y, p1.y, p2.y, p3.y)
                    val maxY = maxOf(p0.y, p1.y, p2.y, p3.y)
                    if (maxX < 0 || minX > canvasW || maxY < 0 || minY > canvasH) continue

                    val cellType = map.grid[gx][gy]
                    val isChecker = (gx + gy) % 2 == 0
                    val tileColor = if (isChecker) FloorTileLight else FloorTileDark

                    val floorPath = Path().apply {
                        moveTo(p0.x, p0.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                    }
                    drawPath(floorPath, tileColor, style = Fill)

                    if (scale > 0.8f) {
                        drawPath(floorPath, FloorGridLine, style = Stroke(0.6f))
                    }

                    if (visualLayers.showChargingStations && cellType == CellType.CHARGING_STATION) {
                        drawPath(floorPath, ChargingPadNeon.copy(alpha = 0.35f), style = Fill)
                        drawPath(floorPath, ChargingPadNeon, style = Stroke(2.0f))
                    }
                    if (cellType == CellType.PICKUP_STATION) {
                        drawPath(floorPath, PickupPadNeon.copy(alpha = 0.30f), style = Fill)
                        drawPath(floorPath, PickupPadNeon, style = Stroke(2.0f))
                    }
                    if (cellType == CellType.DELIVERY_STATION) {
                        drawPath(floorPath, DeliveryPadNeon.copy(alpha = 0.30f), style = Fill)
                        drawPath(floorPath, DeliveryPadNeon, style = Stroke(2.0f))
                    }
                    if (cellType == CellType.NARROW_CORRIDOR) {
                        drawPath(floorPath, NarrowCorridorGlow.copy(alpha = 0.22f), style = Fill)
                        drawPath(floorPath, NarrowCorridorGlow, style = Stroke(1.5f))
                    }
                    if (cellType == CellType.WAITING_AREA) {
                        drawPath(floorPath, GreenSuccess.copy(alpha = 0.22f), style = Fill)
                        drawPath(floorPath, GreenSuccess, style = Stroke(1.5f))
                    }
                }
            }

            // ==========================================
            // 2. DYNAMIC LIGHTING OVERLAY (Camera-Follow & Active Cluster Illumination)
            // ==========================================
            if (showDynamicLighting) {
                // A. Primary Camera-Follow Dynamic Spotlight
                if (lightMode == "CAMERA" || lightMode == "DUAL") {
                    val cameraLightCenter = Offset(canvasW * 0.5f, canvasH * 0.48f)
                    val spotlightRadius = maxOf(canvasW, canvasH) * 0.75f
                    val alphaMultiplier = (0.28f * brightnessLevel).coerceIn(0.1f, 0.65f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x6600E5FF).copy(alpha = alphaMultiplier),
                                Color(0x2200E5FF).copy(alpha = alphaMultiplier * 0.5f),
                                Color.Transparent
                            ),
                            center = cameraLightCenter,
                            radius = spotlightRadius
                        ),
                        radius = spotlightRadius,
                        center = cameraLightCenter
                    )
                }

                // B. Active Robot Swarm Clusters Spotlight Pools
                if (lightMode == "CLUSTERS" || lightMode == "DUAL") {
                    val activeOrChargingBots = robots.filter {
                        it.status == RobotStatus.MOVING || it.status == RobotStatus.WORKING || it.status == RobotStatus.CHARGING || it.status == RobotStatus.LOW_BATTERY
                    }
                    if (activeOrChargingBots.isNotEmpty()) {
                        // Highlight up to 8 active clusters across the facility
                        for (clusterBot in activeOrChargingBots.take(8)) {
                            val lightPos = project(clusterBot.x + 0.5f, clusterBot.y + 0.5f, 0f)
                            val clusterLightRadius = baseCellW * 4.2f * lightPulse
                            val clusterColor = when {
                                clusterBot.status == RobotStatus.CHARGING -> ChargingPadNeon
                                clusterBot.status == RobotStatus.LOW_BATTERY -> RedEmergency
                                clusterBot.robotType == RobotType.CLEANER_AMR -> GreenSuccess
                                clusterBot.robotType == RobotType.ORGANIZER_AMR -> ElectricGold
                                clusterBot.robotType == RobotType.HEAVY_HAUL_AGV -> RedEmergency
                                clusterBot.robotType == RobotType.EXPRESS_HAULER -> CyanAccent
                                clusterBot.robotType == RobotType.INSPECTION_ROVER -> PurpleNegotiating
                                else -> CyanAccent
                            }
                            val poolAlpha = (0.35f * brightnessLevel).coerceIn(0.15f, 0.75f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        clusterColor.copy(alpha = poolAlpha),
                                        clusterColor.copy(alpha = poolAlpha * 0.35f),
                                        Color.Transparent
                                    ),
                                    center = lightPos,
                                    radius = clusterLightRadius
                                ),
                                radius = clusterLightRadius,
                                center = lightPos
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. 3D DIGITAL TWIN ZONE SIGNAGE
            // ==========================================
            if (visualLayers.showWarehouseZones && scale > 0.75f) {
                for (zone in map.warehouseZones) {
                    val labelP = project(zone.center.x.toFloat(), zone.center.y.toFloat(), 1.6f)
                    val textLayout = textMeasurer.measure(
                        text = zone.shortLabel,
                        style = TextStyle(
                            color = when (zone.type) {
                                CellType.CHARGING_STATION -> ChargingPadNeon
                                CellType.PICKUP_STATION -> PickupPadNeon
                                CellType.DELIVERY_STATION -> DeliveryPadNeon
                                CellType.NARROW_CORRIDOR -> AmberWarning
                                else -> CyanAccent
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    val tW = textLayout.size.width.toFloat()
                    val tH = textLayout.size.height.toFloat()
                    drawRoundRect(
                        color = Color(0xCC08101E),
                        topLeft = Offset(labelP.x - tW / 2f - 4f, labelP.y - tH / 2f - 2f),
                        size = Size(tW + 8f, tH + 4f),
                        cornerRadius = CornerRadius(3f)
                    )
                    drawText(textLayout, topLeft = Offset(labelP.x - tW / 2f, labelP.y - tH / 2f))
                }
            }

            // ==========================================
            // 4. 3D STORAGE RACKS, WALLS & OBSTACLES
            // ==========================================
            for (gx in 0 until map.width) {
                for (gy in 0 until map.height) {
                    val cellType = map.grid[gx][gy]
                    val isWall = cellType == CellType.WALL
                    val isRack = cellType == CellType.STORAGE_RACK

                    if (isWall || isRack) {
                        val heightZ = if (isWall) 1.5f else 2.2f
                        drawExtruded3DCube(
                            gx = gx.toFloat(), gy = gy.toFloat(),
                            width = 1.0f, depth = 1.0f, heightZ = heightZ,
                            topColor = if (isWall) WallTop else RackTop,
                            leftColor = if (isWall) WallSideLeft else RackFront,
                            rightColor = if (isWall) WallSideRight else RackSide,
                            edgeColor = if (isRack) RackBeam else Color(0xFF4A658A),
                            project = ::project
                        )

                        if (isRack && scale > 0.9f) {
                            val crateHue = when ((gx * 3 + gy) % 3) {
                                0 -> CrateColor1; 1 -> CrateColor2; else -> CrateColor3
                            }
                            drawExtruded3DCube(
                                gx = gx + 0.2f, gy = gy + 0.2f,
                                width = 0.6f, depth = 0.6f, heightZ = 0.8f, baseZ = heightZ,
                                topColor = crateHue,
                                leftColor = crateHue.copy(alpha = 0.7f),
                                rightColor = crateHue.copy(alpha = 0.5f),
                                edgeColor = Color.White.copy(alpha = 0.6f),
                                project = ::project
                            )
                        }
                    }
                }
            }

            // Dynamic Maintenance Obstacles (Extruded Red Hazard Cubes)
            if (visualLayers.showObstacles) {
                for (obs in map.dynamicObstacles) {
                    drawExtruded3DCube(
                        gx = obs.x.toFloat(), gy = obs.y.toFloat(),
                        width = 1f, depth = 1f, heightZ = 1.2f,
                        topColor = RedEmergency, leftColor = Color(0xFF990022), rightColor = Color(0xFF660015),
                        edgeColor = AmberWarning, project = ::project
                    )
                }
            }

            // ==========================================
            // 5. 3D ROUTES WITH TASK COLOR LANGUAGE
            // ==========================================
            if (visualLayers.showRoutes) {
                // Draw selected robot route or moving robots routes
                val robotsWithRoutes = if (selectedRobot != null) listOf(selectedRobot) else robots.filter { it.status == RobotStatus.MOVING }.take(15)
                for (r in robotsWithRoutes) {
                    val remainingRoute = r.currentRoute.drop(r.routeIndex)
                    if (remainingRoute.isNotEmpty()) {
                        val routeColor = when (r.currentTaskType) {
                            TaskType.LOGISTICS_TRANSPORT -> BlueInfo
                            TaskType.AISLE_CLEANING -> CyanAccent
                            TaskType.SHELF_ORGANIZING -> ElectricGold
                            TaskType.SAFETY_INSPECTION -> PurpleNegotiating
                            TaskType.HEAVY_HAUL -> Color(0xFFE040FB)
                            TaskType.CHARGING -> AmberWarning
                            else -> GreenSuccess
                        }

                        val routePath = Path()
                        val startP = project(r.x + 0.5f, r.y + 0.5f, 0.15f)
                        routePath.moveTo(startP.x, startP.y)
                        for (step in remainingRoute) {
                            val p = project(step.x + 0.5f, step.y + 0.5f, 0.15f)
                            routePath.lineTo(p.x, p.y)
                        }
                        val isSel = selectedRobot?.robotId == r.robotId
                        drawPath(routePath, routeColor.copy(alpha = if (isSel) 0.95f else 0.45f), style = Stroke(if (isSel) 4.5f else 2.5f, cap = StrokeCap.Round))

                        // Destination Marker (◆)
                        if (visualLayers.showDestinations) {
                            val dest = remainingRoute.last()
                            val destP = project(dest.x + 0.5f, dest.y + 0.5f, 0.2f)
                            drawCircle(routeColor, radius = 5.0f, center = destP)
                            drawCircle(Color.White, radius = 2.5f, center = destP)
                        }
                    }
                }
            }

            // ==========================================
            // 6. 3D CONFLICTS & P2P NEGOTIATIONS
            // ==========================================
            if (visualLayers.showConflicts) {
                for (risk in activeCollisionRisks) {
                    val pA = project(risk.robotA.x + 0.5f, risk.robotA.y + 0.5f, 0.5f)
                    val pB = project(risk.robotB.x + 0.5f, risk.robotB.y + 0.5f, 0.5f)
                    val pC = project(risk.conflictLocation.x + 0.5f, risk.conflictLocation.y + 0.5f, 0f)

                    if (visualLayers.showNegotiations) {
                        drawLine(RedEmergency, pA, pB, strokeWidth = 3.5f)
                    }
                    val ringRad = baseCellW * 1.5f * lightPulse
                    drawCircle(AmberWarning.copy(alpha = 0.35f), radius = ringRad, center = pC)
                    drawCircle(RedEmergency, radius = ringRad * 0.7f, center = pC, style = Stroke(2f))

                    // 3D Warning Beacon
                    val topP = project(risk.conflictLocation.x + 0.5f, risk.conflictLocation.y + 0.5f, 2.4f)
                    drawLine(RedEmergency, pC, topP, strokeWidth = 2.5f)
                    drawCircle(RedEmergency, radius = 5.5f, center = topP)
                }
            }

            // 3D Deadlock Cycles
            for (dl in activeDeadlocks) {
                for (i in dl.robotsInCycle.indices) {
                    val rA = dl.robotsInCycle[i]
                    val rB = dl.robotsInCycle[(i + 1) % dl.robotsInCycle.size]
                    val pA = project(rA.x + 0.5f, rA.y + 0.5f, 0.8f)
                    val pB = project(rB.x + 0.5f, rB.y + 0.5f, 0.8f)
                    drawLine(PurpleNegotiating, pA, pB, strokeWidth = 4.5f)
                }
            }

            // ==========================================
            // 7. 3D RECOGNIZABLE ROBOT MODELS
            // ==========================================
            if (visualLayers.showRobots) {
                val sortedRobots = if (projectionMode == CameraProjection.ISOMETRIC_3D) {
                    robots.sortedBy { it.x + it.y }
                } else {
                    robots
                }

                for (robot in sortedRobots) {
                    val rx = robot.x + 0.5f
                    val ry = robot.y + 0.5f
                    val baseGroundP = project(rx, ry, 0f)

                    if (baseGroundP.x < -60 || baseGroundP.x > canvasW + 60 || baseGroundP.y < -60 || baseGroundP.y > canvasH + 60) continue

                    val isSelected = selectedRobot?.robotId == robot.robotId
                    val isDemoHighlighted = highlightedRobotIds.contains(robot.robotId)
                    val isLockedOn = lockedOnRobotId == robot.robotId
                    val headingAngle = atan2(robot.targetY - robot.y, robot.targetX - robot.x)
                    val shadowRadius = (baseCellW * 0.46f).coerceAtLeast(3f)

                    // Category filter matching
                    val matchesFilter = when (selectedCategoryFilter) {
                        "CLEAN" -> robot.robotType == RobotType.CLEANER_AMR
                        "ORGANIZE" -> robot.robotType == RobotType.ORGANIZER_AMR
                        "HAUL" -> robot.robotType == RobotType.HEAVY_HAUL_AGV
                        "EXPRESS" -> robot.robotType == RobotType.EXPRESS_HAULER
                        "INSPECT" -> robot.robotType == RobotType.INSPECTION_ROVER
                        "CHARGING" -> robot.status == RobotStatus.CHARGING || robot.status == RobotStatus.LOW_BATTERY
                        else -> true
                    }
                    val renderAlpha = if (!matchesFilter && selectedCategoryFilter != "ALL") 0.28f else 1.0f

                    // Ground Drop Shadow
                    drawCircle(Color.Black.copy(alpha = 0.6f * renderAlpha), radius = shadowRadius, center = Offset(baseGroundP.x + 2f, baseGroundP.y + 3f))

                    // Category Accent Color
                    val categoryColor = when (robot.robotType) {
                        RobotType.CLEANER_AMR -> GreenSuccess
                        RobotType.ORGANIZER_AMR -> ElectricGold
                        RobotType.HEAVY_HAUL_AGV -> RedEmergency
                        RobotType.EXPRESS_HAULER -> CyanAccent
                        RobotType.INSPECTION_ROVER -> PurpleNegotiating
                        RobotType.GENERAL_TRANSPORT -> BlueInfo
                    }

                    // Vertical Holographic Light Pillar when Filtered or Locked-On
                    if ((matchesFilter && selectedCategoryFilter != "ALL") || isLockedOn || isSelected) {
                        val topPillar = project(rx, ry, 3.2f)
                        drawLine(categoryColor.copy(alpha = 0.85f), baseGroundP, topPillar, strokeWidth = 3.0f)
                        drawCircle(categoryColor, radius = 5.5f, center = topPillar)
                        drawCircle(
                            color = categoryColor.copy(alpha = 0.45f * lightPulse),
                            radius = shadowRadius * 2.5f,
                            center = baseGroundP,
                            style = Stroke(2.5f)
                        )
                    }

                    if (isDemoHighlighted || isSelected) {
                        drawCircle(
                            color = CyanAccent.copy(alpha = 0.6f * lightPulse * renderAlpha),
                            radius = shadowRadius * 2.2f,
                            center = baseGroundP
                        )
                    }

                    drawCircle(
                        color = categoryColor.copy(alpha = (if (isSelected) 0.85f else 0.38f) * renderAlpha),
                        radius = if (isSelected) shadowRadius * 1.9f else shadowRadius * 1.35f,
                        center = baseGroundP
                    )

                    // Directional Headlights
                    if (showLiDARAndLights && (robot.status == RobotStatus.MOVING || robot.status == RobotStatus.REROUTING)) {
                        val lightDist = baseCellW * 2.0f
                        val coneAngle = 0.42f
                        val pLeft = Offset(baseGroundP.x + cos(headingAngle - coneAngle) * lightDist, baseGroundP.y + sin(headingAngle - coneAngle) * lightDist)
                        val pRight = Offset(baseGroundP.x + cos(headingAngle + coneAngle) * lightDist, baseGroundP.y + sin(headingAngle + coneAngle) * lightDist)
                        val conePath = Path().apply {
                            moveTo(baseGroundP.x, baseGroundP.y); lineTo(pLeft.x, pLeft.y); lineTo(pRight.x, pRight.y); close()
                        }
                        drawPath(conePath, brush = Brush.radialGradient(listOf(RobotHeadlight.copy(alpha = 0.8f * renderAlpha), Color.Transparent), center = baseGroundP, radius = lightDist))
                    }

                    // ==========================================
                    // 3D CHASSIS ACCORDING TO ROBOT CATEGORY
                    // ==========================================
                    val robotSize = 0.72f
                    when (robot.robotType) {
                        RobotType.CLEANER_AMR -> {
                            // Circular/Rounded Chassis in Neon Emerald
                            drawExtruded3DCube(
                                gx = rx - robotSize / 2f, gy = ry - robotSize / 2f, width = robotSize, depth = robotSize,
                                heightZ = 0.52f, baseZ = 0.1f,
                                topColor = GreenSuccess.copy(alpha = renderAlpha),
                                leftColor = Color(0xFF00552B).copy(alpha = renderAlpha),
                                rightColor = Color(0xFF00381C).copy(alpha = renderAlpha),
                                edgeColor = Color.White.copy(alpha = renderAlpha), project = ::project
                            )
                            // Dual Spinning Scrub Brushes with Radiating Bristles
                            val brushDist = 0.42f
                            val b1P = project(rx + cos(headingAngle + 0.6f) * brushDist, ry + sin(headingAngle + 0.6f) * brushDist, 0.15f)
                            val b2P = project(rx + cos(headingAngle - 0.6f) * brushDist, ry + sin(headingAngle - 0.6f) * brushDist, 0.15f)
                            val bRad = (shadowRadius * 0.42f).coerceAtLeast(3f)
                            drawCircle(CyanAccent.copy(alpha = renderAlpha), radius = bRad, center = b1P, style = Stroke(2.2f))
                            drawCircle(CyanAccent.copy(alpha = renderAlpha), radius = bRad, center = b2P, style = Stroke(2.2f))
                            // Spinning bristles
                            for (bAngle in listOf(0f, 1.57f, 3.14f, 4.71f)) {
                                val sP1 = Offset(b1P.x + cos(animAngle * 8f + bAngle) * bRad, b1P.y + sin(animAngle * 8f + bAngle) * bRad)
                                val sP2 = Offset(b2P.x + cos(-animAngle * 8f + bAngle) * bRad, b2P.y + sin(-animAngle * 8f + bAngle) * bRad)
                                drawLine(GreenSuccess.copy(alpha = renderAlpha), b1P, sP1, strokeWidth = 1.5f)
                                drawLine(GreenSuccess.copy(alpha = renderAlpha), b2P, sP2, strokeWidth = 1.5f)
                            }
                            // Disinfectant mist spray dots
                            if (robot.status == RobotStatus.MOVING || robot.status == RobotStatus.WORKING) {
                                val mistP = project(rx - cos(headingAngle) * 0.45f, ry - sin(headingAngle) * 0.45f, 0.08f)
                                drawCircle(CyanAccent.copy(alpha = 0.45f * lightPulse * renderAlpha), radius = shadowRadius * 1.2f, center = mistP)
                            }
                        }
                        RobotType.ORGANIZER_AMR -> {
                            // Modular Sorting Chassis in Electric Gold
                            drawExtruded3DCube(
                                gx = rx - robotSize / 2f, gy = ry - robotSize / 2f, width = robotSize, depth = robotSize,
                                heightZ = 0.58f, baseZ = 0.1f,
                                topColor = ElectricGold.copy(alpha = renderAlpha),
                                leftColor = Color(0xFF664800).copy(alpha = renderAlpha),
                                rightColor = Color(0xFF402E00).copy(alpha = renderAlpha),
                                edgeColor = Color.White.copy(alpha = renderAlpha), project = ::project
                            )
                            // Articulated Multi-Joint Robot Arm
                            val armBase = project(rx, ry, 0.68f)
                            val armElbow = project(rx + cos(headingAngle + 0.3f) * 0.22f, ry + sin(headingAngle + 0.3f) * 0.22f, 1.25f)
                            val armWrist = project(rx + cos(headingAngle + 0.3f) * 0.45f, ry + sin(headingAngle + 0.3f) * 0.45f, 0.95f)
                            drawLine(Color(0xFFE2E8F0).copy(alpha = renderAlpha), armBase, armElbow, strokeWidth = 4.0f)
                            drawLine(ElectricGold.copy(alpha = renderAlpha), armElbow, armWrist, strokeWidth = 3.0f)
                            drawCircle(Color.White.copy(alpha = renderAlpha), radius = 3.5f, center = armElbow)
                            // Dual-prong sorting gripper claw
                            drawCircle(PurpleNegotiating.copy(alpha = renderAlpha), radius = 4.5f, center = armWrist)
                            val claw1 = Offset(armWrist.x + 4f, armWrist.y - 3f)
                            val claw2 = Offset(armWrist.x + 4f, armWrist.y + 3f)
                            drawLine(Color.White.copy(alpha = renderAlpha), armWrist, claw1, strokeWidth = 2f)
                            drawLine(Color.White.copy(alpha = renderAlpha), armWrist, claw2, strokeWidth = 2f)
                        }
                        RobotType.HEAVY_HAUL_AGV -> {
                            // Wide Elongated Industrial Flatbed Chassis in Safety Red
                            drawExtruded3DCube(
                                gx = rx - 0.48f, gy = ry - 0.48f, width = 0.96f, depth = 0.96f,
                                heightZ = 0.65f, baseZ = 0.1f,
                                topColor = RedEmergency.copy(alpha = renderAlpha),
                                leftColor = Color(0xFF4A0A17).copy(alpha = renderAlpha),
                                rightColor = Color(0xFF33050E).copy(alpha = renderAlpha),
                                edgeColor = AmberWarning.copy(alpha = renderAlpha), project = ::project
                            )
                            // Caterpillar Track Treads on Sides
                            val trackL = project(rx - 0.5f, ry - 0.48f, 0.05f)
                            val trackR = project(rx + 0.5f, ry + 0.48f, 0.05f)
                            drawCircle(Color(0xFF263238).copy(alpha = renderAlpha), radius = shadowRadius * 0.5f, center = trackL)
                            drawCircle(Color(0xFF263238).copy(alpha = renderAlpha), radius = shadowRadius * 0.5f, center = trackR)
                            // Front & Rear Hazard Warning Stripes
                            val bFront = project(rx + cos(headingAngle) * 0.45f, ry + sin(headingAngle) * 0.45f, 0.4f)
                            drawCircle(AmberWarning.copy(alpha = renderAlpha), radius = 4.0f, center = bFront)
                        }
                        RobotType.EXPRESS_HAULER -> {
                            // Streamlined Wedge Speedster in Electric Cyan & White
                            drawExtruded3DCube(
                                gx = rx - robotSize / 2f, gy = ry - robotSize / 2f, width = robotSize, depth = robotSize,
                                heightZ = 0.42f, baseZ = 0.1f,
                                topColor = CyanAccent.copy(alpha = renderAlpha),
                                leftColor = Color(0xFF004953).copy(alpha = renderAlpha),
                                rightColor = Color(0xFF003037).copy(alpha = renderAlpha),
                                edgeColor = Color.White.copy(alpha = renderAlpha), project = ::project
                            )
                            // Twin Rear Stabilizer Fins / Spoilers
                            val finBase1 = project(rx - cos(headingAngle) * 0.3f + sin(headingAngle) * 0.25f, ry - sin(headingAngle) * 0.3f - cos(headingAngle) * 0.25f, 0.42f)
                            val finTop1 = project(rx - cos(headingAngle) * 0.3f + sin(headingAngle) * 0.25f, ry - sin(headingAngle) * 0.3f - cos(headingAngle) * 0.25f, 0.95f)
                            val finBase2 = project(rx - cos(headingAngle) * 0.3f - sin(headingAngle) * 0.25f, ry - sin(headingAngle) * 0.3f + cos(headingAngle) * 0.25f, 0.42f)
                            val finTop2 = project(rx - cos(headingAngle) * 0.3f - sin(headingAngle) * 0.25f, ry - sin(headingAngle) * 0.3f + cos(headingAngle) * 0.25f, 0.95f)
                            drawLine(CyanAccent.copy(alpha = renderAlpha), finBase1, finTop1, strokeWidth = 3.0f)
                            drawLine(CyanAccent.copy(alpha = renderAlpha), finBase2, finTop2, strokeWidth = 3.0f)
                            // Glowing speed streaks when in motion
                            if (robot.status == RobotStatus.MOVING) {
                                val tailP = project(rx - cos(headingAngle) * 0.7f, ry - sin(headingAngle) * 0.7f, 0.25f)
                                drawLine(CyanAccent.copy(alpha = 0.75f * renderAlpha), baseGroundP, tailP, strokeWidth = 3.5f)
                            }
                        }
                        RobotType.INSPECTION_ROVER -> {
                            // Elevated Rover Chassis in Vivid Magenta
                            drawExtruded3DCube(
                                gx = rx - robotSize / 2f, gy = ry - robotSize / 2f, width = robotSize, depth = robotSize,
                                heightZ = 0.52f, baseZ = 0.12f,
                                topColor = PurpleNegotiating.copy(alpha = renderAlpha),
                                leftColor = Color(0xFF4C0059).copy(alpha = renderAlpha),
                                rightColor = Color(0xFF33003B).copy(alpha = renderAlpha),
                                edgeColor = Color.White.copy(alpha = renderAlpha), project = ::project
                            )
                            // Sensor Mast & Rotating LiDAR Dome
                            val mastBase = project(rx, ry, 0.64f)
                            val mastTop = project(rx, ry, 1.45f)
                            drawLine(Color.White.copy(alpha = renderAlpha), mastBase, mastTop, strokeWidth = 3.5f)
                            drawCircle(PurpleNegotiating.copy(alpha = renderAlpha), radius = 6.0f, center = mastTop)
                            drawCircle(Color.White.copy(alpha = renderAlpha), radius = 2.5f, center = mastTop)
                            // 360-Degree Ground Scanning Cone
                            val scanRad = shadowRadius * 2.2f
                            drawCircle(GreenSuccess.copy(alpha = 0.25f * lightPulse * renderAlpha), radius = scanRad, center = baseGroundP, style = Stroke(1.5f))
                        }
                        else -> {
                            // General Multi-Tasker Chassis in Cobalt Blue
                            drawExtruded3DCube(
                                gx = rx - robotSize / 2f, gy = ry - robotSize / 2f, width = robotSize, depth = robotSize,
                                heightZ = 0.52f, baseZ = 0.1f,
                                topColor = BlueInfo.copy(alpha = renderAlpha),
                                leftColor = RobotChassisDark.copy(alpha = renderAlpha),
                                rightColor = RobotChassisRim.copy(alpha = renderAlpha),
                                edgeColor = Color.White.copy(alpha = renderAlpha), project = ::project
                            )
                        }
                    }

                    // Rotating LiDAR Sensor
                    val turretCenter = project(rx, ry, 0.75f)
                    drawCircle(Color(0xFF212B3E).copy(alpha = renderAlpha), radius = (shadowRadius * 0.45f).coerceAtLeast(2f), center = turretCenter)
                    drawCircle(RobotLiDAR.copy(alpha = renderAlpha), radius = (shadowRadius * 0.28f).coerceAtLeast(1.5f), center = turretCenter)
                    if (showLiDARAndLights) {
                        val sweepP = Offset(turretCenter.x + cos(animAngle + robot.robotId) * shadowRadius * 1.3f, turretCenter.y + sin(animAngle + robot.robotId) * shadowRadius * 1.3f)
                        drawLine(CyanAccent.copy(alpha = 0.75f * renderAlpha), turretCenter, sweepP, strokeWidth = 1.8f)
                    }

                    // 3D Cargo Crate
                    if (robot.currentTaskId != null) {
                        drawExtruded3DCube(
                            gx = rx - 0.22f, gy = ry - 0.22f, width = 0.44f, depth = 0.44f,
                            heightZ = 0.5f, baseZ = 0.75f,
                            topColor = ElectricGold.copy(alpha = renderAlpha),
                            leftColor = ElectricGold.copy(alpha = 0.75f * renderAlpha),
                            rightColor = ElectricGold.copy(alpha = 0.55f * renderAlpha),
                            edgeColor = Color.White.copy(alpha = renderAlpha), project = ::project
                        )
                    }

                    // CHARGING STATE: Ascending High-Voltage Lightning Arcs
                    if (robot.status == RobotStatus.CHARGING) {
                        val padP = project(rx, ry, 0.05f)
                        val topP = project(rx, ry, 1.8f)
                        drawLine(ChargingPadNeon, padP, topP, strokeWidth = 3.5f)
                        drawCircle(ChargingPadNeon.copy(alpha = 0.55f * lightPulse), radius = shadowRadius * 1.8f, center = padP)
                    }

                    // LOW BATTERY STATE: Flashing Red Emergency Beacon
                    if (robot.status == RobotStatus.LOW_BATTERY) {
                        val domeP = project(rx, ry, 1.5f)
                        drawCircle(RedEmergency.copy(alpha = lightPulse), radius = 6.0f, center = domeP)
                    }

                    // Holographic Overhead 3D Badges (Role, ID, Battery)
                    if (visualLayers.showRobotIds && (scale > 1.15f || isSelected || isDemoHighlighted || isLockedOn || (matchesFilter && selectedCategoryFilter != "ALL"))) {
                        val hudP = project(rx, ry, if (robot.currentTaskId != null) 1.85f else 1.4f)
                        val roleIcon = when (robot.robotType) {
                            RobotType.CLEANER_AMR -> "🧹"
                            RobotType.ORGANIZER_AMR -> "📦"
                            RobotType.HEAVY_HAUL_AGV -> "🏗️"
                            RobotType.EXPRESS_HAULER -> "⚡"
                            RobotType.INSPECTION_ROVER -> "🔍"
                            else -> "🚚"
                        }
                        val statusTag = if (robot.status == RobotStatus.CHARGING) " [⚡CHARGE]" else if (robot.status == RobotStatus.LOW_BATTERY) " [!DOCK]" else ""
                        val badgeText = "$roleIcon R${robot.robotId} ${robot.battery.toInt()}%$statusTag"
                        val textLayout = textMeasurer.measure(
                            text = badgeText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        val tW = textLayout.size.width.toFloat()
                        val tH = textLayout.size.height.toFloat()
                        drawRoundRect(
                            color = Color(0xEE090E1A),
                            topLeft = Offset(hudP.x - tW / 2f - 4f, hudP.y - tH / 2f - 2f),
                            size = Size(tW + 8f, tH + 6f),
                            cornerRadius = CornerRadius(4f)
                        )
                        drawText(textLayout, topLeft = Offset(hudP.x - tW / 2f, hudP.y - tH / 2f - 1f))

                        val barW = tW
                        val batColor = if (robot.battery > 50f) GreenSuccess else if (robot.battery > 25f) AmberWarning else RedEmergency
                        drawRect(Color(0xFF1E2D45), topLeft = Offset(hudP.x - barW / 2f, hudP.y + tH / 2f + 1f), size = Size(barW, 2.5f))
                        drawRect(batColor, topLeft = Offset(hudP.x - barW / 2f, hudP.y + tH / 2f + 1f), size = Size(barW * (robot.battery / 100f), 2.5f))
                    }
                }
            }
        }

        // ==========================================
        // CAMERA PRESETS & DIGITAL TWIN TOOLBAR
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xEE0D1526),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            projectionMode = if (projectionMode == CameraProjection.ISOMETRIC_3D) CameraProjection.TOP_DOWN_3D else CameraProjection.ISOMETRIC_3D
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "3D Mode", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { showDynamicLighting = !showDynamicLighting },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Dynamic Lighting", tint = if (showDynamicLighting) CyanAccent else Color.Gray, modifier = Modifier.size(16.dp))
                    }

                    // Brightness Boost Cycle (1.0x, 1.4x, 1.8x)
                    IconButton(
                        onClick = {
                            brightnessLevel = when (brightnessLevel) {
                                1.0f -> 1.4f
                                1.4f -> 1.8f
                                else -> 1.0f
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.BrightnessHigh, contentDescription = "Brightness: ${brightnessLevel}x", tint = if (brightnessLevel > 1.0f) ElectricGold else Color.Gray, modifier = Modifier.size(16.dp))
                    }

                    // Light Mode (Dual, Camera, Clusters)
                    IconButton(
                        onClick = {
                            lightMode = when (lightMode) {
                                "DUAL" -> "CAMERA"
                                "CAMERA" -> "CLUSTERS"
                                else -> "DUAL"
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Light Mode: $lightMode", tint = if (lightMode == "DUAL") CyanAccent else AmberWarning, modifier = Modifier.size(16.dp))
                    }

                    // Camera Lock-On Toggle for selected robot
                    if (selectedRobot != null) {
                        IconButton(
                            onClick = {
                                lockedOnRobotId = if (lockedOnRobotId == selectedRobot.robotId) null else selectedRobot.robotId
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                if (lockedOnRobotId == selectedRobot.robotId) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Camera on Robot",
                                tint = if (lockedOnRobotId == selectedRobot.robotId) CyanAccent else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showLiDARAndLights = !showLiDARAndLights },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Highlight, contentDescription = "LiDAR", tint = if (showLiDARAndLights) ElectricGold else Color.Gray, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { showLayersMenu = !showLayersMenu },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Layers", tint = if (showLayersMenu) CyanAccent else Color.White, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { scale = (scale * 1.3f).coerceAtMost(6.0f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { scale = (scale / 1.3f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { scale = 1.0f; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset Center", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Quick Camera Presets Strip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xEE0B1220),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PresetChip("FLEET VIEW") { scale = 1.0f; offsetX = 0f; offsetY = 0f }
                    PresetChip("CONFLICT") {
                        scale = 2.8f
                        offsetX = -120f; offsetY = -40f
                    }
                    PresetChip("CHARGERS") {
                        scale = 2.2f
                        offsetX = 150f; offsetY = 120f
                    }
                    PresetChip("BOTTLENECK") {
                        scale = 3.0f
                        offsetX = -60f; offsetY = -20f
                    }
                }
            }

            // Expandable Visual Layers Dialog / Popout
            if (showLayersMenu) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFA0D1526),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                    shadowElevation = 10.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("VISUAL LAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        LayerCheckbox("Robots", visualLayers.showRobots) { visualLayers = visualLayers.copy(showRobots = it) }
                        LayerCheckbox("Routes", visualLayers.showRoutes) { visualLayers = visualLayers.copy(showRoutes = it) }
                        LayerCheckbox("Robot IDs", visualLayers.showRobotIds) { visualLayers = visualLayers.copy(showRobotIds = it) }
                        LayerCheckbox("Conflicts", visualLayers.showConflicts) { visualLayers = visualLayers.copy(showConflicts = it) }
                        LayerCheckbox("Zones & Signage", visualLayers.showWarehouseZones) { visualLayers = visualLayers.copy(showWarehouseZones = it) }
                        LayerCheckbox("Charging Stations", visualLayers.showChargingStations) { visualLayers = visualLayers.copy(showChargingStations = it) }
                        LayerCheckbox("Obstacles", visualLayers.showObstacles) { visualLayers = visualLayers.copy(showObstacles = it) }
                    }
                }
            }
        }

        // ==========================================
        // CATEGORY FILTER BAR (Distinguish Robot Capabilities)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xEE0B1220),
            border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryChip("ALL", selectedCategoryFilter == "ALL", CyanAccent) { selectedCategoryFilter = "ALL" }
                CategoryChip("🧹 CLEAN", selectedCategoryFilter == "CLEAN", GreenSuccess) { selectedCategoryFilter = "CLEAN" }
                CategoryChip("📦 ORGANIZE", selectedCategoryFilter == "ORGANIZE", ElectricGold) { selectedCategoryFilter = "ORGANIZE" }
                CategoryChip("🏗️ HAUL", selectedCategoryFilter == "HAUL", RedEmergency) { selectedCategoryFilter = "HAUL" }
                CategoryChip("⚡ EXPRESS", selectedCategoryFilter == "EXPRESS", CyanAccent) { selectedCategoryFilter = "EXPRESS" }
                CategoryChip("🔍 INSPECT", selectedCategoryFilter == "INSPECT", PurpleNegotiating) { selectedCategoryFilter = "INSPECT" }
                CategoryChip("🔋 CHARGING", selectedCategoryFilter == "CHARGING", ChargingPadNeon) { selectedCategoryFilter = "CHARGING" }
            }
        }

        // ==========================================
        // CAMERA LOCK-ON TRACKING INDICATOR
        // ==========================================
        if (lockedOnRobotId != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xF00D1526),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                    Text(
                        text = "LOCK-ON: R$lockedOnRobotId [${lockedRobot?.robotType?.displayName ?: "AMR"}]",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = { lockedOnRobotId = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D45), contentColor = CyanAccent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("UNLOCK", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accentColor.copy(alpha = 0.25f) else Color(0xFF131D30),
            contentColor = if (selected) accentColor else Color(0xFF94A3B8)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null,
        modifier = Modifier.height(26.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162540), contentColor = CyanAccent),
        modifier = Modifier.height(26.dp)
    ) {
        Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LayerCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().height(28.dp)
    ) {
        Text(label, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CyanAccent, checkmarkColor = Color.Black),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun DrawScope.drawExtruded3DCube(
    gx: Float,
    gy: Float,
    width: Float,
    depth: Float,
    heightZ: Float,
    baseZ: Float = 0f,
    topColor: Color,
    leftColor: Color,
    rightColor: Color,
    edgeColor: Color,
    project: (Float, Float, Float) -> Offset
) {
    val b0 = project(gx, gy, baseZ)
    val b1 = project(gx + width, gy, baseZ)
    val b2 = project(gx + width, gy + depth, baseZ)
    val b3 = project(gx, gy + depth, baseZ)

    val t0 = project(gx, gy, baseZ + heightZ)
    val t1 = project(gx + width, gy, baseZ + heightZ)
    val t2 = project(gx + width, gy + depth, baseZ + heightZ)
    val t3 = project(gx, gy + depth, baseZ + heightZ)

    val frontPath = Path().apply {
        moveTo(b3.x, b3.y); lineTo(b2.x, b2.y); lineTo(t2.x, t2.y); lineTo(t3.x, t3.y); close()
    }
    drawPath(frontPath, leftColor, style = Fill)
    drawPath(frontPath, edgeColor.copy(alpha = 0.25f), style = Stroke(0.6f))

    val rightPath = Path().apply {
        moveTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(t2.x, t2.y); lineTo(t1.x, t1.y); close()
    }
    drawPath(rightPath, rightColor, style = Fill)
    drawPath(rightPath, edgeColor.copy(alpha = 0.25f), style = Stroke(0.6f))

    val topPath = Path().apply {
        moveTo(t0.x, t0.y); lineTo(t1.x, t1.y); lineTo(t2.x, t2.y); lineTo(t3.x, t3.y); close()
    }
    drawPath(topPath, topColor, style = Fill)
    drawPath(topPath, edgeColor, style = Stroke(1.0f))
}

private fun findRobotAtScreenPos(
    tapOffset: Offset,
    robots: List<Robot>,
    map: WarehouseMap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    canvasSize: Size,
    mode: CameraProjection
): Robot? {
    val canvasW = canvasSize.width
    val canvasH = canvasSize.height
    val baseCellW = (canvasW / map.width) * scale
    val baseCellH = (canvasH / map.height) * scale

    val isoStepX = baseCellW * 0.866f
    val isoStepY = baseCellH * 0.5f

    val originX = if (mode == CameraProjection.ISOMETRIC_3D) canvasW * 0.5f + offsetX else offsetX
    val originY = if (mode == CameraProjection.ISOMETRIC_3D) canvasH * 0.22f + offsetY else offsetY

    var nearest: Robot? = null
    var minSqDist = Float.MAX_VALUE
    val hitRadiusSq = (baseCellW * 1.5f).let { it * it }.coerceAtLeast(900f)

    for (robot in robots) {
        val rx = robot.x + 0.5f
        val ry = robot.y + 0.5f
        val sx = if (mode == CameraProjection.ISOMETRIC_3D) (rx - ry) * isoStepX + originX else rx * baseCellW + originX
        val sy = if (mode == CameraProjection.ISOMETRIC_3D) (rx + ry) * isoStepY + originY else ry * baseCellH + originY

        val dx = tapOffset.x - sx
        val dy = tapOffset.y - sy
        val dSq = dx * dx + dy * dy
        if (dSq < hitRadiusSq && dSq < minSqDist) {
            minSqDist = dSq
            nearest = robot
        }
    }

    return nearest
}
