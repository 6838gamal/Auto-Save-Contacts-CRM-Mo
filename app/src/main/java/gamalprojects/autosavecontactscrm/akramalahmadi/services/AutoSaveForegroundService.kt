package gamalprojects.autosavecontactscrm.akramalahmadi.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import gamalprojects.autosavecontactscrm.akramalahmadi.database.AppDatabase
import gamalprojects.autosavecontactscrm.akramalahmadi.notifications.NotificationHelper
import gamalprojects.autosavecontactscrm.akramalahmadi.repositories.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AutoSaveForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: ContactsRepository

    override fun onCreate() {
        super.onCreate()
        Log.d("AutoSaveForegroundService", "Service onCreate")
        
        NotificationHelper.createNotificationChannels(this)
        
        val db = AppDatabase.getDatabase(this)
        repository = ContactsRepository(db)

        // Show initial notification
        val notification = NotificationHelper.getForegroundServiceNotification(
            this,
            0,
            "بدء الرصد التلقائي..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                notification
            )
        }

        // Dynamically update the sticky notification count
        serviceScope.launch {
            repository.contactsCount.collect { count ->
                val updatedNotification = NotificationHelper.getForegroundServiceNotification(
                    this@AutoSaveForegroundService,
                    count,
                    "رصد مستمر نشط بالخلفية"
                )
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NotificationHelper.NOTIFICATION_ID_FOREGROUND, updatedNotification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AutoSaveForegroundService", "Service onStartCommand")
        
        // Handle explicit stop action from intent if needed
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AutoSaveForegroundService", "Service onDestroy")
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_STOP_SERVICE = "gamalprojects.autosavecontactscrm.akramalahmadi.services.ACTION_STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, AutoSaveForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AutoSaveForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
