package com.example.jarvis.tools.impl

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

class LockScreenTool : Tool {
    override val id: String = "LOCK_SCREEN"
    override val name: String = "Ekranı Kilidlə"
    override val description: String = "Cihazın ekranını təhlükəsiz şəkildə kilidləyir (API 28+ standart idarəetmə)."
    override val parameters: List<ToolParameter> = emptyList()
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return try {
            dpm.lockNow()
            ToolResult.success(id, "Ekran uğurla kilidləndi.")
        } catch (e: SecurityException) {
            // Non-root safe handling when Device Admin is not yet registered
            ToolResult.permissionRequired(
                toolId = id,
                permissions = listOf("android.app.admin.DeviceAdminReceiver / Accessibility"),
                message = "Ekranı birbaşa kilidləmək üçün 'Cihaz İnzibatçısı' və ya 'Əlçatanlıq Xidməti' icazəsi tələb olunur."
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Ekran kilidlənərkən xəta: ${e.message}")
        }
    }
}
