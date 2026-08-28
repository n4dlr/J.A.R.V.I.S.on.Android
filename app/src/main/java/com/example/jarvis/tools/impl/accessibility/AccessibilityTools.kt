package com.example.jarvis.tools.impl.accessibility

import android.content.Context
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.services.JarvisAccessibilityService
import com.example.jarvis.services.ScrollDirection
import com.example.jarvis.tools.CapabilityDetector
import com.example.jarvis.tools.Tool

private fun requireAccessibility(toolId: String, context: Context): ToolResult? {
    if (!JarvisAccessibilityService.isEnabled(context)) {
        return ToolResult.specialAccessRequired(
            toolId,
            CapabilityDetector.ACCESSIBILITY_SERVICE,
            "Bu əməliyyat üçün JARVIS Əlçatımlılıq Xidmətini aktivləşdirməlisiniz. " +
                    "Parametrlər → Əlçatımlılıq → JARVIS → Aktivləşdir."
        )
    }
    return null
}

/** CLICK_UI_ELEMENT — click a view by text or resource-id. */
class ClickUiElementTool : Tool {
    override val id = "CLICK_UI_ELEMENT"
    override val name = "Ekranda Düyməyə Bas"
    override val description = "Ekranda görünən düymə və ya mətni basır (Əlçatımlılıq xidməti ilə)."
    override val parameters = listOf(
        ToolParameter("text", "string", false, "Basılacaq düymə/elementin mətni", ""),
        ToolParameter("view_id", "string", false, "Elementin resurs ID-si", "")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireAccessibility(id, context)?.let { return it }
        val service = JarvisAccessibilityService.get()
            ?: return ToolResult.failed(id, "Əlçatımlılıq xidməti hazır deyil.")

        val text = params["text"]?.trim() ?: ""
        val viewId = params["view_id"]?.trim() ?: ""

        val success = when {
            text.isNotEmpty() -> service.clickByText(text)
            viewId.isNotEmpty() -> service.clickById(viewId)
            else -> return ToolResult.failed(id, "Basılacaq element mətni və ya ID-si qeyd edilməyib.")
        }

        return if (success) {
            ToolResult.success(id, "Elementə basıldı: ${text.ifEmpty { viewId }}")
        } else {
            ToolResult.failed(id, "Element ekranda tapılmadı və ya basıla bilmədi.")
        }
    }
}

/** SCROLL — scroll up or down in current active window. */
class ScrollTool : Tool {
    override val id = "SCROLL"
    override val name = "Ekranı Sürüşdür"
    override val description = "Ekranda yuxarı və ya aşağı sürüşdürmə əməliyyatı icra edir."
    override val parameters = listOf(
        ToolParameter("direction", "string", false, "UP | DOWN", "DOWN")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireAccessibility(id, context)?.let { return it }
        val service = JarvisAccessibilityService.get()
            ?: return ToolResult.failed(id, "Əlçatımlılıq xidməti hazır deyil.")

        val dirStr = params["direction"]?.uppercase() ?: "DOWN"
        val dir = if (dirStr == "UP") ScrollDirection.UP else ScrollDirection.DOWN

        val scrolled = service.scroll(dir)
        return if (scrolled) {
            ToolResult.success(id, "Ekran ${if (dir == ScrollDirection.UP) "yuxarı" else "aşağı"} sürüşdürüldü.")
        } else {
            ToolResult.failed(id, "Sürüşdürülə bilən səhifə və ya siyahı tapılmadı.")
        }
    }
}

/** READ_VISIBLE_TEXT — read all text nodes visible on the current screen. */
class ReadVisibleTextTool : Tool {
    override val id = "READ_VISIBLE_TEXT"
    override val name = "Ekrandakı Mətni Oxu"
    override val description = "Cari ekranda görünən bütün mətnləri oxuyur."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireAccessibility(id, context)?.let { return it }
        val service = JarvisAccessibilityService.get()
            ?: return ToolResult.failed(id, "Əlçatımlılıq xidməti hazır deyil.")

        val text = service.readVisibleText()
        return if (text.isNotBlank()) {
            val preview = if (text.length > 300) text.take(300) + "…" else text
            ToolResult.success(id, "Ekrandakı mətn:\n$preview", mapOf("fullText" to text))
        } else {
            ToolResult.success(id, "Ekran üzərində oxuna bilən mətn tapılmadı.")
        }
    }
}

/** GO_BACK — trigger system back action. */
class GoBackTool : Tool {
    override val id = "GO_BACK"
    override val name = "Geri Qayıt"
    override val description = "Sistem 'Geri' (Back) düyməsini icra edir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireAccessibility(id, context)?.let { return it }
        val service = JarvisAccessibilityService.get()
            ?: return ToolResult.failed(id, "Əlçatımlılıq xidməti hazır deyil.")

        val done = service.goBack()
        return if (done) ToolResult.success(id, "Geri qayıdıldı.")
        else ToolResult.failed(id, "Geri əmri icra edilə bilmədi.")
    }
}

/** GO_HOME — trigger system home action via accessibility. */
class AccessibilityGoHomeTool : Tool {
    override val id = "GO_HOME"
    override val name = "Ana Ekrana Get"
    override val description = "Sistem 'Home' düyməsini icra edir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val service = JarvisAccessibilityService.get()
        return if (service != null && service.goHome()) {
            ToolResult.success(id, "Ana ekrana keçildi.")
        } else {
            // Fallback to OpenHomeTool logic
            com.example.jarvis.tools.impl.system.OpenHomeTool().execute(context, params)
        }
    }
}

/** INTERACT_WITH_SUPPORTED_UI — complex UI element interaction wrapper. */
class InteractWithUiTool : Tool {
    override val id = "INTERACT_WITH_SUPPORTED_UI"
    override val name = "İnterfeyslə Qarşılıqlı Əlaqə"
    override val description = "Dəstəklənən interfeyslərdə kompleks əməliyyatlar (axtarış, klik, sürüşdürmə) aparır."
    override val parameters = listOf(
        ToolParameter("action", "string", true, "CLICK | SCROLL_DOWN | SCROLL_UP | READ", "READ"),
        ToolParameter("target", "string", false, "Hədəf mətn və ya element adı", "")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        requireAccessibility(id, context)?.let { return it }
        val service = JarvisAccessibilityService.get()
            ?: return ToolResult.failed(id, "Əlçatımlılıq xidməti aktiv deyil.")

        val action = params["action"]?.uppercase() ?: "READ"
        val target = params["target"] ?: ""

        return when (action) {
            "CLICK" -> {
                if (target.isEmpty()) ToolResult.failed(id, "Klik üçün hədəf göstərilməyib.")
                else if (service.clickByText(target) || service.clickById(target)) {
                    ToolResult.success(id, "'$target' elementinə klikləndi.")
                } else ToolResult.failed(id, "'$target' tapılmadı.")
            }
            "SCROLL_DOWN" -> {
                if (service.scroll(ScrollDirection.DOWN)) ToolResult.success(id, "Aşağı sürüşdürüldü.")
                else ToolResult.failed(id, "Sürüşdürmə uğursuz oldu.")
            }
            "SCROLL_UP" -> {
                if (service.scroll(ScrollDirection.UP)) ToolResult.success(id, "Yuxarı sürüşdürüldü.")
                else ToolResult.failed(id, "Sürüşdürmə uğursuz oldu.")
            }
            "READ" -> {
                val txt = service.readVisibleText()
                ToolResult.success(id, "Ekrandakı mətn oxundu:\n" + txt.take(200), mapOf("text" to txt))
            }
            else -> ToolResult.failed(id, "Naməlum interfeys əməliyyatı: $action")
        }
    }
}
