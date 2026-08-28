package com.example.jarvis.tools

import com.example.jarvis.tools.impl.CreateReminderTool
import com.example.jarvis.tools.impl.GetBatteryTool
import com.example.jarvis.tools.impl.GetRamTool
import com.example.jarvis.tools.impl.GetStorageTool
import com.example.jarvis.tools.impl.LockScreenTool
import com.example.jarvis.tools.impl.OpenAppTool
import com.example.jarvis.tools.impl.OpenSettingsTool
import com.example.jarvis.tools.impl.ReadNotificationsTool
import com.example.jarvis.tools.impl.SetVolumeTool
import com.example.jarvis.tools.impl.TakePhotoTool
import com.example.jarvis.tools.impl.TorchTool
import java.util.concurrent.ConcurrentHashMap

class ToolRegistry {

    private val toolsMap = ConcurrentHashMap<String, Tool>()

    init {
        registerDefaultTools()
    }

    private fun registerDefaultTools() {
        register(GetBatteryTool())
        register(GetRamTool())
        register(GetStorageTool())
        register(SetVolumeTool())
        register(TorchTool())
        register(OpenSettingsTool())
        register(OpenAppTool())
        register(LockScreenTool())
        register(TakePhotoTool())
        register(CreateReminderTool())
        register(ReadNotificationsTool())
    }

    fun register(tool: Tool) {
        toolsMap[tool.id.uppercase()] = tool
    }

    fun unregister(toolId: String) {
        toolsMap.remove(toolId.uppercase())
    }

    fun getTool(toolId: String): Tool? {
        return toolsMap[toolId.uppercase()]
    }

    fun getAllTools(): List<Tool> {
        return toolsMap.values.toList()
    }

    fun hasTool(toolId: String): Boolean {
        return toolsMap.containsKey(toolId.uppercase())
    }
}
