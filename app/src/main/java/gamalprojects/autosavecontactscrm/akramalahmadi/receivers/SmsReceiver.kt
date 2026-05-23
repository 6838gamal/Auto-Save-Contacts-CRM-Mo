package gamalprojects.autosavecontactscrm.akramalahmadi.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import gamalprojects.autosavecontactscrm.akramalahmadi.database.AppDatabase
import gamalprojects.autosavecontactscrm.akramalahmadi.repositories.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "SMS broadcast received")
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isEmpty()) return

            val sender = messages[0].originatingAddress
            val body = messages.joinToString(separator = "") { it.messageBody ?: "" }

            if (!sender.isNullOrBlank()) {
                Log.i("SmsReceiver", "Captured SMS from: $sender. Body: ${body.take(20)}...")
                
                val db = AppDatabase.getDatabase(context.applicationContext)
                val repo = ContactsRepository(db)
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repo.processNewConnection(
                            context.applicationContext,
                            sender,
                            "رسالة SMS",
                            "الرسالة: ${body.take(60)}"
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
