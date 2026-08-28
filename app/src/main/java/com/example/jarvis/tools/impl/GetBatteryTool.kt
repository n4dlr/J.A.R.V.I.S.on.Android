package com.example.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class GetBatteryTool : Tool {
    override val id: String = "GET_BATTERY"
    override val name: String = "Batareya Vəziyyəti"
    override val description: String = "Cihazın batareya faizini, enerji yığma vəziyyətini və temperaturunu yoxlayır."
    override val parameters: List<ToolParameter> = emptyList()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct: Int = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else -1

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val plugged: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val powerSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Şəbəkə adapteri"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB kabel"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Simsiz şarj"
                else -> "Batareya"
            }

            val temp: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempCelsius = temp / 10.0

            val chargingText = if (isCharging) "enerji yığır ($powerSource)" else "enerji yığmır"
            val msg = "Batareya səviyyəsi: %$batteryPct ($chargingText), temperatur: ${tempCelsius}°C"

            ToolResult.success(
                toolId = id,
                message = msg,
                data = mapOf(
                    "percentage" to batteryPct,
                    "isCharging" to isCharging,
                    "temperature" to tempCelsius,
                    "powerSource" to powerSource
                )
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Batareya məlumatı alına bilmədi: ${e.message}")
        }
    }
}
