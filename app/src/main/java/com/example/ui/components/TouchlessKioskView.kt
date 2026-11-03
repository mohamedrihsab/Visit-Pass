package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.example.data.Branch
import com.example.ui.VisitorViewModel
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.liquidPressEffect
import com.example.util.QrCodeView

/**
 * Touchless Kiosk Station Mode:
 * Visitors do not need to touch the kiosk tablet screen.
 * They scan the QR code with their mobile phone camera,
 * which opens the SAIT Group visitor registration GitHub Page.
 * When submitted, Cloud Firestore syncs in real-time to this tablet,
 * automatically displaying their official visitor pass!
 */
@Composable
fun TouchlessKioskView(
    viewModel: VisitorViewModel,
    activeBranch: Branch,
    onSwitchToTouchscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val touchlessUrl by viewModel.touchlessQrUrl.collectAsState()
    val touchlessSessionId by viewModel.touchlessSessionId.collectAsState()
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showMobileSimulatorDialog by remember { mutableStateOf(false) }
    var showWebPortalPreview by remember { mutableStateOf(false) }

    if (showSafetyDialog) {
        SafetyInfoDialog(
            branch = activeBranch,
            onDismiss = { showSafetyDialog = false }
        )
    }

    if (showWebPortalPreview) {
        MobileWebPortalPreviewDialog(
            touchlessUrl = touchlessUrl,
            onCopyHtml = {
                try {
                    val html = context.assets.open("mobile_portal.html").bufferedReader().use { it.readText() }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("index.html", html))
                    viewModel.showNotificationSnackbar("Updated docs/index.html copied! Paste into your GitHub repo or click 'Push to GitHub'.")
                } catch (e: Exception) {
                    viewModel.showNotificationSnackbar("Could not copy HTML: ${e.message}")
                }
            },
            onOpenExternal = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(touchlessUrl))
                context.startActivity(intent)
            },
            onDismiss = { showWebPortalPreview = false }
        )
    }

    if (showMobileSimulatorDialog) {
        MobileRegistrationSimulatorDialog(
            viewModel = viewModel,
            activeBranch = activeBranch,
            touchlessSessionId = touchlessSessionId,
            onDismiss = { showMobileSimulatorDialog = false }
        )
    }

    // Radar/Pulse animation for live terminal listening status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Allow system Back gesture to return to Touchscreen Kiosk mode
    BackHandler {
        onSwitchToTouchscreen()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("touchless_kiosk_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Corporate Header Card (iOS 26 Liquid Glass Surface)
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            elevated = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Container - Pure white background
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(68.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    shadowElevation = 2.dp
                ) {
                    if (!activeBranch.logoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = activeBranch.logoUri,
                            contentDescription = "${activeBranch.name} Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.sait_logo),
                            contentDescription = "SAIT Group Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = activeBranch.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = activeBranch.displayFacilityLocation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Touchless Mode Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOUCHLESS CHECK-IN STATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center QR Code Hero Card (Direct GitHub Pages — iOS 26 Liquid Glass)
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevated = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan to Register on Your Phone",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Point your smartphone camera at this QR code to open the mobile registration portal. When submitted, your visitor pass will appear instantly on this tablet screen.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // QR Code
                QrCodeView(
                    content = touchlessUrl,
                    size = 240.dp,
                    centerLogoUri = activeBranch.logoUri,
                    centerLabel = activeBranch.shortCode
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Terminal Listening Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(StatusSuccess.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .background(StatusSuccess, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kiosk Ready • Listening for Mobile Check-in",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusSuccess
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Session ID & Action Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terminal Session: $touchlessSessionId",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.refreshTouchlessSession() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh QR Session",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Registration URL", touchlessUrl))
                            viewModel.showNotificationSnackbar("GitHub Pages registration link copied to clipboard.")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { showWebPortalPreview = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Preview Web Form",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showWebPortalPreview = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_preview_updated_portal"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview Updated Form", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val html = context.assets.open("mobile_portal.html").bufferedReader().use { it.readText() }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("index.html", html))
                                viewModel.showNotificationSnackbar("Updated index.html copied! Paste into your GitHub 'Visit-Pass' repo to update mohamedrihsab.github.io/Visit-Pass/")
                            } catch (e: Exception) {
                                viewModel.showNotificationSnackbar("Could not copy HTML: ${e.message}")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_copy_updated_html"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy index.html", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3-Step Touchless Guidance Card (iOS 26 Liquid Glass)
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            elevated = false
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "How Touchless Check-in Works",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                TouchlessStepRow(
                    stepNumber = "1",
                    icon = Icons.Default.QrCode2,
                    title = "Scan QR Code",
                    description = "Open your smartphone camera and scan the code above to open the secure registration portal."
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                TouchlessStepRow(
                    stepNumber = "2",
                    icon = Icons.Default.PhoneAndroid,
                    title = "Fill Details on Phone",
                    description = "Enter your full name, email ID, contact number, company, and host department on your mobile screen."
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                TouchlessStepRow(
                    stepNumber = "3",
                    icon = Icons.Default.CheckCircle,
                    title = "Pass Appears Instantly Here",
                    description = "This kiosk detects your submission in real-time, displays your official pass badge, and emails your copy."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Actions: Safety Guidelines & Fallback to Touchscreen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showSafetyDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .liquidPressEffect()
                    .liquidGlass(shape = RoundedCornerShape(14.dp), cornerRadius = 14.dp, shadowElevation = 4.dp)
                    .testTag("btn_touchless_safety_info"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Safety Rules", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onSwitchToTouchscreen,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .liquidPressEffect()
                    .testTag("btn_switch_to_touchscreen"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Use Touchscreen", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // In-App Mobile Check-In Simulator (Enables instant demo/testing of the phone registration flow)
        FilledTonalButton(
            onClick = { showMobileSimulatorDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .liquidPressEffect()
                .testTag("btn_demo_mobile_registration"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "⚡ Simulate Phone Registration (Instant Demo)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TouchlessStepRow(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Mobile Registration Simulator Dialog:
 * Enables testing the exact visitor submission flow that runs on the mobile phone browser.
 * When submitted, this writes to the repository / Cloud Firestore with the matching session ID,
 * immediately firing the Kiosk's real-time detection and badge pass modal.
 */
@Composable
fun MobileRegistrationSimulatorDialog(
    viewModel: VisitorViewModel,
    activeBranch: Branch,
    touchlessSessionId: String,
    onDismiss: () -> Unit
) {
    var visitorName by remember { mutableStateOf("David Miller") }
    var mobileNum by remember { mutableStateOf("501234567") }
    var emailId by remember { mutableStateOf("david.miller@example.com") }
    var companyName by remember { mutableStateOf("PetroTech International") }
    var visitType by remember { mutableStateOf("Walk-In") }
    var department by remember { mutableStateOf(activeBranch.defaultDepartments.firstOrNull() ?: "PROJECTS") }
    var purpose by remember { mutableStateOf("Meeting") }
    var personToMeet by remember { mutableStateOf("Eng. Tariq Al-Hashimi") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Mobile Phone Registration Test", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Simulating visitor's mobile browser submission",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Binding to Kiosk Session: $touchlessSessionId\nBranch: ${activeBranch.name}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedTextField(
                    value = visitorName,
                    onValueChange = { visitorName = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = emailId,
                    onValueChange = { emailId = it },
                    label = { Text("Email ID *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobileNum,
                    onValueChange = { mobileNum = it },
                    label = { Text("Mobile Number *") },
                    prefix = { Text("${activeBranch.countryCode} ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Text(
                        text = "Type of Visit *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Walk-In", "Appointment").forEach { type ->
                            val selected = visitType == type
                            FilterChip(
                                selected = selected,
                                onClick = { visitType = type },
                                label = {
                                    Text(
                                        text = type,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department to Visit *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose of Visit *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = personToMeet,
                    onValueChange = { personToMeet = it },
                    label = { Text("Person to Meet (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.simulateMobileTouchlessRegistration(
                        fullName = visitorName,
                        mobileNumber = "${activeBranch.countryCode} $mobileNum".trim(),
                        companyName = companyName,
                        visitType = visitType,
                        department = department,
                        purpose = purpose,
                        personToMeet = personToMeet,
                        email = emailId.trim()
                    )
                    onDismiss()
                },
                enabled = !isSubmitting && visitorName.isNotBlank() && emailId.isNotBlank() && mobileNum.isNotBlank()
            ) {
                Text("Submit Mobile Registration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MobileWebPortalPreviewDialog(
    touchlessUrl: String,
    onCopyHtml: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val queryPart = touchlessUrl.substringAfter("?", "")
    val localAssetUrl = if (queryPart.isNotBlank()) {
        "file:///android_asset/mobile_portal.html?$queryPart"
    } else {
        "file:///android_asset/mobile_portal.html"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Touchless Mobile Web Form (docs/index.html)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Push to GitHub so external phones load this latest version",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onCopyHtml,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy HTML", fontSize = 11.sp)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            @Suppress("DEPRECATION")
                            settings.allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            settings.allowUniversalAccessFromFileURLs = true
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            loadUrl(localAssetUrl)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}
