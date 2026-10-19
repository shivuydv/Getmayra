package com.example

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Maya WhatsApp Reader Notification Listener Service.
 * Listens for incoming WhatsApp messages and allows Maya to announce and read them aloud.
 */
class MayaWhatsAppReaderService : NotificationListenerService() {

    companion object {
        private const val TAG = "MayaWhatsAppReader"

        var onWhatsAppMessageReceived: ((sender: String, message: String) -> Unit)? = null

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val component = ComponentName(context, MayaWhatsAppReaderService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(component.flattenToString())
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (title.isNotBlank() && text.isNotBlank()) {
                Log.i(TAG, "WhatsApp message from $title: $text")
                onWhatsAppMessageReceived?.invoke(title, text)
            }
        }
    }
}
