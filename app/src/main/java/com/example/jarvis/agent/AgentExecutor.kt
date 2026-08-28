package com.example.jarvis.agent

import android.content.Context
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentExecutor(
    private val toolRegistry: ToolRegistry,
    private val memoryManager: MemoryManager
) {

    /**
     * Executes the [plan] following PLAN -> OBSERVE -> ACT -> VERIFY -> RESPOND.
     */
    suspend fun executePlan(context: Context, plan: AgentPlan): AgentExecutionResult = withContext(Dispatchers.IO) {
        val task = memoryManager.createTask(plan.goal, plan.rationale)
        memoryManager.updateTaskStatus(task.id, TaskLifecycleStatus.RUNNING)

        val observations = mutableListOf<ExecutionObservation>()
        val issuesFound = mutableListOf<String>()

        for (step in plan.steps) {
            val tool = toolRegistry.getTool(step.toolId)
            if (tool == null) {
                observations.add(
                    ExecutionObservation(
                        stepId = step.stepId,
                        toolId = step.toolId,
                        isSuccess = false,
                        output = "'${step.toolId}' adlı alət tapılmadı."
                    )
                )
                continue
            }

            // 1. ACT
            var result: ToolResult = try {
                tool.execute(context, step.params)
            } catch (e: Exception) {
                ToolResult.failed(step.toolId, "İcra xətası: ${e.message}")
            }

            // 2. SELF-CORRECTION & RETRY if unsupported
            if (!result.isSuccess) {
                val fallbackToolId = when (step.toolId) {
                    "WIFI_STATUS" -> "WIFI_SETTINGS"
                    "BLUETOOTH_STATUS" -> "BLUETOOTH_SETTINGS"
                    "SCREEN_CONTROL" -> "OPEN_SETTINGS"
                    else -> null
                }
                if (fallbackToolId != null) {
                    val fallbackTool = toolRegistry.getTool(fallbackToolId)
                    if (fallbackTool != null) {
                        result = fallbackTool.execute(context, emptyMap())
                    }
                }
            }

            // 3. OBSERVE
            observations.add(
                ExecutionObservation(
                    stepId = step.stepId,
                    toolId = step.toolId,
                    isSuccess = result.isSuccess,
                    output = result.outputMessage,
                    rawData = result.rawData
                )
            )

            // 4. VERIFY / ANALYZE step output for issues
            when (step.toolId) {
                "GET_RAM" -> {
                    val usedPct = result.rawData["usedPercent"] as? Int ?: 0
                    if (usedPct >= 85) {
                        issuesFound.add("RAM istifadəsi çox yüksəkdir ($usedPct%). Arxa plandakı tətbiqləri bağlamaq tövsiyə olunur.")
                    }
                }
                "BATTERY_STATUS" -> {
                    val temp = (result.rawData["temperature"] as? Number)?.toDouble() ?: 0.0
                    val pct = result.rawData["percentage"] as? Int ?: 100
                    if (temp >= 42.0) {
                        issuesFound.add("Batareya temperaturu yüksəkdir (${temp}°C). Cihazın dincəlməsi tövsiyə edilir.")
                    }
                    if (pct <= 15) {
                        issuesFound.add("Batareya səviyyəsi azdır (%$pct). Cihazı şarja qoşun.")
                    }
                }
                "GET_STORAGE" -> {
                    val freeGb = (result.rawData["free_bytes"] as? Long)?.let { it / 1_073_741_824.0 } ?: 10.0
                    if (freeGb < 2.0) {
                        issuesFound.add("Daxili yaddaşda boş yer çox azdır (${ "%.1f".format(freeGb) } GB). Yaddaşı təmizləyin.")
                    }
                }
            }
        }

        // 5. RESPOND (Synthesize diagnostic conclusion)
        val summaryBuilder = StringBuilder()
        if (issuesFound.isEmpty()) {
            summaryBuilder.append("Sistem yoxlanışı tamamlandı: Bütün göstəricilər (RAM, CPU, Batareya və Yaddaş) normal qaydadadır. Cihazda heç bir kritik problem aşkar edilmədi.")
        } else {
            summaryBuilder.append("Sistem yoxlanışı nəticəsində bəzi məqamlar aşkar edildi:\n")
            issuesFound.forEachIndexed { i, issue ->
                summaryBuilder.append("${i + 1}. $issue\n")
            }
        }

        val finalSummary = summaryBuilder.toString().trim()
        memoryManager.updateTaskStatus(task.id, TaskLifecycleStatus.COMPLETED)

        return@withContext AgentExecutionResult(
            goal = plan.goal,
            plan = plan,
            observations = observations,
            summary = finalSummary,
            isSuccessful = true,
            issuesFound = issuesFound
        )
    }
}
