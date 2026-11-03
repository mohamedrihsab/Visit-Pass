package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.FirestoreService
import com.example.data.KioskPreferences
import com.example.data.UserRole
import com.example.repository.VisitorRepository
import com.example.ui.UiEvent
import com.example.ui.VisitorViewModel
import com.example.ui.VisitorViewModelFactory
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.NotificationCenterDialog
import com.example.ui.components.VisitorPassDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.LogbookScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SafetyInfoScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: VisitorViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val kioskPrefs = KioskPreferences(applicationContext)
        val firestoreService = FirestoreService(applicationContext)
        val repository = VisitorRepository(database.visitorDao(), firestoreService, kioskPrefs)
        VisitorViewModelFactory(repository, kioskPrefs, firestoreService)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val themeMode by viewModel.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                KioskPreferences.THEME_DARK -> true
                KioskPreferences.THEME_LIGHT -> false
                else -> isSystemDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                val currentTab by viewModel.currentTab.collectAsState()
                val isKioskMode by viewModel.isKioskMode.collectAsState()
                val userRole by viewModel.userRole.collectAsState()
                val activeBranch by viewModel.activeBranch.collectAsState()
                val showPinDialog by viewModel.showPinDialog.collectAsState()
                val targetPinRole by viewModel.targetPinRole.collectAsState()
                val adminPin by viewModel.adminPin.collectAsState()
                val receptionistPin by viewModel.receptionistPin.collectAsState()
                val activePass by viewModel.activeVisitorPass.collectAsState()
                val countdownSec by viewModel.passCountdownSeconds.collectAsState()
                val autoResetSec by viewModel.autoResetSeconds.collectAsState()
                val allVisitors by viewModel.allVisitorsList.collectAsState()
                val totalLoggedCount = allVisitors.size
                val unsyncedCount by viewModel.unsyncedCount.collectAsState()
                val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()
                val showNotificationCenter by viewModel.showNotificationCenter.collectAsState()
                val notificationsList by viewModel.notifications.collectAsState()

                // 3-second touch-and-hold state for the single Top Bar user access shortcut
                var isHoldingAccessShortcut by remember { mutableStateOf(false) }

                LaunchedEffect(isHoldingAccessShortcut) {
                    if (isHoldingAccessShortcut) {
                        delay(3000L)
                        isHoldingAccessShortcut = false
                        when (userRole) {
                            UserRole.VISITOR_KIOSK -> viewModel.requestRoleUnlock()
                            UserRole.RECEPTIONIST -> viewModel.requestRoleUnlock("ADMIN")
                            UserRole.ADMIN -> viewModel.enterKioskMode()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                            is UiEvent.SendEmailNotification -> {
                                if (event.recipients.isNotEmpty()) {
                                    try {
                                        val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:" + event.recipients.joinToString(","))
                                            putExtra(android.content.Intent.EXTRA_EMAIL, event.recipients.toTypedArray())
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, event.subject)
                                            putExtra(android.content.Intent.EXTRA_TEXT, event.body)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(emailIntent)
                                    } catch (e: Exception) {
                                        try {
                                            val chooser = android.content.Intent.createChooser(
                                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "message/rfc822"
                                                    putExtra(android.content.Intent.EXTRA_EMAIL, event.recipients.toTypedArray())
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, event.subject)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, event.body)
                                                }, "Send Visitor Pass & Notification"
                                            ).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            startActivity(chooser)
                                        } catch (ex: Exception) {
                                            android.util.Log.w("MainActivity", "Email dispatch warning: ${ex.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    ) {
                                        if (!activeBranch.logoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = activeBranch.logoUri,
                                                contentDescription = "${activeBranch.name} Logo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White)
                                                    .padding(3.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (userRole) {
                                                        UserRole.VISITOR_KIOSK -> Icons.Default.Store
                                                        UserRole.RECEPTIONIST -> Icons.Default.Badge
                                                        UserRole.ADMIN -> Icons.Default.Security
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (userRole) {
                                            UserRole.VISITOR_KIOSK -> activeBranch.name
                                            UserRole.RECEPTIONIST -> if (currentTab == 0) activeBranch.name else "${activeBranch.shortCode} RECEPTION"
                                            UserRole.ADMIN -> if (currentTab == 0) activeBranch.name else "${activeBranch.shortCode} ADMIN"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            },
                            actions = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Notification Center Bell & Theme Switcher: ONLY visible in Receptionist and Admin Mode
                                    if (userRole != UserRole.VISITOR_KIOSK) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadNotifications > 0) {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ) {
                                                        Text(
                                                            text = unreadNotifications.toString(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.openNotificationCenter() },
                                                modifier = Modifier.testTag("appbar_notifications_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (unreadNotifications > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                                    contentDescription = "Notification Center",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Theme Switcher Button (Only Receptionist and Admin can change theme)
                                        IconButton(
                                            onClick = { viewModel.toggleTheme() },
                                            modifier = Modifier.testTag("appbar_theme_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = "Toggle Light/Dark Theme",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (totalLoggedCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = StatusSuccess.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$totalLoggedCount Logged",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        // Return to Kiosk Mode button ONLY in Admin mode (Receptionist mode does not have switch-to-kiosk button)
                                        if (userRole == UserRole.ADMIN) {
                                            IconButton(
                                                onClick = { viewModel.enterKioskMode() },
                                                modifier = Modifier.testTag("appbar_launch_kiosk")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Store,
                                                    contentDescription = "Launch Kiosk",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // User Access Shortcut in the Top Bar:
                                    // - In Visitor Kiosk Mode: works ONLY on 3-second touch and hold
                                    // - In Receptionist Mode: switches to Admin directly using the button (tap or hold)
                                    if (userRole == UserRole.RECEPTIONIST) {
                                        IconButton(
                                            onClick = { viewModel.requestRoleUnlock("ADMIN") },
                                            modifier = Modifier.testTag("appbar_admin_lock")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = "Switch to Admin",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (userRole == UserRole.VISITOR_KIOSK) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("appbar_admin_lock")
                                                .semantics {
                                                    contentDescription = "Hold 3 Seconds for Staff/Admin Access"
                                                    role = Role.Button
                                                }
                                                .pointerInput(userRole) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            isHoldingAccessShortcut = true
                                                            tryAwaitRelease()
                                                            isHoldingAccessShortcut = false
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Hold 3 Seconds for Role Unlock",
                                                tint = if (isHoldingAccessShortcut) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        // Kiosk Mode: NO navigation bar (locked self-service form, with Safety button directly on form)
                        if (userRole == UserRole.RECEPTIONIST) {
                            // Receptionist Mode: Manual Entry, Visitor Logbook, and Safety
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp,
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { viewModel.setTab(0) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 0) Icons.Default.EditNote else Icons.Outlined.EditNote,
                                            contentDescription = "Manual Entry"
                                        )
                                    },
                                    label = { Text("Manual Entry") },
                                    modifier = Modifier.testTag("nav_register")
                                )

                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { viewModel.setTab(1) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (totalLoggedCount > 0) {
                                                    Badge { Text(totalLoggedCount.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (currentTab == 1) Icons.Default.ListAlt else Icons.Default.Assignment,
                                                contentDescription = "Visitor Logbook"
                                            )
                                        }
                                    },
                                    label = { Text("Visitor Logbook") },
                                    modifier = Modifier.testTag("nav_logbook")
                                )

                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { viewModel.setTab(3) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 3) Icons.Default.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                                            contentDescription = "Safety"
                                        )
                                    },
                                    label = { Text("Safety") },
                                    modifier = Modifier.testTag("nav_safety")
                                )
                            }
                        } else if (userRole == UserRole.ADMIN) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp,
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                // Tab 0: Register / Kiosk View
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { viewModel.setTab(0) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 0) Icons.Default.HowToReg else Icons.Outlined.HowToReg,
                                            contentDescription = "Register"
                                        )
                                    },
                                    label = { Text("Register") },
                                    modifier = Modifier.testTag("nav_register")
                                )

                                // Tab 1: Logbook
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { viewModel.setTab(1) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (totalLoggedCount > 0) {
                                                    Badge { Text(totalLoggedCount.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (currentTab == 1) Icons.Default.ListAlt else Icons.Default.Assignment,
                                                contentDescription = "Logbook"
                                            )
                                        }
                                    },
                                    label = { Text("Logbook") },
                                    modifier = Modifier.testTag("nav_logbook")
                                )

                                // Tab 2: Cloud Sync & Admin Controls
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { viewModel.setTab(2) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (unsyncedCount > 0) {
                                                    Badge(containerColor = StatusWarning) {
                                                        Text(unsyncedCount.toString(), color = Color.Black)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (currentTab == 2) Icons.Default.CloudDone else Icons.Default.AdminPanelSettings,
                                                contentDescription = "Cloud & Admin"
                                            )
                                        }
                                    },
                                    label = { Text("Cloud & Admin") },
                                    modifier = Modifier.testTag("nav_admin")
                                )

                                // Tab 3: Safety & Info
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { viewModel.setTab(3) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 3) Icons.Default.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                                            contentDescription = "Safety"
                                        )
                                    },
                                    label = { Text("Safety") },
                                    modifier = Modifier.testTag("nav_safety")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (userRole) {
                        UserRole.VISITOR_KIOSK -> {
                            RegistrationScreen(viewModel = viewModel, contentPadding = innerPadding)
                        }
                        UserRole.RECEPTIONIST -> {
                            when (currentTab) {
                                0 -> RegistrationScreen(viewModel = viewModel, contentPadding = innerPadding)
                                3 -> SafetyInfoScreen(contentPadding = innerPadding, branch = activeBranch)
                                else -> LogbookScreen(viewModel = viewModel, contentPadding = innerPadding)
                            }
                        }
                        UserRole.ADMIN -> {
                            when (currentTab) {
                                0 -> RegistrationScreen(viewModel = viewModel, contentPadding = innerPadding)
                                1 -> LogbookScreen(viewModel = viewModel, contentPadding = innerPadding)
                                2 -> AdminDashboardScreen(viewModel = viewModel, contentPadding = innerPadding)
                                3 -> SafetyInfoScreen(contentPadding = innerPadding, branch = activeBranch)
                            }
                        }
                    }

                    // Show active visitor pass popup if non-null
                    activePass?.let { visitor ->
                        VisitorPassDialog(
                            visitor = visitor,
                            branchLogoUri = activeBranch.logoUri,
                            countdownSeconds = countdownSec,
                            totalCountdownSeconds = autoResetSec,
                            isKioskMode = isKioskMode,
                            onDismiss = { viewModel.dismissPass() }
                        )
                    }

                    // Role PIN Entry Dialog for role elevation
                    if (showPinDialog) {
                        AdminPinDialog(
                            adminPin = adminPin,
                            receptionistPin = receptionistPin,
                            targetRoleOnly = targetPinRole,
                            onDismiss = { viewModel.dismissPinDialog() },
                            onAdminSuccess = { viewModel.unlockRole(UserRole.ADMIN) },
                            onReceptionistSuccess = { viewModel.unlockRole(UserRole.RECEPTIONIST) }
                        )
                    }

                    // In-App Notification Center Dialog (Receptionist & Admin only)
                    if (showNotificationCenter && userRole != UserRole.VISITOR_KIOSK) {
                        NotificationCenterDialog(
                            notifications = notificationsList,
                            onDismiss = { viewModel.closeNotificationCenter() },
                            onClearAll = { viewModel.clearNotifications() },
                            onMarkAllRead = { viewModel.markAllNotificationsRead() }
                        )
                    }
                }
            }
        }
    }
}

