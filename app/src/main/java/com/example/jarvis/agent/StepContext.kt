package com.example.jarvis.agent

/**
 * Holds shared context that flows between consecutive plan steps.
 * Each step can read from and write into this context, allowing
 * one step's output to become the next step's input.
 *
 * Example:
 *   Step 1 "OPEN_APP" writes: stepContext["last_opened_app"] = "instagram"
 *   Step 2 "ACC_SCROLL" reads: stepContext["last_opened_app"]
 */
data class StepContext(
    val data: MutableMap<String, Any?> = mutableMapOf()
) {
    fun put(key: String, value: Any?) { data[key] = value }
    fun get(key: String): Any? = data[key]
    fun getString(key: String, default: String = ""): String = data[key]?.toString() ?: default
    fun getInt(key: String, default: Int = 0): Int = (data[key] as? Number)?.toInt() ?: default
    fun contains(key: String): Boolean = data.containsKey(key)
    fun asParams(): Map<String, String> = data.mapValues { it.value?.toString() ?: "" }
}
