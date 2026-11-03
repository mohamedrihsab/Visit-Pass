package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.components.SafetyInfoDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.KioskPreferences
import com.example.data.SmtpConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.data.CountryDialOption
import com.example.ui.VisitorViewModel
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.GoogleDriveExportHelper
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: VisitorViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val syncedCount by viewModel.syncedCount.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncingAll.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeBranch by viewModel.activeBranch.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val visitPurposes by viewModel.visitPurposes.collectAsState()
    val departmentEmails by viewModel.departmentEmails.collectAsState()
    val autoResetSec by viewModel.autoResetSeconds.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val smtpConfig by viewModel.smtpConfig.collectAsState()
    val isFirebaseLoggedIn = currentUser != null

    var smtpEnabled by remember(smtpConfig) { mutableStateOf(smtpConfig.enabled) }
    var smtpHost by remember(smtpConfig) { mutableStateOf(smtpConfig.host) }
    var smtpPort by remember(smtpConfig) { mutableStateOf(smtpConfig.port.toString()) }
    var smtpEncryption by remember(smtpConfig) { mutableStateOf(smtpConfig.encryption) }
    var smtpUsername by remember(smtpConfig) { mutableStateOf(smtpConfig.username) }
    var smtpPassword by remember(smtpConfig) { mutableStateOf(smtpConfig.password) }
    var smtpSenderEmail by remember(smtpConfig) { mutableStateOf(smtpConfig.senderEmail) }
    var smtpSenderName by remember(smtpConfig) { mutableStateOf(smtpConfig.senderName) }
    var smtpFirebaseEnabled by remember(smtpConfig) { mutableStateOf(smtpConfig.firebaseTriggerEnabled) }

    var showSmtpPassword by remember { mutableStateOf(false) }
    var testRecipientEmail by remember { mutableStateOf("") }
    var isTestingSmtp by remember { mutableStateOf(false) }
    var smtpTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var showPinChangeDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var showReceptionistPinDialog by remember { mutableStateOf(false) }
    var newReceptionistPinInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    // Dialog states for dynamic branch, department, and purpose of visit management
    var showAddPurposeDialog by remember { mutableStateOf(false) }
    var newPurposeNameInput by remember { mutableStateOf("") }
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    var newBranchShortCode by remember { mutableStateOf("") }
    var newBranchCity by remember { mutableStateOf("") }
    var newBranchSubtitle by remember { mutableStateOf("") }
    var newBranchCountryCode by remember { mutableStateOf("+971") }
    var newBranchFacilityLocation by remember { mutableStateOf("") }

    var editingBranchDetails by remember { mutableStateOf<Branch?>(null) }
    var editBranchName by remember { mutableStateOf("") }
    var editBranchShortCode by remember { mutableStateOf("") }
    var editBranchCity by remember { mutableStateOf("") }
    var editBranchSubtitle by remember { mutableStateOf("") }
    var editBranchCountryCode by remember { mutableStateOf("+971") }
    var editBranchFacilityLocation by remember { mutableStateOf("") }

    var branchToDelete by remember { mutableStateOf<Branch?>(null) }
    var branchToAddDept by remember { mutableStateOf<Branch?>(null) }
    var newDeptNameInput by remember { mutableStateOf("") }
    var selectedEmailBranchId by remember { mutableStateOf(activeBranch.id) }

    // Helper to persist Photo Picker image URIs to internal storage (preserving PNG/WEBP transparency)
    fun persistPickedImageUri(uri: android.net.Uri, prefix: String): String {
        return try {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val ext = when {
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val outFile = java.io.File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            uri.toString()
        }
    }

    // Logo & Safety Information management state
    var selectedBranchForLogo by remember { mutableStateOf<Branch?>(null) }
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedBranchForLogo?.let { b ->
            if (uri != null) {
                val persistedPath = persistPickedImageUri(uri, "logo_${b.id}")
                viewModel.updateBranchLogo(b.id, persistedPath)
            }
        }
        selectedBranchForLogo = null
    }

    var editingSafetyBranch by remember { mutableStateOf<Branch?>(null) }
    var safetyGuidelinesInput by remember { mutableStateOf("") }
    var safetyHelplineInput by remember { mutableStateOf("") }
    var safetyContactNameInput by remember { mutableStateOf("") }
    var safetyFacilityLocationInput by remember { mutableStateOf("") }
    var safetyExitDrawingUri by remember { mutableStateOf<String?>(null) }
    var previewSafetyBranch by remember { mutableStateOf<Branch?>(null) }
    val exitPlanPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val branchId = editingSafetyBranch?.id ?: "branch"
            safetyExitDrawingUri = persistPickedImageUri(uri, "exit_plan_$branchId")
        }
    }

    // Email edit dialog state
    var emailEditBranchDept by remember { mutableStateOf<Pair<Branch, String>?>(null) }
    var emailEditInput by remember { mutableStateOf("") }

    fun signInWithGoogle() {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e("AdminAuth", "default_web_client_id not found", e)
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        isSigningIn = true
        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    viewModel.onAuthUserChanged(Firebase.auth.currentUser)
                    viewModel.syncAllPending(silent = false)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("AdminAuth", "Sign-in cancelled: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("AdminAuth", "Sign-in failed", e)
            } finally {
                isSigningIn = false
            }
        }
    }

    fun signOut() {
        Firebase.auth.signOut()
        viewModel.onAuthUserChanged(null)
        coroutineScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AdminAuth", "Sign out error", e)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner with Role Mode Switchers
        item(key = "top_banner_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                if (!activeBranch.logoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeBranch.logoUri,
                                        contentDescription = "${activeBranch.name} Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                            .padding(4.dp),
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
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SAIT Group Admin Portal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Terminal Role & Multi-Branch Control",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Admin Mode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Put this tablet into one of the 3 user access modes:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.enterReceptionistMode() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_switch_to_receptionist")
                        ) {
                            Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Receptionist Mode", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.enterKioskMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_switch_to_kiosk")
                        ) {
                            Icon(imageVector = Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Kiosk", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Active Branch Selection Card
        item(key = "active_branch_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Active Device Branch",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Current: ${activeBranch.name} (${activeBranch.countryCode})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Select which branch or company this kiosk/tablet terminal represents. Switching branch automatically updates the pass code prefix, facility location, and default mobile country code.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        branches.forEach { branch ->
                            val isSelected = branch.id == activeBranch.id || branch.name == activeBranch.name
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                tonalElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isFirebaseLoggedIn) { viewModel.setBranch(branch) }
                                    .testTag("branch_item_${branch.shortCode}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = branch.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Code: ${branch.shortCode} • Dial: ${branch.countryCode} • ${branch.defaultDepartments.size} Depts",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ) {
                                        Text(
                                            text = branch.city,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Appearance & Display Theme Card
        item(key = "theme_settings_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_theme_settings"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Appearance & Theme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Light & Dark Mode Color Palette",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose the display theme for this kiosk terminal. High-contrast colors ensure status bar and notification panel icons remain prominently visible in both light and dark environments.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val themeOptions = listOf(
                        Triple(KioskPreferences.THEME_LIGHT, "Light Theme (Corporate Crisp)", Icons.Default.LightMode),
                        Triple(KioskPreferences.THEME_DARK, "Dark Theme (Midnight Obsidian)", Icons.Default.DarkMode),
                        Triple(KioskPreferences.THEME_SYSTEM, "System Default", Icons.Default.BrightnessAuto)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themeOptions.forEach { (mode, label, icon) ->
                            val isSelected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                tonalElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isFirebaseLoggedIn) { viewModel.setThemeMode(mode) }
                                    .testTag("theme_option_$mode")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = label,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (mode == KioskPreferences.THEME_LIGHT) "Recommended for daylight reception desks"
                                                else if (mode == KioskPreferences.THEME_DARK) "Deep obsidian palette with vivid cyan accents"
                                                else "Follows system Android OS setting",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Firebase Cloud Firestore Authentication & Database Card
        item(key = "firebase_auth_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isFirebaseLoggedIn) StatusSuccess else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firebase Cloud Firestore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Connection Status Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFirebaseLoggedIn) StatusSuccess.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isFirebaseLoggedIn) StatusSuccess else StatusWarning)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFirebaseLoggedIn) "Connected & Unlocked" else "Login Required to Edit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirebaseLoggedIn) StatusSuccess else StatusWarning
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time Multi-Device Sync: Every admin edit requires Firebase login and is synchronized instantly across all kiosk tablets and receptionist/admin terminals via Cloud Firestore.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account Status or Sign-In Button
                    if (isFirebaseLoggedIn) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = currentUser?.displayName ?: "Reception Admin",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = currentUser?.email ?: "",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(onClick = { signOut() }) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign Out", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusWarning.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = StatusWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Admin editing is locked. Sign in with Google below to unlock all Admin edits and sync changes to Firebase.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { signInWithGoogle() },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_google_signin"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signing in...")
                            } else {
                                Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Google (Unlock Admin Edits & Sync)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("In Cloud Firestore", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$syncedCount Records", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Pending Sync", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$unsyncedCount Records", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (unsyncedCount > 0) StatusWarning else MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sync All Button
                    Button(
                        onClick = { viewModel.syncAllPending() },
                        enabled = !isSyncing && isFirebaseLoggedIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_sync_firestore"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing with Firestore...", fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Pending & Settings to Firestore", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Branch & Department Management Header Card (Flattened for Smooth LazyColumn Scrolling)
        item(key = "branch_mgmt_header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Branches, Logos & Safety Info",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFirebaseLoggedIn) "Cloud Synced & Unlocked for Edits" else "Sign in with Google above to edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isFirebaseLoggedIn) StatusSuccess else StatusWarning
                                )
                            }
                        }

                        Button(
                            onClick = {
                                newBranchName = ""
                                newBranchShortCode = ""
                                newBranchCity = ""
                                newBranchSubtitle = ""
                                newBranchCountryCode = "+971"
                                newBranchFacilityLocation = ""
                                showAddBranchDialog = true
                            },
                            enabled = isFirebaseLoggedIn,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_add_branch")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Branch", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Manage company details, default mobile country code, facility location text, company logos, safety guidelines, emergency helplines & contact person, and evacuation plans for each branch.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Individual Branch Cards as Separate LazyColumn Items for Smooth 60fps Scrolling
        items(
            items = branches,
            key = { branch -> "branch_card_${branch.id}" }
        ) { branch ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = branch.shortCode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = branch.countryCode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${branch.city} • ${branch.subtitle}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Facility Location: ${branch.displayFacilityLocation}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit Branch Details button
                            OutlinedButton(
                                onClick = {
                                    editingBranchDetails = branch
                                    editBranchName = branch.name
                                    editBranchShortCode = branch.shortCode
                                    editBranchCity = branch.city
                                    editBranchSubtitle = branch.subtitle
                                    editBranchCountryCode = branch.countryCode
                                    editBranchFacilityLocation = branch.displayFacilityLocation
                                },
                                enabled = isFirebaseLoggedIn,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("btn_edit_branch_${branch.shortCode}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Branch Details",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Branch", fontSize = 11.sp)
                            }

                            // Delete Branch button (if more than 1)
                            if (branches.size > 1) {
                                IconButton(
                                    onClick = { branchToDelete = branch },
                                    enabled = isFirebaseLoggedIn,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Branch",
                                        tint = if (isFirebaseLoggedIn) StatusError else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Departments (${branch.defaultDepartments.size}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        branch.defaultDepartments.forEach { dept ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = dept, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    if (branch.defaultDepartments.size > 1 && isFirebaseLoggedIn) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.removeDepartment(branch.id, dept) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove department", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }

                        // Add department button chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFirebaseLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(enabled = isFirebaseLoggedIn) {
                                branchToAddDept = branch
                                newDeptNameInput = ""
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isFirebaseLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Dept",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirebaseLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Company Logo Management (Always Solid White Background)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            ) {
                                if (!branch.logoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = branch.logoUri,
                                        contentDescription = "${branch.name} Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                            .padding(2.dp),
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
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Company Logo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (branch.logoUri != null) "Custom Logo Active (PNG / WEBP / JPG)" else "Preferred: PNG (transparent) / WEBP / JPG",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedBranchForLogo = branch
                                    logoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = isFirebaseLoggedIn,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_upload_logo_${branch.shortCode}")
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (branch.logoUri != null) "Change" else "Upload Logo", fontSize = 11.sp)
                            }

                            if (branch.logoUri != null) {
                                OutlinedButton(
                                    onClick = { viewModel.updateBranchLogo(branch.id, null) },
                                    enabled = isFirebaseLoggedIn,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_remove_logo_${branch.shortCode}")
                                ) {
                                    Text("Remove", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Safety Information & Emergency Exit Evacuation Plan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Safety & Emergency Info",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = buildString {
                                    append("Helpline: ")
                                    append(branch.safetyHelpline.ifBlank { "999 / Reception Security" })
                                    if (branch.safetyContactName.isNotBlank()) {
                                        append(" • ${branch.safetyContactName}")
                                    }
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    previewSafetyBranch = branch
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_preview_safety_${branch.shortCode}")
                            ) {
                                Text("Preview", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    editingSafetyBranch = branch
                                    safetyGuidelinesInput = branch.safetyGuidelines
                                    safetyHelplineInput = branch.safetyHelpline
                                    safetyContactNameInput = branch.safetyContactName
                                    safetyFacilityLocationInput = branch.displayFacilityLocation
                                    safetyExitDrawingUri = branch.emergencyExitDrawingUri
                                },
                                enabled = isFirebaseLoggedIn,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_edit_safety_${branch.shortCode}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Safety", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Purpose of Visit Options Management Card
        item(key = "visit_purposes_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_visit_purposes"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Purpose of Visit Options",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Registration Form Purpose Selector (${visitPurposes.size} Active)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                newPurposeNameInput = ""
                                showAddPurposeDialog = true
                            },
                            enabled = isFirebaseLoggedIn,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_add_visit_purpose")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Purpose", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Customize the Purpose of Visit options shown on the Visitor Registration screen. Changes are synced across all terminals via Cloud Firestore.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        visitPurposes.forEach { purpose ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = purpose,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (visitPurposes.size > 1 && isFirebaseLoggedIn) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.removeVisitPurpose(purpose) },
                                            modifier = Modifier
                                                .size(20.dp)
                                                .testTag("btn_remove_purpose_${purpose.replace(" ", "_")}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove $purpose",
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quick "+ Purpose" chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFirebaseLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable(enabled = isFirebaseLoggedIn) {
                                    newPurposeNameInput = ""
                                    showAddPurposeDialog = true
                                }
                                .testTag("chip_add_visit_purpose")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isFirebaseLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Add Option",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirebaseLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Automated Department Email Notification Groups Card
        item(key = "email_alerts_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Automated Email Alerts",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Per Department & Company Recipients",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "When a visitor successfully checks in at any kiosk, an automated email notification is dispatched to the group of email addresses defined for that department.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Branch Tabs / Filter (State hoisted to top of AdminDashboardScreen)
                    val currentSelectedBranch = branches.find { it.id == selectedEmailBranchId } ?: branches.first()

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        branches.forEach { branch ->
                            val isSelected = branch.id == currentSelectedBranch.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedEmailBranchId = branch.id },
                                label = { Text(branch.shortCode, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Department Email Directory for: ${currentSelectedBranch.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentSelectedBranch.defaultDepartments.forEach { dept ->
                            val configuredEmails = viewModel.getEmailsForDepartment(currentSelectedBranch, dept)
                            val key = "${currentSelectedBranch.id}::${dept}"
                            val customConfigured = departmentEmails[key]

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = dept, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (customConfigured != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = StatusSuccess.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Custom",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StatusSuccess,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = configuredEmails.joinToString(", "),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            emailEditBranchDept = Pair(currentSelectedBranch, dept)
                                            emailEditInput = customConfigured ?: configuredEmails.joinToString(", ")
                                        },
                                        enabled = isFirebaseLoggedIn,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Outgoing Mail Server (SMTP) & Automated Delivery Engine Card
        item(key = "smtp_settings_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_smtp_settings"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Outgoing Mail Server (SMTP)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automated Dispatch via SMTP or Firebase",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Configure automated email dispatch when visitors register. You can deliver automatically via Google Firebase Trigger Email extension or configure a custom corporate SMTP outgoing mail server.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Educational Callout: What is Firebase Trigger Email vs Direct SMTP
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Understanding Email Delivery Options",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Firebase Trigger Email requires installing the 'firestore-send-email' extension in your Google Firebase Console (requires Blaze billing). Without that extension, documents in 'mail' are logged in Firestore but not emailed.\n• Direct Outgoing SMTP Server (Recommended): Connects directly to Gmail, Office 365, or company SMTP to deliver emails automatically in the background.\n• If SMTP is not yet configured, the app will automatically open your device email app pre-filled so no alerts are missed.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option A: Firebase Cloud Email Delivery
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Google Firebase Trigger Email",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StatusSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Firestore 'mail'",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Automatically writes pass documents into Firestore 'mail' collection for delivery via Firebase Trigger Email extension.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Switch(
                                checked = smtpFirebaseEnabled,
                                onCheckedChange = {
                                    smtpFirebaseEnabled = it
                                    val updated = smtpConfig.copy(firebaseTriggerEnabled = it)
                                    viewModel.updateSmtpConfig(updated)
                                },
                                modifier = Modifier.testTag("switch_firebase_email")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option B: Outgoing Mail Server (SMTP) Custom Configuration
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Custom Outgoing Mail Server (SMTP)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Direct SMTP delivery using corporate exchange, Gmail, Office 365, or local relay.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Switch(
                                    checked = smtpEnabled,
                                    onCheckedChange = {
                                        smtpEnabled = it
                                        val updated = smtpConfig.copy(enabled = it)
                                        viewModel.updateSmtpConfig(updated)
                                    },
                                    modifier = Modifier.testTag("switch_smtp_enabled")
                                )
                            }

                            AnimatedVisibility(visible = smtpEnabled) {
                                Column(
                                    modifier = Modifier.padding(top = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Quick Provider Presets
                                    Text(
                                        text = "Quick Setup Presets (One-Tap):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        FilterChip(
                                            selected = smtpHost == "smtp.gmail.com",
                                            onClick = {
                                                smtpHost = "smtp.gmail.com"
                                                smtpPort = "587"
                                                smtpEncryption = "STARTTLS"
                                                if (smtpSenderName.isBlank()) smtpSenderName = "SAIT Group Visitor Alert"
                                            },
                                            label = { Text("Gmail", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        FilterChip(
                                            selected = smtpHost == "smtp.office365.com",
                                            onClick = {
                                                smtpHost = "smtp.office365.com"
                                                smtpPort = "587"
                                                smtpEncryption = "STARTTLS"
                                                if (smtpSenderName.isBlank()) smtpSenderName = "SAIT Group Visitor Alert"
                                            },
                                            label = { Text("Office 365", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        FilterChip(
                                            selected = smtpHost == "mail.saitgroup.com",
                                            onClick = {
                                                smtpHost = "mail.saitgroup.com"
                                                smtpPort = "587"
                                                smtpEncryption = "STARTTLS"
                                                if (smtpSenderName.isBlank()) smtpSenderName = "SAIT Group Visitor Alert"
                                            },
                                            label = { Text("Corporate SMTP", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }

                                    Text(
                                        text = if (smtpHost == "smtp.gmail.com") "Tip: For Gmail, use a 16-character 'App Password' generated at myaccount.google.com/apppasswords" else "Enter your mail credentials below and click 'Send Test' to verify.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Host & Port
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = smtpHost,
                                            onValueChange = { smtpHost = it },
                                            label = { Text("SMTP Host", fontSize = 12.sp) },
                                            placeholder = { Text("smtp.gmail.com", fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(2.2f)
                                                .testTag("input_smtp_host")
                                        )

                                        OutlinedTextField(
                                            value = smtpPort,
                                            onValueChange = { smtpPort = it },
                                            label = { Text("Port", fontSize = 12.sp) },
                                            placeholder = { Text("587", fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_smtp_port")
                                        )
                                    }

                                    // Encryption selection
                                    Column {
                                        Text(
                                            text = "Security / Encryption Type",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("STARTTLS" to "587", "SSL" to "465", "NONE" to "25").forEach { (enc, defaultPort) ->
                                                val isSelected = smtpEncryption.equals(enc, ignoreCase = true)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        smtpEncryption = enc
                                                        if (smtpPort.isBlank() || smtpPort in listOf("587", "465", "25")) {
                                                            smtpPort = defaultPort
                                                        }
                                                    },
                                                    label = { Text(enc, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Username / Email
                                    OutlinedTextField(
                                        value = smtpUsername,
                                        onValueChange = { smtpUsername = it },
                                        label = { Text("SMTP Username / Auth Email", fontSize = 12.sp) },
                                        placeholder = { Text("kiosk-alerts@saitgroup.com", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_smtp_username")
                                    )

                                    // Password (with visibility toggle)
                                    OutlinedTextField(
                                        value = smtpPassword,
                                        onValueChange = { smtpPassword = it },
                                        label = { Text("SMTP Password / App Password", fontSize = 12.sp) },
                                        placeholder = { Text("••••••••••••", fontSize = 12.sp) },
                                        visualTransformation = if (showSmtpPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showSmtpPassword = !showSmtpPassword }) {
                                                Icon(
                                                    imageVector = if (showSmtpPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showSmtpPassword) "Hide password" else "Show password",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_smtp_password")
                                    )

                                    // Sender Email & Name
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = smtpSenderEmail,
                                            onValueChange = { smtpSenderEmail = it },
                                            label = { Text("Sender Email Address", fontSize = 12.sp) },
                                            placeholder = { Text("no-reply@saitgroup.com", fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .testTag("input_smtp_sender_email")
                                        )

                                        OutlinedTextField(
                                            value = smtpSenderName,
                                            onValueChange = { smtpSenderName = it },
                                            label = { Text("Display Name", fontSize = 12.sp) },
                                            placeholder = { Text("SAIT Visitor Pass", fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .testTag("input_smtp_sender_name")
                                        )
                                    }

                                    // Save Button
                                    Button(
                                        onClick = {
                                            val portNum = smtpPort.toIntOrNull() ?: 587
                                            val newConfig = SmtpConfig(
                                                enabled = smtpEnabled,
                                                host = smtpHost.trim(),
                                                port = portNum,
                                                encryption = smtpEncryption,
                                                username = smtpUsername.trim(),
                                                password = smtpPassword,
                                                senderEmail = smtpSenderEmail.trim(),
                                                senderName = smtpSenderName.trim(),
                                                firebaseTriggerEnabled = smtpFirebaseEnabled
                                            )
                                            viewModel.updateSmtpConfig(newConfig)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("btn_save_smtp_config")
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save SMTP Mail Server Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Test Connection Section
                                    Text(
                                        text = "Verify Outgoing Server Connection",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = testRecipientEmail,
                                            onValueChange = { testRecipientEmail = it },
                                            label = { Text("Test Recipient Email", fontSize = 12.sp) },
                                            placeholder = { Text("admin@saitgroup.com", fontSize = 12.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_test_recipient_email")
                                        )

                                        Button(
                                            onClick = {
                                                if (testRecipientEmail.isBlank()) return@Button
                                                isTestingSmtp = true
                                                smtpTestResult = null
                                                coroutineScope.launch {
                                                    val portNum = smtpPort.toIntOrNull() ?: 587
                                                    val tempConfig = SmtpConfig(
                                                        enabled = true,
                                                        host = smtpHost.trim(),
                                                        port = portNum,
                                                        encryption = smtpEncryption,
                                                        username = smtpUsername.trim(),
                                                        password = smtpPassword,
                                                        senderEmail = smtpSenderEmail.trim(),
                                                        senderName = smtpSenderName.trim(),
                                                        firebaseTriggerEnabled = smtpFirebaseEnabled
                                                    )
                                                    viewModel.updateSmtpConfig(tempConfig)

                                                    val res = viewModel.testSmtpConnection(testRecipientEmail.trim())
                                                    isTestingSmtp = false
                                                    smtpTestResult = if (res.isSuccess) {
                                                        Pair(true, res.getOrNull() ?: "Test email delivered successfully!")
                                                    } else {
                                                        Pair(false, res.exceptionOrNull()?.message ?: "SMTP connection failed")
                                                    }
                                                }
                                            },
                                            enabled = !isTestingSmtp && testRecipientEmail.isNotBlank(),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(52.dp)
                                                .testTag("btn_test_smtp_connection")
                                        ) {
                                            if (isTestingSmtp) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Send Test", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Test Result Banner
                                    smtpTestResult?.let { (success, message) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (success) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("smtp_test_result_banner")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (success) StatusSuccess else StatusError,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = message,
                                                    fontSize = 12.sp,
                                                    color = if (success) StatusSuccess else StatusError,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Drive Storage & Cloud Backup Card
        item(key = "google_drive_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderShared,
                            contentDescription = null,
                            tint = Color(0xFF0F9D58),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Data to Google Drive",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Store all visitor log entries directly in your Google Drive as a structured CSV document. Creates an instant, cost-free backup accessible from Google Sheets on any device.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val csvContent = viewModel.getCsvData()
                            GoogleDriveExportHelper.shareToGoogleDrive(context, csvContent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_save_to_google_drive"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                    ) {
                        Icon(imageVector = Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Logbook to Google Drive", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Touchless Kiosk & GitHub Pages Web Registration Configuration
        item(key = "touchless_kiosk_settings_card") {
            val isTouchlessMode by viewModel.isTouchlessKioskMode.collectAsState()
            val touchlessUrl by viewModel.touchlessQrUrl.collectAsState()
            val githubBaseUrl by viewModel.touchlessGithubUrl.collectAsState()
            var editingGithubUrl by remember(githubBaseUrl) { mutableStateOf(githubBaseUrl) }
            var showAdminWebPortalPreview by remember { mutableStateOf(false) }

            if (showAdminWebPortalPreview) {
                com.example.ui.components.MobileWebPortalPreviewDialog(
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
                    onDismiss = { showAdminWebPortalPreview = false }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Touchless Mobile Check-In (QR)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Allow visitors to register from their smartphone",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Default Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Kiosk in Touchless Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "When enabled, the Kiosk displays the large camera-scannable QR code by default.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTouchlessMode,
                            onCheckedChange = { viewModel.setTouchlessKioskMode(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Pass Sequence Counter Management
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sequential Pass Numbering", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Passes increment sequentially (e.g., ${activeBranch.shortCode}-260001, 0002). Tap to reset counter.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.resetPassSequenceCounter() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_reset_pass_sequence")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset to 0001", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // GitHub Pages URL Settings
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "GitHub Pages Hosted Web Portal URL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "The QR code points directly to this live URL with branch & session parameters. When visitors submit the form on their phone, Cloud Firestore syncs in real-time to this tablet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = editingGithubUrl,
                            onValueChange = { editingGithubUrl = it },
                            label = { Text("GitHub Pages URL") },
                            placeholder = { Text("https://<username>.github.io/<repo>/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateTouchlessGithubUrl(editingGithubUrl) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save GitHub URL", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    editingGithubUrl = KioskPreferences.DEFAULT_GITHUB_PAGES_URL
                                    viewModel.updateTouchlessGithubUrl(editingGithubUrl)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Default", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = { showAdminWebPortalPreview = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview Form", fontSize = 12.sp)
                            }
                        }
                    }

                    // Live QR Preview
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Live Dynamic QR Code Preview (${activeBranch.shortCode})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            com.example.util.QrCodeView(
                                content = touchlessUrl,
                                size = 160.dp,
                                centerLogoUri = activeBranch.logoUri,
                                centerLabel = activeBranch.shortCode
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = touchlessUrl,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // GitHub Pages 1-Minute Setup Guide
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🚀 Important: Syncing Form Changes to GitHub Pages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. The updated web portal code (with Type of Visit & no Pass Number on confirmation) is saved in /docs/index.html and /index.html.\n" +
                                        "2. Because GitHub Pages (mohamedrihsab.github.io) is hosted externally on GitHub, click 'Push to GitHub' in AI Studio (or tap 'Copy Updated HTML' below and paste into your GitHub repo's index.html) so your live GitHub Pages site updates.\n" +
                                        "3. Tap 'Preview Form' above at any time to preview the updated form directly inside the app.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val html = context.assets.open("mobile_portal.html").bufferedReader().use { it.readText() }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("index.html", html))
                                        viewModel.showNotificationSnackbar("Updated docs/index.html copied to clipboard!")
                                    } catch (e: Exception) {
                                        viewModel.showNotificationSnackbar("Could not copy HTML: ${e.message}")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Updated index.html Code for GitHub", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Kiosk Configuration & Security Settings
        item(key = "kiosk_security_settings_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Kiosk Terminal & Security Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Admin PIN Setting - Masked, no default password displayed!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Admin PIN", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Security Status: Protected (••••)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedButton(
                            onClick = {
                                newPinInput = ""
                                showPinChangeDialog = true
                            },
                            enabled = isFirebaseLoggedIn,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_change_pin")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change PIN", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Receptionist PIN Setting - Masked, no default password displayed!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Receptionist PIN", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Security Status: Protected (••••)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedButton(
                            onClick = {
                                newReceptionistPinInput = ""
                                showReceptionistPinDialog = true
                            },
                            enabled = isFirebaseLoggedIn,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_change_receptionist_pin")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change PIN", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Auto-Reset Timer for Kiosk
                    Column {
                        Text("Kiosk Auto-Reset Timer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Duration the visitor pass is shown on screen before resetting to a blank form for the next visitor.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 30).forEach { seconds ->
                                val selected = autoResetSec == seconds
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.updateAutoResetSeconds(seconds) },
                                    enabled = isFirebaseLoggedIn,
                                    label = { Text("${seconds}s", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Clear records
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        enabled = isFirebaseLoggedIn,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_clear_all_records"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Local Visitor Records")
                    }
                }
            }
        }
    }

    // Change Admin PIN Dialog (Masked with PasswordVisualTransformation, 4 to 6 digits)
    if (showPinChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = { Text("Change Admin PIN") },
            text = {
                Column {
                    Text("Enter a new 4 to 6-digit PIN for receptionist and admin mode access:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("4 to 6-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length in 4..6) {
                            viewModel.updateAdminPin(newPinInput)
                            showPinChangeDialog = false
                        }
                    },
                    enabled = newPinInput.length in 4..6
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change Receptionist PIN Dialog (Masked with PasswordVisualTransformation, 4 to 6 digits)
    if (showReceptionistPinDialog) {
        AlertDialog(
            onDismissRequest = { showReceptionistPinDialog = false },
            title = { Text("Change Receptionist PIN") },
            text = {
                Column {
                    Text("Enter a new 4 to 6-digit PIN for reception desk access (manual entry & visitor logbook only):")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newReceptionistPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newReceptionistPinInput = it },
                        label = { Text("4 to 6-digit Receptionist PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newReceptionistPinInput.length in 4..6) {
                            viewModel.updateReceptionistPin(newReceptionistPinInput)
                            showReceptionistPinDialog = false
                        }
                    },
                    enabled = newReceptionistPinInput.length in 4..6
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReceptionistPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Branch Dialog
    if (showAddBranchDialog) {
        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("Add New Branch / Company") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Define branch details, country from dropdown, and facility location:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("Company / Branch Name *") },
                        placeholder = { Text("e.g. SAIT Marine - Sharjah") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBranchShortCode,
                        onValueChange = { newBranchShortCode = it.uppercase() },
                        label = { Text("Short Code *") },
                        placeholder = { Text("SAIT-SHJ") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // All-Countries Dropdown List
                    BranchCountryDropdownField(
                        selectedDialCode = newBranchCountryCode,
                        onCountrySelected = { option ->
                            newBranchCountryCode = option.dialCode
                            if (newBranchCity.isBlank()) {
                                newBranchCity = option.countryName
                            }
                        },
                        testTag = "input_add_branch_country_code"
                    )

                    OutlinedTextField(
                        value = newBranchCity,
                        onValueChange = {
                            newBranchCity = it
                            newBranchCountryCode = BranchRegistry.inferCountryCode(it, newBranchName)
                        },
                        label = { Text("City / Country *") },
                        placeholder = { Text("e.g. Sharjah / Riyadh / Doha") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBranchSubtitle,
                        onValueChange = { newBranchSubtitle = it },
                        label = { Text("Subtitle / Facility Type") },
                        placeholder = { Text("e.g. Marine Engineering Yard") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBranchFacilityLocation,
                        onValueChange = { newBranchFacilityLocation = it },
                        label = { Text("Facility Location Text") },
                        placeholder = { Text("e.g. Sharjah • Marine Engineering Yard") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_add_branch_facility_location")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBranchName.isNotBlank() && newBranchShortCode.isNotBlank() && newBranchCity.isNotBlank()) {
                            viewModel.addBranch(
                                name = newBranchName,
                                shortCode = newBranchShortCode,
                                city = newBranchCity,
                                subtitle = newBranchSubtitle,
                                initialDepartments = listOf("PROJECTS", "QHSE", "OPERATIONS", "MANAGEMENT", "FINANCE"),
                                countryCode = newBranchCountryCode.ifBlank { "+971" },
                                facilityLocation = newBranchFacilityLocation
                            )
                            showAddBranchDialog = false
                        }
                    },
                    enabled = isFirebaseLoggedIn && newBranchName.isNotBlank() && newBranchShortCode.isNotBlank() && newBranchCity.isNotBlank()
                ) {
                    Text("Create Branch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBranchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Branch Details Dialog
    editingBranchDetails?.let { branch ->
        AlertDialog(
            onDismissRequest = { editingBranchDetails = null },
            title = { Text("Edit Branch Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Update company name, pass serial prefix-city code, country from dropdown, city, and facility location:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editBranchName,
                        onValueChange = { editBranchName = it },
                        label = { Text("Company / Branch Name *") },
                        placeholder = { Text("e.g. Sait FZCO - Dubai") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_branch_name")
                    )
                    OutlinedTextField(
                        value = editBranchShortCode,
                        onValueChange = { editBranchShortCode = it.uppercase() },
                        label = { Text("Short Code *") },
                        placeholder = { Text("SAIT-DXB") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_branch_code")
                    )

                    // All-Countries Dropdown List
                    BranchCountryDropdownField(
                        selectedDialCode = editBranchCountryCode,
                        onCountrySelected = { option ->
                            editBranchCountryCode = option.dialCode
                        },
                        testTag = "input_edit_branch_country_code"
                    )

                    OutlinedTextField(
                        value = editBranchCity,
                        onValueChange = { editBranchCity = it },
                        label = { Text("Emirate / City *") },
                        placeholder = { Text("e.g. Dubai") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_branch_city")
                    )
                    OutlinedTextField(
                        value = editBranchSubtitle,
                        onValueChange = { editBranchSubtitle = it },
                        label = { Text("Subtitle / Facility Description") },
                        placeholder = { Text("e.g. Industrial Free Zone Facility") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_branch_subtitle")
                    )
                    OutlinedTextField(
                        value = editBranchFacilityLocation,
                        onValueChange = { editBranchFacilityLocation = it },
                        label = { Text("Facility Location Text") },
                        placeholder = { Text("e.g. Jebel Ali Free Zone, Plot S204, Dubai, UAE") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_branch_facility_location")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editBranchName.isNotBlank() && editBranchShortCode.isNotBlank() && editBranchCity.isNotBlank()) {
                            viewModel.updateBranchDetails(
                                branchId = branch.id,
                                name = editBranchName,
                                shortCode = editBranchShortCode,
                                city = editBranchCity,
                                subtitle = editBranchSubtitle,
                                countryCode = editBranchCountryCode.ifBlank { "+971" },
                                facilityLocation = editBranchFacilityLocation
                            )
                            editingBranchDetails = null
                        }
                    },
                    enabled = isFirebaseLoggedIn && editBranchName.isNotBlank() && editBranchShortCode.isNotBlank() && editBranchCity.isNotBlank(),
                    modifier = Modifier.testTag("btn_save_branch_details")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBranchDetails = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Branch Confirmation Dialog
    branchToDelete?.let { branch ->
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusError) },
            title = { Text("Remove Branch?") },
            text = {
                Text("Are you sure you want to remove '${branch.name}' (${branch.shortCode})? This will update cloud configuration for all group kiosks.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeBranch(branch.id)
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { branchToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Department Dialog
    branchToAddDept?.let { branch ->
        AlertDialog(
            onDismissRequest = { branchToAddDept = null },
            title = { Text("Add Department") },
            text = {
                Column {
                    Text("Add a new department to ${branch.name}:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDeptNameInput,
                        onValueChange = { newDeptNameInput = it.uppercase() },
                        label = { Text("Department Name") },
                        placeholder = { Text("e.g. IT, LEGAL, LOGISTICS") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeptNameInput.isNotBlank()) {
                            viewModel.addDepartment(branch.id, newDeptNameInput)
                            branchToAddDept = null
                        }
                    },
                    enabled = newDeptNameInput.isNotBlank()
                ) {
                    Text("Add Department")
                }
            },
            dismissButton = {
                TextButton(onClick = { branchToAddDept = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Purpose of Visit Dialog
    if (showAddPurposeDialog) {
        AlertDialog(
            onDismissRequest = { showAddPurposeDialog = false },
            title = { Text("Add Purpose of Visit Option") },
            text = {
                Column {
                    Text(
                        text = "Enter a new Purpose of Visit option to appear on the Visitor Registration screen across all branches:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPurposeNameInput,
                        onValueChange = { newPurposeNameInput = it },
                        label = { Text("Purpose of Visit *") },
                        placeholder = { Text("e.g. Audit, Training, Vendor Visit") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_visit_purpose")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPurposeNameInput.isNotBlank()) {
                            viewModel.addVisitPurpose(newPurposeNameInput)
                            showAddPurposeDialog = false
                        }
                    },
                    enabled = isFirebaseLoggedIn && newPurposeNameInput.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_add_visit_purpose")
                ) {
                    Text("Add Option")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPurposeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Department Emails Dialog
    emailEditBranchDept?.let { (branch, dept) ->
        AlertDialog(
            onDismissRequest = { emailEditBranchDept = null },
            title = { Text("Department Email Recipients") },
            text = {
                Column {
                    Text("Enter recipient email addresses for $dept at ${branch.shortCode} (comma-separated):")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailEditInput,
                        onValueChange = { emailEditInput = it },
                        label = { Text("Email Addresses") },
                        placeholder = { Text("dept@saitgroup.com, manager@saitgroup.com") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Separate multiple email addresses with a comma (,).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDepartmentEmails(branch.id, dept, emailEditInput)
                        emailEditBranchDept = null
                    }
                ) {
                    Text("Save Recipients")
                }
            },
            dismissButton = {
                TextButton(onClick = { emailEditBranchDept = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Records Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusError) },
            title = { Text("Clear All Records?") },
            text = {
                Text("This will delete all visitor records stored locally on this device. Submissions already saved in Cloud Firestore or Google Drive will remain safe in your cloud accounts.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllRecords()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Safety Guidelines & Emergency Exit Evacuation Plan Dialog
    editingSafetyBranch?.let { branch ->
        AlertDialog(
            onDismissRequest = { editingSafetyBranch = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit ${branch.name} Safety Info")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Facility location, safety guidelines, emergency helpline numbers, contact person name, and exit evacuation drawing are synced across all terminals.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = safetyFacilityLocationInput,
                        onValueChange = { safetyFacilityLocationInput = it },
                        label = { Text("Facility Location *") },
                        placeholder = { Text("e.g. Dubai • Corporate & Free Zone Facility") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_safety_facility_location")
                    )

                    OutlinedTextField(
                        value = safetyGuidelinesInput,
                        onValueChange = { safetyGuidelinesInput = it },
                        label = { Text("Safety Rules & Guidelines *") },
                        placeholder = { Text("Enter facility safety rules, PPE requirements, smoking policy...") },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_safety_guidelines")
                    )

                    OutlinedTextField(
                        value = safetyHelplineInput,
                        onValueChange = { safetyHelplineInput = it },
                        label = { Text("Emergency Helpline Numbers *") },
                        placeholder = { Text("+971 4 881 2345 / 999") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_safety_helpline")
                    )

                    OutlinedTextField(
                        value = safetyContactNameInput,
                        onValueChange = { safetyContactNameInput = it },
                        label = { Text("Contact Person Name (Optional)") },
                        placeholder = { Text("e.g. Duty HSE Officer / Security Desk") },
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_safety_contact_name")
                    )

                    // Evacuation Drawing Attachment Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Emergency Evacuation Plan Drawing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (safetyExitDrawingUri != null) "Custom floor plan active (PNG / JPG / WEBP)" else "Preferred: PNG or JPG landscape (1280×720 px)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        exitPlanPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_upload_exit_plan")
                                ) {
                                    Text(if (safetyExitDrawingUri != null) "Replace" else "Upload Drawing", fontSize = 11.sp)
                                }
                                if (safetyExitDrawingUri != null) {
                                    OutlinedButton(
                                        onClick = { safetyExitDrawingUri = null },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Reset", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBranchSafetyInfo(
                            branchId = branch.id,
                            guidelines = safetyGuidelinesInput,
                            helpline = safetyHelplineInput,
                            contactName = safetyContactNameInput,
                            facilityLocation = safetyFacilityLocationInput
                        )
                        viewModel.updateBranchEmergencyExitDrawing(branch.id, safetyExitDrawingUri)
                        editingSafetyBranch = null
                    },
                    enabled = isFirebaseLoggedIn,
                    modifier = Modifier.testTag("btn_save_safety_info")
                ) {
                    Text("Save Safety Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSafetyBranch = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Safety Information Preview Dialog
    previewSafetyBranch?.let { branch ->
        SafetyInfoDialog(
            branch = branch,
            onDismiss = { previewSafetyBranch = null }
        )
    }
}

@Composable
fun BranchCountryDropdownField(
    selectedDialCode: String,
    onCountrySelected: (CountryDialOption) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allCountries = BranchRegistry.COUNTRY_DIAL_OPTIONS
    val selectedOption = remember(selectedDialCode, allCountries) {
        allCountries.find { it.dialCode == selectedDialCode }
    }

    val filteredCountries = remember(allCountries, searchQuery) {
        if (searchQuery.isBlank()) {
            allCountries
        } else {
            val q = searchQuery.trim()
            allCountries.filter {
                it.countryName.contains(q, ignoreCase = true) ||
                    it.label.contains(q, ignoreCase = true) ||
                    it.dialCode.contains(q, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (selectedOption != null) {
                    "${selectedOption.flagEmoji} ${selectedOption.countryName} (${selectedOption.dialCode})"
                } else {
                    "🌐 Custom / Dial Code ($selectedDialCode)"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Country & Dial Code *") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Country",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        expanded = !expanded
                        if (!expanded) searchQuery = ""
                    }
                    .testTag(testTag)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search country or dial code...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Country",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTag}_search")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        items(
                            items = filteredCountries,
                            key = { "${it.countryName}_${it.dialCode}" }
                        ) { option ->
                            val isSelected = option.dialCode == selectedDialCode &&
                                (selectedOption == null || option.countryName == selectedOption.countryName)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCountrySelected(option)
                                        expanded = false
                                        searchQuery = ""
                                    }
                                    .testTag("${testTag}_item_${option.countryName.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${option.flagEmoji} ${option.countryName}",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option.dialCode,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

