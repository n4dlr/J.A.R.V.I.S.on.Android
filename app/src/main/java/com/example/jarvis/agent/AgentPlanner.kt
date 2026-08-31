package com.example.jarvis.agent

import com.example.jarvis.ai.normalizer.AzerbaijaniTextNormalizer

class AgentPlanner(
    private val normalizer: AzerbaijaniTextNormalizer = AzerbaijaniTextNormalizer(),
    private val multiStepEngine: MultiStepAgentEngine = MultiStepAgentEngine(AzerbaijaniTextNormalizer())
) {

    private val diagnosticPatterns = listOf(
        Regex("""(?i)\b(telefonum|cihazim|telefon|cihaz)\s*(niye|neden)?\s*(yavasdir|donur|qizir|agir\s*isleyir|zeifdir|problem|yoxla)\b"""),
        Regex("""(?i)\b(telefonumu|cihazi)\s*(yoxla|diaqnostika\s*et|test\s*et|analiz\s*et)\b"""),
        Regex("""(?i)\b(problem\s*varmi|sistem\s*diaqnostikasi|tam\s*yoxlanis)\b""")
    )

    /** Check if a query warrants multi-step agent planning. */
    fun shouldPlan(rawQuery: String): Boolean {
        val normalized = normalizer.normalize(rawQuery)
        val isDiagnostic = diagnosticPatterns.any { it.containsMatchIn(normalized) }
        val isMultiStep = multiStepEngine.isMultiStep(rawQuery)
        return isDiagnostic || isMultiStep
    }

    /** Generate an [AgentPlan] for the user's high-level goal. */
    fun createPlan(rawQuery: String): AgentPlan {
        val normalized = normalizer.normalize(rawQuery)

        // 1. Full System Diagnostic Plan
        if (diagnosticPatterns.any { it.containsMatchIn(normalized) }) {
            return AgentPlan(
                goal = rawQuery,
                rationale = "Cihazın ümumi vəziyyətini, RAM, CPU, yaddaş və batareya göstəricilərini ardıcıl yoxlayıb analiz etmək.",
                steps = listOf(
                    PlanStep("step_1", "GET_RAM", description = "RAM istifadəsini yoxla"),
                    PlanStep("step_2", "CPU_STATUS", description = "CPU yükünü yoxla"),
                    PlanStep("step_3", "GET_STORAGE", description = "Daxili yaddaşın boş yerini yoxla"),
                    PlanStep("step_4", "BATTERY_STATUS", description = "Batareya səviyyəsi və temperaturunu yoxla"),
                    PlanStep("step_5", "NETWORK_STATUS", description = "Şəbəkə bağlantısını yoxla")
                )
            )
        }

        // 2. Multi-step NLU Plan (e.g. "Instagram-a gir, son posta bax, şərh yaz")
        if (multiStepEngine.isMultiStep(rawQuery)) {
            return multiStepEngine.parse(rawQuery)
        }

        // 3. Default Single-step Plan (fallback)
        return AgentPlan(
            goal = rawQuery,
            rationale = "Birbaşa əmr icrası planı.",
            steps = listOf(
                PlanStep("step_1", "UNKNOWN", description = "Tək addımlı əmr")
            )
        )
    }
}
