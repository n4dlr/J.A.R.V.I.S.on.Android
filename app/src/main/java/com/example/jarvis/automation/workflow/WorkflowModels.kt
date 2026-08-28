package com.example.jarvis.automation.workflow

sealed class WorkflowTrigger {
    data class TimeTrigger(val hour: Int, val minute: Int, val isDaily: Boolean = true) : WorkflowTrigger()
    data class BatteryTrigger(val threshold: Int, val whenCharging: Boolean? = null) : WorkflowTrigger()
    data class NetworkTrigger(val requireWifi: Boolean) : WorkflowTrigger()
    data class ManualTrigger(val label: String) : WorkflowTrigger()
}

sealed class WorkflowCondition {
    object AlwaysTrue : WorkflowCondition()
    data class BatteryAbove(val minPercentage: Int) : WorkflowCondition()
    data class WifiConnected(val isRequired: Boolean) : WorkflowCondition()
}

sealed class WorkflowAction {
    data class ExecuteTool(val toolId: String, val params: Map<String, String> = emptyMap()) : WorkflowAction()
    data class NotifyUser(val title: String, val message: String) : WorkflowAction()
    data class SpeakResponse(val speechText: String) : WorkflowAction()
}

data class Workflow(
    val id: String,
    val name: String,
    val trigger: WorkflowTrigger,
    val condition: WorkflowCondition = WorkflowCondition.AlwaysTrue,
    val action: WorkflowAction,
    val isEnabled: Boolean = true,
    val lastRunTimestamp: Long? = null
)
