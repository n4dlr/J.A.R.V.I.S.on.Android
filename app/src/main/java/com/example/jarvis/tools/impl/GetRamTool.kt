package com.example.jarvis.tools.impl

import android.app.ActivityManager
import android.content.Context
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class GetRamTool : Tool {
    override val id: String = "GET_RAM"
    override val name: String = "RAM Monitor"
    override val description: String = "Cihazın ümumi, istifadə olunan və boş RAM həcmini ölçür."
    override val parameters: List<ToolParameter> = emptyList()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = (totalMb - availMb).coerceAtLeast(0)
            val usedPercent = if (totalMb > 0) ((usedMb.toDouble() / totalMb) * 100).toInt() else 0
            val isLowMem = memInfo.lowMemory

            val stateLabel = if (isLowMem) "Kritik dərəcədə az RAM" else "Normal"
            val msg = "RAM vəziyyəti: ${usedMb} MB / ${totalMb} MB istifadədədir (%$usedPercent doludur). Boş RAM: ${availMb} MB ($stateLabel)."

            ToolResult.success(
                toolId = id,
                message = msg,
                data = mapOf(
                    "totalRamMb" to totalMb,
                    "availableRamMb" to availMb,
                    "usedRamMb" to usedMb,
                    "usagePercent" to usedPercent,
                    "isLowMemory" to isLowMem
                )
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "RAM məlumatı oxunarkən xəta baş verdi: ${e.message}")
        }
    }
}
