package gamalprojects.autosavecontactscrm.akramalahmadi.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import gamalprojects.autosavecontactscrm.akramalahmadi.database.AppDatabase
import gamalprojects.autosavecontactscrm.akramalahmadi.repositories.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("CallReceiver", "Received action: $action")

        val db = AppDatabase.getDatabase(context.applicationContext)
        val repo = ContactsRepository(db)

        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            if (!phoneNumber.isNullOrBlank()) {
                Log.i("CallReceiver", "Detected New Outgoing Call to: $phoneNumber")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repo.processNewConnection(
                            context.applicationContext,
                            phoneNumber,
                            "مكالمة صادرة",
                            "مكالمة صادرة إلى الرقم"
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("CallReceiver", "Phone state changed: $state")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // Incoming call ringing
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (!phoneNumber.isNullOrBlank()) {
                    Log.i("CallReceiver", "Detected Incoming Call from: $phoneNumber")
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repo.processNewConnection(
                                context.applicationContext,
                                phoneNumber,
                                "مكالمة واردة",
                                "رنين مكالمة واردة من العميل"
                            )
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
