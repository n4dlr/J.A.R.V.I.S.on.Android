package com.example.jarvis.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * JARVIS Accessibility Service.
 *
 * Provides system-level UI interaction without root:
 * - Global actions: home, back, recents, notifications, quick settings
 * - Node interaction: click, scroll, read visible text
 * - Available only when the user has explicitly enabled it in Settings → Accessibility.
 *
 * All tool consumers MUST call [isEnabled] before using any accessor.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.also { info ->
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* passive — not used */ }

    override fun onInterrupt() { /* required override */ }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ─── Global Actions ─────────────────────────────────────────────────────

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    // ─── Node Interaction ────────────────────────────────────────────────────

    /** Read all visible text from the current window. */
    fun readVisibleText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        collectText(root, sb)
        root.recycle()
        return sb.toString().trim()
    }

    /** Find the first node with the given [viewId] and click it. */
    fun clickById(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        val clicked = nodes.firstOrNull { it.isClickable }?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        root.recycle()
        return clicked
    }

    /** Find first clickable node whose text matches [text] (case-insensitive). */
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        val clicked = nodes.firstOrNull { it.isClickable }?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        root.recycle()
        return clicked
    }

    /** Scroll the first scrollable view in the active window. */
    fun scroll(direction: ScrollDirection): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (direction == ScrollDirection.DOWN)
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        else
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val result = findFirstScrollable(root)?.performAction(action) ?: false
        root.recycle()
        return result
    }

    // ─── Private Helpers ────────────────────────────────────────────────────

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.takeIf { it.isNotBlank() }?.let { sb.appendLine(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectText(child, sb)
                child.recycle()
            }
        }
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstScrollable(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    // ─── Companion ──────────────────────────────────────────────────────────

    companion object {
        @Volatile
        private var instance: JarvisAccessibilityService? = null

        fun get(): JarvisAccessibilityService? = instance

        /** Returns true if the accessibility service is enabled in system settings. */
        fun isEnabled(context: Context): Boolean {
            val expectedComponent = ComponentName(context, JarvisAccessibilityService::class.java)
                .flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
        }
    }
}

enum class ScrollDirection { UP, DOWN }
