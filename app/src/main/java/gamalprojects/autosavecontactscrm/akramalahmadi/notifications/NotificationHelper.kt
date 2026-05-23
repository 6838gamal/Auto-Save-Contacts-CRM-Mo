package gamalprojects.autosavecontactscrm.akramalahmadi.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import gamalprojects.autosavecontactscrm.akramalahmadi.MainActivity

object NotificationHelper {

    private const val CHANNEL_CRM_EVENTS = "channel_crm_events"
    private const val CHANNEL_FOREGROUND_SERVICE = "channel_foreground_service"
    private const val NOTIFICATION_ID_EVENT = 101
    const val NOTIFICATION_ID_FOREGROUND = 102

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Foreground Service Sticky Notification
            val serviceChannel = NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                "حالة نظام المراقبة CRM",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "يعرض حالة خدمة رصد المكالمات والرسائل بالخلفية"
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // Channel 2: Intelligent AutoSave Events notifications
            val eventChannel = NotificationChannel(
                CHANNEL_CRM_EVENTS,
                "إشعارات الحفظ التلقائي والعملاء",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات فوريّة عند رصد وحفظ الأرقام الجديدة تلقائياً"
            }
            notificationManager.createNotificationChannel(eventChannel)
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CRM_EVENTS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_EVENT, builder.build())
    }

    fun getForegroundServiceNotification(
        context: Context,
        savedCount: Int,
        statusText: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle("AutoSave Contacts CRM نشط")
            .setContentText("الحالة: $statusText | العملاء المحفوظون: $savedCount")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
