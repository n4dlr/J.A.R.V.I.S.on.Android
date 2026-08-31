package com.example.jarvis.tools.impl.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import java.net.URLEncoder

object ContactsResolver {
    fun findPhoneNumberByName(context: Context, nameQuery: String): String? {
        val cleanName = nameQuery.trim().lowercase()
        if (cleanName.isBlank()) return null

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                var bestMatchNumber: String? = null
                while (it.moveToNext()) {
                    val displayName = it.getString(nameIdx) ?: continue
                    val phone = it.getString(numIdx) ?: continue

                    if (displayName.lowercase() == cleanName) {
                        return cleanPhoneNumber(phone)
                    }
                    if (displayName.lowercase().contains(cleanName) && bestMatchNumber == null) {
                        bestMatchNumber = cleanPhoneNumber(phone)
                    }
                }
                return bestMatchNumber
            }
        } catch (e: Exception) {
            Log.w("ContactsResolver", "Error searching contacts: ${e.message}")
        }
        return null
    }

    private fun cleanPhoneNumber(phone: String): String {
        return phone.replace("[^0-9+]".toRegex(), "")
    }
}

class SendWhatsAppMessageTool : Tool {

    companion object {
        private const val TAG = "SendWhatsAppMessageTool"
    }

    override val id: String = "SEND_WHATSAPP_MESSAGE"
    override val name: String = "WhatsApp Mesaj Göndər"
    override val description: String = "WhatsApp vasitəsilə kontakt və ya nömrəyə mesaj hazırlayır və göndərir"
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(name = "recipient", type = "string", isRequired = true, description = "Alıcının adı və ya telefon nömrəsi"),
        ToolParameter(name = "message", type = "string", isRequired = true, description = "Göndəriləcək mesajın mətni")
    )
    override val requiredPermissions: List<String> = listOf("android.permission.READ_CONTACTS")
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val recipient = params["recipient"]?.trim() ?: ""
        val message = params["message"]?.trim() ?: ""

        if (recipient.isBlank() || message.isBlank()) {
            return ToolResult.failed(id, "Alıcı və mesaj mətni mütləq daxil edilməlidir.")
        }

        return try {
            val directPhone = if (recipient.startsWith("+") || recipient.matches(Regex("""\d{7,15}"""))) {
                recipient.replace("[^0-9+]".toRegex(), "")
            } else {
                ContactsResolver.findPhoneNumberByName(context, recipient)
            }

            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = if (!directPhone.isNullOrBlank()) {
                val cleanDigits = directPhone.removePrefix("+")
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanDigits&text=$encodedMsg")
            } else {
                Uri.parse("whatsapp://send?text=$encodedMsg")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                context.startActivity(intent)
                ToolResult.success(
                    toolId = id,
                    message = "WhatsApp mesajı '$recipient' üçün hazırlandı: \"$message\"",
                    data = mapOf("recipient" to recipient, "message" to message)
                )
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Mesajı paylaş").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                ToolResult.success(
                    toolId = id,
                    message = "WhatsApp quraşdırılmadığı üçün paylaşma pəncərəsi açıldı: \"$message\""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WhatsApp message: ${e.message}", e)
            ToolResult.failed(id, "WhatsApp mesajı göndərilə bilmədi: ${e.message}")
        }
    }
}

class SendTelegramMessageTool : Tool {

    companion object {
        private const val TAG = "SendTelegramMessageTool"
    }

    override val id: String = "SEND_TELEGRAM_MESSAGE"
    override val name: String = "Telegram Mesaj Göndər"
    override val description: String = "Telegram vasitəsilə mesaj göndərir"
    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(name = "message", type = "string", isRequired = true, description = "Göndəriləcək mesajın mətni"),
        ToolParameter(name = "recipient", type = "string", isRequired = false, description = "Alıcının istifadəçi adı və ya nömrəsi")
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>): Boolean = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val message = params["message"]?.trim() ?: ""
        val recipient = params["recipient"]?.trim() ?: ""

        if (message.isBlank()) {
            return ToolResult.failed(id, "Mesaj mətni boş ola bilməz.")
        }

        return try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = if (recipient.isNotBlank() && recipient.startsWith("@")) {
                Uri.parse("https://t.me/${recipient.removePrefix("@")}?text=$encodedMsg")
            } else {
                Uri.parse("tg://msg?text=$encodedMsg")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                context.startActivity(intent)
                ToolResult.success(
                    toolId = id,
                    message = "Telegram mesajı hazırlandı: \"$message\"",
                    data = mapOf("message" to message)
                )
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=&text=$encodedMsg")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                ToolResult.success(
                    toolId = id,
                    message = "Telegram veb pəncərəsində mesaj hazırlandı: \"$message\""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Telegram message: ${e.message}", e)
            ToolResult.failed(id, "Telegram mesajı göndərilə bilmədi: ${e.message}")
        }
    }
}
