package com.example.jarvis.agent

import com.example.jarvis.domain.model.ToolResult

data class PlanStep(
    val stepId: String,
    val toolId: String,
    val params: Map<String, String> = emptyMap(),
    val description: String = ""
)

data class ExecutionObservation(
    val stepId: String,
    val toolId: String,
    val isSuccess: Boolean,
    val output: String,
    val rawData: Map<String, Any?> = emptyMap()
)

data class AgentPlan(
    val goal: String,
    val steps: List<PlanStep>,
    val rationale: String
)

data class AgentExecutionResult(
    val goal: String,
    val plan: AgentPlan,
    val observations: List<ExecutionObservation>,
    val summary: String,
    val isSuccessful: Boolean,
    val issuesFound: List<String> = emptyList()
)
