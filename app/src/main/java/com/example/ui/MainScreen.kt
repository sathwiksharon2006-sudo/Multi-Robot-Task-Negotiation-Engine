package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FleetSimulator
import com.example.engine.JuryDemoController
import com.example.model.Robot
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.IndustrialDarkBg
import com.example.ui.theme.IndustrialSurface
import com.example.ui.theme.IndustrialSurfaceVariant
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RedEmergency
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    simulator: FleetSimulator = remember { FleetSimulator() }
) {
    val coroutineScope = rememberCoroutineScope()
    val juryDemoController = remember { JuryDemoController(simulator, coroutineScope) }

    val metrics by simulator.metrics.collectAsState()
    val events by simulator.eventsFlow.collectAsState()
    val juryDemoState by juryDemoController.demoState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRobot by remember { mutableStateOf<Robot?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showControllerDialog by remember { mutableStateOf(false) }
    var showEnergyDialog by remember { mutableStateOf(false) }

    // High performance continuous simulation tick loop (30 FPS)
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            val now = System.nanoTime()
            val dtSec = ((now - lastTime) / 1_000_000_000f).coerceIn(0.01f, 0.08f)
            lastTime = now
            simulator.tick(dtSec)
            delay(33L)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            // Section 4, 5, 6, 11: Compact 48dp System Status Header
            CompactSystemHeader(
                metrics = metrics,
                isDemoActive = juryDemoState.isActive,
                onControllerClick = { showControllerDialog = true },
                onEnergyClick = { showEnergyDialog = true },
                onRobotsClick = { selectedTab = 0 },
                onConflictsClick = { selectedTab = 0 },
                onDemoClick = {
                    if (juryDemoState.isActive) {
                        juryDemoController.stopDemo()
                    } else {
                        selectedTab = 0
                        juryDemoController.startJuryDemo()
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = IndustrialSurface,
                tonalElevation = 4.dp,
                modifier = Modifier.height(56.dp)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Warehouse Map", modifier = Modifier.size(20.dp)) },
                    label = { Text("3D SWARM", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFF00363D)
                    ),
                    modifier = Modifier.testTag("tab_map")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Control Center", modifier = Modifier.size(20.dp)) },
                    label = { Text("CONTROLS", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFF00363D)
                    ),
                    modifier = Modifier.testTag("tab_controls")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Event Log", modifier = Modifier.size(20.dp)) },
                    label = { Text("EVENTS", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFF00363D)
                    ),
                    modifier = Modifier.testTag("tab_events")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Tasks", modifier = Modifier.size(20.dp)) },
                    label = { Text("TASKS", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFF00363D)
                    ),
                    modifier = Modifier.testTag("tab_tasks")
                )
            }
        },
        containerColor = IndustrialDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Jury Demo Banner (shown when active)
            JuryDemoBanner(
                demoState = juryDemoState,
                onStopDemo = { juryDemoController.stopDemo() }
            )

            // Screen Hierarchy: Tab Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> {
                        // Section 8 & 9: 3D Warehouse dominant (65-75% height) + Bottom Fleet Stats panel
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Dominant 3D Simulation Viewport
                            Box(
                                modifier = Modifier
                                    .weight(0.72f)
                                    .fillMaxWidth()
                            ) {
                                WarehouseCanvas(
                                    map = simulator.map,
                                    robots = simulator.robots,
                                    activeCollisionRisks = simulator.activeCollisionRisks,
                                    activeDeadlocks = simulator.activeDeadlocks,
                                    selectedRobot = selectedRobot,
                                    onSelectRobot = { selectedRobot = it }
                                )
                            }

                            // Compact Fleet Stats / Telemetry / Work Sharing panel
                            FleetStatsBottomPanel(
                                metrics = metrics,
                                modifier = Modifier
                                    .weight(0.28f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                    1 -> {
                        // Control Center & Failure Injections
                        ControlPanel(
                            simulator = simulator,
                            onStartJuryDemo = {
                                selectedTab = 0
                                juryDemoController.startJuryDemo()
                            }
                        )
                    }
                    2 -> {
                        // Real-time Event Log with Category Filters
                        EventLogView(events = events)
                    }
                    3 -> {
                        // Task Queue and Assignment Telemetry
                        TaskListView(
                            tasks = simulator.tasks,
                            onSelectTask = { selectedTask = it }
                        )
                    }
                }
            }
        }
    }

    // Modal Inspectors & Dialogs
    if (showControllerDialog) {
        CentralControllerDialog(
            isOnline = metrics.controllerOnline,
            onDismiss = { showControllerDialog = false },
            onToggleController = {
                if (metrics.controllerOnline) {
                    simulator.killCentralController()
                } else {
                    simulator.restoreCentralController()
                }
            }
        )
    }

    if (showEnergyDialog) {
        FleetEnergyDialog(
            avgBatteryPercent = metrics.fleetAvgBatteryPercent,
            healthyCount = metrics.batteryHighCount,
            lowCount = metrics.batteryMediumCount,
            criticalCount = metrics.batteryCriticalCount,
            chargingCount = metrics.chargingRobots,
            autoDockThreshold = metrics.lowBatteryThreshold,
            onDismiss = { showEnergyDialog = false }
        )
    }


    // Modal Inspectors
    selectedRobot?.let { robot ->
        RobotDetailDialog(
            robot = robot,
            onDismiss = { selectedRobot = null },
            onFailRobot = {
                simulator.failRobot(it)
                selectedRobot = null
            },
            onRestoreRobot = {
                simulator.restoreRobot(it)
                selectedRobot = null
            },
            onDrainBattery = {
                simulator.drainBattery(it)
                selectedRobot = null
            },
            onDisconnect = {
                simulator.disconnectRobot(it)
                selectedRobot = null
            }
        )
    }

    selectedTask?.let { task ->
        TaskDetailDialog(
            task = task,
            onDismiss = { selectedTask = null }
        )
    }
}

@Composable
fun TaskListView(
    tasks: List<Task>,
    onSelectTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialSurface)
            .border(1.dp, IndustrialBorder)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOGISTICS MISSION QUEUE (${tasks.size} TOTAL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "TAP ROW TO INSPECT BIDS",
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tasks.take(200), key = { it.taskId }) { task ->
                TaskItemRow(task = task, onClick = { onSelectTask(task) })
            }
        }
    }
}

@Composable
private fun TaskItemRow(task: Task, onClick: () -> Unit) {
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> GreenSuccess
        TaskStatus.IN_PROGRESS -> CyanAccent
        TaskStatus.ASSIGNED -> Color(0xFF448AFF)
        TaskStatus.NEGOTIATING -> PurpleNegotiating
        TaskStatus.PENDING -> AmberWarning
        TaskStatus.FAILED -> RedEmergency
        TaskStatus.CANCELLED -> Color(0xFF9E9E9E)
        else -> AmberWarning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C1322), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Column {
                Text(
                    text = "${task.taskId} • ${task.taskType.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Req: ${task.requiredCapability.name} • ${task.weight.toInt()}kg • Priority: ${task.priority.name}",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = task.status.name,
                fontSize = 10.sp,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (task.assignedRobotId != null) "AMR R${task.assignedRobotId}" else "Unassigned",
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
