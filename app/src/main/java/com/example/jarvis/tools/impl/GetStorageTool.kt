package com.example.jarvis.tools.impl

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import java.io.File

class GetStorageTool : Tool {
    override val id: String = "GET_STORAGE"
    override val name: String = "Yaddaş Monitoru"
    override val description: String = "Daxili yaddaşın tutumunu və boş sahəsini göstərir."
    override val parameters: List<ToolParameter> = emptyList()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0))
            val freeGb = String.format(java.util.Locale.US, "%.1f", freeBytes / (1024.0 * 1024.0 * 1024.0))
            val usedGb = String.format(java.util.Locale.US, "%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0))
            val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            val msg = "Daxili Yaddaş: $freeGb GB boşdur ($usedGb GB / $totalGb GB istifadə olunur, %$usedPercent)."

            ToolResult.success(
                toolId = id,
                message = msg,
                data = mapOf(
                    "totalGb" to totalGb,
                    "freeGb" to freeGb,
                    "usedGb" to usedGb,
                    "usedPercent" to usedPercent
                )
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Yaddaş məlumatı hesablana bilmədi: ${e.message}")
        }
    }
}
