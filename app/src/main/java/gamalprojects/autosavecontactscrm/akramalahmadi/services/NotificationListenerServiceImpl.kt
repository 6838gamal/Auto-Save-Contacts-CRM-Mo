package gamalprojects.autosavecontactscrm.akramalahmadi.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import gamalprojects.autosavecontactscrm.akramalahmadi.database.AppDatabase
import gamalprojects.autosavecontactscrm.akramalahmadi.repositories.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListenerServiceImpl : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var repo: ContactsRepository

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repo = ContactsRepository(db)
        Log.d("NotificationListener", "Notification Listener Service Created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        
        // WhatsApp notification processing
        if (packageName == "com.whatsapp") {
            val extras = sbn.notification.extras ?: return
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            Log.i("NotificationListener", "WhatsApp Notification Posted: Title: $title, Text: $text")

            val potentialPhone = extractPhoneNumber(title)
            if (potentialPhone != null) {
                Log.d("NotificationListener", "Found WhatsApp phone number to parse: $potentialPhone")
                scope.launch {
                    repo.processNewConnection(
                        applicationContext,
                        potentialPhone,
                        "واتساب",
                        "رسالة واتساب: ${text.take(60)}"
                    )
                }
            } else {
                Log.d("NotificationListener", "WhatsApp Notification Title contains letters or short, ignoring (probably saved contact): $title")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not used
    }

    /**
     * Determines if a text contains an unsaved phone number (starts with +, digits, spaces)
     * and does not contain alphabetic names (which indicates a saved contact name).
     */
    private fun extractPhoneNumber(title: String): String? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null

        // If it contains Arabic alphabets or English letters, WhatsApp is showing a saved contact name.
        // Unsaved numbers are shown purely as "+967 777 777 777" or "777 777 777".
        val hasLetters = trimmed.any { c -> 
            c.isLetter() || (c.code in 0x0600..0x06FF) // Arabic characters block range
        }
        if (hasLetters) {
            return null
        }

        // Clean up formatting spaces, hyphens, and brackets.
        val cleaned = trimmed.replace(Regex("[^0-9+]"), "")
        if (cleaned.length >= 7) {
            return cleaned
        }

        return null
    }
}
