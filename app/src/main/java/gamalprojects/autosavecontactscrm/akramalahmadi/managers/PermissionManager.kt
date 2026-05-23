package gamalprojects.autosavecontactscrm.akramalahmadi.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import gamalprojects.autosavecontactscrm.akramalahmadi.services.ContactAccessibilityService
import gamalprojects.autosavecontactscrm.akramalahmadi.services.NotificationListenerServiceImpl

object PermissionManager {

    // Runtime permissions
    val RUNTIME_PERMISSIONS = buildList {
        add(android.Manifest.permission.READ_CONTACTS)
        add(android.Manifest.permission.WRITE_CONTACTS)
        add(android.Manifest.permission.READ_PHONE_STATE)
        add(android.Manifest.permission.READ_CALL_LOG)
        add(android.Manifest.permission.RECEIVE_SMS)
        add(android.Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Checks if all runtime permissions are granted.
     */
    fun hasAllRuntimePermissions(context: Context): Boolean {
        return RUNTIME_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if notification listener service is approved.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (flat != null) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == context.packageName && cn.className == NotificationListenerServiceImpl::class.java.name) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Prompts the user to enable Notification Listener settings.
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks if our Accessibility Service is active.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, ContactAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = enabledServicesSetting.split(":")
        for (component in colonSplitter) {
            val enabledService = ComponentName.unflattenFromString(component)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    /**
     * Prompts the user to enable Accessibility settings.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks if the app is ignored from Battery Optimization.
     */
    fun isIgnoringBatteryOptimization(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * Requests ignoring Battery Optimization.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
