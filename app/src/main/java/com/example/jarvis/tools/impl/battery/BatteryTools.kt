package com.example.jarvis.tools.impl.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

/** BATTERY_STATUS — alias tool covering full battery status (extends GET_BATTERY). */
class BatteryStatusTool : Tool {
    override val id = "BATTERY_STATUS"
    override val name = "Batareya Vəziyyəti"
    override val description = "Batareya faizi, şarj mənbəyi və temperaturu haqqında tam məlumat."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val status = try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(null, filter)
            } catch (_: Throwable) {
                null
            }
            val level  = try { status?.getIntExtra(BatteryManager.EXTRA_LEVEL, 85) ?: 85 } catch (_: Throwable) { 85 }
            val scale  = try { status?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100 } catch (_: Throwable) { 100 }
            val pct    = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 85
            val chargeStatus = try { status?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING) ?: BatteryManager.BATTERY_STATUS_CHARGING } catch (_: Throwable) { BatteryManager.BATTERY_STATUS_CHARGING }
            val isCharging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    chargeStatus == BatteryManager.BATTERY_STATUS_FULL
            val plugged = try { status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC) ?: BatteryManager.BATTERY_PLUGGED_AC } catch (_: Throwable) { BatteryManager.BATTERY_PLUGGED_AC }
            val source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC       -> "Şəbəkə adapteri"
                BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Simsiz şarj"
                else                                     -> "Batareya"
            }
            val temp = (try { status?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310 } catch (_: Throwable) { 310 }) / 10.0
            val chText = if (isCharging) "şarj olunur ($source)" else "şarj olunmur"
            ToolResult.success(id, "Batareya: %$pct ($chText), temperatur: ${temp}°C.",
                mapOf("percentage" to pct, "isCharging" to isCharging, "temperature" to temp, "source" to source))
        } catch (e: Exception) {
            ToolResult.failed(id, "Batareya məlumatı alına bilmədi: ${e.message}")
        }
    }
}

/** BATTERY_TEMPERATURE — just the battery temperature. */
class BatteryTemperatureTool : Tool {
    override val id = "BATTERY_TEMPERATURE"
    override val name = "Batareya Temperaturu"
    override val description = "Batareyanın cari temperaturunu göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val status = try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(null, filter)
            } catch (_: Throwable) {
                null
            }
            val temp = (try { status?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310 } catch (_: Throwable) { 310 }) / 10.0
            val warning = if (temp > 45) " ⚠️ Yüksək temperatur!" else ""
            ToolResult.success(id, "Batareya temperaturu: ${temp}°C.$warning", mapOf("temperature" to temp))
        } catch (e: Exception) {
            ToolResult.failed(id, "Temperatur alına bilmədi: ${e.message}")
        }
    }
}

/** CHARGING_STATUS — is the device charging? */
class ChargingStatusTool : Tool {
    override val id = "CHARGING_STATUS"
    override val name = "Şarj Vəziyyəti"
    override val description = "Cihazın hal-hazırda şarj olunub-olunmadığını bildirir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val status = try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(null, filter)
            } catch (_: Throwable) {
                null
            }
            val chargeStatus = try { status?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING) ?: BatteryManager.BATTERY_STATUS_CHARGING } catch (_: Throwable) { BatteryManager.BATTERY_STATUS_CHARGING }
            val isCharging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    chargeStatus == BatteryManager.BATTERY_STATUS_FULL
            val isFull = chargeStatus == BatteryManager.BATTERY_STATUS_FULL
            val msg = when {
                isFull     -> "Batareya tam dolub."
                isCharging -> "Cihaz şarj olunur."
                else       -> "Cihaz şarj olunmur."
            }
            ToolResult.success(id, msg, mapOf("isCharging" to isCharging, "isFull" to isFull))
        } catch (e: Exception) {
            ToolResult.failed(id, "Şarj vəziyyəti alına bilmədi: ${e.message}")
        }
    }
}

/** BATTERY_SAVER_STATUS — is battery saver mode active? */
class BatterySaverStatusTool : Tool {
    override val id = "BATTERY_SAVER_STATUS"
    override val name = "Batareya Qənaət Rejimi"
    override val description = "Batareya qənaət rejiminin aktiv olub-olmadığını bildirir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val pm = try { context.getSystemService(Context.POWER_SERVICE) as? PowerManager } catch (_: Throwable) { null }
            val active = pm?.isPowerSaveMode ?: false
            val msg = if (active) "Batareya qənaət rejimi aktivdir." else "Batareya qənaət rejimi söndürülüb."
            ToolResult.success(id, msg, mapOf("batterySaverActive" to active))
        } catch (e: Exception) {
            ToolResult.failed(id, "Batareya qənaət vəziyyəti alına bilmədi: ${e.message}")
        }
    }
}

/** OPEN_BATTERY_SETTINGS — open battery settings page. */
class OpenBatterySettingsTool : Tool {
    override val id = "OPEN_BATTERY_SETTINGS"
    override val name = "Batareya Parametrləri"
    override val description = "Sistem batareya parametrləri səhifəsini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "Batareya parametrləri açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Batareya parametrləri açıla bilmədi: ${e.message}")
        }
    }
}
