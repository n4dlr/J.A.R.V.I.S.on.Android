package com.example.jarvis.automation.observation

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.example.jarvis.services.AccessibilityController

data class DeviceUiObservation(
    val foregroundPackage: String?,
    val activeWindowTitle: String?,
    val visibleText: String,
    val isMediaPlaying: Boolean,
    val mediaActiveTitle: String?,
    val accessibilityEnabled: Boolean
)

class ObservationEngine(
    private val context: Context,
    private val accessibilityController: AccessibilityController
) {

    fun observeCurrentState(): DeviceUiObservation {
        val root = accessibilityController.getRootNode()
        val fgPkg = root?.packageName?.toString()
        val visible = accessibilityController.readVisibleText()
        val isAcc = accessibilityController.isServiceEnabled()

        var isPlaying = false
        var mediaTitle: String? = null

        try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val controllers = sessionManager?.getActiveSessions(null) ?: emptyList()
            for (ctrl in controllers) {
                val state = ctrl.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                    isPlaying = true
                    mediaTitle = ctrl.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                    break
                }
            }
        } catch (_: SecurityException) {
            // NotificationListener or special access not granted for getActiveSessions
        } catch (_: Throwable) {}

        return DeviceUiObservation(
            foregroundPackage = fgPkg,
            activeWindowTitle = root?.className?.toString(),
            visibleText = visible,
            isMediaPlaying = isPlaying,
            mediaActiveTitle = mediaTitle,
            accessibilityEnabled = isAcc
        )
    }

    fun isAppInForeground(expectedPackage: String): Boolean {
        val fg = observeCurrentState().foregroundPackage ?: ""
        return fg.equals(expectedPackage, ignoreCase = true) || fg.contains(expectedPackage, ignoreCase = true)
    }
}
