package com.example.jarvis.core

import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.JarvisAccessibilityService

sealed class FallbackAction {
    data class UseAlternativeTool(val toolId: String, val message: String) : FallbackAction()
    data class OpenSettings(val settingsType: String, val message: String) : FallbackAction()
    data class TextOnlyResponse(val message: String) : FallbackAction()
}

class CrashRecoveryManager {

    fun recoverFromToolFailure(toolId: String, error: String): FallbackAction {
        return when (toolId) {
            "WIFI_STATUS", "WIFI_TOGGLE" -> FallbackAction.OpenSettings(
                "WIFI",
                "Wi-Fi əməliyyatında xəta baş verdi. Wi-Fi parametrləri açılır."
            )
            "BLUETOOTH_STATUS", "BLUETOOTH_TOGGLE" -> FallbackAction.OpenSettings(
                "BLUETOOTH",
                "Bluetooth əməliyyatında xəta baş verdi. Bluetooth parametrləri açılır."
            )
            "SCREEN_CONTROL" -> FallbackAction.OpenSettings(
                "DISPLAY",
                "Ekran parlaqlığını dəyişmək üçün parametrlər açılır."
            )
            "CLICK_UI_ELEMENT", "SCROLL", "READ_VISIBLE_TEXT" -> FallbackAction.OpenSettings(
                "ACCESSIBILITY",
                "JARVIS Əlçatımlılıq xidməti aktiv deyil. Əlçatımlılıq parametrləri açılır."
            )
            else -> FallbackAction.TextOnlyResponse(
                "'$toolId' əməliyyatı icra edilə bilmədi: $error"
            )
        }
    }

    fun recoverFromSlmCrash(error: Throwable): String {
        return "Lokal SLM modelində xəta baş verdi. Sistem deterministik qayda mühərrikinə keçdi."
    }

    fun recoverFromCloudFailure(): String {
        return "İnternet bağlantısı və ya bulud modeli əlçatan deyil. Əmr lokal mühərrik vasitəsilə icra edilir."
    }
}
