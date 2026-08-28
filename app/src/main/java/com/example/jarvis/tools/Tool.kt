package com.example.jarvis.tools

import android.content.Context
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult

interface Tool {
    val id: String
    val name: String
    val description: String
    val parameters: List<ToolParameter>
    val requiredPermissions: List<String>
    val riskLevel: RiskLevel

    suspend fun canExecute(context: Context, params: Map<String, String>): Boolean
    suspend fun execute(context: Context, params: Map<String, String>): ToolResult
}
