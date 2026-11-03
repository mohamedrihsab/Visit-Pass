package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassBottomDock
import com.example.ui.theme.LiquidNavTabItem
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.liquidPressEffect
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

                // Handle system Back navigation gracefully on non-home tabs
                BackHandler(enabled = userRole != UserRole.VISITOR_KIOSK && currentTab != 0) {
                    viewModel.setTab(0)
                }

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

                LiquidGlassBackground {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            // Floating iOS 26 Liquid Glass Top Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlass(
                                            shape = RoundedCornerShape(26.dp),
                                            cornerRadius = 26.dp,
                                            elevated = true,
                                            strokeWidth = 1.4.dp,
                                            shadowElevation = 12.dp
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Company Logo Pill (Pure White Badge with SAIT Logo)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
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
                                            Image(
                                                painter = painterResource(id = R.drawable.sait_logo),
                                                contentDescription = "SAIT Group Logo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White)
                                                    .padding(4.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = when (userRole) {
                                            UserRole.VISITOR_KIOSK -> activeBranch.name
                                            UserRole.RECEPTIONIST -> if (currentTab == 0) activeBranch.name else "${activeBranch.shortCode} RECEPTION"
                                            UserRole.ADMIN -> if (currentTab == 0) activeBranch.name else "${activeBranch.shortCode} ADMIN"
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Theme Switcher Button (Available across modes for instant Light/Dark Liquid Glass toggle)
                                        IconButton(
                                            onClick = { viewModel.toggleTheme() },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .liquidPressEffect()
                                                .testTag("appbar_theme_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = "Toggle Light/Dark Theme",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Notification Center Bell & Logged Counter: visible in Receptionist and Admin Mode
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
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .liquidPressEffect()
                                                        .testTag("appbar_notifications_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = if (unreadNotifications > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                                        contentDescription = "Notification Center",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            if (totalLoggedCount > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = StatusSuccess.copy(alpha = 0.16f),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$totalLoggedCount Logged",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StatusSuccess,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            if (userRole == UserRole.ADMIN) {
                                                IconButton(
                                                    onClick = { viewModel.enterKioskMode() },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .liquidPressEffect()
                                                        .testTag("appbar_launch_kiosk")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Store,
                                                        contentDescription = "Launch Kiosk",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (userRole == UserRole.RECEPTIONIST) {
                                            IconButton(
                                                onClick = { viewModel.requestRoleUnlock("ADMIN") },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .liquidPressEffect()
                                                    .testTag("appbar_admin_lock")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AdminPanelSettings,
                                                    contentDescription = "Switch to Admin",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else if (userRole == UserRole.VISITOR_KIOSK) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
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
                                                    },
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (userRole == UserRole.RECEPTIONIST) {
                                LiquidGlassBottomDock(
                                    items = listOf(
                                        LiquidNavTabItem(
                                            index = 0,
                                            label = "Manual Entry",
                                            selectedIcon = Icons.Default.EditNote,
                                            unselectedIcon = Icons.Outlined.EditNote,
                                            testTag = "nav_register"
                                        ),
                                        LiquidNavTabItem(
                                            index = 1,
                                            label = "Visitor Logbook",
                                            selectedIcon = Icons.Default.ListAlt,
                                            unselectedIcon = Icons.Default.Assignment,
                                            badgeCount = totalLoggedCount,
                                            testTag = "nav_logbook"
                                        ),
                                        LiquidNavTabItem(
                                            index = 3,
                                            label = "Safety",
                                            selectedIcon = Icons.Default.HealthAndSafety,
                                            unselectedIcon = Icons.Outlined.HealthAndSafety,
                                            testTag = "nav_safety"
                                        )
                                    ),
                                    selectedTab = currentTab,
                                    onSelectTab = { viewModel.setTab(it) }
                                )
                            } else if (userRole == UserRole.ADMIN) {
                                LiquidGlassBottomDock(
                                    items = listOf(
                                        LiquidNavTabItem(
                                            index = 0,
                                            label = "Register",
                                            selectedIcon = Icons.Default.HowToReg,
                                            unselectedIcon = Icons.Outlined.HowToReg,
                                            testTag = "nav_register"
                                        ),
                                        LiquidNavTabItem(
                                            index = 1,
                                            label = "Logbook",
                                            selectedIcon = Icons.Default.ListAlt,
                                            unselectedIcon = Icons.Default.Assignment,
                                            badgeCount = totalLoggedCount,
                                            testTag = "nav_logbook"
                                        ),
                                        LiquidNavTabItem(
                                            index = 2,
                                            label = "Cloud & Admin",
                                            selectedIcon = Icons.Default.CloudDone,
                                            unselectedIcon = Icons.Default.AdminPanelSettings,
                                            badgeCount = unsyncedCount,
                                            badgeIsWarning = true,
                                            testTag = "nav_admin"
                                        ),
                                        LiquidNavTabItem(
                                            index = 3,
                                            label = "Safety",
                                            selectedIcon = Icons.Default.HealthAndSafety,
                                            unselectedIcon = Icons.Outlined.HealthAndSafety,
                                            testTag = "nav_safety"
                                        )
                                    ),
                                    selectedTab = currentTab,
                                    onSelectTab = { viewModel.setTab(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        val screenStateKey = Pair(userRole, currentTab)
                        AnimatedContent(
                            targetState = screenStateKey,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) +
                                    scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )).togetherWith(
                                    fadeOut(animationSpec = tween(160)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(160))
                                )
                            },
                            label = "liquid_screen_transition"
                        ) { (role, tab) ->
                            when (role) {
                                UserRole.VISITOR_KIOSK -> {
                                    RegistrationScreen(viewModel = viewModel, contentPadding = innerPadding)
                                }
                                UserRole.RECEPTIONIST -> {
                                    when (tab) {
                                        0 -> RegistrationScreen(viewModel = viewModel, contentPadding = innerPadding)
                                        3 -> SafetyInfoScreen(contentPadding = innerPadding, branch = activeBranch)
                                        else -> LogbookScreen(viewModel = viewModel, contentPadding = innerPadding)
                                    }
                                }
                                UserRole.ADMIN -> {
                                    when (tab) {
                                        0 -> RegistrationScreen(viewModel = viewModel, contentPadding = innerPadding)
                                        1 -> LogbookScreen(viewModel = viewModel, contentPadding = innerPadding)
                                        2 -> AdminDashboardScreen(viewModel = viewModel, contentPadding = innerPadding)
                                        3 -> SafetyInfoScreen(contentPadding = innerPadding, branch = activeBranch)
                                    }
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
                                onDismiss = { viewModel.dismissPass() },
                                onCheckOut = { id -> viewModel.checkOutVisitor(id) }
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
}
