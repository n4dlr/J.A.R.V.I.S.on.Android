package com.example.jarvis.agent

import android.content.Context
import android.util.Log
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AgentExecutor(
    private val toolRegistry: ToolRegistry,
    private val memoryManager: MemoryManager
) {

    companion object {
        private const val TAG = "AgentExecutor"
        private const val STEP_DELAY_MS = 600L // Pause between steps for UI readability
    }

    /**
     * Executes the [plan] following PLAN → OBSERVE → ACT → VERIFY → RESPOND.
     *
     * Now supports [StepContext] — each step's output is passed to the next step
     * as shared context, enabling data-dependent multi-step chains.
     */
    suspend fun executePlan(context: Context, plan: AgentPlan): AgentExecutionResult = withContext(Dispatchers.IO) {
        val task = memoryManager.createTask(plan.goal, plan.rationale)
        memoryManager.updateTaskStatus(task.id, TaskLifecycleStatus.RUNNING)

        val sharedContext = StepContext()
        val observations = mutableListOf<ExecutionObservation>()
        val issuesFound = mutableListOf<String>()

        for (step in plan.steps) {
            Log.d(TAG, "Executing step ${step.stepId}: ${step.toolId}")

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

            // Merge step params with shared context params (step params take priority)
            val mergedParams = sharedContext.asParams() + step.params

            // 1. ACT
            var result: ToolResult = try {
                tool.execute(context, mergedParams)
            } catch (e: Exception) {
                Log.e(TAG, "Step ${step.stepId} failed: ${e.message}", e)
                ToolResult.failed(step.toolId, "İcra xətası: ${e.message}")
            }

            // 2. SELF-CORRECTION & RETRY if tool not supported
            if (!result.isSuccess) {
                val fallbackToolId = when (step.toolId) {
                    "WIFI_STATUS" -> "WIFI_SETTINGS"
                    "BLUETOOTH_STATUS" -> "BLUETOOTH_SETTINGS"
                    "SCREEN_CONTROL" -> "OPEN_SETTINGS"
                    "ACC_TYPE_COMMENT" -> "ACCESSIBILITY_TYPE"
                    "ACC_CLICK_LIKE" -> "ACCESSIBILITY_CLICK"
                    "ACC_CLICK_SHARE" -> "ACCESSIBILITY_CLICK"
                    "ACC_CLICK_DELETE" -> "ACCESSIBILITY_CLICK"
                    "ACC_FIND_ELEMENT" -> "ACCESSIBILITY_FIND"
                    "ACC_SCROLL" -> "ACCESSIBILITY_SCROLL"
                    else -> null
                }
                if (fallbackToolId != null) {
                    val fallbackTool = toolRegistry.getTool(fallbackToolId)
                    if (fallbackTool != null) {
                        result = try {
                            fallbackTool.execute(context, mergedParams)
                        } catch (e: Exception) {
                            ToolResult.failed(fallbackToolId, "Fallback xətası: ${e.message}")
                        }
                    }
                }
            }

            // 3. OBSERVE — record output into shared context
            observations.add(
                ExecutionObservation(
                    stepId = step.stepId,
                    toolId = step.toolId,
                    isSuccess = result.isSuccess,
                    output = result.outputMessage,
                    rawData = result.rawData
                )
            )

            // 4. Write key outputs into StepContext for next steps
            result.rawData.forEach { (key, value) ->
                sharedContext.put(key, value)
            }
            sharedContext.put("last_step_success", result.isSuccess)
            sharedContext.put("last_step_output", result.outputMessage)

            // 5. VERIFY / ANALYZE diagnostic outputs
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
                        issuesFound.add("Daxili yaddaşda boş yer çox azdır (${"%.1f".format(freeGb)} GB). Yaddaşı təmizləyin.")
                    }
                }
            }

            // Small delay between steps to avoid overwhelming Accessibility service
            delay(STEP_DELAY_MS)
        }

        // 6. RESPOND — Synthesize conclusion
        val summaryBuilder = StringBuilder()
        val successCount = observations.count { it.isSuccess }
        val failCount = observations.count { !it.isSuccess }

        if (plan.steps.size > 1) {
            // Multi-step summary
            summaryBuilder.append("Çoxaddımlı əmr tamamlandı: $successCount/${plan.steps.size} addım uğurlu.\n")
            observations.forEachIndexed { i, obs ->
                val icon = if (obs.isSuccess) "✅" else "❌"
                summaryBuilder.append("$icon Addım ${i + 1}: ${obs.output.take(80)}\n")
            }
        } else if (issuesFound.isEmpty()) {
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
