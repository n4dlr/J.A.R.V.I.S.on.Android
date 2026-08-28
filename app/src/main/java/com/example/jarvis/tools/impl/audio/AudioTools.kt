package com.example.jarvis.tools.impl.audio

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.view.KeyEvent
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool

private fun dispatchMediaKey(context: Context, keyCode: Int): Boolean {
    return try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val up   = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(down)
        am.dispatchMediaKeyEvent(up)
        true
    } catch (_: Exception) { false }
}

/** GET_VOLUME — read current media and ring volumes. */
class GetVolumeTool : Tool {
    override val id = "GET_VOLUME"
    override val name = "Səs Səviyyəsi"
    override val description = "Cari media, zəng və bildiriş səs səviyyələrini göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val am   = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val media = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxMedia = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val ring  = am.getStreamVolume(AudioManager.STREAM_RING)
            val maxRing = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            val mediaPct = (media * 100.0 / maxMedia).toInt()
            val ringPct  = (ring  * 100.0 / maxRing).toInt()

            ToolResult.success(id,
                "Media səsi: $mediaPct%, Zəng səsi: $ringPct%.",
                mapOf("mediaVolume" to mediaPct, "ringVolume" to ringPct)
            )
        } catch (e: Exception) {
            ToolResult.failed(id, "Səs səviyyəsi alına bilmədi: ${e.message}")
        }
    }
}

/** MUTE — mute all sound streams. */
class MuteTool : Tool {
    override val id = "MUTE"
    override val name = "Səsi Kəs"
    override val description = "Cihazın səsini tamamilə kəsir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
            ToolResult.success(id, "Cihaz susturuldu.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Səs kəsilə bilmədi: ${e.message}")
        }
    }
}

/** UNMUTE — restore audio to normal ringer mode. */
class UnmuteTool : Tool {
    override val id = "UNMUTE"
    override val name = "Səsi Aç"
    override val description = "Cihazın səsini bərpa edir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.MEDIUM

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            ToolResult.success(id, "Cihazın səsi bərpa edildi.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Səs bərpa edilə bilmədi: ${e.message}")
        }
    }
}

/** MEDIA_PLAY — send play key event to active media player. */
class MediaPlayTool : Tool {
    override val id = "MEDIA_PLAY"
    override val name = "Media Oynat"
    override val description = "Aktiv media oynatıcıya oxutma əmri göndərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val ok = dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        return if (ok) ToolResult.success(id, "Media oxutma başladıldı.")
               else   ToolResult.failed(id, "Media əmri göndərilə bilmədi.")
    }
}

/** MEDIA_PAUSE — pause active media player. */
class MediaPauseTool : Tool {
    override val id = "MEDIA_PAUSE"
    override val name = "Media Dayandır"
    override val description = "Aktiv media oynatıcını dayandırır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val ok = dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
        return if (ok) ToolResult.success(id, "Media dayandırıldı.")
               else   ToolResult.failed(id, "Media əmri göndərilə bilmədi.")
    }
}

/** MEDIA_NEXT — skip to next track. */
class MediaNextTool : Tool {
    override val id = "MEDIA_NEXT"
    override val name = "Növbəti Mahnı"
    override val description = "Növbəti musiqi parçasına keçir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val ok = dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        return if (ok) ToolResult.success(id, "Növbəti parçaya keçildi.")
               else   ToolResult.failed(id, "Media əmri göndərilə bilmədi.")
    }
}

/** MEDIA_PREVIOUS — go back to previous track. */
class MediaPreviousTool : Tool {
    override val id = "MEDIA_PREVIOUS"
    override val name = "Əvvəlki Mahnı"
    override val description = "Əvvəlki musiqi parçasına qayıdır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        val ok = dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return if (ok) ToolResult.success(id, "Əvvəlki parçaya keçildi.")
               else   ToolResult.failed(id, "Media əmri göndərilə bilmədi.")
    }
}
