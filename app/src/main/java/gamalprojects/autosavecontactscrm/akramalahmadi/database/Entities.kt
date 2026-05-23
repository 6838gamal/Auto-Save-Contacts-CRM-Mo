package gamalprojects.autosavecontactscrm.akramalahmadi.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts_table",
    indices = [Index(value = ["phone"], unique = true)]
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String, // Normalized phone number
    val originalPhone: String,
    val source: String, // "مكالمة", "رسالة SMS", "واتساب"
    val timestamp: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val interactionCount: Int = 1,
    val isSyncedToPhone: Boolean = false
)

@Entity(tableName = "logs_table")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phone: String,
    val source: String,
    val status: String, // "تم الحفظ تلقائياً", "تم التجاهل (محفوظ مسبقاً)", "تحديث النشاط", "خطأ في الحفظ"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

@Entity(tableName = "events_table")
data class PhoneEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phone: String,
    val type: String, // "مكالمة واردة", "مكالمة صادرة", "رسالة SMS", "إشعار واتساب"
    val timestamp: Long = System.currentTimeMillis(),
    val content: String
)

@Entity(tableName = "settings_table")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
