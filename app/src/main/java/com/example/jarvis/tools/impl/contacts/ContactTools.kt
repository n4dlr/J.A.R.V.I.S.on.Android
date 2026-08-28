package com.example.jarvis.tools.impl.contacts

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SEARCH_CONTACT — search contacts by name or phone number. */
class SearchContactTool : Tool {
    override val id = "SEARCH_CONTACT"
    override val name = "Kontakt Axtar"
    override val description = "Ad və ya telefon nömrəsinə görə kontakt axtarışı edir."
    override val parameters = listOf(
        ToolParameter("query", "string", true, "Ad və ya nömrə")
    )
    override val requiredPermissions = listOf(Manifest.permission.READ_CONTACTS)
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val query = params["query"] ?: return@withContext ToolResult.failed(id, "Axtarış sorğusu göstərilməyib.")
        return@withContext try {
            val results = mutableListOf<Pair<String, String>>() // name to phone
            val uri = Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                Uri.encode(query)
            )
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext() && results.size < 5) {
                    val name = cursor.getString(0) ?: continue
                    val num  = cursor.getString(1) ?: continue
                    results.add(name to num)
                }
            }
            if (results.isEmpty()) {
                ToolResult.success(id, "'$query' üçün kontakt tapılmadı.")
            } else {
                val lines = results.joinToString("\n") { "${it.first}: ${it.second}" }
                ToolResult.success(id, "${results.size} kontakt tapıldı:\n$lines",
                    mapOf("contacts" to results.map { mapOf("name" to it.first, "phone" to it.second) }))
            }
        } catch (e: SecurityException) {
            ToolResult.permissionRequired(id, listOf(Manifest.permission.READ_CONTACTS),
                "Kontakt axtarışı üçün Kontakt oxuma icazəsi tələb olunur.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Kontakt axtarışı uğursuz oldu: ${e.message}")
        }
    }
}

/** OPEN_CONTACTS — open the contacts app. */
class OpenContactsTool : Tool {
    override val id = "OPEN_CONTACTS"
    override val name = "Kontaktları Aç"
    override val description = "Cihazın kontaktlar tətbiqini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            ToolResult.success(id, "Kontaktlar açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Kontaktlar açıla bilmədi: ${e.message}")
        }
    }
}

/** CREATE_CONTACT — create a new contact. */
class CreateContactTool : Tool {
    override val id = "CREATE_CONTACT"
    override val name = "Kontakt Yarat"
    override val description = "Yeni kontakt yaradır."
    override val parameters = listOf(
        ToolParameter("name", "string", true, "Ad Soyad"),
        ToolParameter("phone", "string", true, "Telefon nömrəsi")
    )
    override val requiredPermissions = listOf(Manifest.permission.WRITE_CONTACTS)
    override val riskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val name  = params["name"]  ?: return ToolResult.failed(id, "Ad göstərilməyib.")
        val phone = params["phone"] ?: return ToolResult.failed(id, "Telefon nömrəsi göstərilməyib.")

        // Open contact creator UI — safer than writing directly
        return try {
            val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
                putExtra(ContactsContract.Intents.Insert.NAME, name)
                putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult.success(id, "'$name' kontaktu yaratmaq üçün forma açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Kontakt forması açıla bilmədi: ${e.message}")
        }
    }
}
