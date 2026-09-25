package com.example.engine

import com.example.model.Robot
import kotlin.math.floor

/**
 * High-performance spatial hash grid for O(1) neighbor lookups.
 * Critical for scaling to 500+ robots at 60 FPS without O(N^2) performance penalties.
 */
class SpatialHashGrid(
    private val cellSize: Float = 3.0f
) {
    // Key: (cellX shl 16) or (cellY and 0xFFFF)
    private val buckets = HashMap<Int, ArrayList<Robot>>(256)

    fun clear() {
        buckets.clear()
    }

    private fun hash(gx: Int, gy: Int): Int {
        return (gx shl 16) or (gy and 0xFFFF)
    }

    fun insert(robot: Robot) {
        val gx = floor(robot.x / cellSize).toInt()
        val gy = floor(robot.y / cellSize).toInt()
        val key = hash(gx, gy)
        val list = buckets.getOrPut(key) { ArrayList(8) }
        list.add(robot)
    }

    fun getNearbyRobots(x: Float, y: Float, radius: Float): List<Robot> {
        val results = ArrayList<Robot>(16)
        val minGx = floor((x - radius) / cellSize).toInt()
        val maxGx = floor((x + radius) / cellSize).toInt()
        val minGy = floor((y - radius) / cellSize).toInt()
        val maxGy = floor((y + radius) / cellSize).toInt()

        val rSq = radius * radius

        for (gx in minGx..maxGx) {
            for (gy in minGy..maxGy) {
                val bucket = buckets[hash(gx, gy)] ?: continue
                for (i in 0 until bucket.size) {
                    val robot = bucket[i]
                    val dx = robot.x - x
                    val dy = robot.y - y
                    if (dx * dx + dy * dy <= rSq) {
                        results.add(robot)
                    }
                }
            }
        }
        return results
    }
}
