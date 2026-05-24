package gamalprojects.autosavecontactscrm.akramalahmadi.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gamalprojects.autosavecontactscrm.akramalahmadi.database.AppDatabase
import gamalprojects.autosavecontactscrm.akramalahmadi.database.Contact
import gamalprojects.autosavecontactscrm.akramalahmadi.database.LogEntry
import gamalprojects.autosavecontactscrm.akramalahmadi.database.SettingEntity
import gamalprojects.autosavecontactscrm.akramalahmadi.export.ExportManager
import gamalprojects.autosavecontactscrm.akramalahmadi.managers.PermissionManager
import gamalprojects.autosavecontactscrm.akramalahmadi.repositories.ContactsRepository
import gamalprojects.autosavecontactscrm.akramalahmadi.services.AutoSaveForegroundService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CrmViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = ContactsRepository(db)

    // UI screen handling
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Search and filtering state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _logFilterSource = MutableStateFlow("الكل")
    val logFilterSource: StateFlow<String> = _logFilterSource.asStateFlow()

    // CRM Contacts Flow
    val contacts: StateFlow<List<Contact>> = combine(repo.allContacts, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.phone.contains(query) || 
                it.originalPhone.contains(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logs Flow
    val logs: StateFlow<List<LogEntry>> = combine(repo.allLogs, _logFilterSource) { list, filter ->
        if (filter == "الكل") {
            list
        } else {
            list.filter { it.source == filter }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard core variables
    val contactsCount: StateFlow<Int> = repo.contactsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val latestContact: StateFlow<Contact?> = repo.latestContact.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Service operational monitoring state
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // Live permission monitoring states
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    private val _notificationAccessGranted = MutableStateFlow(false)
    val notificationAccessGranted: StateFlow<Boolean> = _notificationAccessGranted.asStateFlow()

    private val _accessibilityAccessGranted = MutableStateFlow(false)
    val accessibilityAccessGranted: StateFlow<Boolean> = _accessibilityAccessGranted.asStateFlow()

    private val _batteryOptimizationIgnored = MutableStateFlow(false)
    val batteryOptimizationIgnored: StateFlow<Boolean> = _batteryOptimizationIgnored.asStateFlow()

    // Theme state (Dark/Light Mode)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Gmail Sync States
    private val _gmailAccount = MutableStateFlow("")
    val gmailAccount: StateFlow<String> = _gmailAccount.asStateFlow()

    private val _googleSyncEnabled = MutableStateFlow(false)
    val googleSyncEnabled: StateFlow<Boolean> = _googleSyncEnabled.asStateFlow()

    private val _autoExportCrm = MutableStateFlow(false)
    val autoExportCrm: StateFlow<Boolean> = _autoExportCrm.asStateFlow()

    init {
        checkAllPermissionStates()
        // Check stored theme preference
        viewModelScope.launch {
            val setting = db.settingDao().getSetting("app_theme")
            if (setting != null) {
                _isDarkMode.value = (setting.value == "dark")
            }

            val emailSetting = db.settingDao().getSetting("gmail_account")
            if (emailSetting != null) {
                _gmailAccount.value = emailSetting.value
            }

            val syncSetting = db.settingDao().getSetting("google_sync_enabled")
            if (syncSetting != null) {
                _googleSyncEnabled.value = (syncSetting.value == "1")
            }

            val autoExpSetting = db.settingDao().getSetting("auto_export_crm")
            if (autoExpSetting != null) {
                _autoExportCrm.value = (autoExpSetting.value == "1")
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLogFilterSource(source: String) {
        _logFilterSource.value = source
    }

    /**
     * Checks all required permissions on demand.
     */
    fun checkAllPermissionStates() {
        val context = getApplication<Application>()
        _permissionsGranted.value = PermissionManager.hasAllRuntimePermissions(context)
        _notificationAccessGranted.value = PermissionManager.isNotificationListenerEnabled(context)
        _accessibilityAccessGranted.value = PermissionManager.isAccessibilityServiceEnabled(context)
        _batteryOptimizationIgnored.value = PermissionManager.isIgnoringBatteryOptimization(context)
        
        // Auto update service running state in ViewModel
        // WorkManager or simple boolean can verify if service started
        _isServiceRunning.value = PermissionManager.hasAllRuntimePermissions(context)
    }

    /**
     * Controls background service lifecycle.
     */
    fun toggleService() {
        val context = getApplication<Application>()
        if (_isServiceRunning.value) {
            AutoSaveForegroundService.stop(context)
            _isServiceRunning.value = false
        } else {
            AutoSaveForegroundService.start(context)
            _isServiceRunning.value = true
        }
    }

    fun tryStartMonitoringAutomatically() {
        val context = getApplication<Application>()
        if (PermissionManager.hasAllRuntimePermissions(context)) {
            AutoSaveForegroundService.start(context)
            _isServiceRunning.value = true
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val nextVal = !_isDarkMode.value
            _isDarkMode.value = nextVal
            db.settingDao().insertSetting(SettingEntity("app_theme", if (nextVal) "dark" else "light"))
        }
    }

    // Actions
    fun updateContactName(contact: Contact, newName: String) {
        viewModelScope.launch {
            repo.updateContactName(getApplication(), contact, newName)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repo.deleteContact(contact)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repo.clearLogs()
        }
    }

    fun exportContacts() {
        val list = contacts.value
        ExportManager.exportContactsToCsv(getApplication(), list)
    }

    // Gmail & Sync Settings actions
    fun updateGmailAccount(email: String) {
        viewModelScope.launch {
            _gmailAccount.value = email
            db.settingDao().insertSetting(SettingEntity("gmail_account", email))
        }
    }

    fun toggleGoogleSync() {
        viewModelScope.launch {
            val nextVal = !_googleSyncEnabled.value
            _googleSyncEnabled.value = nextVal
            db.settingDao().insertSetting(SettingEntity("google_sync_enabled", if (nextVal) "1" else "0"))
        }
    }

    fun toggleAutoExportCrm() {
        viewModelScope.launch {
            val nextVal = !_autoExportCrm.value
            _autoExportCrm.value = nextVal
            db.settingDao().insertSetting(SettingEntity("auto_export_crm", if (nextVal) "1" else "0"))
        }
    }

    fun importFromGoogleContacts(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val account = _gmailAccount.value
            val imported = gamalprojects.autosavecontactscrm.akramalahmadi.managers.ContactsManager.importFromGoogleContacts(getApplication(), if (account.isBlank()) null else account)
            var count = 0
            imported.forEach { (name, phone) ->
                val normalized = gamalprojects.autosavecontactscrm.akramalahmadi.managers.PhoneNumberUtils.normalize(phone)
                val existing = db.contactDao().getContactByPhone(normalized)
                if (existing == null) {
                    val nextId = db.settingDao().getSetting("next_customer_id")?.value?.toIntOrNull() ?: 1
                    val clientName = "عميل $nextId"
                    val newContact = Contact(
                        name = clientName,
                        phone = normalized,
                        originalPhone = phone,
                        source = "واتساب",
                        isSyncedToPhone = true
                    )
                    db.contactDao().insertContact(newContact)
                    db.settingDao().insertSetting(SettingEntity("next_customer_id", (nextId + 1).toString()))

                    db.logEntryDao().insertLog(
                        LogEntry(
                            phone = phone,
                            source = "استيراد من جمايل",
                            status = "تم الاستيراد",
                            details = "تم استيراد جهة الاتصال: $name وحفظه كعميل: $clientName"
                        )
                    )
                    count++
                }
            }
            onResult(count)
        }
    }

    fun exportToGoogleContacts(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val account = _gmailAccount.value
            if (account.isBlank()) {
                onResult(-1)
                return@launch
            }
            var count = 0
            val list = contacts.value
            list.forEach { contact ->
                if (!contact.isSyncedToPhone) {
                    val success = gamalprojects.autosavecontactscrm.akramalahmadi.managers.ContactsManager.saveToGoogleContacts(
                        getApplication(),
                        contact.name,
                        contact.originalPhone,
                        account
                    )
                    if (success) {
                        db.contactDao().insertContact(contact.copy(isSyncedToPhone = true))
                        db.logEntryDao().insertLog(
                            LogEntry(
                                phone = contact.originalPhone,
                                source = "تصدير إلى جمايل",
                                status = "تصدير ناجح",
                                details = "تم تصدير العميل ${contact.name} إلى حساب جوجل: $account"
                            )
                        )
                        count++
                    }
                }
            }
            onResult(count)
        }
    }
}

enum class Screen {
    SPLASH,
    DASHBOARD,
    SAVED_CONTACTS,
    LOGS,
    EXPORT,
    PERMISSIONS,
    SETTINGS
}
