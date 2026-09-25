package com.example.model

data class GridPoint(val x: Int, val y: Int) {
    fun distanceTo(other: GridPoint): Float {
        val dx = (x - other.x).toFloat()
        val dy = (y - other.y).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun manhattanTo(other: GridPoint): Int {
        return kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    }
}

enum class CellType {
    FLOOR,
    NARROW_CORRIDOR,
    INTERSECTION,
    STORAGE_RACK,
    PICKUP_STATION,
    DELIVERY_STATION,
    CHARGING_STATION,
    WAITING_AREA,
    RESTRICTED_ZONE,
    WALL
}

data class WarehouseZone(
    val name: String,
    val type: CellType,
    val center: GridPoint,
    val shortLabel: String
)

class WarehouseMap(
    val width: Int = 60,
    val height: Int = 40
) {
    val grid: Array<Array<CellType>> = Array(width) { Array(height) { CellType.FLOOR } }
    
    // Dynamic temporary obstacles (e.g., blocked corridors or failed robots)
    val dynamicObstacles = mutableSetOf<GridPoint>()
    
    val pickupStations = mutableListOf<GridPoint>()
    val deliveryStations = mutableListOf<GridPoint>()
    val chargingStations = mutableListOf<GridPoint>()
    val waitingAreas = mutableListOf<GridPoint>()
    val warehouseZones = mutableListOf<WarehouseZone>()

    init {
        buildDefaultWarehouse()
    }

    private fun buildDefaultWarehouse() {
        // Outer boundary walls
        for (x in 0 until width) {
            grid[x][0] = CellType.WALL
            grid[x][height - 1] = CellType.WALL
        }
        for (y in 0 until height) {
            grid[0][y] = CellType.WALL
            grid[width - 1][y] = CellType.WALL
        }

        // Charging Stations along North Wall (x: 4..24 step 2, y: 1)
        for (x in 4..24 step 2) {
            grid[x][1] = CellType.CHARGING_STATION
            chargingStations.add(GridPoint(x, 1))
        }
        warehouseZones.add(WarehouseZone("CHARGING DEPOT NORTH", CellType.CHARGING_STATION, GridPoint(14, 2), "⚡ CHARGING DEPOT"))

        // Pickup Stations along West Wall (x: 1, y: 6..34 step 4)
        for (y in 6..34 step 4) {
            grid[1][y] = CellType.PICKUP_STATION
            pickupStations.add(GridPoint(1, y))
        }
        warehouseZones.add(WarehouseZone("PICKUP TERMINAL WEST", CellType.PICKUP_STATION, GridPoint(2, 20), "📦 PICKUP TERMINAL"))

        // Delivery Stations along East Wall (x: width - 2, y: 6..34 step 4)
        for (y in 6..34 step 4) {
            grid[width - 2][y] = CellType.DELIVERY_STATION
            deliveryStations.add(GridPoint(width - 2, y))
        }
        warehouseZones.add(WarehouseZone("DELIVERY DOCKS EAST", CellType.DELIVERY_STATION, GridPoint(width - 3, 20), "🚚 OUTBOUND DOCKS"))

        // Storage Racks (Grid blocks of 4x6 racks with aisles between them)
        // Leave main avenues open: y = 5, y = 20, y = 35 (horizontal avenues)
        // x = 3, x = 18, x = 30, x = 42, x = 56 (vertical avenues)
        val bayNames = listOf("BAY A", "BAY B", "BAY C", "BAY D", "BAY E")
        for (rackCol in 0..4) {
            val startX = 6 + rackCol * 10
            for (rackRow in 0..2) {
                val startY = 8 + rackRow * 10
                for (dx in 0..5) {
                    for (dy in 0..4) {
                        val gx = startX + dx
                        val gy = startY + dy
                        if (gx < width - 4 && gy < height - 3) {
                            grid[gx][gy] = CellType.STORAGE_RACK
                        }
                    }
                }
            }
            if (rackCol < bayNames.size) {
                warehouseZones.add(WarehouseZone("STORAGE ${bayNames[rackCol]}", CellType.STORAGE_RACK, GridPoint(startX + 3, 7), bayNames[rackCol]))
            }
        }

        // Narrow bottleneck corridor (center bottleneck between sections at x: 28..32, y: 20)
        for (x in 27..33) {
            grid[x][19] = CellType.WALL
            grid[x][21] = CellType.WALL
            grid[x][20] = CellType.NARROW_CORRIDOR
        }
        warehouseZones.add(WarehouseZone("BOTTLENECK ALPHA", CellType.NARROW_CORRIDOR, GridPoint(30, 20), "⚠ BOTTLENECK ALPHA"))

        // Waiting Areas (turnout pockets next to narrow corridors and intersections)
        val turnouts = listOf(
            GridPoint(26, 18),
            GridPoint(26, 22),
            GridPoint(34, 18),
            GridPoint(34, 22),
            GridPoint(18, 5),
            GridPoint(42, 5),
            GridPoint(18, 35),
            GridPoint(42, 35)
        )
        for (p in turnouts) {
            if (grid[p.x][p.y] == CellType.FLOOR) {
                grid[p.x][p.y] = CellType.WAITING_AREA
                waitingAreas.add(p)
            }
        }
        warehouseZones.add(WarehouseZone("TURNOUT BAY WEST", CellType.WAITING_AREA, GridPoint(26, 18), "WAITING BAY 1"))
        warehouseZones.add(WarehouseZone("TURNOUT BAY EAST", CellType.WAITING_AREA, GridPoint(34, 22), "WAITING BAY 2"))

        // Mark Intersections at Avenue Crossings
        val avenueX = listOf(3, 18, 30, 42, 56)
        val avenueY = listOf(5, 20, 35)
        for (ax in avenueX) {
            for (ay in avenueY) {
                if (grid[ax][ay] == CellType.FLOOR || grid[ax][ay] == CellType.NARROW_CORRIDOR) {
                    grid[ax][ay] = CellType.INTERSECTION
                }
            }
        }
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        if (x < 0 || x >= width || y < 0 || y >= height) return false
        if (dynamicObstacles.contains(GridPoint(x, y))) return false
        val cell = grid[x][y]
        return cell != CellType.WALL && cell != CellType.STORAGE_RACK
    }

    fun isWalkable(point: GridPoint): Boolean = isWalkable(point.x, point.y)

    fun getRandomWalkablePoint(): GridPoint {
        var tries = 0
        while (tries < 1000) {
            val rx = (2 until width - 2).random()
            val ry = (2 until height - 2).random()
            if (isWalkable(rx, ry)) {
                return GridPoint(rx, ry)
            }
            tries++
        }
        return GridPoint(3, 5)
    }
}
