package com.example.jarvis.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class AccessibilityController(
    private val context: Context
) {
    companion object {
        private const val TAG = "AccessibilityController"
    }

    fun isServiceEnabled(): Boolean = JarvisAccessibilityService.isEnabled(context)

    fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.get()

    fun getRootNode(): AccessibilityNodeInfo? = getService()?.rootInActiveWindow

    /**
     * Wait for a condition to become true within [timeoutMs].
     */
    suspend fun waitUntil(timeoutMs: Long = 3000L, intervalMs: Long = 200L, condition: suspend () -> Boolean): Boolean {
        val result = withTimeoutOrNull(timeoutMs) {
            while (!condition()) {
                delay(intervalMs)
            }
            true
        }
        return result ?: false
    }

    /**
     * Waits for the specified package to become the foreground active window.
     */
    suspend fun waitForAppForeground(packageName: String, timeoutMs: Long = 3000L): Boolean {
        return waitUntil(timeoutMs = timeoutMs) {
            val root = getRootNode()
            val currentPkg = root?.packageName?.toString() ?: ""
            currentPkg.equals(packageName, ignoreCase = true) || currentPkg.contains(packageName, ignoreCase = true)
        }
    }

    /**
     * Finds nodes matching text.
     */
    fun findByText(text: String, exact: Boolean = false): List<AccessibilityNodeInfo> {
        val root = getRootNode() ?: return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        val matches = root.findAccessibilityNodeInfosByText(text)
        if (exact) {
            for (node in matches) {
                if (node.text?.toString().equals(text, ignoreCase = true) ||
                    node.contentDescription?.toString().equals(text, ignoreCase = true)
                ) {
                    results.add(node)
                }
            }
        } else {
            results.addAll(matches)
        }
        return results
    }

    /**
     * Finds nodes matching content description.
     */
    fun findByContentDescription(desc: String): List<AccessibilityNodeInfo> {
        val root = getRootNode() ?: return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo) {
            if (node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) {
                results.add(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { search(it) }
            }
        }
        search(root)
        return results
    }

    /**
     * Finds nodes by view id resource name.
     */
    fun findByViewId(viewId: String): List<AccessibilityNodeInfo> {
        val root = getRootNode() ?: return emptyList()
        return root.findAccessibilityNodeInfosByViewId(viewId) ?: emptyList()
    }

    /**
     * Finds the first editable field in the active window.
     */
    fun findEditable(): AccessibilityNodeInfo? {
        val root = getRootNode() ?: return null
        return findFirstRecursive(root) { it.isEditable || it.className?.contains("EditText", ignoreCase = true) == true }
    }

    /**
     * Waits for a node satisfying [predicate].
     */
    suspend fun waitForNode(timeoutMs: Long = 3000L, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        var found: AccessibilityNodeInfo? = null
        withTimeoutOrNull(timeoutMs) {
            while (found == null) {
                val root = getRootNode()
                if (root != null) {
                    found = findFirstRecursive(root, predicate)
                }
                if (found != null) break
                delay(200)
            }
        }
        return found
    }

    private fun findFirstRecursive(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val matched = findFirstRecursive(child, predicate)
            if (matched != null) return matched
        }
        return null
    }

    /**
     * Clicks a node, climbing parent tree if the node itself is not marked clickable.
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = node
        while (cur != null) {
            if (cur.isClickable) {
                return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            cur = cur.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Types text into the currently focused or first editable field.
     */
    fun typeText(text: String): Boolean {
        val editable = findEditable() ?: return false
        editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /**
     * Scrolls in given direction.
     */
    fun scroll(direction: ScrollDirection): Boolean {
        val service = getService() ?: return false
        return service.scroll(direction)
    }

    /**
     * Performs global back action.
     */
    fun pressBack(): Boolean = getService()?.goBack() ?: false

    /**
     * Performs global home action.
     */
    fun pressHome(): Boolean = getService()?.goHome() ?: false

    /**
     * Reads all visible text in current window.
     */
    fun readVisibleText(): String {
        return getService()?.readVisibleText().orEmpty()
    }

    /**
     * Returns a debug string representing the current accessibility node hierarchy.
     */
    fun getAccessibilityTree(): String {
        val root = getRootNode() ?: return "<No active window>"
        val sb = StringBuilder()
        dumpTreeRecursive(root, sb, 0)
        return sb.toString()
    }

    private fun dumpTreeRecursive(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        sb.append(indent)
        sb.append("[${node.className}] ")
        node.viewIdResourceName?.let { sb.append("id=$it ") }
        node.text?.let { sb.append("text=\"$it\" ") }
        node.contentDescription?.let { sb.append("desc=\"$it\" ") }
        if (node.isClickable) sb.append("(clickable) ")
        if (node.isEditable) sb.append("(editable) ")
        sb.append("\n")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            dumpTreeRecursive(child, sb, depth + 1)
        }
    }
}
