package com.example.jarvis.tools.impl.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import java.io.BufferedReader
import java.io.FileReader

/** CPU_STATUS — reads CPU usage from /proc/stat (non-root). */
class CpuStatusTool : Tool {
    override val id = "CPU_STATUS"
    override val name = "CPU Vəziyyəti"
    override val description = "CPU istifadəsini və çekirdək sayını göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            // Read two snapshots 200ms apart for a basic usage estimate
            val snap1 = readCpuStats()
            Thread.sleep(200)
            val snap2 = readCpuStats()

            val totalDelta = snap2.total - snap1.total
            val idleDelta  = snap2.idle  - snap1.idle
            val usagePct   = if (totalDelta > 0)
                ((totalDelta - idleDelta) * 100.0 / totalDelta).toInt()
            else -1

            val msg = if (usagePct >= 0)
                "CPU: $usagePct% istifadə edilir, $cores çekirdək."
            else
                "CPU: $cores çekirdək (istifadə faizi ölçülə bilmədi)."

            ToolResult.success(id, msg, mapOf("cores" to cores, "usagePercent" to usagePct))
        } catch (e: Exception) {
            ToolResult.failed(id, "CPU məlumatı alına bilmədi: ${e.message}")
        }
    }

    private data class CpuSnapshot(val total: Long, val idle: Long)

    private fun readCpuStats(): CpuSnapshot {
        val line = BufferedReader(FileReader("/proc/stat")).use { it.readLine() } ?: return CpuSnapshot(0L, 0L)
        val parts = line.trim().split("\\s+".toRegex()).drop(1).map { it.toLongOrNull() ?: 0L }
        val total = parts.sum()
        val idle  = if (parts.size > 3) parts[3] else 0L
        return CpuSnapshot(total, idle)
    }
}

/** DEVICE_INFO — hardware and OS info. */
class DeviceInfoTool : Tool {
    override val id = "DEVICE_INFO"
    override val name = "Cihaz Məlumatı"
    override val description = "Cihazın model, Android versiyası, istehsalçı məlumatlarını göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model        = Build.MODEL
            val androidVer   = Build.VERSION.RELEASE
            val sdk          = Build.VERSION.SDK_INT
            val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Build.VERSION.SECURITY_PATCH else "bilinmir"

            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalRamGb = "%.1f".format(memInfo.totalMem / 1_073_741_824.0)

            val msg = "$manufacturer $model — Android $androidVer (API $sdk), " +
                    "RAM: ${totalRamGb} GB, Təhlükəsizlik yamağı: $securityPatch."

            ToolResult.success(id, msg, mapOf(
                "manufacturer" to manufacturer,
                "model"        to model,
                "android"      to androidVer,
                "sdk"          to sdk,
                "security"     to securityPatch,
                "totalRamGb"   to totalRamGb
            ))
        } catch (e: Exception) {
            ToolResult.failed(id, "Cihaz məlumatı alına bilmədi: ${e.message}")
        }
    }
}
