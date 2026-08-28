package com.example.jarvis.tools.impl.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

/** STORAGE_INFO — detailed storage breakdown. */
class StorageInfoTool : Tool {
    override val id = "STORAGE_INFO"
    override val name = "Yaddaş Məlumatı"
    override val description = "Daxili yaddaşın ümumi, istifadə olunan və boş həcmini göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val total = stat.blockCountLong * blockSize
            val free  = stat.availableBlocksLong * blockSize

            fun Long.toGb() = "%.1f GB".format(this / 1_073_741_824.0)
            val msg = "Daxili yaddaş: ${total.toGb()} (boş: ${free.toGb()}, istifadə: ${(total - free).toGb()})."
            ToolResult.success(id, msg, mapOf("total_bytes" to total, "free_bytes" to free))
        } catch (e: Exception) {
            ToolResult.failed(id, "Yaddaş məlumatı alına bilmədi: ${e.message}")
        }
    }
}

/** SEARCH_FILES — search MediaStore for files by name/type. */
class SearchFilesTool : Tool {
    override val id = "SEARCH_FILES"
    override val name = "Fayl Axtarışı"
    override val description = "MediaStore vasitəsilə fayl axtarışı edir."
    override val parameters = listOf(
        ToolParameter("query", "string", true, "Fayl adı və ya açar söz"),
        ToolParameter("type", "string", false, "image | video | audio | document | all", "all")
    )
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        else
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val query = params["query"] ?: return@withContext ToolResult.failed(id, "Axtarış sorğusu göstərilməyib.")
        val type  = params["type"]?.lowercase() ?: "all"

        val uri = when (type) {
            "image"    -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video"    -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio"    -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else       -> MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selArgs = arrayOf("%$query%")

        return@withContext try {
            val results = mutableListOf<String>()
            context.contentResolver.query(uri, projection, selection, selArgs, null)?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (cursor.moveToNext() && results.size < 20) {
                    results.add(cursor.getString(nameCol))
                }
            }
            if (results.isEmpty()) {
                ToolResult.success(id, "'$query' üçün heç bir fayl tapılmadı.")
            } else {
                ToolResult.success(id,
                    "'$query' axtarışı — ${results.size} fayl tapıldı: ${results.joinToString(", ")}.",
                    mapOf("files" to results)
                )
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Fayl axtarışı uğursuz oldu: ${e.message}")
        }
    }
}

/** OPEN_FILE — open a file with the appropriate viewer app. */
class OpenFileTool : Tool {
    override val id = "OPEN_FILE"
    override val name = "Fayl Aç"
    override val description = "Seçilmiş faylı uyğun tətbiqlə açır."
    override val parameters = listOf(
        ToolParameter("path", "string", true, "Faylın tam yolu və ya adı")
    )
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val path = params["path"] ?: return ToolResult.failed(id, "Fayl yolu göstərilməyib.")
        return try {
            val file = File(path)
            if (!file.exists()) return ToolResult.failed(id, "'$path' faylı mövcud deyil.")

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX,
                file
            )
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult.success(id, "'${file.name}' faylı açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Fayl açıla bilmədi: ${e.message}")
        }
    }
}

/** SHARE_FILE — share a file via Android share sheet. */
class ShareFileTool : Tool {
    override val id = "SHARE_FILE"
    override val name = "Fayl Paylaş"
    override val description = "Seçilmiş faylı paylaşım panosu ilə paylaşır."
    override val parameters = listOf(ToolParameter("path", "string", true, "Faylın tam yolu"))
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val path = params["path"] ?: return ToolResult.failed(id, "Fayl yolu göstərilməyib.")
        return try {
            val file = File(path)
            if (!file.exists()) return ToolResult.failed(id, "'$path' faylı mövcud deyil.")
            val uri = FileProvider.getUriForFile(
                context, context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX, file)
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            val intent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mime
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Paylaş"
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
            ToolResult.success(id, "'${file.name}' paylaşıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Fayl paylaşıla bilmədi: ${e.message}")
        }
    }
}

/** DELETE_FILE — delete a file (MEDIUM risk, requires confirmation from pipeline). */
class DeleteFileTool : Tool {
    override val id = "DELETE_FILE"
    override val name = "Faylı Sil"
    override val description = "Göstərilən faylı silir."
    override val parameters = listOf(ToolParameter("path", "string", true, "Faylın tam yolu"))
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) else emptyList()
    override val riskLevel = RiskLevel.HIGH

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val path = params["path"] ?: return@withContext ToolResult.failed(id, "Fayl yolu göstərilməyib.")
        val file = File(path)
        if (!file.exists()) return@withContext ToolResult.failed(id, "'$path' faylı mövcud deyil.")
        return@withContext if (file.delete()) {
            ToolResult.success(id, "'${file.name}' silindi.")
        } else {
            ToolResult.failed(id, "'${file.name}' silinə bilmədi. Fayl qorunmuş ola bilər.")
        }
    }
}

/** COPY_FILE — copy a file to a target directory. */
class CopyFileTool : Tool {
    override val id = "COPY_FILE"
    override val name = "Faylı Kopyala"
    override val description = "Seçilmiş faylı başqa qovluğa kopyalayır."
    override val parameters = listOf(
        ToolParameter("source", "string", true, "Mənbə faylın tam yolu"),
        ToolParameter("destination", "string", true, "Hədəf qovluğun tam yolu")
    )
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) else emptyList()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val src  = params["source"] ?: return@withContext ToolResult.failed(id, "Mənbə fayl göstərilməyib.")
        val dest = params["destination"] ?: return@withContext ToolResult.failed(id, "Hədəf qovluq göstərilməyib.")
        return@withContext try {
            val srcFile  = File(src).also { if (!it.exists()) return@withContext ToolResult.failed(id, "'$src' mövcud deyil.") }
            val destDir  = File(dest).also { it.mkdirs() }
            val destFile = File(destDir, srcFile.name)
            srcFile.copyTo(destFile, overwrite = true)
            ToolResult.success(id, "'${srcFile.name}' '$dest' qovluğuna kopyalandı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Kopyalama uğursuz oldu: ${e.message}")
        }
    }
}

/** MOVE_FILE — move/rename a file. */
class MoveFileTool : Tool {
    override val id = "MOVE_FILE"
    override val name = "Faylı Köçür"
    override val description = "Seçilmiş faylı başqa yerə köçürür."
    override val parameters = listOf(
        ToolParameter("source", "string", true, "Mənbə faylın tam yolu"),
        ToolParameter("destination", "string", true, "Hədəf qovluğun tam yolu")
    )
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) else emptyList()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val src  = params["source"] ?: return@withContext ToolResult.failed(id, "Mənbə fayl göstərilməyib.")
        val dest = params["destination"] ?: return@withContext ToolResult.failed(id, "Hədəf qovluq göstərilməyib.")
        return@withContext try {
            val srcFile  = File(src).also { if (!it.exists()) return@withContext ToolResult.failed(id, "'$src' mövcud deyil.") }
            val destDir  = File(dest).also { it.mkdirs() }
            val destFile = File(destDir, srcFile.name)
            srcFile.copyTo(destFile, overwrite = true)
            srcFile.delete()
            ToolResult.success(id, "'${srcFile.name}' '$dest' qovluğuna köçürüldü.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Köçürmə uğursuz oldu: ${e.message}")
        }
    }
}

/** RENAME_FILE — rename a file. */
class RenameFileTool : Tool {
    override val id = "RENAME_FILE"
    override val name = "Faylı Yenidən Adlandır"
    override val description = "Seçilmiş faylın adını dəyişir."
    override val parameters = listOf(
        ToolParameter("path", "string", true, "Faylın tam yolu"),
        ToolParameter("new_name", "string", true, "Yeni ad (uzantısız və ya uzantılı)")
    )
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) else emptyList()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val path    = params["path"] ?: return@withContext ToolResult.failed(id, "Fayl yolu göstərilməyib.")
        val newName = params["new_name"] ?: return@withContext ToolResult.failed(id, "Yeni ad göstərilməyib.")
        return@withContext try {
            val file   = File(path).also { if (!it.exists()) return@withContext ToolResult.failed(id, "'$path' mövcud deyil.") }
            val target = File(file.parent, newName)
            if (file.renameTo(target)) {
                ToolResult.success(id, "'${file.name}' → '$newName' olaraq adlandırıldı.")
            } else {
                ToolResult.failed(id, "Adlandırma uğursuz oldu.")
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "Adlandırma xətası: ${e.message}")
        }
    }
}

/** CREATE_FOLDER — create a new directory. */
class CreateFolderTool : Tool {
    override val id = "CREATE_FOLDER"
    override val name = "Qovluq Yarat"
    override val description = "Göstərilən yolda yeni qovluq yaradır."
    override val parameters = listOf(
        ToolParameter("path", "string", true, "Yaradılacaq qovluğun tam yolu")
    )
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) else emptyList()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val path = params["path"] ?: return@withContext ToolResult.failed(id, "Qovluq yolu göstərilməyib.")
        val dir  = File(path)
        return@withContext if (dir.exists()) {
            ToolResult.success(id, "'$path' qovluğu artıq mövcuddur.")
        } else if (dir.mkdirs()) {
            ToolResult.success(id, "'$path' qovluğu yaradıldı.")
        } else {
            ToolResult.failed(id, "'$path' qovluğu yaradıla bilmədi.")
        }
    }
}
