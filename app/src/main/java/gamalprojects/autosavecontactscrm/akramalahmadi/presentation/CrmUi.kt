@file:OptIn(ExperimentalMaterial3Api::class)

package gamalprojects.autosavecontactscrm.akramalahmadi.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamalprojects.autosavecontactscrm.akramalahmadi.R
import gamalprojects.autosavecontactscrm.akramalahmadi.database.Contact
import gamalprojects.autosavecontactscrm.akramalahmadi.database.LogEntry
import gamalprojects.autosavecontactscrm.akramalahmadi.managers.PermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AutoSaveCrmApp(viewModel: CrmViewModel) {
    // Force Arab Arabic RTL LayoutDirection
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val darkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle()
        
        MaterialTheme(
            colorScheme = if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF10B981), // Emerald/Success
                    secondary = Color(0xFF3B82F6), // Azure Blue
                    tertiary = Color(0xFFF59E0B), // Golden accent
                    background = Color(0xFF0F172A), // Dark slate
                    surface = Color(0xFF1E293B), // Card slate
                    onPrimary = Color.White,
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFF1F5F9)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF059669),
                    secondary = Color(0xFF2563EB),
                    tertiary = Color(0xFFD97706),
                    background = Color(0xFFF8FAFC),
                    surface = Color.White,
                    onPrimary = Color.White,
                    onBackground = Color(0xFF0F172A),
                    onSurface = Color(0xFF0F172A)
                )
            },
            content = {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                
                if (currentScreen == Screen.SPLASH) {
                    SplashScreen(onFinished = {
                        viewModel.navigateTo(Screen.DASHBOARD)
                        viewModel.tryStartMonitoringAutomatically()
                    })
                } else {
                    AppNavigationDrawer(viewModel = viewModel, currentScreen = currentScreen)
                }
            }
        )
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startProgress by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        startProgress = true
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ContactPhone,
                    contentDescription = "CRM App Icon",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "نظام الـ CRM المتكامل والذكي لحفظ العملاء تلقائياً",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = Color(0xFF10B981),
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "مظلة أمان لقائمة عملائك...",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun AppNavigationDrawer(viewModel: CrmViewModel, currentScreen: Screen) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe statistics
    val totalContacts by viewModel.contactsCount.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(310.dp)
            ) {
                // Drawer header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 32.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Filled.ContactPhone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "بناء وإدارة قاعدة العملاء تلقائياً",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Menu items
                DrawerMenuItem(
                    label = stringResource(R.string.menu_home),
                    icon = Icons.Filled.Dashboard,
                    isSelected = currentScreen == Screen.DASHBOARD,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(Screen.DASHBOARD)
                    }
                )
                
                DrawerMenuItem(
                    label = stringResource(R.string.menu_contacts),
                    icon = Icons.Filled.People,
                    isSelected = currentScreen == Screen.SAVED_CONTACTS,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(Screen.SAVED_CONTACTS)
                    }
                )

                DrawerMenuItem(
                    label = stringResource(R.string.menu_logs),
                    icon = Icons.Filled.HistoryToggleOff,
                    isSelected = currentScreen == Screen.LOGS,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(Screen.LOGS)
                    }
                )

                DrawerMenuItem(
                    label = stringResource(R.string.menu_export),
                    icon = Icons.Filled.Share,
                    isSelected = currentScreen == Screen.EXPORT,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(Screen.EXPORT)
                    }
                )

                DrawerMenuItem(
                    label = stringResource(R.string.menu_permissions),
                    icon = Icons.Filled.AdminPanelSettings,
                    isSelected = currentScreen == Screen.PERMISSIONS,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(Screen.PERMISSIONS)
                    }
                )

                DrawerMenuItem(
                    label = stringResource(R.string.menu_settings),
                    icon = Icons.Filled.Settings,
                    isSelected = currentScreen == Screen.SETTINGS,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(Screen.SETTINGS)
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer section
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "إجمالي العملاء: $totalContacts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الإصدار v1.0.0",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                Screen.DASHBOARD -> stringResource(R.string.title_dashboard)
                                Screen.SAVED_CONTACTS -> stringResource(R.string.title_saved_contacts)
                                Screen.LOGS -> stringResource(R.string.title_logs)
                                Screen.EXPORT -> stringResource(R.string.title_export)
                                Screen.PERMISSIONS -> stringResource(R.string.title_permissions)
                                Screen.SETTINGS -> stringResource(R.string.title_settings)
                                else -> ""
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "فتح القائمة الجانبية")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (viewModel.isDarkMode.value) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "تبديل المظهر"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(viewModel)
                    Screen.SAVED_CONTACTS -> SavedContactsScreen(viewModel)
                    Screen.LOGS -> LogsScreen(viewModel)
                    Screen.EXPORT -> ExportScreen(viewModel)
                    Screen.PERMISSIONS -> PermissionsScreen(viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel)
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        selected = isSelected,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(52.dp)
            .testTag("menu_item_${label.replace(" ", "_").lowercase()}")
    )
}

@Composable
fun DashboardScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current
    val totalContacts by viewModel.contactsCount.collectAsStateWithLifecycle()
    val latestContact by viewModel.latestContact.collectAsStateWithLifecycle()
    val isRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()

    val permGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
    val notifGranted by viewModel.notificationAccessGranted.collectAsStateWithLifecycle()
    val accessGranted by viewModel.accessibilityAccessGranted.collectAsStateWithLifecycle()
    val batteryIgnored by viewModel.batteryOptimizationIgnored.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = true) {
        viewModel.checkAllPermissionStates()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Monitoring status card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.stat_service_status),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) Color(0xFF10B981) else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRunning) stringResource(id = R.string.stat_running) else stringResource(id = R.string.stat_stopped),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isRunning) Color(0xFF10B981) else Color.Red
                                )
                            }
                        }
                        Switch(
                            checked = isRunning,
                            onCheckedChange = { viewModel.toggleService() },
                            modifier = Modifier.testTag("switch_service_activation")
                        )
                    }
                    if (!permGranted) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.08f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "يرجى منح صلاحيات التشغيل لتفعيل التتبع المتكامل.",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Stats summary Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Total contacts card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(R.string.stat_saved_contacts), fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$totalContacts", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Last added contact
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(R.string.stat_latest_contact), fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestContact?.name ?: "—",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Integrations Status Checklist
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "جاهزية الأنظمة المدمجة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    IntegrationItemRow("مراقبة المكالمات ورسائل الهاتف", permGranted)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    IntegrationItemRow("رصد إشعارات WhatsApp (CRM)", notifGranted)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    IntegrationItemRow("บริการ إمكانية الوصول (Accessibility)", accessGranted)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    IntegrationItemRow("تحسين استهلاك البطارية (مستبعد)", batteryIgnored)
                }
            }
        }

        // Quick tip info card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "فكرة الحفظ الذكي المدمجة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "بمجرد وصول أي اتصال أو رسالة SMS أو إشعار واتساب مجهول غير محفوظ على الهاتف، يتم تلقائيًا حفظ الرقم برمز مسلسل (عميل 1، عميل 2...) ويتم تسجيل كافة التفاعلات وتحديثها بشكل فوري وبدون تدخل منك.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationItemRow(title: String, isWorking: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isWorking) "مفعل ومستقر" else "يحتاج تنشيط",
                color = if (isWorking) Color(0xFF10B981) else Color(0xFFF59E0B),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isWorking) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (isWorking) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SavedContactsScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current
    val list by viewModel.contacts.collectAsStateWithLifecycle()
    val searchStr by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf<Contact?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Contact?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar filled style
        OutlinedTextField(
            value = searchStr,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(text = stringResource(id = R.string.action_search)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchStr.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("search_contacts_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PeopleOutline,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.msg_empty_contacts),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.id }) { contact ->
                    ContactItemCard(
                        contact = contact,
                        onEdit = { showEditDialog = contact },
                        onDelete = { showDeleteDialog = contact }
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Modal Edit Name Dialog
    if (showEditDialog != null) {
        var tempName by remember { mutableStateOf(showEditDialog!!.name) }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(text = stringResource(R.string.msg_edit_contact_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.msg_edit_contact_name_label), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) {
                        viewModel.updateContactName(showEditDialog!!, tempName.trim())
                        showEditDialog = null
                    }
                }) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Modal Confirm Delete Dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(text = stringResource(R.string.msg_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.msg_delete_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteContact(showDeleteDialog!!)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(text = stringResource(R.string.action_delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun ContactItemCard(
    contact: Contact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sDateFormater = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
    val captureString = sDateFormater.format(Date(contact.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.filter { it.isDigit() },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        // Display Arabic compliant text numbers
                        Text(
                            text = contact.originalPhone,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                        )
                    }
                }

                // Edit/Delete action row
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection badge source
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (contact.source) {
                            "مكالمة واردة", "مكالمة صادرة" -> Icons.Default.Phone
                            "رسالة SMS" -> Icons.Default.Sms
                            "واتساب" -> Icons.Default.ChatBubble
                            else -> Icons.Default.SettingsAccessibility
                        }
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = contact.source, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Interactions / sync details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Loop, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تفاعل: ${contact.interactionCount}", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    val isSynced = contact.isSyncedToPhone
                    Icon(
                        imageVector = if (isSynced) Icons.Default.Check else Icons.Default.SyncProblem,
                        contentDescription = null,
                        tint = if (isSynced) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSynced) "مزامنة الهاتف" else "مكتنز محلياً",
                        fontSize = 11.sp,
                        color = if (isSynced) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "رُصد: $captureString", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun LogsScreen(viewModel: CrmViewModel) {
    val auditLogs by viewModel.logs.collectAsStateWithLifecycle()
    val activeFilter by viewModel.logFilterSource.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Source Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sources = listOf("الكل", "مكالمة واردة", "رسالة SMS", "واتساب", "تعديل يدوي")
            sources.forEach { src ->
                val selected = src == activeFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.setLogFilterSource(src) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .border(1.dp, if (selected) Color.Transparent else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = src,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onBackground,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Divider()

        // Clear button
        if (auditLogs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.clearAllLogs() }) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "مسح سجلات عمليات التدقيق", color = Color.Red, fontSize = 13.sp)
                }
            }
        }

        // Logs display list
        if (auditLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.HistoryToggleOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = stringResource(id = R.string.msg_empty_logs), color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(auditLogs) { log ->
                    val sDateFormater = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale("ar"))
                    val logDate = sDateFormater.format(Date(log.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = when (log.status) {
                                        "تم الحفظ تلقائياً" -> Color(0xFF10B981)
                                        "حذف العميل" -> Color.Red
                                        "تعديل الاسم" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = log.status, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(text = logDate, fontSize = 9.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الهاتف: ${log.phone} | المصدر: ${log.source}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun ExportScreen(viewModel: CrmViewModel) {
    val totalContacts by viewModel.contactsCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "تصدير قاعدة بيانات العملاء", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "قم بتصدير العملاء المحتلين والمسجلين تلقائيًا داخل التطبيق لمشاركتهم بصيغة ملف CSV متوافق بالكامل مع Microsoft Excel وجهات اتصال Google.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "إجمالي السجلات المتاحة:", fontSize = 13.sp)
                    Text(text = "$totalContacts عملاء", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (totalContacts == 0) {
                            Toast.makeText(context, context.getString(R.string.msg_export_empty), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.exportContacts()
                            Toast.makeText(context, context.getString(R.string.msg_export_success), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_csv_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.menu_export))
                }
            }
        }
    }
}

@Composable
fun PermissionsScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current

    val permGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
    val notifGranted by viewModel.notificationAccessGranted.collectAsStateWithLifecycle()
    val accessGranted by viewModel.accessibilityAccessGranted.collectAsStateWithLifecycle()
    val batteryIgnored by viewModel.batteryOptimizationIgnored.collectAsStateWithLifecycle()

    // Setup permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.checkAllPermissionStates()
    }

    LaunchedEffect(key1 = true) {
        viewModel.checkAllPermissionStates()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "صلاحيات الوصول والتشغيل المتكاملة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يعتمد نظام الحذف الذكي والرصد التلقائي للعملاء بالخلفية على الصلاحيات التالية للوصول وإدارة أرقام الطيف والرسائل.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 1. Android Runtime phone permissions
        item {
            PermissionControlCard(
                title = "صلاحيات المكالمات، الأسماء والـ SMS",
                description = "يتطلب قراءة جهات الاتصال للتحقق، وحفظ الأرقام الجديدة، ورصد المكالمات الهاتفية والرسائل النصية الواردة.",
                isGranted = permGranted,
                onClick = {
                    launcher.launch(PermissionManager.RUNTIME_PERMISSIONS.toTypedArray())
                }
            )
        }

        // 2. Notification listener access
        item {
            PermissionControlCard(
                title = "رصد إشعارات التطبيقات (Notification Access)",
                description = "صلاحية إلزامية لرصد إشعارات com.whatsapp والبيانات الواردة منها وحفظ رقم مرسل الواتساب غير المحفوظ تلقائياً.",
                isGranted = notifGranted,
                onClick = {
                    PermissionManager.openNotificationListenerSettings(context)
                }
            )
        }

        // 3. Accessibility Service
        item {
            PermissionControlCard(
                title = "خدمة إمكانية الوصول الذكية (Accessibility Service)",
                description = "توقيع مدمج لتعزيز الحفظ التلقائي والتقاط الأرقام من واجهات المحادثات والاتصال النشطة مباشرة من الشاشة.",
                isGranted = accessGranted,
                onClick = {
                    PermissionManager.openAccessibilitySettings(context)
                }
            )
        }

        // 4. Battery Ignored
        item {
            PermissionControlCard(
                title = "استثناء من تحسينات البطارية (Battery Optimizations)",
                description = "لضمان استقرار عالي ومنع نظام تشغيل الـ Android من إقفال وقتل الخدمة الخلفية للتطبيق بعد إغلاق شاشة الهاتف.",
                isGranted = batteryIgnored,
                onClick = {
                    PermissionManager.requestIgnoreBatteryOptimization(context)
                }
            )
        }
    }
}

@Composable
fun PermissionControlCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "مفعل" else "معطل",
                        color = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isGranted) "ممنوح بالفعل" else "تعديل الإعداد الآن", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Style Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "إعدادات المظهر", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "تشغيل المظهر الداكن (Dark Mode)", fontSize = 13.sp)
                        Switch(
                            checked = viewModel.isDarkMode.value,
                            onCheckedChange = { viewModel.toggleTheme() }
                        )
                    }
                }
            }
        }

        // Developer Info card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "مطور النظام", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = stringResource(id = R.string.setting_developer_desc), fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "يتيح تطبيق AutoSave CRM للمؤسسات والشركات بناء أكبر وأدق داتا دافئة ومستهدفة للعملاء بمجرد رنين الاتصال عبر الهاتف أو دردشات الواتساب.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val uri = Uri.parse("mailto:gamalalmaqtary6838@gmail.com")
                            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "تطبيق AutoSave Contacts CRM")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.setting_contact_dev), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Versioning and info details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "التفاصيل القانونية وحالة الإصدار", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "إصدار التطبيق:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "1.0.0 (بناء احترافي مسجل)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "ترخيص الاستخدام:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "مفتوح المصدر (GPL)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "سياسة الخصوصية:", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "الخصوصية محلية 100% (أوفلاين)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
