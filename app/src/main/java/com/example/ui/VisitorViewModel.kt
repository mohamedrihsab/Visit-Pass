package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppNotification
import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.data.FirestoreService
import com.example.data.KioskPreferences
import com.example.data.NotificationType
import com.example.data.SmtpConfig
import com.example.data.UserRole
import com.example.data.VisitorEntity
import com.example.network.SmtpMailer
import com.example.repository.VisitorRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RegistrationFormState(
    val fullName: String = "",
    val countryCode: String = "+971",
    val mobileNumber: String = "",
    val email: String = "",
    val companyName: String = "",
    val visitType: String = "Walk-In",
    val department: String = "PROJECTS",
    val purpose: String = "Meeting",
    val customPurpose: String = "",
    val personToMeet: String = "",
    val fullNameError: String? = null,
    val mobileError: String? = null,
    val emailError: String? = null,
    val companyError: String? = null,
    val customPurposeError: String? = null,
    val isSubmitting: Boolean = false
) {
    val fullMobileNumber: String
        get() = if (mobileNumber.startsWith("+")) mobileNumber else "$countryCode $mobileNumber".trim()

    val isValid: Boolean
        get() = fullName.isNotBlank() &&
                mobileNumber.isNotBlank() &&
                mobileNumber.all { it.isDigit() || it == '+' || it == ' ' || it == '-' } &&
                companyName.isNotBlank() &&
                (email.isBlank() || (email.contains("@") && email.contains("."))) &&
                (purpose != "Other" || customPurpose.isNotBlank())
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class SendEmailNotification(
        val recipients: List<String>,
        val subject: String,
        val body: String
    ) : UiEvent()
}

class VisitorViewModel(
    private val repository: VisitorRepository,
    private val kioskPrefs: KioskPreferences,
    private val firestoreService: FirestoreService
) : ViewModel() {

    // Firebase Auth State
    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // 3 User Roles: VISITOR_KIOSK, RECEPTIONIST, ADMIN
    private val _userRole = MutableStateFlow(kioskPrefs.userRole)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    val isKioskMode: StateFlow<Boolean> = _userRole
        .map { it == UserRole.VISITOR_KIOSK }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _userRole.value == UserRole.VISITOR_KIOSK)

    // Dynamic Branches (Admin configurable after Firebase login)
    private val _branches = MutableStateFlow(kioskPrefs.getBranches())
    val branches: StateFlow<List<Branch>> = _branches.asStateFlow()

    // Dynamic Purpose of Visit Options (Admin configurable after Firebase login)
    private val _visitPurposes = MutableStateFlow(kioskPrefs.getVisitPurposes())
    val visitPurposes: StateFlow<List<String>> = _visitPurposes.asStateFlow()

    // Department Email Recipients Map: "${branchId}::${department}" -> comma-separated emails
    private val _departmentEmails = MutableStateFlow(kioskPrefs.getDepartmentEmails())
    val departmentEmails: StateFlow<Map<String, String>> = _departmentEmails.asStateFlow()

    // Active Branch selection
    private val _activeBranch = MutableStateFlow(
        BranchRegistry.getBranchByName(kioskPrefs.activeBranchName, _branches.value)
    )
    val activeBranch: StateFlow<Branch> = _activeBranch.asStateFlow()

    // Security PINs (Never shown in UI)
    private val _adminPin = MutableStateFlow(kioskPrefs.adminPin)
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    private val _receptionistPin = MutableStateFlow(kioskPrefs.receptionistPin)
    val receptionistPin: StateFlow<String> = _receptionistPin.asStateFlow()

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    private val _targetPinRole = MutableStateFlow<String?>(null)
    val targetPinRole: StateFlow<String?> = _targetPinRole.asStateFlow()

    private val _autoResetSeconds = MutableStateFlow(kioskPrefs.kioskAutoResetSeconds)
    val autoResetSeconds: StateFlow<Int> = _autoResetSeconds.asStateFlow()

    // Screen timeout setting for all terminals
    private val _screenTimeoutMinutes = MutableStateFlow(kioskPrefs.screenTimeoutMinutes)
    val screenTimeoutMinutes: StateFlow<Int> = _screenTimeoutMinutes.asStateFlow()

    // Theme Mode ("LIGHT", "DARK", "SYSTEM") - strictly local per device
    private val _themeMode = MutableStateFlow(kioskPrefs.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Liquid Glass Background Palette Style ("OCEANIC") - strictly local
    private val _liquidPaletteStyle = MutableStateFlow(kioskPrefs.liquidPaletteStyle)
    val liquidPaletteStyle: StateFlow<String> = _liquidPaletteStyle.asStateFlow()

    // SMTP Outgoing Mail Server Configuration
    private val _smtpConfig = MutableStateFlow(kioskPrefs.getSmtpConfig())
    val smtpConfig: StateFlow<SmtpConfig> = _smtpConfig.asStateFlow()

    // Touchless Mobile QR Registration via GitHub Pages
    private val _isTouchlessKioskMode = MutableStateFlow(kioskPrefs.touchlessKioskMode)
    val isTouchlessKioskMode: StateFlow<Boolean> = _isTouchlessKioskMode.asStateFlow()

    private val _touchlessGithubUrl = MutableStateFlow(kioskPrefs.touchlessGithubPageUrl)
    val touchlessGithubUrl: StateFlow<String> = _touchlessGithubUrl.asStateFlow()

    private fun generateTouchlessSessionId(branch: Branch = _activeBranch.value): String {
        val (prefix, cityCode) = repository.resolvePrefixAndCity(branch)
        val term = kioskPrefs.kioskTerminalId.ifBlank { "01" }
        return "KS-$prefix-$cityCode-T$term"
    }

    private val _touchlessSessionId = MutableStateFlow(generateTouchlessSessionId())
    val touchlessSessionId: StateFlow<String> = _touchlessSessionId.asStateFlow()

    val touchlessQrUrl: StateFlow<String> = combine(
        _touchlessGithubUrl,
        _activeBranch,
        _touchlessSessionId
    ) { baseUrl, branch, session ->
        val cleanUrl = if (baseUrl.isBlank()) KioskPreferences.DEFAULT_GITHUB_PAGES_URL else baseUrl.trim()
        val sep = if (cleanUrl.contains("?")) "&" else "?"
        val (prefix, cityCode) = repository.resolvePrefixAndCity(branch)
        val canonicalCode = "$prefix-$cityCode"
        val deptsParam = Uri.encode(branch.defaultDepartments.joinToString(","))
        val helplineParam = Uri.encode(branch.safetyHelpline)
        val contactParam = Uri.encode(branch.safetyContactName)
        "${cleanUrl}${sep}v=6&branch=${Uri.encode(canonicalCode)}&branchName=${Uri.encode(branch.name)}&session=${Uri.encode(session)}&city=${Uri.encode(branch.city)}&location=${Uri.encode(branch.displayFacilityLocation)}&countryCode=${Uri.encode(branch.countryCode)}&depts=$deptsParam&helpline=$helplineParam&contact=$contactParam"
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "${KioskPreferences.DEFAULT_GITHUB_PAGES_URL}?v=6&branch=${_activeBranch.value.shortCode}&branchName=${Uri.encode(_activeBranch.value.name)}&session=${_touchlessSessionId.value}"
    )

    // In-App Notification Center
    private val _notifications = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                title = "SAIT System Ready",
                message = "Visitor Pass kiosk initialized for ${activeBranch.value.name}.",
                type = NotificationType.SYSTEM_ALERT
            )
        )
    )
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadNotificationCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _showNotificationCenter = MutableStateFlow(false)
    val showNotificationCenter: StateFlow<Boolean> = _showNotificationCenter.asStateFlow()

    // Form State initialized with activeBranch's countryCode, default department & default purpose
    private val _formState = MutableStateFlow(
        RegistrationFormState(
            countryCode = _activeBranch.value.countryCode.ifBlank { "+971" },
            department = _activeBranch.value.defaultDepartments.firstOrNull() ?: "PROJECTS",
            purpose = _visitPurposes.value.firstOrNull() ?: "Meeting"
        )
    )
    val formState: StateFlow<RegistrationFormState> = _formState.asStateFlow()

    // Active Pass Dialog & Countdown
    private val _activeVisitorPass = MutableStateFlow<VisitorEntity?>(null)
    val activeVisitorPass: StateFlow<VisitorEntity?> = _activeVisitorPass.asStateFlow()

    private val _passCountdownSeconds = MutableStateFlow(0)
    val passCountdownSeconds: StateFlow<Int> = _passCountdownSeconds.asStateFlow()
    private var countdownJob: Job? = null

    // Navigation Tab for Receptionist & Admin
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Logbook Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _logFilter = MutableStateFlow("ALL") // "ALL", "CHECKED_IN", "CHECKED_OUT"
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private val _logBranchFilter = MutableStateFlow<String?>(
        if (_userRole.value == UserRole.ADMIN) "ALL" else "CURRENT"
    ) // "CURRENT" or "ALL" or specific branch name/code
    val logBranchFilter: StateFlow<String?> = _logBranchFilter.asStateFlow()

    // Cloud Online Sync Health
    private val _isSyncingAll = MutableStateFlow(false)
    val isSyncingAll: StateFlow<Boolean> = _isSyncingAll.asStateFlow()

    // Event Flow
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Counts from database
    val unsyncedCount: StateFlow<Int> = repository.unsyncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedCount: StateFlow<Int> = repository.syncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeCount: StateFlow<Int> = repository.activeCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allVisitorsList: StateFlow<List<VisitorEntity>> = repository.allVisitors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Visitors list with search, status filter, and role-enforced branch filter applied
    val visitors: StateFlow<List<VisitorEntity>> = combine(
        repository.allVisitors,
        _searchQuery,
        _logBranchFilter,
        _activeBranch,
        _userRole
    ) { all, query, branchFilter, currentBranch, role ->
        all.filter { visitor ->
            val matchesSearch = query.isBlank() ||
                    visitor.fullName.contains(query, ignoreCase = true) ||
                    visitor.companyName.contains(query, ignoreCase = true) ||
                    visitor.department.contains(query, ignoreCase = true) ||
                    visitor.passNumber.contains(query, ignoreCase = true) ||
                    visitor.personToMeet.contains(query, ignoreCase = true) ||
                    visitor.branchName.contains(query, ignoreCase = true) ||
                    visitor.purpose.contains(query, ignoreCase = true) ||
                    visitor.mobileNumber.contains(query, ignoreCase = true) ||
                    visitor.email.contains(query, ignoreCase = true)

            val isCurrentBranchVisitor = visitor.branchName.equals(currentBranch.name, ignoreCase = true) ||
                    (visitor.branchCode.isNotBlank() && visitor.branchCode.equals(currentBranch.shortCode, ignoreCase = true))

            val matchesBranch = if (role == UserRole.RECEPTIONIST) {
                // Receptionist logbook ONLY shows details related to the selected active branch
                isCurrentBranchVisitor
            } else {
                when (branchFilter) {
                    "ALL", null -> true
                    "CURRENT" -> isCurrentBranchVisitor
                    else -> visitor.branchName.equals(branchFilter, ignoreCase = true) ||
                            visitor.branchCode.equals(branchFilter, ignoreCase = true)
                }
            }

            matchesSearch && matchesBranch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        attachOrganizationSettingsListener()
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupDuplicateVisitors()
            withContext(Dispatchers.Main) {
                startLiveMultiDeviceSync()
            }
        }
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user
            if (user != null) {
                attachOrganizationSettingsListener()
                // Ensure current/default settings are pushed to Firebase upon authentication
                syncAllSettingsToFirebase()
                viewModelScope.launch(Dispatchers.IO) {
                    repository.cleanupDuplicateVisitors()
                    syncAllPending(silent = true)
                }
            }
        }
    }

    private fun buildVisitorSignature(v: VisitorEntity): String {
        return "${v.fullName.trim().lowercase()}|${v.mobileNumber.trim()}|${v.timestamp}"
    }

    fun startLiveMultiDeviceSync() {
        firestoreService.startRealtimeMultiDeviceSync(
            scope = viewModelScope,
            onVisitorDeleted = { passNumber ->
                repository.deleteRemoteVisitorByPassNumber(passNumber)
            },
            onVisitorUpsert = { remote ->
                val visitorSig = buildVisitorSignature(remote)
                val alreadyProcessed = processedRemotePassNumbers.contains(remote.passNumber) ||
                        processedRemotePassNumbers.contains(visitorSig) ||
                        kioskPrefs.isRemoteVisitorProcessed(remote.passNumber) ||
                        kioskPrefs.isRemoteVisitorProcessed(visitorSig)

                val upsertResult = repository.upsertRemoteVisitor(remote, _activeBranch.value)
                val savedVisitor = upsertResult.visitor

                processedRemotePassNumbers.add(remote.passNumber)
                processedRemotePassNumbers.add(savedVisitor.passNumber)
                processedRemotePassNumbers.add(visitorSig)
                kioskPrefs.markRemoteVisitorProcessed(
                    remote.passNumber,
                    savedVisitor.passNumber,
                    visitorSig
                )

                // If a random/temporary remote document was normalized to a clean sequential pass number,
                // save the clean pass document and delete the obsolete remote document so it never triggers again on app reopen
                val obsoletePass = upsertResult.obsoleteRemotePassNumber
                if (obsoletePass != null && firestoreService.isAuthenticated) {
                    viewModelScope.launch(Dispatchers.IO) {
                        firestoreService.saveVisitorToFirestore(savedVisitor)
                        firestoreService.deleteObsoletePassDocument(obsoletePass)
                    }
                }

                // Only trigger live pass popup and email if this is a brand-new check-in not previously in local DB
                if (upsertResult.isNewlyInserted && !alreadyProcessed) {
                    handleIncomingRemoteVisitor(savedVisitor)
                }
            }
        )
    }

    private fun requireFirebaseLoginForAdminEdit(): Boolean {
        if (_currentUser.value == null) {
            viewModelScope.launch {
                _eventFlow.emit(
                    UiEvent.ShowSnackbar("Firebase Sign-In required: Please sign in with Google in Cloud & Admin to make changes.")
                )
            }
            return false
        }
        return true
    }

    private fun syncFormWithBranch(branch: Branch) {
        val currentDepts = branch.defaultDepartments
        val nextDept = if (currentDepts.isNotEmpty() && !currentDepts.contains(_formState.value.department)) {
            currentDepts.first()
        } else {
            _formState.value.department
        }
        _formState.value = _formState.value.copy(
            countryCode = branch.countryCode.ifBlank { "+971" },
            department = nextDept
        )
    }

    private fun attachOrganizationSettingsListener() {
        firestoreService.startOrganizationConfigSync(
            scope = viewModelScope,
            onSeedDefaults = {
                if (_currentUser.value != null) {
                    syncAllSettingsToFirebase()
                }
            },
            onConfigUpdated = { remoteBranches, remoteEmails, remoteActiveBranch, remoteAdminPin, remoteReceptionistPin, remoteAutoReset, remoteTheme, remoteSmtp, remotePurposes, remoteTimeout ->
                if (remoteBranches.isNotEmpty()) {
                    _branches.value = remoteBranches
                    kioskPrefs.saveBranches(remoteBranches)
                    val targetName = remoteActiveBranch ?: _activeBranch.value.name
                    val updatedActive = remoteBranches.find {
                        it.name.equals(targetName, ignoreCase = true) || it.id == _activeBranch.value.id
                    } ?: remoteBranches.first()
                    _activeBranch.value = updatedActive
                    _touchlessSessionId.value = generateTouchlessSessionId(updatedActive)
                    kioskPrefs.activeBranchName = updatedActive.name
                    syncFormWithBranch(updatedActive)
                }
                if (!remotePurposes.isNullOrEmpty()) {
                    _visitPurposes.value = remotePurposes
                    kioskPrefs.saveVisitPurposes(remotePurposes)
                    if (!remotePurposes.contains(_formState.value.purpose)) {
                        _formState.value = _formState.value.copy(purpose = remotePurposes.first())
                    }
                }
                if (remoteEmails.isNotEmpty()) {
                    val merged = _departmentEmails.value.toMutableMap().apply { putAll(remoteEmails) }
                    _departmentEmails.value = merged
                    kioskPrefs.saveDepartmentEmails(merged)
                }
                if (!remoteAdminPin.isNullOrBlank()) {
                    _adminPin.value = remoteAdminPin
                    kioskPrefs.adminPin = remoteAdminPin
                }
                if (!remoteReceptionistPin.isNullOrBlank()) {
                    _receptionistPin.value = remoteReceptionistPin
                    kioskPrefs.receptionistPin = remoteReceptionistPin
                }
                if (remoteAutoReset != null) {
                    _autoResetSeconds.value = remoteAutoReset
                    kioskPrefs.kioskAutoResetSeconds = remoteAutoReset
                }
                if (remoteTimeout != null) {
                    _screenTimeoutMinutes.value = remoteTimeout
                    kioskPrefs.screenTimeoutMinutes = remoteTimeout
                }
                // Dark/Light theme mode is strictly device-local; do not overwrite from remote sync
                if (remoteSmtp != null) {
                    _smtpConfig.value = remoteSmtp
                    kioskPrefs.saveSmtpConfig(remoteSmtp)
                }
            }
        )
    }

    fun syncAllSettingsToFirebase() {
        if (_currentUser.value == null) return
        viewModelScope.launch {
            firestoreService.saveOrganizationConfig(
                branches = _branches.value,
                departmentEmails = _departmentEmails.value,
                activeBranchName = _activeBranch.value.name,
                adminPin = _adminPin.value,
                receptionistPin = _receptionistPin.value,
                autoResetSeconds = _autoResetSeconds.value,
                themeMode = null, // Theme remains local to each terminal, not synced across devices
                smtpConfig = _smtpConfig.value,
                visitPurposes = _visitPurposes.value,
                screenTimeoutMinutes = _screenTimeoutMinutes.value
            )
        }
    }

    fun onAuthUserChanged(user: FirebaseUser?) {
        _currentUser.value = user
        if (user != null) {
            startLiveMultiDeviceSync()
            attachOrganizationSettingsListener()
            syncAllSettingsToFirebase()
        }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreService.stopRealtimeSync()
    }

    // Role Switcher & PIN Auth
    fun requestRoleUnlock(targetRole: String? = null) {
        _targetPinRole.value = targetRole
        _showPinDialog.value = true
    }

    fun requestAdminAccess() {
        requestRoleUnlock("ADMIN")
    }

    fun dismissPinDialog() {
        _showPinDialog.value = false
        _targetPinRole.value = null
    }

    fun unlockRole(role: UserRole) {
        _showPinDialog.value = false
        _targetPinRole.value = null
        _userRole.value = role
        kioskPrefs.userRole = role
        _logBranchFilter.value = if (role == UserRole.ADMIN) "ALL" else "CURRENT"
        _currentTab.value = 0
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Switched to ${role.displayName}"))
        }
    }

    fun unlockAdmin() {
        unlockRole(UserRole.ADMIN)
    }

    fun enterKioskMode() {
        unlockRole(UserRole.VISITOR_KIOSK)
    }

    fun enterReceptionistMode() {
        unlockRole(UserRole.RECEPTIONIST)
    }

    fun setBranch(branch: Branch) {
        if (!requireFirebaseLoginForAdminEdit()) return
        _activeBranch.value = branch
        _touchlessSessionId.value = generateTouchlessSessionId(branch)
        kioskPrefs.activeBranchName = branch.name
        syncFormWithBranch(branch)
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Active branch set to: ${branch.name} and synced!"))
        }
    }

    // Dynamic Branches Management
    fun addBranch(
        name: String,
        shortCode: String,
        city: String,
        subtitle: String,
        countryCode: String = "+971",
        facilityLocation: String = "",
        initialDepartments: List<String>
    ) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val cleanId = shortCode.lowercase().replace("-", "_").ifBlank { name.lowercase().replace(" ", "_") }
        val cleanCity = city.trim()
        val cleanSub = subtitle.trim().ifBlank { "SAIT Group Operations Facility" }
        val cleanLoc = facilityLocation.trim().ifBlank { "$cleanSub, $cleanCity" }
        val cleanCountry = countryCode.trim().ifBlank { BranchRegistry.inferCountryCode(cleanCity, name) }
        val newBranch = Branch(
            id = cleanId,
            name = name.trim(),
            shortCode = shortCode.trim().uppercase(),
            city = cleanCity,
            subtitle = cleanSub,
            defaultDepartments = if (initialDepartments.isNotEmpty()) initialDepartments else listOf("PROJECTS", "QHSE", "MANAGEMENT"),
            countryCode = cleanCountry,
            facilityLocation = cleanLoc
        )

        val updatedList = _branches.value + newBranch
        _branches.value = updatedList
        kioskPrefs.saveBranches(updatedList)
        syncAllSettingsToFirebase()

        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Branch '${newBranch.name}' added and synced to Firebase!"))
        }
    }

    fun updateBranchDetails(
        branchId: String,
        name: String,
        shortCode: String,
        city: String,
        subtitle: String,
        countryCode: String = "+971",
        facilityLocation: String = ""
    ) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val cleanName = name.trim()
        val cleanCode = shortCode.trim().uppercase()
        val cleanCity = city.trim()
        val cleanSub = subtitle.trim().ifBlank { "SAIT Group Operations Facility" }
        val cleanLoc = facilityLocation.trim().ifBlank { "$cleanSub, $cleanCity" }
        val cleanCountry = countryCode.trim().ifBlank { BranchRegistry.inferCountryCode(cleanCity, cleanName) }
        if (cleanName.isBlank() || cleanCode.isBlank() || cleanCity.isBlank()) return

        val updatedList = _branches.value.map { b ->
            if (b.id == branchId) {
                b.copy(
                    name = cleanName,
                    shortCode = cleanCode,
                    city = cleanCity,
                    subtitle = cleanSub,
                    countryCode = cleanCountry,
                    facilityLocation = cleanLoc
                )
            } else b
        }
        _branches.value = updatedList
        kioskPrefs.saveBranches(updatedList)

        if (_activeBranch.value.id == branchId) {
            val updatedActive = updatedList.first { it.id == branchId }
            _activeBranch.value = updatedActive
            kioskPrefs.activeBranchName = updatedActive.name
            syncFormWithBranch(updatedActive)
        }

        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Branch details updated and synced to Firebase!"))
        }
    }

    fun removeBranch(branchId: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        if (_branches.value.size <= 1) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot delete the only remaining branch."))
            }
            return
        }

        val updatedList = _branches.value.filterNot { it.id == branchId }
        _branches.value = updatedList
        kioskPrefs.saveBranches(updatedList)

        // If the active branch was deleted, switch to the first available branch
        if (_activeBranch.value.id == branchId) {
            val fallback = updatedList.first()
            _activeBranch.value = fallback
            kioskPrefs.activeBranchName = fallback.name
            syncFormWithBranch(fallback)
        }

        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Branch removed and synced to Firebase."))
        }
    }

    // Dynamic Departments Management
    fun addDepartment(branchId: String, departmentName: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val deptClean = departmentName.trim().uppercase()
        if (deptClean.isBlank()) return

        val updatedList = _branches.value.map { branch ->
            if (branch.id == branchId && !branch.defaultDepartments.contains(deptClean)) {
                branch.copy(defaultDepartments = branch.defaultDepartments + deptClean)
            } else {
                branch
            }
        }
        _branches.value = updatedList
        kioskPrefs.saveBranches(updatedList)

        if (_activeBranch.value.id == branchId) {
            _activeBranch.value = updatedList.first { it.id == branchId }
        }

        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Department '$deptClean' added and synced."))
        }
    }

    fun removeDepartment(branchId: String, departmentName: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val targetBranch = _branches.value.find { it.id == branchId } ?: return
        if (targetBranch.defaultDepartments.size <= 1) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot remove the last remaining department."))
            }
            return
        }

        val updatedList = _branches.value.map { branch ->
            if (branch.id == branchId) {
                branch.copy(defaultDepartments = branch.defaultDepartments.filterNot { it == departmentName })
            } else {
                branch
            }
        }
        _branches.value = updatedList
        kioskPrefs.saveBranches(updatedList)

        if (_activeBranch.value.id == branchId) {
            val updatedActive = updatedList.first { it.id == branchId }
            _activeBranch.value = updatedActive
            syncFormWithBranch(updatedActive)
        }

        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Department '$departmentName' removed and synced."))
        }
    }

    // Dynamic Purpose of Visit Options Management
    fun addVisitPurpose(purposeName: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val clean = purposeName.trim()
        if (clean.isBlank()) return
        if (_visitPurposes.value.any { it.equals(clean, ignoreCase = true) }) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Purpose '$clean' already exists."))
            }
            return
        }
        // Keep "Other" at the end if present
        val current = _visitPurposes.value.toMutableList()
        val otherIndex = current.indexOfFirst { it.equals("Other", ignoreCase = true) }
        if (otherIndex >= 0) {
            current.add(otherIndex, clean)
        } else {
            current.add(clean)
        }
        _visitPurposes.value = current
        kioskPrefs.saveVisitPurposes(current)
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Purpose of visit '$clean' added and synced!"))
        }
    }

    fun removeVisitPurpose(purposeName: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        if (_visitPurposes.value.size <= 1) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot remove the last remaining purpose option."))
            }
            return
        }
        val updated = _visitPurposes.value.filterNot { it.equals(purposeName, ignoreCase = true) }
        _visitPurposes.value = updated
        kioskPrefs.saveVisitPurposes(updated)
        if (_formState.value.purpose.equals(purposeName, ignoreCase = true)) {
            _formState.value = _formState.value.copy(purpose = updated.first())
        }
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Purpose of visit '$purposeName' removed and synced."))
        }
    }

    // Department Email Recipients Management
    fun updateDepartmentEmails(branchId: String, department: String, emailString: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val key = "${branchId}::${department}"
        val updatedMap = _departmentEmails.value.toMutableMap()
        if (emailString.isBlank()) {
            updatedMap.remove(key)
        } else {
            updatedMap[key] = emailString.trim()
        }
        _departmentEmails.value = updatedMap
        kioskPrefs.saveDepartmentEmails(updatedMap)
        syncAllSettingsToFirebase()

        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Email recipients for $department updated and synced."))
        }
    }

    fun getEmailsForDepartment(branch: Branch, department: String): List<String> {
        val key = "${branch.id}::${department}"
        val raw = _departmentEmails.value[key]
        if (!raw.isNullOrBlank()) {
            return raw.split(",")
                .map { it.trim() }
                .filter { it.contains("@") }
        }
        // Fallback default format:
        val cleanDept = department.lowercase().replace(" ", ".").replace("&", "and")
        val cleanCity = branch.city.lowercase().take(3)
        return listOf("$cleanDept.$cleanCity@saitgroup.com", "reception.$cleanCity@saitgroup.com")
    }

    fun updateAdminPin(newPin: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        kioskPrefs.adminPin = newPin
        _adminPin.value = newPin
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Admin PIN updated and synced to Firebase."))
        }
    }

    fun updateReceptionistPin(newPin: String) {
        if (!requireFirebaseLoginForAdminEdit()) return
        kioskPrefs.receptionistPin = newPin
        _receptionistPin.value = newPin
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Receptionist PIN updated and synced to Firebase."))
        }
    }

    fun updateAutoResetSeconds(seconds: Int) {
        if (!requireFirebaseLoginForAdminEdit()) return
        kioskPrefs.kioskAutoResetSeconds = seconds
        _autoResetSeconds.value = seconds
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Auto-reset timer set to ${seconds}s and synced."))
        }
    }

    fun updateScreenTimeoutMinutes(minutes: Int) {
        if (!requireFirebaseLoginForAdminEdit()) return
        kioskPrefs.screenTimeoutMinutes = minutes
        _screenTimeoutMinutes.value = minutes
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            val label = when (minutes) {
                KioskPreferences.TIMEOUT_ALWAYS_ON -> "Always On (Kiosk mode)"
                KioskPreferences.TIMEOUT_SYSTEM -> "Device System Default"
                else -> "${minutes} minute(s)"
            }
            _eventFlow.emit(UiEvent.ShowSnackbar("Screen timeout set to $label across all devices."))
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun setLogBranchFilter(filter: String) {
        _logBranchFilter.value = if (_userRole.value == UserRole.RECEPTIONIST) "CURRENT" else filter
    }

    fun onFullNameChange(value: String) {
        _formState.value = _formState.value.copy(
            fullName = value,
            fullNameError = if (value.isBlank()) "Full name is required" else null
        )
    }

    fun onMobileNumberChange(value: String) {
        val hasInvalidChars = value.any { !it.isDigit() && it != '+' && it != ' ' && it != '-' }
        _formState.value = _formState.value.copy(
            mobileNumber = value,
            mobileError = when {
                value.isBlank() -> "Mobile number is required"
                hasInvalidChars -> "Enter numbers only"
                else -> null
            }
        )
    }

    fun onCompanyNameChange(value: String) {
        _formState.value = _formState.value.copy(
            companyName = value,
            companyError = if (value.isBlank()) "Company name is required" else null
        )
    }

    fun onVisitTypeChange(value: String) {
        _formState.value = _formState.value.copy(visitType = value)
    }

    fun onDepartmentChange(value: String) {
        _formState.value = _formState.value.copy(department = value)
    }

    fun onPurposeChange(value: String) {
        _formState.value = _formState.value.copy(
            purpose = value,
            customPurposeError = if (value == "Other" && _formState.value.customPurpose.isBlank()) "Please specify purpose" else null
        )
    }

    fun onCustomPurposeChange(value: String) {
        _formState.value = _formState.value.copy(
            customPurpose = value,
            customPurposeError = if (value.isBlank()) "Please specify purpose" else null
        )
    }

    fun onCountryCodeChange(code: String) {
        _formState.value = _formState.value.copy(countryCode = code)
    }

    fun onEmailChange(value: String) {
        val trimmed = value.trim()
        val emailErr = when {
            trimmed.isBlank() -> "Email ID is required"
            !trimmed.contains("@") || !trimmed.contains(".") -> "Please enter a valid email address"
            else -> null
        }
        _formState.value = _formState.value.copy(
            email = value,
            emailError = emailErr
        )
    }

    fun onPersonToMeetChange(value: String) {
        _formState.value = _formState.value.copy(personToMeet = value)
    }

    fun updateBranchSafetyInfo(
        branchId: String,
        guidelines: String,
        helpline: String,
        contactName: String = "",
        facilityLocation: String? = null
    ) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val updated = _branches.value.map { b ->
            if (b.id == branchId) {
                b.copy(
                    safetyGuidelines = guidelines.trim(),
                    safetyHelpline = helpline.trim(),
                    safetyContactName = contactName.trim(),
                    facilityLocation = facilityLocation?.trim() ?: b.facilityLocation
                )
            } else b
        }
        _branches.value = updated
        kioskPrefs.saveBranches(updated)
        if (_activeBranch.value.id == branchId) {
            _activeBranch.value = updated.first { it.id == branchId }
        }
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Safety guidelines, helpline, and facility location updated and synced!"))
        }
    }

    fun updateBranchLogo(branchId: String, logoUri: String?) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val updated = _branches.value.map { b ->
            if (b.id == branchId) {
                b.copy(logoUri = logoUri)
            } else b
        }
        _branches.value = updated
        kioskPrefs.saveBranches(updated)
        if (_activeBranch.value.id == branchId) {
            _activeBranch.value = updated.first { it.id == branchId }
        }
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            val msg = if (logoUri != null) "Company logo updated and synced." else "Company logo removed and synced."
            _eventFlow.emit(UiEvent.ShowSnackbar(msg))
        }
    }

    fun updateBranchEmergencyExitDrawing(branchId: String, drawingUri: String?) {
        if (!requireFirebaseLoginForAdminEdit()) return
        val updated = _branches.value.map { b ->
            if (b.id == branchId) {
                b.copy(emergencyExitDrawingUri = drawingUri)
            } else b
        }
        _branches.value = updated
        kioskPrefs.saveBranches(updated)
        if (_activeBranch.value.id == branchId) {
            _activeBranch.value = updated.first { it.id == branchId }
        }
        syncAllSettingsToFirebase()
        viewModelScope.launch {
            val msg = if (drawingUri != null) "Emergency exit drawing updated and synced." else "Emergency exit drawing removed and synced."
            _eventFlow.emit(UiEvent.ShowSnackbar(msg))
        }
    }

    fun submitRegistration() {
        val current = _formState.value

        var hasError = false
        var nameErr: String? = null
        var phoneErr: String? = null
        var emailErr: String? = null
        var compErr: String? = null
        var custErr: String? = null

        if (current.fullName.isBlank()) {
            nameErr = "Full name is required"
            hasError = true
        }
        if (current.mobileNumber.isBlank()) {
            phoneErr = "Mobile number is required"
            hasError = true
        } else if (current.mobileNumber.any { !it.isDigit() && it != '+' && it != ' ' && it != '-' }) {
            phoneErr = "Enter numbers only"
            hasError = true
        }
        if (current.email.isBlank()) {
            emailErr = "Email ID is required"
            hasError = true
        } else if (!current.email.trim().contains("@") || !current.email.trim().contains(".")) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }
        if (current.companyName.isBlank()) {
            compErr = "Company name is required"
            hasError = true
        }
        if (current.purpose == "Other" && current.customPurpose.isBlank()) {
            custErr = "Please specify purpose"
            hasError = true
        }

        if (hasError) {
            _formState.value = current.copy(
                fullNameError = nameErr,
                mobileError = phoneErr,
                emailError = emailErr,
                companyError = compErr,
                customPurposeError = custErr
            )
            return
        }

        _formState.value = current.copy(isSubmitting = true)

        viewModelScope.launch {
            val branch = _activeBranch.value
            val result = repository.registerVisitor(
                fullName = current.fullName,
                mobileNumber = current.fullMobileNumber,
                companyName = current.companyName,
                visitType = current.visitType,
                department = current.department,
                purpose = current.purpose,
                customPurpose = current.customPurpose,
                personToMeet = current.personToMeet,
                email = current.email.trim(),
                branch = branch
            )

            val nextDept = branch.defaultDepartments.firstOrNull() ?: "PROJECTS"
            _formState.value = RegistrationFormState(
                countryCode = branch.countryCode.ifBlank { "+971" },
                department = nextDept
            )

            result.onSuccess { visitor ->
                val sig = buildVisitorSignature(visitor)
                processedRemotePassNumbers.add(visitor.passNumber)
                processedRemotePassNumbers.add(sig)
                kioskPrefs.markRemoteVisitorProcessed(visitor.passNumber, sig)
                showPass(visitor)
                dispatchRegistrationEmails(visitor, branch, triggerIntentFallbackIfNoSmtp = true)

                val currentSmtp = _smtpConfig.value
                val recipients = getEmailsForDepartment(branch, visitor.department)
                val deliveryMethod = when {
                    currentSmtp.enabled && currentSmtp.host.isNotBlank() -> "Outgoing SMTP (${currentSmtp.host})"
                    else -> "Device Email App (Configure SMTP in Admin for automatic delivery)"
                }

                val statusMsg = if (visitor.email.isNotBlank()) {
                    "Pass ${visitor.passNumber} issued! Pass & Safety Briefing sent to ${visitor.email} and alert dispatched to ${visitor.department}."
                } else {
                    "Pass ${visitor.passNumber} issued! Alert routed via $deliveryMethod to ${visitor.department} (${recipients.size} recipients)."
                }
                _eventFlow.emit(UiEvent.ShowSnackbar(statusMsg))
            }.onFailure {
                _eventFlow.emit(UiEvent.ShowSnackbar("Failed to register. Please try again."))
            }
        }
    }

    private fun dispatchRegistrationEmails(
        visitor: VisitorEntity,
        branch: Branch,
        triggerIntentFallbackIfNoSmtp: Boolean = true
    ) {
        val currentSmtp = _smtpConfig.value
        val recipients = getEmailsForDepartment(branch, visitor.department)
        val subject = "[SAIT Visitor Alert - ${branch.shortCode}] Visitor Arrived: ${visitor.fullName} (${visitor.companyName})"
        val emailBody = buildString {
            appendLine("=================================================================")
            appendLine("SAIT GROUP — VISITOR ARRIVAL & CHECK-IN NOTIFICATION")
            appendLine("=================================================================")
            appendLine("Facility:       ${branch.name} (${branch.city})")
            appendLine("Address:        ${branch.displayFacilityLocation}")
            appendLine("Department:     ${visitor.department}")
            appendLine("Pass Number:    ${visitor.passNumber}")
            appendLine("Check-In Time:  ${visitor.checkInFormattedTime}")
            appendLine("Status:         ${visitor.status}")
            appendLine()
            appendLine("VISITOR INFORMATION:")
            appendLine("-----------------------------------------------------------------")
            appendLine("• Full Name:        ${visitor.fullName}")
            appendLine("• Company:          ${visitor.companyName}")
            appendLine("• Contact Number:   ${visitor.mobileNumber}")
            if (visitor.email.isNotBlank()) {
                appendLine("• Email Address:    ${visitor.email}")
            }
            appendLine("• Visit Type:       ${visitor.visitType}")
            appendLine("• Purpose of Visit: ${visitor.purpose}")
            appendLine("• Host / To Meet:   ${if (visitor.personToMeet.isNotBlank()) visitor.personToMeet else "Reception Desk"}")
            appendLine()
            appendLine("HOST ACTION:")
            appendLine("Please proceed to the Reception / Lobby to welcome your visitor,")
            appendLine("or notify the front desk if you require them to be directed to a meeting room.")
            appendLine()
            appendLine("Automated notification generated by SAIT Pass+.")
        }

        val emailHtml = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"/></head>
            <body style="margin: 0; padding: 20px; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 620px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(15, 41, 66, 0.08); border: 1px solid #e2e8f0;">
                    <!-- Executive Header -->
                    <div style="background: linear-gradient(135deg, #0F2942 0%, #173859 60%, #0284C7 100%); padding: 24px 28px; color: #ffffff;">
                        <table style="width: 100%; border-collapse: collapse;">
                            <tr>
                                <td>
                                    <div style="display: inline-block; padding: 4px 10px; border-radius: 20px; background: rgba(16, 185, 129, 0.22); border: 1px solid rgba(16, 185, 129, 0.5); color: #34d399; font-size: 11px; font-weight: 700; letter-spacing: 0.6px; margin-bottom: 8px;">
                                        🟢 VISITOR ARRIVED AT RECEPTION
                                    </div>
                                    <h1 style="margin: 0; font-size: 21px; font-weight: 800; letter-spacing: 0.3px; color: #ffffff;">SAIT GROUP VISITOR ALERT</h1>
                                    <p style="margin: 6px 0 0 0; font-size: 13px; color: #bae6fd; font-weight: 500;">${branch.name} &bull; ${branch.displayFacilityLocation}</p>
                                </td>
                            </tr>
                        </table>
                    </div>

                    <!-- Pass ID Ribbon -->
                    <div style="background-color: #f8fafc; padding: 14px 28px; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: space-between;">
                        <div>
                            <span style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.8px;">OFFICIAL PASS NUMBER</span><br/>
                            <span style="font-size: 20px; font-weight: 800; color: #0f2942; font-family: 'SF Mono', Consolas, Monaco, monospace; letter-spacing: 1px;">${visitor.passNumber}</span>
                        </div>
                        <div style="text-align: right;">
                            <span style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase;">CHECK-IN TIME</span><br/>
                            <span style="font-size: 14px; font-weight: 700; color: #0284c7;">${visitor.checkInFormattedTime}</span>
                        </div>
                    </div>

                    <!-- Visitor Details Table -->
                    <div style="padding: 24px 28px;">
                        <table style="width: 100%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600; width: 36%; border-bottom: 1px solid #f1f5f9;">Visitor Name:</td>
                                <td style="padding: 10px 0; font-weight: 800; color: #0f172a; font-size: 15px; border-bottom: 1px solid #f1f5f9;">${visitor.fullName}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600; border-bottom: 1px solid #f1f5f9;">Company:</td>
                                <td style="padding: 10px 0; font-weight: 700; color: #0f172a; font-size: 14px; border-bottom: 1px solid #f1f5f9;">${visitor.companyName}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600; border-bottom: 1px solid #f1f5f9;">Host / Meeting With:</td>
                                <td style="padding: 10px 0; font-weight: 800; color: #0284c7; font-size: 14px; border-bottom: 1px solid #f1f5f9;">${if (visitor.personToMeet.isNotBlank()) visitor.personToMeet else "Reception Desk"}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600; border-bottom: 1px solid #f1f5f9;">Department:</td>
                                <td style="padding: 10px 0; font-weight: 700; color: #0f172a; font-size: 13px; border-bottom: 1px solid #f1f5f9;">${visitor.department}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600; border-bottom: 1px solid #f1f5f9;">Type &amp; Purpose:</td>
                                <td style="padding: 10px 0; color: #0f172a; font-size: 13px; border-bottom: 1px solid #f1f5f9;">${visitor.visitType} &bull; ${visitor.purpose}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600;">Contact Phone:</td>
                                <td style="padding: 10px 0; font-weight: 700; color: #0f172a; font-size: 13px;">
                                    <a href="tel:${visitor.mobileNumber.replace(" ", "")}" style="color: #0284c7; text-decoration: none; font-weight: 700;">📞 ${visitor.mobileNumber}</a>
                                </td>
                            </tr>
                            ${if (visitor.email.isNotBlank()) """
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; font-size: 13px; font-weight: 600; border-top: 1px solid #f1f5f9;">Email Address:</td>
                                <td style="padding: 10px 0; border-top: 1px solid #f1f5f9;">
                                    <a href="mailto:${visitor.email}" style="color: #0284c7; text-decoration: none; font-size: 13px; font-weight: 600;">✉️ ${visitor.email}</a>
                                </td>
                            </tr>
                            """ else ""}
                        </table>

                        <!-- Host Action Callout Box -->
                        <div style="margin-top: 22px; padding: 14px 18px; background: linear-gradient(135deg, #eff6ff 0%, #f0fdf4 100%); border-left: 4px solid #0284c7; border-radius: 8px;">
                            <p style="margin: 0; font-size: 13px; font-weight: 700; color: #0f2942;">Host Action Recommended:</p>
                            <p style="margin: 4px 0 0 0; font-size: 12px; color: #334155; line-height: 1.5;">
                                Your visitor is waiting at the Reception / Main Lobby. Please receive your visitor promptly or inform the receptionist if you are in a meeting.
                            </p>
                        </div>
                    </div>

                    <!-- Footer -->
                    <div style="padding: 16px 28px; font-size: 11px; color: #94a3b8; text-align: center; border-top: 1px solid #f1f5f9; background-color: #f8fafc;">
                        Dispatched automatically by SAIT Pass+ &bull; Confidential
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        // 1. Automatically send Official Pass & Safety Briefing to visitor's email
        val cleanVisitorEmail = visitor.email.trim()
        if (cleanVisitorEmail.isNotBlank() && cleanVisitorEmail.contains("@")) {
            val regDateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(visitor.timestamp))
            val regTimeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(visitor.timestamp))
            val visitorSubject = "[Visitor Pass - ${visitor.passNumber}] Official Pass & Safety Briefing - ${branch.name}"
            val visitorBody = buildString {
                appendLine("=================================================================")
                appendLine("${branch.name.uppercase()} — OFFICIAL VISITOR PASS & SAFETY INDUCTION")
                appendLine("=================================================================")
                appendLine("Dear ${visitor.fullName},")
                appendLine("Welcome to ${branch.name} (${branch.displayFacilityLocation}).")
                appendLine("Your visitor pass has been confirmed and registered in our central security system.")
                appendLine()
                appendLine("OFFICIAL VISITOR PASS DETAILS:")
                appendLine("-----------------------------------------------------------------")
                appendLine("• Pass Number:       ${visitor.passNumber}")
                appendLine("• Registration Date: $regDateStr")
                appendLine("• Registration Time: $regTimeStr")
                appendLine("• Visitor Name:      ${visitor.fullName}")
                appendLine("• Company:           ${visitor.companyName}")
                appendLine("• Facility Address:  ${branch.displayFacilityLocation}")
                appendLine("• Department:        ${visitor.department}")
                appendLine("• Host / To Meet:    ${if (visitor.personToMeet.isNotBlank()) visitor.personToMeet else "Reception Desk"}")
                appendLine("• Purpose:           ${visitor.purpose}")
                appendLine()
                appendLine("MANDATORY QHSE SAFETY GUIDELINES:")
                appendLine("-----------------------------------------------------------------")
                appendLine(branch.safetyGuidelines)
                appendLine()
                appendLine("KEY SAFETY RULES FOR VISITORS:")
                appendLine("1. Visitor Identification: Keep your visitor pass visible at all times.")
                appendLine("2. Escort Requirement: Remain accompanied by your host in all operational areas.")
                appendLine("3. Mandatory PPE: Wear safety shoes, high-vis vest, and eye protection in workshop zones.")
                appendLine("4. Photography: Unauthorized photography or filming is strictly prohibited.")
                appendLine("5. Emergency Evacuation: In case of alarm, follow green signs to Assembly Point 1.")
                appendLine()
                appendLine("EMERGENCY ASSISTANCE & HELPLINE:")
                appendLine("-----------------------------------------------------------------")
                if (branch.safetyContactName.isNotBlank()) {
                    appendLine("Safety Officer: ${branch.safetyContactName}")
                }
                appendLine("Emergency Hotline: ${branch.safetyHelpline}")
                appendLine()
                appendLine("Thank you for your cooperation and keeping our workplace safe.")
                appendLine("${branch.name} Facility & Security Management")
            }

            val visitorHtml = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"/></head>
                <body style="margin: 0; padding: 20px; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <div style="max-width: 620px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(15, 41, 66, 0.08); border: 1px solid #e2e8f0;">
                        <!-- Header Banner -->
                        <div style="background: linear-gradient(135deg, #0F2942 0%, #173859 60%, #0284C7 100%); padding: 24px 28px; color: #ffffff;">
                            <div style="display: inline-block; padding: 4px 10px; border-radius: 20px; background: rgba(56, 189, 248, 0.22); border: 1px solid rgba(125, 211, 252, 0.5); color: #bae6fd; font-size: 11px; font-weight: 700; letter-spacing: 0.6px; margin-bottom: 8px;">
                                OFFICIAL DIGITAL PASS &bull; VERIFIED
                            </div>
                            <h1 style="margin: 0; font-size: 21px; font-weight: 800; letter-spacing: 0.3px; color: #ffffff;">OFFICIAL VISITOR PASS</h1>
                            <p style="margin: 6px 0 0 0; font-size: 13px; color: #bae6fd; font-weight: 500;">${branch.name} &bull; ${branch.displayFacilityLocation}</p>
                        </div>

                        <!-- Pass ID Card Showcase -->
                        <div style="padding: 24px 28px 12px 28px;">
                            <div style="background: linear-gradient(135deg, #0F2942 0%, #0284C7 100%); border-radius: 14px; padding: 18px 22px; color: #ffffff; text-align: center; box-shadow: 0 6px 18px rgba(2, 132, 199, 0.25);">
                                <span style="font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 1.2px; color: #bae6fd;">AUTHORIZED VISITOR ACCESS</span>
                                <div style="font-size: 26px; font-weight: 900; letter-spacing: 2px; font-family: 'SF Mono', Consolas, Monaco, monospace; margin: 6px 0;">
                                    ${visitor.passNumber}
                                </div>
                                <span style="font-size: 12px; opacity: 0.9;">Please present this pass ID at security checkpoints</span>
                            </div>
                        </div>

                        <!-- Main Content -->
                        <div style="padding: 12px 28px 24px 28px;">
                            <p style="font-size: 14px; color: #334155; margin-top: 10px;">Dear <strong>${visitor.fullName}</strong>,</p>
                            <p style="font-size: 13px; color: #475569; line-height: 1.5; margin: 0 0 16px 0;">
                                Your visitor pass for <strong>${branch.name}</strong> is active. Please keep this digital receipt accessible on your phone during your visit.
                            </p>

                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px; width: 36%; border-bottom: 1px solid #f1f5f9;">Registration Date:</td>
                                    <td style="padding: 8px 0; font-weight: 700; color: #0f172a; font-size: 13px; border-bottom: 1px solid #f1f5f9;">$regDateStr</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px; border-bottom: 1px solid #f1f5f9;">Registration Time:</td>
                                    <td style="padding: 8px 0; font-weight: 700; color: #0284c7; font-size: 13px; border-bottom: 1px solid #f1f5f9;">$regTimeStr</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px; width: 36%; border-bottom: 1px solid #f1f5f9;">Visitor Name:</td>
                                    <td style="padding: 8px 0; font-weight: 800; color: #0f172a; font-size: 14px; border-bottom: 1px solid #f1f5f9;">${visitor.fullName}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px; border-bottom: 1px solid #f1f5f9;">Company:</td>
                                    <td style="padding: 8px 0; font-weight: 700; color: #0f172a; font-size: 13px; border-bottom: 1px solid #f1f5f9;">${visitor.companyName}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px; border-bottom: 1px solid #f1f5f9;">Department:</td>
                                    <td style="padding: 8px 0; font-weight: 700; color: #0284c7; font-size: 13px; border-bottom: 1px solid #f1f5f9;">${visitor.department}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px; border-bottom: 1px solid #f1f5f9;">Host / Meeting:</td>
                                    <td style="padding: 8px 0; font-weight: 700; color: #0f172a; font-size: 13px; border-bottom: 1px solid #f1f5f9;">${if (visitor.personToMeet.isNotBlank()) visitor.personToMeet else "Reception Desk"}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-size: 13px;">Facility Location:</td>
                                    <td style="padding: 8px 0; color: #0f172a; font-size: 13px;">${branch.displayFacilityLocation}</td>
                                </tr>
                            </table>

                            <!-- QHSE Safety Rules Card -->
                            <div style="background-color: #fffbeb; border: 1.5px solid #fde68a; border-radius: 12px; padding: 16px 18px; margin-bottom: 16px;">
                                <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                                    <span style="font-size: 18px;">🛡️</span>
                                    <h3 style="margin: 0; color: #92400e; font-size: 14px; font-weight: 800;">MANDATORY QHSE SAFETY GUIDELINES</h3>
                                </div>
                                <p style="margin: 0 0 10px 0; font-size: 12px; color: #78350f; line-height: 1.55; white-space: pre-line;">${branch.safetyGuidelines}</p>
                                <ul style="margin: 0; padding-left: 18px; font-size: 12px; color: #78350f; line-height: 1.55;">
                                    <li><b>Wear PPE:</b> Safety footwear, high-visibility vest &amp; eye protection required in workshop zones.</li>
                                    <li><b>Badge Display:</b> Keep your badge visible at all times on company premises.</li>
                                    <li><b>Stay Accompanied:</b> Do not enter unauthorized or restricted production areas unescorted.</li>
                                    <li><b>No Filming:</b> Photography and recording are strictly prohibited on site.</li>
                                </ul>
                            </div>

                            <!-- Emergency Helpline Box -->
                            <div style="background-color: #f0fdf4; border: 1.5px solid #bbf7d0; border-radius: 12px; padding: 14px 18px;">
                                <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap;">
                                    <div>
                                        <h4 style="margin: 0 0 4px 0; color: #166534; font-size: 13px; font-weight: 800;">🚨 EMERGENCY &amp; SAFETY HELPLINE</h4>
                                        ${if (branch.safetyContactName.isNotBlank()) "<p style=\"margin: 0 0 4px 0; font-size: 12px; font-weight: 600; color: #166534;\">Safety Officer: " + branch.safetyContactName + "</p>" else ""}
                                        <p style="margin: 0; font-size: 12px; color: #15803d;">In event of fire alarm, proceed calmly to <b>Assembly Point 1</b>.</p>
                                    </div>
                                    <div style="margin-top: 8px;">
                                        <a href="tel:${branch.safetyHelpline.replace(" ", "")}" style="display: inline-block; background-color: #16a34a; color: #ffffff; padding: 8px 14px; border-radius: 8px; text-decoration: none; font-size: 12px; font-weight: 700;">
                                            📞 ${branch.safetyHelpline}
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="padding: 16px 28px; font-size: 11px; color: #94a3b8; text-align: center; border-top: 1px solid #f1f5f9; background-color: #f8fafc;">
                            Issued by SAIT Group QHSE &bull; Security &amp; Facility Operations Management
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            if (currentSmtp.enabled && currentSmtp.host.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val visitorSmtpRes = SmtpMailer.sendEmail(
                        config = currentSmtp,
                        recipients = listOf(cleanVisitorEmail),
                        subject = visitorSubject,
                        bodyText = visitorBody,
                        bodyHtml = visitorHtml
                    )
                    if (visitorSmtpRes.isSuccess) {
                        addNotification(
                            AppNotification(
                                title = "Pass & Safety Briefing Sent",
                                message = "Official pass ${visitor.passNumber} & safety briefing emailed via SMTP to $cleanVisitorEmail.",
                                type = NotificationType.EMAIL_SENT,
                                passNumber = visitor.passNumber
                            )
                        )
                    } else {
                        val err = visitorSmtpRes.exceptionOrNull()?.message ?: "SMTP connection error"
                        addNotification(
                            AppNotification(
                                title = "Visitor Pass SMTP Error",
                                message = "Failed to email pass to $cleanVisitorEmail ($err). Opening email app fallback.",
                                type = NotificationType.EMAIL_FAILED,
                                passNumber = visitor.passNumber
                            )
                        )
                        if (triggerIntentFallbackIfNoSmtp) {
                            _eventFlow.emit(
                                UiEvent.SendEmailNotification(
                                    recipients = listOf(cleanVisitorEmail),
                                    subject = visitorSubject,
                                    body = visitorBody
                                )
                            )
                        }
                    }
                }
            } else if (triggerIntentFallbackIfNoSmtp) {
                viewModelScope.launch {
                    _eventFlow.emit(
                        UiEvent.SendEmailNotification(
                            recipients = listOf(cleanVisitorEmail),
                            subject = visitorSubject,
                            body = visitorBody
                        )
                    )
                }
                addNotification(
                    AppNotification(
                        title = "Pass & Briefing Ready to Send",
                        message = "Prepared official pass & safety briefing for $cleanVisitorEmail. Configure SMTP in Admin for automatic background delivery.",
                        type = NotificationType.EMAIL_SENT,
                        passNumber = visitor.passNumber
                    )
                )
            }

            if (currentSmtp.firebaseTriggerEnabled) {
                viewModelScope.launch {
                    firestoreService.recordEmailNotification(
                        visitorPassNumber = visitor.passNumber,
                        branchName = branch.name,
                        department = "VISITOR_PASS_RECEIPT",
                        recipients = listOf(cleanVisitorEmail),
                        subject = visitorSubject,
                        body = visitorBody,
                        htmlBody = visitorHtml
                    )
                }
            }
        }

        // 2. Dispatch Department Alert via Outgoing SMTP Mail Server if enabled
        if (currentSmtp.enabled && currentSmtp.host.isNotBlank() && recipients.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val smtpRes = SmtpMailer.sendEmail(
                    config = currentSmtp,
                    recipients = recipients,
                    subject = subject,
                    bodyText = emailBody,
                    bodyHtml = emailHtml
                )
                if (smtpRes.isSuccess) {
                    addNotification(
                        AppNotification(
                            title = "Department SMTP Alert Sent",
                            message = "Automated alert sent to ${recipients.size} recipient(s) (${visitor.department}).",
                            type = NotificationType.EMAIL_SENT,
                            passNumber = visitor.passNumber
                        )
                    )
                } else {
                    val err = smtpRes.exceptionOrNull()?.message ?: "SMTP delivery error"
                    addNotification(
                        AppNotification(
                            title = "Department SMTP Error",
                            message = "$err",
                            type = NotificationType.EMAIL_FAILED,
                            passNumber = visitor.passNumber
                        )
                    )
                    if (triggerIntentFallbackIfNoSmtp && cleanVisitorEmail.isBlank()) {
                        _eventFlow.emit(
                            UiEvent.SendEmailNotification(
                                recipients = recipients,
                                subject = subject,
                                body = emailBody
                            )
                        )
                    }
                }
            }
        } else if (recipients.isNotEmpty() && triggerIntentFallbackIfNoSmtp && cleanVisitorEmail.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(
                    UiEvent.SendEmailNotification(
                        recipients = recipients,
                        subject = subject,
                        body = emailBody
                    )
                )
            }
        }

        // 3. Queue Department Alert in Firebase 'mail' collection
        if (currentSmtp.firebaseTriggerEnabled && recipients.isNotEmpty()) {
            viewModelScope.launch {
                firestoreService.recordEmailNotification(
                    visitorPassNumber = visitor.passNumber,
                    branchName = branch.name,
                    department = visitor.department,
                    recipients = recipients,
                    subject = subject,
                    body = emailBody,
                    htmlBody = emailHtml
                )
            }
        }

        // 4. Record Visitor Check-In Notification
        addNotification(
            AppNotification(
                title = "Visitor Checked In",
                message = "${visitor.fullName} (${visitor.companyName}) registered for ${visitor.department}. Pass: ${visitor.passNumber}",
                type = NotificationType.VISITOR_CHECK_IN,
                passNumber = visitor.passNumber
            )
        )
    }

    fun showPass(visitor: VisitorEntity) {
        _activeVisitorPass.value = visitor

        if (_userRole.value == UserRole.VISITOR_KIOSK && _autoResetSeconds.value > 0) {
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                val total = _autoResetSeconds.value
                for (i in total downTo 1) {
                    _passCountdownSeconds.value = i
                    delay(1000)
                }
                dismissPass()
            }
        }
    }

    fun dismissPass() {
        countdownJob?.cancel()
        _activeVisitorPass.value = null
        _passCountdownSeconds.value = 0
    }

    fun prefillForRepeatVisitor(visitor: VisitorEntity) {
        _formState.value = RegistrationFormState(
            fullName = visitor.fullName,
            countryCode = _activeBranch.value.countryCode.ifBlank { "+971" },
            mobileNumber = visitor.mobileNumber,
            email = visitor.email,
            companyName = visitor.companyName,
            visitType = visitor.visitType,
            department = visitor.department,
            personToMeet = visitor.personToMeet
        )
    }

    fun checkOutVisitor(id: Long) {
        viewModelScope.launch {
            val target = allVisitorsList.value.find { it.id == id }
            repository.checkOutVisitor(id)
            _eventFlow.emit(UiEvent.ShowSnackbar("Visitor marked as checked out."))
            val active = _activeVisitorPass.value
            if (active != null && active.id == id) {
                _activeVisitorPass.value = active.copy(status = "CHECKED_OUT")
            }
            addNotification(
                AppNotification(
                    title = "Visitor Checked Out",
                    message = "${target?.fullName ?: "Visitor"} (${target?.passNumber ?: "#$id"}) completed visit and checked out.",
                    type = NotificationType.VISITOR_CHECK_OUT,
                    passNumber = target?.passNumber
                )
            )
        }
    }

    // Theme Management - Strictly Local to this Device (not synced across terminals)
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        kioskPrefs.themeMode = mode
    }

    fun toggleTheme() {
        val nextMode = when (_themeMode.value) {
            KioskPreferences.THEME_LIGHT -> KioskPreferences.THEME_DARK
            KioskPreferences.THEME_DARK -> KioskPreferences.THEME_LIGHT
            else -> KioskPreferences.THEME_DARK
        }
        setThemeMode(nextMode)
    }

    // Liquid Glass Background Palette Style Management - Local to this Device
    fun setLiquidPaletteStyle(style: String = KioskPreferences.PALETTE_OCEANIC) {
        _liquidPaletteStyle.value = KioskPreferences.PALETTE_OCEANIC
        kioskPrefs.liquidPaletteStyle = KioskPreferences.PALETTE_OCEANIC
    }

    fun toggleLiquidPaletteStyle() {
        setLiquidPaletteStyle(KioskPreferences.PALETTE_OCEANIC)
    }

    // SMTP Configuration Management - Saves locally immediately and syncs to Firebase if signed in
    fun updateSmtpConfig(config: SmtpConfig) {
        _smtpConfig.value = config
        kioskPrefs.saveSmtpConfig(config)
        if (_currentUser.value != null) {
            syncAllSettingsToFirebase()
        }
        viewModelScope.launch {
            val status = if (config.enabled && config.host.isNotBlank()) {
                "SMTP active (${config.host}:${config.port}) — automatic pass & briefing email enabled!"
            } else {
                "Outgoing mail server settings saved."
            }
            _eventFlow.emit(UiEvent.ShowSnackbar(status))
        }
    }

    suspend fun testSmtpConnection(targetRecipient: String): Result<String> {
        return SmtpMailer.testConnection(_smtpConfig.value, targetRecipient)
    }

    fun resetPassSequenceCounter() {
        val branch = _activeBranch.value
        val (prefix, city) = repository.resolvePrefixAndCity(branch)
        val yy = java.text.SimpleDateFormat("yy", java.util.Locale.US).format(java.util.Date())
        kioskPrefs.resetPassSequence(prefix, city, yy, 1)
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Pass sequence counter reset for $prefix-$city-$yy (starts from 0001)."))
        }
    }

    // Notification Center Management
    fun openNotificationCenter() {
        _showNotificationCenter.value = true
    }

    fun closeNotificationCenter() {
        _showNotificationCenter.value = false
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun addNotification(notification: AppNotification) {
        _notifications.value = listOf(notification) + _notifications.value.take(49)
    }

    fun retrySync(visitor: VisitorEntity) {
        viewModelScope.launch {
            val success = repository.retrySync(visitor)
            if (success) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Synced to Cloud Firestore!"))
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar("Sign in with Google in Admin tab to sync."))
            }
        }
    }

    fun syncAllPending(silent: Boolean = false) {
        viewModelScope.launch {
            _isSyncingAll.value = true
            val count = repository.syncAllPending()
            _isSyncingAll.value = false
            if (!silent) {
                if (count > 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Synced $count record(s) to Cloud Firestore!"))
                } else if (_currentUser.value == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Sign in with Google in Admin tab to sync with Firebase."))
                } else {
                    _eventFlow.emit(UiEvent.ShowSnackbar("All records are up to date in Cloud Firestore."))
                }
            }
        }
    }

    fun getCsvData(): String {
        return repository.exportToCsv(allVisitorsList.value)
    }

    fun setTouchlessKioskMode(enabled: Boolean) {
        _isTouchlessKioskMode.value = enabled
        kioskPrefs.touchlessKioskMode = enabled
    }

    fun toggleTouchlessKioskMode() {
        setTouchlessKioskMode(!_isTouchlessKioskMode.value)
    }

    fun refreshTouchlessSession() {
        _touchlessSessionId.value = generateTouchlessSessionId()
    }

    fun updateTouchlessGithubUrl(url: String) {
        val clean = url.trim().ifBlank { KioskPreferences.DEFAULT_GITHUB_PAGES_URL }
        _touchlessGithubUrl.value = clean
        kioskPrefs.touchlessGithubPageUrl = clean
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("GitHub Pages URL updated."))
        }
    }

    fun showNotificationSnackbar(msg: String) {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar(msg))
        }
    }

    private val processedRemotePassNumbers = mutableSetOf<String>()

    private fun handleIncomingRemoteVisitor(remote: VisitorEntity) {
        val branch = _activeBranch.value
        val (prefix, city) = repository.resolvePrefixAndCity(branch)
        val canonicalCode = "$prefix-$city"

        val isForCurrentBranch = remote.branchName.equals(branch.name, ignoreCase = true) ||
                (remote.branchCode.isNotBlank() && (
                    remote.branchCode.equals(branch.shortCode, ignoreCase = true) ||
                    remote.branchCode.equals(canonicalCode, ignoreCase = true) ||
                    remote.branchCode.contains(branch.shortCode, ignoreCase = true) ||
                    branch.shortCode.contains(remote.branchCode, ignoreCase = true)
                )) || remote.branchCode.isBlank()

        val timeDiff = Math.abs(System.currentTimeMillis() - remote.timestamp)
        val isFresh = timeDiff < 180_000L // 3 minutes window for live check-ins

        if (isFresh && isForCurrentBranch && remote.status == "CHECKED_IN") {
            // Keep local sequence counter synchronized with valid sequential pass numbers
            val seqNum = remote.passNumber.takeLast(4).toIntOrNull() ?: 0
            if (seqNum in 1..999) {
                val yy = java.text.SimpleDateFormat("yy", java.util.Locale.US).format(java.util.Date())
                kioskPrefs.setSequenceCounter(prefix, city, yy, seqNum)
            }

            viewModelScope.launch(Dispatchers.Main) {
                // Instantly pop up the official visitor pass on screen for Kiosk or Touchless mode
                if (_userRole.value == UserRole.VISITOR_KIOSK || _isTouchlessKioskMode.value) {
                    showPass(remote)
                    _eventFlow.emit(UiEvent.ShowSnackbar("✨ Touchless Check-in Verified: Welcome, ${remote.fullName}! (Pass: ${remote.passNumber})"))
                } else {
                    _eventFlow.emit(UiEvent.ShowSnackbar("🔔 Mobile Check-In: ${remote.fullName} (${remote.companyName}) registered (Pass: ${remote.passNumber})."))
                }
            }

            // Automatically email official Visitor Pass & Mandatory Safety Briefing to the visitor and notify department
            dispatchRegistrationEmails(remote, branch, triggerIntentFallbackIfNoSmtp = true)
        }
    }

    fun simulateMobileTouchlessRegistration(
        fullName: String,
        mobileNumber: String,
        companyName: String,
        visitType: String = "Walk-In",
        department: String,
        purpose: String,
        personToMeet: String,
        email: String
    ) {
        viewModelScope.launch {
            val branch = _activeBranch.value
            val qrVisitType = if (visitType.contains("(QR)", ignoreCase = true)) {
                visitType.trim()
            } else {
                "${visitType.trim()} (QR)"
            }
            val result = repository.registerVisitor(
                fullName = fullName,
                mobileNumber = mobileNumber,
                companyName = companyName,
                visitType = qrVisitType,
                department = department,
                purpose = purpose,
                customPurpose = "",
                personToMeet = personToMeet,
                email = email,
                branch = branch
            )

            result.onSuccess { visitor ->
                val sig = buildVisitorSignature(visitor)
                processedRemotePassNumbers.add(visitor.passNumber)
                processedRemotePassNumbers.add(sig)
                kioskPrefs.markRemoteVisitorProcessed(visitor.passNumber, sig)
                showPass(visitor)
                refreshTouchlessSession()
                dispatchRegistrationEmails(visitor, branch, triggerIntentFallbackIfNoSmtp = true)
                _eventFlow.emit(UiEvent.ShowSnackbar("✨ Touchless Check-in Verified: Welcome, ${visitor.fullName}!"))
            }.onFailure {
                _eventFlow.emit(UiEvent.ShowSnackbar("Failed to register mobile visitor."))
            }
        }
    }

    fun deleteVisitor(id: Long) {
        if (!requireFirebaseLoginForAdminEdit()) return
        viewModelScope.launch {
            val synced = repository.deleteVisitor(id)
            val msg = if (synced) {
                "Visitor log removed from local database and Firebase."
            } else {
                "Visitor log removed."
            }
            _eventFlow.emit(UiEvent.ShowSnackbar(msg))
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            val synced = repository.clearAllVisitors()
            resetPassSequenceCounter()
            val msg = if (synced) {
                "All visitor records cleared and pass sequence reset to 0001."
            } else {
                "Local visitor records cleared and pass sequence reset to 0001."
            }
            _eventFlow.emit(UiEvent.ShowSnackbar(msg))
        }
    }

}

class VisitorViewModelFactory(
    private val repository: VisitorRepository,
    private val kioskPrefs: KioskPreferences,
    private val firestoreService: FirestoreService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VisitorViewModel(repository, kioskPrefs, firestoreService) as T
    }
}
