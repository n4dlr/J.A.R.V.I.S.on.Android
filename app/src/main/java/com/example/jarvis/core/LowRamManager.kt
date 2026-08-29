package com.example.jarvis.core

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class MemoryTelemetry(
    val totalRamMb: Long,
    val availRamMb: Long,
    val usedRamMb: Long,
    val ramUsagePercent: Int,
    val isLowRamDevice: Boolean,
    val isLowMemoryPressure: Boolean,
    val availableStorageMb: Long,
    val totalStorageMb: Long
)

class LowRamManager(private val context: Context? = null) : ComponentCallbacks2 {

    private val activityManager: ActivityManager? = try {
        context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    } catch (_: Throwable) {
        null
    }

    private val _telemetry = MutableStateFlow(fetchCurrentMemoryInfo())
    val telemetry: StateFlow<MemoryTelemetry> = _telemetry.asStateFlow()

    private val _isMemoryPressureHigh = MutableStateFlow(false)
    val isMemoryPressureHigh: StateFlow<Boolean> = _isMemoryPressureHigh.asStateFlow()

    init {
        try {
            context?.registerComponentCallbacks(this)
        } catch (_: Throwable) {}
    }

    fun refreshTelemetry(): MemoryTelemetry {
        val current = fetchCurrentMemoryInfo()
        _telemetry.value = current
        return current
    }

    fun isLowRamEnvironment(): Boolean {
        return activityManager?.isLowRamDevice ?: (fetchCurrentMemoryInfo().totalRamMb <= 4096)
    }

    fun getMaxContextWindowSize(): Int {
        // Enforce tight bounded context window on 4GB devices to avoid OOM
        return if (isLowRamEnvironment()) 4 else 12
    }

    fun getMaxCacheEntries(): Int {
        return if (isLowRamEnvironment()) 20 else 100
    }

    fun shouldUnloadModelImmediately(): Boolean {
        val mem = fetchCurrentMemoryInfo()
        return mem.isLowMemoryPressure || mem.ramUsagePercent > 80 || mem.availRamMb < 500
    }

    private fun fetchCurrentMemoryInfo(): MemoryTelemetry {
        val am = activityManager
        if (am == null) {
            return MemoryTelemetry(
                totalRamMb = 8192,
                availRamMb = 4096,
                usedRamMb = 4096,
                ramUsagePercent = 50,
                isLowRamDevice = false,
                isLowMemoryPressure = false,
                availableStorageMb = 64000,
                totalStorageMb = 128000
            )
        }
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)

        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availRamMb = memoryInfo.availMem / (1024 * 1024)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0)
        val percent = if (totalRamMb > 0) ((usedRamMb.toDouble() / totalRamMb) * 100).toInt() else 0

        val storageStats = getStorageStats()

        return MemoryTelemetry(
            totalRamMb = totalRamMb,
            availRamMb = availRamMb,
            usedRamMb = usedRamMb,
            ramUsagePercent = percent,
            isLowRamDevice = activityManager.isLowRamDevice || totalRamMb <= 4096,
            isLowMemoryPressure = memoryInfo.lowMemory,
            availableStorageMb = storageStats.first,
            totalStorageMb = storageStats.second
        )
    }

    private fun getStorageStats(): Pair<Long, Long> {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            val availMb = (availableBlocks * blockSize) / (1024 * 1024)
            val totalMb = (totalBlocks * blockSize) / (1024 * 1024)
            Pair(availMb, totalMb)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        ) {
            _isMemoryPressureHigh.value = true
        } else if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            _isMemoryPressureHigh.value = false
        }
        refreshTelemetry()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        _isMemoryPressureHigh.value = true
        refreshTelemetry()
    }

    fun unregister() {
        try {
            context?.unregisterComponentCallbacks(this)
        } catch (_: Exception) {}
    }
}
