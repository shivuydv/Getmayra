package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Maya Accessibility Service.
 * "Maya uses this to control apps for you — like sending WhatsApp messages,
 * making WhatsApp calls, playing YouTube videos and reading your screen when you ask her to.
 * Maya only acts on your voice commands."
 */
class MayaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MayaAccessibility"

        @Volatile
        var instance: MayaAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun isServiceEnabled(context: Context): Boolean {
            val expectedService = "${context.packageName}/${MayaAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(":").any {
                it.equals(expectedService, ignoreCase = true) ||
                        it.contains(MayaAccessibilityService::class.java.simpleName, ignoreCase = true)
            }
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private var currentPackageName: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "MayaAccessibilityService Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            currentPackageName = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "MayaAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Reads all text currently visible on the screen.
     * "reading your screen when you ask her to."
     */
    fun readCurrentScreen(): String {
        val root = rootInActiveWindow ?: return "स्क्रीन से डेटा प्राप्त नहीं हो सका या स्क्रीन खाली है।"
        val textList = mutableListOf<String>()

        fun extractNodes(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()

            if (!text.isNullOrBlank() && text !in textList) {
                textList.add(text)
            } else if (!desc.isNullOrBlank() && desc !in textList) {
                textList.add(desc)
            }

            for (i in 0 until node.childCount) {
                extractNodes(node.getChild(i))
            }
        }

        extractNodes(root)

        return if (textList.isEmpty()) {
            "स्क्रीन पर कोई टेक्स्ट दिखाई नहीं दे रहा है।"
        } else {
            textList.take(25).joinToString(separator = "\n• ")
        }
    }

    /**
     * Finds WhatsApp input field, sets text, and taps the Send button.
     * "sending WhatsApp messages... on your voice commands"
     */
    fun sendWhatsAppMessage(messageText: String): Boolean {
        val root = rootInActiveWindow ?: return false

        // 1. Find editable text field
        val inputNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        val targetInput = if (inputNodes.isNotEmpty()) {
            inputNodes[0]
        } else {
            // Fallback: search any editable node
            findFirstEditableNode(root)
        }

        if (targetInput != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, messageText)
            }
            targetInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        // 2. Find send button
        val sendNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendNodes.isNotEmpty()) {
            return sendNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Fallback: find node with description "Send" or "भेजें"
        val byDesc = root.findAccessibilityNodeInfosByText("Send")
        if (byDesc.isNotEmpty()) {
            for (node in byDesc) {
                if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findFirstEditableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    /**
     * Clicks any visible element by text
     */
    fun clickText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in nodes) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) {
                    return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                current = current.parent
            }
        }
        return false
    }

    /**
     * Navigation global gestures
     */
    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
}
