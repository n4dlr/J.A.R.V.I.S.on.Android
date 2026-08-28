package com.example.jarvis.core

import com.example.jarvis.domain.model.StructuredIntent
import com.example.jarvis.tools.ToolRegistry

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

class ToolSecurityValidator(
    private val toolRegistry: ToolRegistry
) {

    private val suspiciousPathPatterns = listOf(
        Regex("""(?i)(\.\./|\.\.\\)"""),
        Regex("""(?i)^/(data/data|system|etc|proc|sys|root)\b""")
    )

    private val promptInjectionPatterns = listOf(
        Regex("""(?i)\b(ignore previous instructions|disregard|system prompt|reveal your prompt|jailbreak)\b"""),
        Regex("""(?i)\b(sən artıq jarvis deyilsən|əvvəlki əmrləri unut və)\b""")
    )

    fun validateToolExecution(
        toolId: String,
        arguments: Map<String, String>,
        rawQuery: String
    ): ValidationResult {
        // 1. Tool Allowlist Check
        if (!toolRegistry.hasTool(toolId)) {
            return ValidationResult.Invalid("Alət icazə verilən alətlər siyahısında (allowlist) yoxdur: $toolId")
        }

        // 2. Prompt Injection Check
        if (promptInjectionPatterns.any { it.containsMatchIn(rawQuery) }) {
            return ValidationResult.Invalid("Təhlükəli prompt injection cəhdi aşkar edildi.")
        }

        // 3. Path Traversal & Restricted Paths for File operations
        val pathParam = arguments["path"] ?: arguments["source"] ?: arguments["destination"]
        if (pathParam != null) {
            if (suspiciousPathPatterns.any { it.containsMatchIn(pathParam) }) {
                return ValidationResult.Invalid("Qorunan və ya icazəsiz sistem fayl yoluna giriş qadağandır.")
            }
        }

        // 4. Parameter length boundary check
        for ((key, value) in arguments) {
            if (value.length > 250) {
                return ValidationResult.Invalid("'$key' parametri icazə verilən həddi aşır (maksimum 250 simvol).")
            }
        }

        return ValidationResult.Valid
    }
}
