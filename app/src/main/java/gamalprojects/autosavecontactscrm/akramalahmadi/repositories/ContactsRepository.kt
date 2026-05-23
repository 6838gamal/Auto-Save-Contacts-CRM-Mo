package gamalprojects.autosavecontactscrm.akramalahmadi.repositories

import android.content.Context
import android.util.Log
import gamalprojects.autosavecontactscrm.akramalahmadi.database.*
import gamalprojects.autosavecontactscrm.akramalahmadi.managers.ContactsManager
import gamalprojects.autosavecontactscrm.akramalahmadi.managers.PhoneNumberUtils
import gamalprojects.autosavecontactscrm.akramalahmadi.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ContactsRepository(private val db: AppDatabase) {

    private val contactDao = db.contactDao()
    private val logEntryDao = db.logEntryDao()
    private val phoneEventDao = db.phoneEventDao()
    private val settingDao = db.settingDao()

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val allLogs: Flow<List<LogEntry>> = logEntryDao.getAllLogs()
    val allEvents: Flow<List<PhoneEvent>> = phoneEventDao.getAllEvents()
    val contactsCount: Flow<Int> = contactDao.getContactsCount()
    val latestContact: Flow<Contact?> = contactDao.getLatestContact()

    /**
     * Engine's unified point of processing for any newly captured number.
     * Implements "ContactExistenceChecker" and "Smart Save Engine".
     */
    suspend fun processNewConnection(
        context: Context,
        rawNumber: String,
        source: String,
        contentSnippet: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val cleaned = PhoneNumberUtils.cleanNumber(rawNumber)
        if (cleaned.isEmpty()) {
            return@withContext false
        }

        // 1. Normalize the phone number
        val normalized = PhoneNumberUtils.normalize(rawNumber)

        // 2. Check Device Native Contacts: Ignore completely if exists there, do not log, do not save.
        if (ContactsManager.isNumberInNativeContacts(context, rawNumber)) {
            Log.d("ContactsRepository", "Number $rawNumber already exists in native contacts. Ignoring entirely.")
            return@withContext false
        }

        // 3. Keep record of the event in events_table
        phoneEventDao.insertEvent(
            PhoneEvent(
                phone = rawNumber,
                type = source,
                content = contentSnippet.ifEmpty { "تواصل متبادل عبر $source" }
            )
        )

        // 4. Check Internal database
        val existingContact = contactDao.getContactByPhone(normalized)
        if (existingContact != null) {
            // Already saved in our CRM! Update last activity, interaction count, and save log
            val updated = existingContact.copy(
                lastActivity = System.currentTimeMillis(),
                interactionCount = existingContact.interactionCount + 1
            )
            contactDao.updateContact(updated)

            logEntryDao.insertLog(
                LogEntry(
                    phone = rawNumber,
                    source = source,
                    status = "تحديث النشاط",
                    details = "تحديث آخر نشاط للعميل ${existingContact.name}. عدد التفاعلات: ${updated.interactionCount}"
                )
            )
            return@withContext true
        }

        // 5. If fully new, perform "Smart Save"
        // Generate customer name with incremental count from settings
        val nextId = getNextCustomerId()
        val clientName = "عميل $nextId"

        // Save to native contacts
        val isSynced = ContactsManager.saveToNativeContacts(context, clientName, rawNumber)

        // Save to internal CRM database
        val newContact = Contact(
            name = clientName,
            phone = normalized,
            originalPhone = rawNumber,
            source = source,
            isSyncedToPhone = isSynced
        )
        contactDao.insertContact(newContact)

        // Increment customer counter for future use
        incrementCustomerId(nextId)

        // Register custom audit log entry
        logEntryDao.insertLog(
            LogEntry(
                phone = rawNumber,
                source = source,
                status = if (isSynced) "تم الحفظ تلقائياً" else "خطأ في الحفظ ومكتنز محلياً",
                details = "تم حفظ $clientName برقم $rawNumber بنجاح. المزامنة للهاتف: ${if (isSynced) "مكتمل" else "فشل (انتظار صلاحيات)"}"
            )
        )

        // Push notify user
        NotificationHelper.showNotification(
            context,
            "تم حفظ عميل جديد تلقائياً 👤",
            "تم التعرف على تواصل من رقم جديد وحفظه باسم '$clientName'"
        )

        return@withContext true
    }

    /**
     * Gets next ID from settings to prevent reuse upon deletion.
     */
    private suspend fun getNextCustomerId(): Int {
        val setting = settingDao.getSetting("next_customer_id")
        return setting?.value?.toIntOrNull() ?: 1
    }

    private suspend fun incrementCustomerId(currentId: Int) {
        settingDao.insertSetting(SettingEntity("next_customer_id", (currentId + 1).toString()))
    }

    /**
     * Updates contact name inside both Native phonebook and local database
     */
    suspend fun updateContactName(context: Context, contact: Contact, newName: String): Boolean = withContext(Dispatchers.IO) {
        val updated = contact.copy(name = newName)
        contactDao.updateContact(updated)

        // Attempt updating native contact as well if synced before
        if (contact.isSyncedToPhone) {
            // Updating names inside Contacts contract is complex. If it fails, we flag isSyncedToPhone.
            ContactsManager.saveToNativeContacts(context, newName, contact.originalPhone)
        }

        logEntryDao.insertLog(
            LogEntry(
                phone = contact.originalPhone,
                source = "تعديل يدوي",
                status = "تعديل الاسم",
                details = "تم تعديل اسم العميل من ${contact.name} إلى $newName"
            )
        )
        return@withContext true
    }

    suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(contact)
        logEntryDao.insertLog(
            LogEntry(
                phone = contact.originalPhone,
                source = "حذف يدوي",
                status = "حذف العميل",
                details = "تم حذف ${contact.name} من سجلات الـ CRM"
            )
        )
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logEntryDao.deleteAllLogs()
    }

    suspend fun clearEvents() = withContext(Dispatchers.IO) {
        phoneEventDao.deleteAllEvents()
    }
}
