package com.example.jarvis.security

import com.example.jarvis.domain.model.PendingActionConfirmation
import com.example.jarvis.domain.model.RiskAssessment
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.StructuredIntent
import java.util.UUID

enum class ConfirmationProfile {
    /** Prompts confirmation for all HIGH and CRITICAL tools. */
    STRICT,
    /** Standard profile: prompts for destructive actions (DELETE, CALL) and CRITICAL. */
    STANDARD,
    /** Automation mode: prompts only for CRITICAL actions. */
    AUTOMATED
}

class CommandSanitizer {

    private val dangerousPatterns = listOf(
        Regex("""(?i)\b(su|sudo|chmod|chown|rm\s+-rf|reboot|setenforce|busybox|sh|bash|mount|dd\s+if=)\b"""),
        Regex("""(?i)\b(system/bin|system/xbin|init\.rc|build\.prop|data/data)\b"""),
        Regex("""(?i)[;&|`$><]""") // Shell meta-characters
    )

    fun sanitizeInput(rawText: String): String {
        var clean = rawText.trim()
        if (clean.length > 500) {
            clean = clean.substring(0, 500)
        }
        return clean
    }

    fun isSuspiciousShellAttempt(text: String): Boolean {
        return dangerousPatterns.any { it.containsMatchIn(text) }
    }

    fun sanitizeArgument(key: String, value: String): String {
        return value.trim().take(120)
    }
}

class RiskManager(
    private val sanitizer: CommandSanitizer = CommandSanitizer(),
    var confirmationProfile: ConfirmationProfile = ConfirmationProfile.STANDARD
) {
    fun assessRisk(intent: StructuredIntent, toolRiskLevel: RiskLevel): RiskAssessment {
        val warnings = mutableListOf<String>()

        // 1. Security Check: Reject shell injection or exploit signatures
        if (sanitizer.isSuspiciousShellAttempt(intent.rawQuery)) {
            return RiskAssessment(
                riskLevel = RiskLevel.CRITICAL,
                requiresExplicitConfirmation = true,
                rationale = "Təhlükəli sistem əmri və ya shell script şübhəsi aşkar edildi. Birbaşa icraya icazə verilmir.",
                securityWarnings = listOf("Arbitrary shell execution strictly prohibited.")
            )
        }

        // 2. Map risk according to tool level and active confirmation profile
        val requiresConfirmation = when (confirmationProfile) {
            ConfirmationProfile.STRICT -> when (toolRiskLevel) {
                RiskLevel.LOW -> false
                RiskLevel.MEDIUM -> false
                RiskLevel.HIGH -> true
                RiskLevel.CRITICAL -> true
            }
            ConfirmationProfile.STANDARD -> when (toolRiskLevel) {
                RiskLevel.LOW -> false
                RiskLevel.MEDIUM -> false
                RiskLevel.HIGH -> intent.intentId in listOf("DELETE_FILE", "CALL_CONTACT", "DELETE_EVENT", "LOCK_SCREEN")
                RiskLevel.CRITICAL -> true
            }
            ConfirmationProfile.AUTOMATED -> when (toolRiskLevel) {
                RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH -> false
                RiskLevel.CRITICAL -> true
            }
        }

        val rationaleAz = when (toolRiskLevel) {
            RiskLevel.LOW -> "Təhlükəsiz oxu/məlumat əməliyyatı."
            RiskLevel.MEDIUM -> "Cihazın tənzimləməsinə təsir edən standart əməliyyat."
            RiskLevel.HIGH -> "Cihazın vəziyyətini dəyişən yüksək riskli əməliyyat."
            RiskLevel.CRITICAL -> "Kritik əməliyyat — istifadəçinin dəqiq təsdiqi tələb olunur."
        }

        return RiskAssessment(
            riskLevel = toolRiskLevel,
            requiresExplicitConfirmation = requiresConfirmation,
            rationale = rationaleAz,
            securityWarnings = warnings
        )
    }

    fun createPendingConfirmation(
        toolId: String,
        intent: StructuredIntent,
        assessment: RiskAssessment
    ): PendingActionConfirmation {
        val promptText = when (toolId) {
            "LOCK_SCREEN" -> "Ekranı dərhal kilidləmək istədiyinizə əminsiniz?"
            "TAKE_PHOTO" -> "Kameranı açıb şəkil çəkməyə icazə verirsiniz?"
            "CREATE_REMINDER", "CREATE_ALARM" -> "Xatırlatma/Zəngli saat qurulsun: '${intent.arguments["title"] ?: intent.arguments["message"] ?: intent.rawQuery}'?"
            "DELETE_FILE" -> "'${intent.arguments["path"] ?: "Seçilmiş faylı"}' faylını silmək istədiyinizə əminsiniz?"
            "CALL_CONTACT" -> "'${intent.arguments["name"] ?: intent.arguments["number"] ?: "bu nömrəyə"}' zəng etmək istəyirsiniz?"
            "DIAL_NUMBER" -> "'${intent.arguments["number"] ?: "bu nömrəni"}' yığmaq istəyirsiniz?"
            "DELETE_EVENT" -> "Təqvim hadisəsini silmək istəyirsiniz?"
            "CREATE_CONTACT" -> "'${intent.arguments["name"] ?: "Yeni kontakt"}' kontaktını yaratmaq istəyirsiniz?"
            else -> "'$toolId' əməliyyatını icra etmək istəyirsiniz?"
        }

        return PendingActionConfirmation(
            id = UUID.randomUUID().toString(),
            toolId = toolId,
            structuredIntent = intent,
            riskAssessment = assessment,
            userPromptText = promptText
        )
    }
}
