package gamalprojects.autosavecontactscrm.akramalahmadi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Contact::class, LogEntry::class, PhoneEvent::class, SettingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun phoneEventDao(): PhoneEventDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosave_contacts_crm_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
