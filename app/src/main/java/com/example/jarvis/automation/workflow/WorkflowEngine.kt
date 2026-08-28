package com.example.jarvis.automation.workflow

import android.content.Context
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.voice.TextToSpeechHelper
import java.util.concurrent.ConcurrentHashMap

class WorkflowEngine(
    private val toolRegistry: ToolRegistry,
    private val memoryManager: MemoryManager,
    private val ttsHelper: TextToSpeechHelper? = null
) {

    private val workflows = ConcurrentHashMap<String, Workflow>()

    fun registerWorkflow(workflow: Workflow) {
        workflows[workflow.id] = workflow
    }

    fun unregisterWorkflow(workflowId: String) {
        workflows.remove(workflowId)
    }

    fun getAllWorkflows(): List<Workflow> = workflows.values.toList()

    /**
     * Executes a workflow if its condition evaluates to true.
     */
    suspend fun executeWorkflow(context: Context, workflowId: String): ToolResult? {
        val workflow = workflows[workflowId] ?: return null
        if (!workflow.isEnabled) return null

        // 1. Evaluate condition
        val conditionMet = when (val cond = workflow.condition) {
            is WorkflowCondition.AlwaysTrue -> true
            is WorkflowCondition.BatteryAbove -> {
                val batteryTool = toolRegistry.getTool("BATTERY_STATUS")
                val res = batteryTool?.execute(context, emptyMap())
                val pct = res?.rawData?.get("percentage") as? Int ?: 100
                pct >= cond.minPercentage
            }
            is WorkflowCondition.WifiConnected -> {
                val wifiTool = toolRegistry.getTool("WIFI_STATUS")
                val res = wifiTool?.execute(context, emptyMap())
                val connected = res?.rawData?.get("connectedToWifi") as? Boolean ?: false
                connected == cond.isRequired
            }
        }

        if (!conditionMet) {
            return ToolResult.failed(workflow.id, "İş axını şərti ödənmədi.")
        }

        // 2. Execute Action
        return when (val act = workflow.action) {
            is WorkflowAction.ExecuteTool -> {
                val tool = toolRegistry.getTool(act.toolId)
                val res = tool?.execute(context, act.params) ?: ToolResult.failed(act.toolId, "Alət tapılmadı.")
                ttsHelper?.speak(res.outputMessage)
                res
            }
            is WorkflowAction.NotifyUser -> {
                ToolResult.success(workflow.id, "${act.title}: ${act.message}")
            }
            is WorkflowAction.SpeakResponse -> {
                ttsHelper?.speak(act.speechText)
                ToolResult.success(workflow.id, act.speechText)
            }
        }
    }
}
