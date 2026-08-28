package com.example

import com.example.jarvis.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    @Test
    fun `tool registry registers all default tools correctly`() {
        val registry = ToolRegistry()
        val allTools = registry.getAllTools()

        assertTrue("Expected at least 10 tools, found ${allTools.size}", allTools.size >= 10)

        assertNotNull(registry.getTool("GET_BATTERY"))
        assertNotNull(registry.getTool("GET_RAM"))
        assertNotNull(registry.getTool("GET_STORAGE"))
        assertNotNull(registry.getTool("SET_VOLUME"))
        assertNotNull(registry.getTool("TORCH"))
        assertNotNull(registry.getTool("OPEN_SETTINGS"))
        assertNotNull(registry.getTool("OPEN_APP"))
        assertNotNull(registry.getTool("LOCK_SCREEN"))
        assertNotNull(registry.getTool("TAKE_PHOTO"))
        assertNotNull(registry.getTool("CREATE_REMINDER"))
        assertNotNull(registry.getTool("READ_NOTIFICATIONS"))
    }

    @Test
    fun `tool registry lookup is case-insensitive`() {
        val registry = ToolRegistry()
        assertNotNull(registry.getTool("get_battery"))
        assertNotNull(registry.getTool("Torch"))
        assertNotNull(registry.getTool("LOCK_SCREEN"))
    }
}
