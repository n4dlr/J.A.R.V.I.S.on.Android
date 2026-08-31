package com.example.jarvis.tools.impl.smarthome

import android.content.Context
import android.util.Log
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

// ── Helper: resolve entity ID from natural language room/device name ──────────

private fun resolveEntityId(device: String, room: String, type: String): String {
    val normalizedRoom = room.lowercase()
        .replace("qonaq", "living_room")
        .replace("yataq", "bedroom")
        .replace("mutfaq", "kitchen")
        .replace("mətbəx", "kitchen")
        .replace("hamam", "bathroom")
        .replace("uşaq", "kids_room")
        .replace("ofis", "office")
        .replace("koridor", "hallway")
        .replace(" ", "_")

    return when (type) {
        "light" -> "light.${normalizedRoom.ifBlank { "all" }}"
        "climate" -> "climate.${normalizedRoom.ifBlank { "living_room" }}"
        "lock" -> "lock.${normalizedRoom.ifBlank { "front_door" }}"
        "switch" -> "switch.${normalizedRoom.ifBlank { "main" }}_${device.replace(" ", "_")}"
        "scene" -> "scene.${device.replace(" ", "_").lowercase()}"
        else -> "homeassistant.all"
    }
}

// ── SMART_HOME_LIGHT ─────────────────────────────────────────────────────────

class SmartHomeLightTool(private val getClient: (Context) -> HomeAssistantClient) : Tool {
    override val id = "SMART_HOME_LIGHT"
    override val name = "Ağıllı Ev İşıqları"
    override val description = "Ev işıqlarını açır, söndürür, parlaqlığını tənzimləyir"
    override val parameters = listOf(
        ToolParameter("action", "string", isRequired = true, description = "ac / sondur / toggle / parlaqlig"),
        ToolParameter("room", "string", isRequired = false, description = "Otaq adı (qonaq, yataq, mətbəx...)", defaultValue = ""),
        ToolParameter("brightness", "integer", isRequired = false, description = "Parlaqlıq 0-255", defaultValue = "255")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val action = params["action"]?.lowercase() ?: "toggle"
        val room = params["room"] ?: ""
        val brightness = params["brightness"]?.toIntOrNull() ?: 255
        val entityId = resolveEntityId("", room, "light")

        val (service, serviceData) = when {
            action.contains("ac") || action.contains("aç") || action.contains("yandır") || action == "on" ->
                Pair("turn_on", mapOf("brightness" to brightness))
            action.contains("sondur") || action.contains("söndür") || action == "off" ->
                Pair("turn_off", emptyMap<String, Any>())
            action.contains("parlaq") ->
                Pair("turn_on", mapOf("brightness" to brightness))
            else ->
                Pair("toggle", emptyMap<String, Any>())
        }

        val result = getClient(context).callService("light", service, entityId, serviceData)
        return if (result.success) {
            val roomStr = if (room.isNotBlank()) "$room otağında " else ""
            val actionStr = when (service) {
                "turn_on" -> "yandırıldı 💡"
                "turn_off" -> "söndürüldü"
                else -> "dəyişdirildi"
            }
            ToolResult.success(id, "İşıq ${roomStr}$actionStr")
        } else {
            ToolResult.failed(id, result.message)
        }
    }
}

// ── SMART_HOME_CLIMATE ───────────────────────────────────────────────────────

class SmartHomeClimateTool(private val getClient: (Context) -> HomeAssistantClient) : Tool {
    override val id = "SMART_HOME_CLIMATE"
    override val name = "Ağıllı Ev Kondisioner/Isıtma"
    override val description = "Kondisioneri, isitma sistemini açır/söndürür, temperatur təyin edir"
    override val parameters = listOf(
        ToolParameter("action", "string", isRequired = true, description = "ac / sondur / temp"),
        ToolParameter("temperature", "number", isRequired = false, description = "Hədəf temperatur (məs: 24)", defaultValue = "22"),
        ToolParameter("room", "string", isRequired = false, description = "Otaq adı", defaultValue = "")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val action = params["action"]?.lowercase() ?: "ac"
        val temperature = params["temperature"]?.toDoubleOrNull() ?: 22.0
        val room = params["room"] ?: ""
        val entityId = resolveEntityId("", room, "climate")

        val (service, serviceData) = when {
            action.contains("sondur") || action == "off" ->
                Pair("turn_off", emptyMap<String, Any>())
            action.contains("temp") || action.contains("dərəcə") || action.contains("derece") ->
                Pair("set_temperature", mapOf("temperature" to temperature))
            else ->
                Pair("turn_on", mapOf("temperature" to temperature))
        }

        val result = getClient(context).callService("climate", service, entityId, serviceData)
        return if (result.success) {
            val roomStr = if (room.isNotBlank()) "$room otağında " else ""
            val msg = if (service == "set_temperature") {
                "Kondisioner ${roomStr}${temperature}°C-yə ayarlandı 🌡️"
            } else {
                "Kondisioner ${roomStr}${if (service == "turn_on") "işə salındı ❄️" else "söndürüldü"}"
            }
            ToolResult.success(id, msg)
        } else {
            ToolResult.failed(id, result.message)
        }
    }
}

// ── SMART_HOME_LOCK ──────────────────────────────────────────────────────────

class SmartHomeLockTool(private val getClient: (Context) -> HomeAssistantClient) : Tool {
    override val id = "SMART_HOME_LOCK"
    override val name = "Ağıllı Qapı Kilidi"
    override val description = "Ağıllı qapı kilidini açır və ya bağlayır"
    override val parameters = listOf(
        ToolParameter("action", "string", isRequired = true, description = "kilidle / ac / unlock / lock"),
        ToolParameter("door", "string", isRequired = false, description = "Qapı adı (ön, arxa...)", defaultValue = "front_door")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val action = params["action"]?.lowercase() ?: "lock"
        val door = params["door"] ?: "front_door"
        val entityId = "lock.$door"

        val service = when {
            action.contains("ac") || action.contains("aç") || action == "unlock" -> "unlock"
            else -> "lock"
        }

        val result = getClient(context).callService("lock", service, entityId)
        return if (result.success) {
            val msg = if (service == "lock") "Qapı kilidləndi 🔒" else "Qapı kilidi açıldı 🔓"
            ToolResult.success(id, msg)
        } else {
            ToolResult.failed(id, result.message)
        }
    }
}

// ── SMART_HOME_SCENE ─────────────────────────────────────────────────────────

class SmartHomeSceneTool(private val getClient: (Context) -> HomeAssistantClient) : Tool {
    override val id = "SMART_HOME_SCENE"
    override val name = "Ağıllı Ev Ssenarisi"
    override val description = "Hazır ağıllı ev ssenarisi aktivləşdirir (Film rejimi, Yataq rejimi...)"
    override val parameters = listOf(
        ToolParameter("scene", "string", isRequired = true, description = "Ssenari adı (film, yataq, romantik, normal...)")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel = RiskLevel.LOW

    companion object {
        private val SCENE_ALIASES = mapOf(
            "film" to "movie", "kino" to "movie", "sinema" to "movie",
            "yataq" to "sleep", "yuxu" to "sleep",
            "romantik" to "romantic",
            "normal" to "normal", "adi" to "normal",
            "sabah" to "morning", "sehər" to "morning",
            "axsam" to "evening", "gecə" to "night"
        )
    }

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val rawScene = params["scene"]?.lowercase() ?: ""
        val sceneId = SCENE_ALIASES[rawScene] ?: rawScene.replace(" ", "_")
        val entityId = "scene.$sceneId"

        val result = getClient(context).callService("scene", "turn_on", entityId)
        return if (result.success) {
            ToolResult.success(id, "\"$rawScene\" ssenarisi aktivləşdirildi 🏠✨")
        } else {
            ToolResult.failed(id, result.message)
        }
    }
}
