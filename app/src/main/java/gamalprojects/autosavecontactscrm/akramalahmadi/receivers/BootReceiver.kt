package gamalprojects.autosavecontactscrm.akramalahmadi.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import gamalprojects.autosavecontactscrm.akramalahmadi.services.AutoSaveForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device boot completed. Re-triggering CRM monitoring foreground service.")
            AutoSaveForegroundService.start(context)
        }
    }
}
