package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import com.example.data.AppDatabase
import com.example.data.Branch
import com.example.data.FirestoreService
import com.example.data.KioskPreferences
import com.example.data.UserRole
import com.example.repository.VisitorRepository
import com.example.ui.UiEvent
import com.example.ui.VisitorViewModel
import com.example.ui.VisitorViewModelFactory
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.CompanyBrandLogoBadge
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
import com.example.ui.theme.NotificationAlertColor
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.liquidInnerGlass
import com.example.ui.theme.liquidPressEffect
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getInstance(context) }
            val firestoreService = remember { FirestoreService(context) }
            val prefs = remember { KioskPreferences(context) }
            val repository = remember {
                VisitorRepository(
                    visitorDao = database.visitorDao(),
                    firestoreService = firestoreService,
                    kioskPrefs = prefs
                )
            }
            val viewModel: VisitorViewModel = viewModel(
                factory = VisitorViewModelFactory(repository, prefs, firestoreService)
            )

            val themeMode by viewModel.themeMode.collectAsState()
            val screenTimeoutMinutes by viewModel.screenTimeoutMinutes.collectAsState()

            LaunchedEffect(screenTimeoutMinutes) {
                if (screenTimeoutMinutes == KioskPreferences.TIMEOUT_ALWAYS_ON) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                KioskPreferences.THEME_DARK -> true
                KioskPreferences.THEME_SYSTEM -> systemDark
                else -> false
            }

            MyApplicationTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SaitVisitorApp(
                        viewModel = viewModel,
                        isDarkTheme = useDarkTheme
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaitVisitorApp(
    viewModel: VisitorViewModel,
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val newlyCreatedPass by viewModel.activeVisitorPass.collectAsState()
    val allVisitors by viewModel.allVisitorsList.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isKioskMode by viewModel.isKioskMode.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isAdminMode = userRole == UserRole.ADMIN
    val isReceptionistMode = userRole == UserRole.RECEPTIONIST
    val adminPin by viewModel.adminPin.collectAsState()
    val receptionistPin by viewModel.receptionistPin.collectAsState()
    val countdownSec by viewModel.passCountdownSeconds.collectAsState()
    val totalAutoResetSec by viewModel.autoResetSeconds.collectAsState()
    val activeBranch by viewModel.activeBranch.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val pinDialogTargetRole by viewModel.targetPinRole.collectAsState()
    val showNotificationDialog by viewModel.showNotificationCenter.collectAsState()

    val activeBranchVisitorsCount = remember(allVisitors, activeBranch) {
        allVisitors.count { visitor ->
            visitor.branchCode.equals(activeBranch.shortCode, ignoreCase = true) ||
                visitor.branchName.equals(activeBranch.name, ignoreCase = true)
        }
    }

    BackHandler(enabled = selectedTab != 0) {
        viewModel.setTab(0)
    }

    DisposableEffect(Unit) {
        val auth = Firebase.auth
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            viewModel.onAuthUserChanged(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.SendEmailNotification -> {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, event.recipients.toTypedArray())
                            putExtra(Intent.EXTRA_SUBJECT, event.subject)
                            putExtra(Intent.EXTRA_TEXT, event.body)
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Visitor Pass Email"))
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    LaunchedEffect(isKioskMode, isReceptionistMode, isAdminMode) {
        if (isKioskMode && selectedTab > 1) {
            viewModel.setTab(0)
        } else if (isReceptionistMode && selectedTab > 1) {
            viewModel.setTab(0)
        }
    }

    val navItems = remember(isKioskMode, isReceptionistMode, isAdminMode, activeBranchVisitorsCount, allVisitors.size, unsyncedCount, activeBranch.shortCode) {
        when {
            isKioskMode -> listOf(
                LiquidNavTabItem(
                    index = 0,
                    label = "Visitor Check-In",
                    selectedIcon = Icons.Default.HowToReg,
                    unselectedIcon = Icons.Outlined.HowToReg,
                    testTag = "nav_register"
                ),
                LiquidNavTabItem(
                    index = 1,
                    label = "Safety & QHSE",
                    selectedIcon = Icons.Default.HealthAndSafety,
                    unselectedIcon = Icons.Outlined.HealthAndSafety,
                    testTag = "nav_safety"
                )
            )
            isReceptionistMode -> listOf(
                LiquidNavTabItem(
                    index = 0,
                    label = "Manual Entry",
                    selectedIcon = Icons.Default.HowToReg,
                    unselectedIcon = Icons.Outlined.HowToReg,
                    testTag = "nav_register"
                ),
                LiquidNavTabItem(
                    index = 1,
                    label = "${activeBranch.shortCode} Logbook",
                    selectedIcon = Icons.AutoMirrored.Filled.Assignment,
                    unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
                    badgeCount = activeBranchVisitorsCount,
                    testTag = "nav_logbook"
                )
            )
            else -> listOf(
                LiquidNavTabItem(
                    index = 0,
                    label = "Entry",
                    selectedIcon = Icons.Default.HowToReg,
                    unselectedIcon = Icons.Outlined.HowToReg,
                    testTag = "nav_register"
                ),
                LiquidNavTabItem(
                    index = 1,
                    label = "All Logs",
                    selectedIcon = Icons.AutoMirrored.Filled.Assignment,
                    unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
                    badgeCount = allVisitors.size,
                    testTag = "nav_logbook"
                ),
                LiquidNavTabItem(
                    index = 2,
                    label = "Cloud & Admin",
                    selectedIcon = Icons.Default.CloudSync,
                    unselectedIcon = Icons.Outlined.CloudSync,
                    badgeCount = unsyncedCount,
                    badgeIsWarning = unsyncedCount > 0,
                    testTag = "nav_admin"
                )
            )
        }
    }

    LiquidGlassBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LiquidGlassTopAppBar(
                    activeBranch = activeBranch,
                    isKioskMode = isKioskMode,
                    isReceptionistMode = isReceptionistMode,
                    isAdminMode = isAdminMode,
                    isDarkTheme = isDarkTheme,
                    unreadCount = unreadCount,
                    onToggleTheme = {
                        val nextMode = if (isDarkTheme) {
                            KioskPreferences.THEME_LIGHT
                        } else {
                            KioskPreferences.THEME_DARK
                        }
                        viewModel.setThemeMode(nextMode)
                    },
                    onOpenNotifications = {
                        viewModel.markAllNotificationsRead()
                        viewModel.openNotificationCenter()
                    },
                    onEnterKioskMode = {
                        viewModel.enterKioskMode()
                        viewModel.setTab(0)
                    },
                    onOpenPinDialog = { targetRole ->
                        viewModel.requestRoleUnlock(targetRole)
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 84.dp)
                )
            },
            bottomBar = {
                LiquidGlassBottomDock(
                    items = navItems,
                    selectedTab = selectedTab,
                    onSelectTab = { viewModel.setTab(it) }
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = Pair(isKioskMode, selectedTab),
                transitionSpec = {
                    val isForward = targetState.second >= initialState.second
                    val slideSign = if (isForward) 1 else -1
                    (fadeIn(animationSpec = tween(240)) +
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetX = { fullWidth -> (fullWidth * 0.14f * slideSign).toInt() }
                        ) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )).togetherWith(
                        fadeOut(animationSpec = tween(160)) +
                            slideOutHorizontally(
                                animationSpec = tween(180),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.10f * slideSign).toInt() }
                            ) +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(160))
                    )
                },
                label = "main_tab_transition"
            ) { (kiosk, tab) ->
                val adjustedPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                if (kiosk) {
                    when (tab) {
                        0 -> RegistrationScreen(
                            viewModel = viewModel,
                            contentPadding = adjustedPadding,
                            onNavigateToSafety = { viewModel.setTab(1) }
                        )
                        1 -> SafetyInfoScreen(
                            contentPadding = adjustedPadding,
                            branch = activeBranch
                        )
                    }
                } else {
                    when (tab) {
                        0 -> RegistrationScreen(
                            viewModel = viewModel,
                            contentPadding = adjustedPadding,
                            onNavigateToSafety = {}
                        )
                        1 -> LogbookScreen(
                            viewModel = viewModel,
                            contentPadding = adjustedPadding
                        )
                        2 -> if (isAdminMode) {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                contentPadding = adjustedPadding
                            )
                        } else {
                            RegistrationScreen(
                                viewModel = viewModel,
                                contentPadding = adjustedPadding,
                                onNavigateToSafety = {}
                            )
                        }
                    }
                }
            }
        }

        newlyCreatedPass?.let { passVisitor ->
            VisitorPassDialog(
                visitor = passVisitor,
                branchLogoUri = activeBranch.logoUri,
                countdownSeconds = countdownSec,
                totalCountdownSeconds = totalAutoResetSec,
                isKioskMode = isKioskMode,
                onDismiss = { viewModel.dismissPass() },
                onCheckOut = {}
            )
        }

        if (showNotificationDialog) {
            NotificationCenterDialog(
                notifications = notifications,
                onDismiss = { viewModel.closeNotificationCenter() },
                onClearAll = { viewModel.clearNotifications() },
                onMarkAllRead = { viewModel.markAllNotificationsRead() }
            )
        }

        if (showPinDialog) {
            AdminPinDialog(
                adminPin = adminPin,
                receptionistPin = receptionistPin,
                targetRoleOnly = pinDialogTargetRole,
                onDismiss = { viewModel.dismissPinDialog() },
                onAdminSuccess = {
                    viewModel.unlockAdmin()
                },
                onReceptionistSuccess = {
                    viewModel.enterReceptionistMode()
                }
            )
        }
    }
}

@Composable
private fun LiquidGlassTopAppBar(
    activeBranch: Branch,
    isKioskMode: Boolean,
    isReceptionistMode: Boolean,
    isAdminMode: Boolean,
    isDarkTheme: Boolean,
    unreadCount: Int,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    onEnterKioskMode: () -> Unit,
    onOpenPinDialog: (String?) -> Unit
) {
    val themeRotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "themeIconRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(20.dp),
                    cornerRadius = 20.dp,
                    elevated = true,
                    strokeWidth = 1.4.dp,
                    shadowElevation = 6.dp
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Brand Logo + Branch Title + Role Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                CompanyBrandLogoBadge(
                    modifier = Modifier
                        .width(62.dp)
                        .height(28.dp),
                    logoUri = activeBranch.logoUri,
                    branchName = activeBranch.name,
                    testTag = "company_logo_topbar"
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .animateContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeBranch.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val roleBadgeColor = when {
                            isKioskMode -> StatusSuccess
                            isReceptionistMode -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val roleLabel = when {
                            isKioskMode -> "KIOSK"
                            isReceptionistMode -> "RECEPTION"
                            else -> "ADMIN"
                        }
                        Box(
                            modifier = Modifier
                                .liquidInnerGlass(
                                    shape = RoundedCornerShape(6.dp),
                                    cornerRadius = 6.dp,
                                    accentColor = roleBadgeColor
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = roleLabel,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = roleBadgeColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right: Well-spaced Refractive Glass Circle Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TopBarGlassIconButton(
                    icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = if (isDarkTheme) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                    testTag = "appbar_theme_toggle",
                    iconRotation = themeRotation,
                    onClick = onToggleTheme
                )

                if (!isKioskMode) {
                    TopBarGlassIconButton(
                        icon = if (unreadCount > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = if (unreadCount > 0) NotificationAlertColor else MaterialTheme.colorScheme.primary,
                        badgeCount = unreadCount,
                        testTag = "appbar_notifications_btn",
                        onClick = onOpenNotifications
                    )
                }

                if (!isKioskMode && !isReceptionistMode) {
                    TopBarGlassIconButton(
                        icon = Icons.Default.Store,
                        contentDescription = "Switch to Kiosk Mode",
                        tint = MaterialTheme.colorScheme.secondary,
                        testTag = "appbar_launch_kiosk",
                        onClick = onEnterKioskMode
                    )
                }

                TopBarGlassIconButton(
                    icon = when {
                        isKioskMode -> Icons.Default.Lock
                        isReceptionistMode -> Icons.Default.SupportAgent
                        else -> Icons.Default.AdminPanelSettings
                    },
                    contentDescription = "Role Access",
                    tint = MaterialTheme.colorScheme.primary,
                    testTag = "appbar_admin_lock",
                    onClick = {
                        when {
                            isKioskMode -> onOpenPinDialog(null)
                            isReceptionistMode -> onOpenPinDialog("ADMIN")
                            isAdminMode -> onEnterKioskMode()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBarGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    badgeCount: Int = 0,
    iconRotation: Float = 0f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .liquidPressEffect(pressedScale = 0.90f)
            .liquidInnerGlass(
                shape = CircleShape,
                cornerRadius = 16.dp
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    rotationZ = iconRotation
                }
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-2).dp)
                    .defaultMinSize(minWidth = 15.dp, minHeight = 15.dp)
                    .background(NotificationAlertColor, RoundedCornerShape(8.dp))
                    .border(0.7.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 3.5.dp, vertical = 0.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                    color = Color(0xFF051020),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
