package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Branch
import com.example.data.UserRole
import com.example.data.VisitorEntity
import com.example.ui.VisitorViewModel
import java.util.Calendar
import kotlin.math.roundToInt

private enum class LogTimeRange(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

private fun VisitorEntity.matchesBranch(branch: Branch): Boolean {
    return branchName.equals(branch.name, ignoreCase = true) ||
            (branchCode.isNotBlank() && branchCode.equals(branch.shortCode, ignoreCase = true))
}

private fun getStartOfTodayMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    viewModel: VisitorViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visitors by viewModel.visitors.collectAsStateWithLifecycle()
    val allVisitors by viewModel.allVisitorsList.collectAsStateWithLifecycle()
    val branches by viewModel.branches.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val logBranchFilter by viewModel.logBranchFilter.collectAsStateWithLifecycle()
    val activeBranch by viewModel.activeBranch.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val isAdmin = userRole == UserRole.ADMIN
    val isFirebaseSignedIn = currentUser != null

    var visitorPendingDelete by remember { mutableStateOf<VisitorEntity?>(null) }
    var selectedTimeRange by remember { mutableStateOf(LogTimeRange.ALL_TIME) }
    var selectedDepartment by remember { mutableStateOf("ALL") }
    var selectedVisitType by remember { mutableStateOf("ALL") }
    var isAnalyticsExpanded by remember { mutableStateOf(true) }

    val startOfTodayMillis = remember { getStartOfTodayMillis() }
    val sevenDaysAgoMillis = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val thirtyDaysAgoMillis = remember { System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 }

    // Visitors strictly scoped to the selected active branch (always used for Receptionist stats & list)
    val activeBranchAllVisitors = remember(allVisitors, activeBranch) {
        allVisitors.filter { it.matchesBranch(activeBranch) }
    }

    // Base dataset for analytics cards depending on role & selected branch scope
    val scopedAnalyticsVisitors = remember(allVisitors, activeBranchAllVisitors, isAdmin, logBranchFilter, activeBranch) {
        if (!isAdmin) {
            activeBranchAllVisitors
        } else {
            when (logBranchFilter) {
                "ALL", null -> allVisitors
                "CURRENT" -> activeBranchAllVisitors
                else -> allVisitors.filter {
                    it.branchName.equals(logBranchFilter, ignoreCase = true) ||
                            it.branchCode.equals(logBranchFilter, ignoreCase = true)
                }
            }
        }
    }

    // Apply additional UI filters (Time Range, Department, Visit Type) on top of ViewModel's role-safe visitors flow
    val displayedVisitors = remember(
        visitors,
        activeBranch,
        isAdmin,
        selectedTimeRange,
        selectedDepartment,
        selectedVisitType,
        startOfTodayMillis,
        sevenDaysAgoMillis,
        thirtyDaysAgoMillis
    ) {
        visitors.filter { visitor ->
            val branchSafe = if (!isAdmin) visitor.matchesBranch(activeBranch) else true

            val matchesTime = when (selectedTimeRange) {
                LogTimeRange.ALL_TIME -> true
                LogTimeRange.TODAY -> visitor.timestamp >= startOfTodayMillis
                LogTimeRange.LAST_7_DAYS -> visitor.timestamp >= sevenDaysAgoMillis
                LogTimeRange.LAST_30_DAYS -> visitor.timestamp >= thirtyDaysAgoMillis
            }

            val matchesDept = selectedDepartment == "ALL" ||
                    visitor.department.equals(selectedDepartment, ignoreCase = true)

            val matchesType = selectedVisitType == "ALL" ||
                    visitor.visitType.equals(selectedVisitType, ignoreCase = true)

            branchSafe && matchesTime && matchesDept && matchesType
        }
    }

    val availableDepartments = remember(scopedAnalyticsVisitors, activeBranch, isAdmin) {
        if (!isAdmin) {
            (activeBranch.defaultDepartments + scopedAnalyticsVisitors.map { it.department })
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() }
                .distinct()
        } else {
            scopedAnalyticsVisitors
                .map { it.department.trim().uppercase() }
                .filter { it.isNotBlank() }
                .distinct()
                .ifEmpty { activeBranch.defaultDepartments }
        }
    }

    val hasExtraFilters = selectedTimeRange != LogTimeRange.ALL_TIME ||
            selectedDepartment != "ALL" ||
            selectedVisitType != "ALL" ||
            searchQuery.isNotBlank() ||
            (isAdmin && logBranchFilter != "ALL" && logBranchFilter != null)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxSize()
                .testTag("logbook_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAdmin) {
                // =========================================================================
                // ADMIN MODE: COMPREHENSIVE EXECUTIVE VISITOR LOGBOOK DASHBOARD
                // =========================================================================
                item(key = "admin_dashboard_header") {
                    AdminComprehensiveDashboard(
                        allVisitors = allVisitors,
                        scopedVisitors = scopedAnalyticsVisitors,
                        branches = branches,
                        activeBranch = activeBranch,
                        logBranchFilter = logBranchFilter,
                        startOfTodayMillis = startOfTodayMillis,
                        sevenDaysAgoMillis = sevenDaysAgoMillis,
                        isExpanded = isAnalyticsExpanded,
                        onToggleExpanded = { isAnalyticsExpanded = !isAnalyticsExpanded },
                        onSelectBranchScope = { filterValue ->
                            viewModel.setLogBranchFilter(filterValue)
                        },
                        onSelectDepartment = { dept ->
                            selectedDepartment = if (selectedDepartment == dept) "ALL" else dept
                        },
                        selectedDepartment = selectedDepartment,
                        onExportCsv = {
                            val csv = viewModel.getCsvData()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "SAIT_Visitor_Logbook_Export.csv")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Export Visitor Logbook CSV")
                            )
                        }
                    )
                }

                // Multi-Dimensional Filter Controls for Admin
                item(key = "admin_filters_card") {
                    AdminMultiFilterCard(
                        allVisitors = allVisitors,
                        branches = branches,
                        activeBranch = activeBranch,
                        logBranchFilter = logBranchFilter,
                        selectedTimeRange = selectedTimeRange,
                        selectedVisitType = selectedVisitType,
                        selectedDepartment = selectedDepartment,
                        availableDepartments = availableDepartments,
                        searchQuery = searchQuery,
                        displayedCount = displayedVisitors.size,
                        totalScopedCount = scopedAnalyticsVisitors.size,
                        hasExtraFilters = hasExtraFilters,
                        onBranchFilterChange = { viewModel.setLogBranchFilter(it) },
                        onTimeRangeChange = { selectedTimeRange = it },
                        onVisitTypeChange = { selectedVisitType = it },
                        onDepartmentChange = { selectedDepartment = it },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onResetFilters = {
                            viewModel.setLogBranchFilter("ALL")
                            viewModel.setSearchQuery("")
                            selectedTimeRange = LogTimeRange.ALL_TIME
                            selectedDepartment = "ALL"
                            selectedVisitType = "ALL"
                        }
                    )
                }
            } else {
                // =========================================================================
                // RECEPTIONIST MODE: STRICTLY BRANCH-SCOPED VISITOR LOGBOOK
                // =========================================================================
                item(key = "receptionist_branch_header") {
                    ReceptionistBranchLogbookHeader(
                        activeBranch = activeBranch,
                        branchVisitors = activeBranchAllVisitors,
                        displayedVisitorsCount = displayedVisitors.size,
                        startOfTodayMillis = startOfTodayMillis,
                        selectedTimeRange = selectedTimeRange,
                        selectedDepartment = selectedDepartment,
                        selectedVisitType = selectedVisitType,
                        availableDepartments = availableDepartments,
                        searchQuery = searchQuery,
                        onTimeRangeChange = { selectedTimeRange = it },
                        onDepartmentChange = { selectedDepartment = it },
                        onVisitTypeChange = { selectedVisitType = it },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onResetFilters = {
                            viewModel.setSearchQuery("")
                            selectedTimeRange = LogTimeRange.ALL_TIME
                            selectedDepartment = "ALL"
                            selectedVisitType = "ALL"
                        }
                    )
                }
            }

            // Visitor Records List or Empty State
            if (displayedVisitors.isEmpty()) {
                item(key = "empty_logbook_state") {
                    EmptyLogbookState(
                        isSearching = searchQuery.isNotBlank() ||
                                selectedTimeRange != LogTimeRange.ALL_TIME ||
                                selectedDepartment != "ALL" ||
                                selectedVisitType != "ALL",
                        branchName = if (isAdmin && (logBranchFilter == "ALL" || logBranchFilter == null)) {
                            "All Group Branches"
                        } else {
                            activeBranch.name
                        },
                        onResetFilters = if (hasExtraFilters) {
                            {
                                viewModel.setSearchQuery("")
                                selectedTimeRange = LogTimeRange.ALL_TIME
                                selectedDepartment = "ALL"
                                selectedVisitType = "ALL"
                                if (isAdmin) viewModel.setLogBranchFilter("ALL")
                            }
                        } else null
                    )
                }
            } else {
                items(
                    items = displayedVisitors,
                    key = { it.id }
                ) { visitor ->
                    VisitorLogCard(
                        visitor = visitor,
                        isAdmin = isAdmin,
                        isFirebaseSignedIn = isFirebaseSignedIn,
                        showBranchBadge = isAdmin,
                        onDeleteClick = { visitorPendingDelete = visitor }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation Dialog before deleting a visitor record (Admin only)
    visitorPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { visitorPendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Record",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Visitor Record?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove ${target.fullName} (Pass ${target.passNumber}) from the visitor logbook?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVisitor(target.id)
                        visitorPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_visitor_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { visitorPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================================
// ADMIN COMPREHENSIVE DASHBOARD COMPOSABLES
// =========================================================================================

@Composable
private fun AdminComprehensiveDashboard(
    allVisitors: List<VisitorEntity>,
    scopedVisitors: List<VisitorEntity>,
    branches: List<Branch>,
    activeBranch: Branch,
    logBranchFilter: String?,
    startOfTodayMillis: Long,
    sevenDaysAgoMillis: Long,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectBranchScope: (String) -> Unit,
    onSelectDepartment: (String) -> Unit,
    selectedDepartment: String,
    onExportCsv: () -> Unit
) {
    val totalScoped = scopedVisitors.size
    val todayCount = remember(scopedVisitors, startOfTodayMillis) {
        scopedVisitors.count { it.timestamp >= startOfTodayMillis }
    }
    val last7DaysCount = remember(scopedVisitors, sevenDaysAgoMillis) {
        scopedVisitors.count { it.timestamp >= sevenDaysAgoMillis }
    }
    val walkInCount = remember(scopedVisitors) {
        scopedVisitors.count { it.visitType.equals("Walk-In", ignoreCase = true) }
    }
    val appointmentCount = remember(scopedVisitors) {
        scopedVisitors.count { it.visitType.equals("Appointment", ignoreCase = true) }
    }
    val syncedCount = remember(scopedVisitors) {
        scopedVisitors.count { it.isSyncedToGoogleForm }
    }
    val pendingSyncCount = (totalScoped - syncedCount).coerceAtLeast(0)
    val emailCoverageCount = remember(scopedVisitors) {
        scopedVisitors.count { it.email.isNotBlank() }
    }
    val uniqueCompaniesCount = remember(scopedVisitors) {
        scopedVisitors.map { it.companyName.trim().lowercase() }.filter { it.isNotBlank() }.distinct().size
    }
    val activeBranchesWithTraffic = remember(allVisitors) {
        allVisitors.map { it.branchName.trim().lowercase() }.filter { it.isNotBlank() }.distinct().size
    }

    val scopeLabel = when (logBranchFilter) {
        "ALL", null -> "All Group Branches (${branches.size} Configured)"
        "CURRENT" -> "${activeBranch.name} (${activeBranch.shortCode})"
        else -> {
            val matched = branches.find {
                it.name.equals(logBranchFilter, ignoreCase = true) ||
                        it.shortCode.equals(logBranchFilter, ignoreCase = true)
            }
            matched?.let { "${it.name} (${it.shortCode})" } ?: logBranchFilter
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_comprehensive_dashboard"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Executive Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(44.dp)
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
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "Executive Analytics",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Executive Visitor Intelligence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scope: $scopeLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_logbook_export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableView,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.testTag("toggle_admin_analytics_btn")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Detailed Charts" else "Expand Detailed Charts"
                        )
                    }
                }
            }

            // Primary 6-Card Executive Metrics Grid (2 Rows x 3 Columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExecutiveKpiTile(
                    icon = Icons.Default.Groups,
                    label = "Total Visitors",
                    value = totalScoped.toString(),
                    subtitle = "${allVisitors.size} Org-Wide",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ExecutiveKpiTile(
                    icon = Icons.Default.CalendarToday,
                    label = "Today's Traffic",
                    value = todayCount.toString(),
                    subtitle = "$last7DaysCount in last 7d",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                val walkInPct = if (totalScoped > 0) ((walkInCount * 100f) / totalScoped).roundToInt() else 0
                ExecutiveKpiTile(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Walk-In / Appt",
                    value = "$walkInCount / $appointmentCount",
                    subtitle = "$walkInPct% Walk-In",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExecutiveKpiTile(
                    icon = Icons.Default.CloudDone,
                    label = "Cloud Synced",
                    value = "$syncedCount / $totalScoped",
                    subtitle = if (pendingSyncCount == 0) "100% Synced" else "$pendingSyncCount Pending",
                    accentColor = if (pendingSyncCount == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                val emailPct = if (totalScoped > 0) ((emailCoverageCount * 100f) / totalScoped).roundToInt() else 0
                ExecutiveKpiTile(
                    icon = Icons.Default.Email,
                    label = "Email Passes",
                    value = emailCoverageCount.toString(),
                    subtitle = "$emailPct% Digital Delivery",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ExecutiveKpiTile(
                    icon = Icons.Default.Business,
                    label = "Organizations",
                    value = uniqueCompaniesCount.toString(),
                    subtitle = "$activeBranchesWithTraffic/${branches.size} Active Branches",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Collapsible Deep-Dive Breakdown Panels
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1. Branch-by-Branch Visitor Distribution (Interactive)
                    BranchDistributionPanel(
                        allVisitors = allVisitors,
                        branches = branches,
                        startOfTodayMillis = startOfTodayMillis,
                        logBranchFilter = logBranchFilter,
                        activeBranch = activeBranch,
                        onSelectBranchScope = onSelectBranchScope
                    )

                    // 2. Department & Visit Purpose Breakdown
                    DepartmentAndPurposeBreakdownPanel(
                        scopedVisitors = scopedVisitors,
                        selectedDepartment = selectedDepartment,
                        onSelectDepartment = onSelectDepartment
                    )

                    // 3. Top Visiting Companies & Recent Activity Insights
                    TopCompaniesAndPeakInsightsPanel(
                        scopedVisitors = scopedVisitors
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutiveKpiTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BranchDistributionPanel(
    allVisitors: List<VisitorEntity>,
    branches: List<Branch>,
    startOfTodayMillis: Long,
    logBranchFilter: String?,
    activeBranch: Branch,
    onSelectBranchScope: (String) -> Unit
) {
    val totalAll = allVisitors.size.coerceAtLeast(1)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = "Branch Traffic",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Branch-by-Branch Visitor Distribution",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Tap branch to filter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            branches.forEach { branch ->
                val branchTotal = allVisitors.count { it.matchesBranch(branch) }
                val branchToday = allVisitors.count { it.matchesBranch(branch) && it.timestamp >= startOfTodayMillis }
                val ratio = (branchTotal.toFloat() / totalAll.toFloat()).coerceIn(0f, 1f)
                val pct = if (allVisitors.isNotEmpty()) ((branchTotal * 100f) / allVisitors.size).roundToInt() else 0

                val isSelected = (logBranchFilter == "CURRENT" && branch.id == activeBranch.id) ||
                        logBranchFilter.equals(branch.name, ignoreCase = true) ||
                        logBranchFilter.equals(branch.shortCode, ignoreCase = true)

                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) {
                                onSelectBranchScope("ALL")
                            } else {
                                onSelectBranchScope(branch.name)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = branch.shortCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${branch.name} (${branch.city})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (branchToday > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "+$branchToday today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$branchTotal ($pct%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentAndPurposeBreakdownPanel(
    scopedVisitors: List<VisitorEntity>,
    selectedDepartment: String,
    onSelectDepartment: (String) -> Unit
) {
    val totalScoped = scopedVisitors.size.coerceAtLeast(1)

    val topDepartments = remember(scopedVisitors) {
        scopedVisitors
            .groupingBy { it.department.trim().uppercase().ifBlank { "GENERAL" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    val topPurposes = remember(scopedVisitors) {
        scopedVisitors
            .groupingBy {
                if (it.purpose.equals("Other", ignoreCase = true) && it.customPurpose.isNotBlank()) {
                    it.customPurpose.trim()
                } else {
                    it.purpose.trim().ifBlank { "General Visit" }
                }
            }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Department Traffic Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "Departments",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Department Traffic",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (topDepartments.isEmpty()) {
                    Text(
                        text = "No department data yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topDepartments.forEach { (dept, count) ->
                        val isDeptSelected = selectedDepartment.equals(dept, ignoreCase = true)
                        val ratio = (count.toFloat() / totalScoped.toFloat()).coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectDepartment(dept) }
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dept,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isDeptSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isDeptSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Purpose Breakdown Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Visit Purposes",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Visit Purposes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (topPurposes.isEmpty()) {
                    Text(
                        text = "No purpose data yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topPurposes.forEach { (purpose, count) ->
                        val ratio = (count.toFloat() / totalScoped.toFloat()).coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = purpose,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCompaniesAndPeakInsightsPanel(
    scopedVisitors: List<VisitorEntity>
) {
    val topCompanies = remember(scopedVisitors) {
        scopedVisitors
            .groupingBy { it.companyName.trim().ifBlank { "Independent" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    val topHosts = remember(scopedVisitors) {
        scopedVisitors
            .map { it.personToMeet.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Top Visiting Companies",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Top Visiting Organizations & Frequent Hosts",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (topCompanies.isEmpty()) {
                Text(
                    text = "Register visitors to see top organizations and host insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topCompanies, key = { it.key }) { (company, count) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = company,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (topHosts.isNotEmpty()) {
                    Text(
                        text = "Frequent Hosts: " + topHosts.joinToString(" • ") { "${it.key} (${it.value})" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMultiFilterCard(
    allVisitors: List<VisitorEntity>,
    branches: List<Branch>,
    activeBranch: Branch,
    logBranchFilter: String?,
    selectedTimeRange: LogTimeRange,
    selectedVisitType: String,
    selectedDepartment: String,
    availableDepartments: List<String>,
    searchQuery: String,
    displayedCount: Int,
    totalScopedCount: Int,
    hasExtraFilters: Boolean,
    onBranchFilterChange: (String) -> Unit,
    onTimeRangeChange: (LogTimeRange) -> Unit,
    onVisitTypeChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onResetFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search across branches by name, pass, company, host, mobile, email...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Visitors"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logbook_search_input")
            )

            // 1. Branch Scope Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = "Branch:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    FilterChip(
                        selected = logBranchFilter == "ALL" || logBranchFilter == null,
                        onClick = { onBranchFilterChange("ALL") },
                        label = { Text("All Group Branches (${allVisitors.size})") },
                        modifier = Modifier.testTag("filter_branch_all")
                    )
                }
                item {
                    val activeCount = allVisitors.count { it.matchesBranch(activeBranch) }
                    FilterChip(
                        selected = logBranchFilter == "CURRENT",
                        onClick = { onBranchFilterChange("CURRENT") },
                        label = { Text("${activeBranch.shortCode} (Active: $activeCount)") },
                        modifier = Modifier.testTag("filter_branch_current")
                    )
                }
                items(branches.filter { it.id != activeBranch.id }, key = { it.id }) { branch ->
                    val bCount = allVisitors.count { it.matchesBranch(branch) }
                    FilterChip(
                        selected = logBranchFilter.equals(branch.name, ignoreCase = true) ||
                                logBranchFilter.equals(branch.shortCode, ignoreCase = true),
                        onClick = { onBranchFilterChange(branch.name) },
                        label = { Text("${branch.shortCode} ($bCount)") }
                    )
                }
            }

            // 2. Time Range & Visit Type Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = "Period & Type:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(LogTimeRange.entries) { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { onTimeRangeChange(range) },
                        label = { Text(range.label) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Walk-In",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Walk-In") "ALL" else "Walk-In")
                        },
                        label = { Text("Walk-In Only") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Appointment",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Appointment") "ALL" else "Appointment")
                        },
                        label = { Text("Appointments Only") }
                    )
                }
            }

            // 3. Department Filter Row
            if (availableDepartments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "Department:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDepartment == "ALL",
                            onClick = { onDepartmentChange("ALL") },
                            label = { Text("All Depts") }
                        )
                    }
                    items(availableDepartments, key = { it }) { dept ->
                        FilterChip(
                            selected = selectedDepartment.equals(dept, ignoreCase = true),
                            onClick = {
                                onDepartmentChange(
                                    if (selectedDepartment.equals(dept, ignoreCase = true)) "ALL" else dept
                                )
                            },
                            label = { Text(dept) }
                        )
                    }
                }
            }

            // Filter summary bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing $displayedCount of $totalScopedCount visitor records",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (hasExtraFilters) {
                    TextButton(
                        onClick = onResetFilters,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("admin_reset_filters_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = "Reset Filters",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Filters", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// RECEPTIONIST STRICTLY BRANCH-SCOPED LOGBOOK HEADER
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceptionistBranchLogbookHeader(
    activeBranch: Branch,
    branchVisitors: List<VisitorEntity>,
    displayedVisitorsCount: Int,
    startOfTodayMillis: Long,
    selectedTimeRange: LogTimeRange,
    selectedDepartment: String,
    selectedVisitType: String,
    availableDepartments: List<String>,
    searchQuery: String,
    onTimeRangeChange: (LogTimeRange) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onVisitTypeChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onResetFilters: () -> Unit
) {
    val branchTotal = branchVisitors.size
    val branchToday = remember(branchVisitors, startOfTodayMillis) {
        branchVisitors.count { it.timestamp >= startOfTodayMillis }
    }
    val branchWalkIns = remember(branchVisitors) {
        branchVisitors.count { it.visitType.equals("Walk-In", ignoreCase = true) }
    }
    val branchAppointments = remember(branchVisitors) {
        branchVisitors.count { it.visitType.equals("Appointment", ignoreCase = true) }
    }
    val branchSynced = remember(branchVisitors) {
        branchVisitors.count { it.isSyncedToGoogleForm }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receptionist_branch_logbook_header"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Branch-Specific Reception Identity Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        if (!activeBranch.logoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = activeBranch.logoUri,
                                contentDescription = "${activeBranch.name} Logo",
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = activeBranch.shortCode,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${activeBranch.name} Visitor Logbook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Facility Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeBranch.displayFacilityLocation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${activeBranch.city} Only",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Branch-Only Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExecutiveKpiTile(
                    icon = Icons.Default.CalendarToday,
                    label = "Today (${activeBranch.shortCode})",
                    value = branchToday.toString(),
                    subtitle = "Checked in today",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                ExecutiveKpiTile(
                    icon = Icons.Default.Groups,
                    label = "Branch Total",
                    value = branchTotal.toString(),
                    subtitle = activeBranch.shortCode,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ExecutiveKpiTile(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Walk-In / Appt",
                    value = "$branchWalkIns / $branchAppointments",
                    subtitle = "$branchSynced Synced",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar scoped to this branch
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search ${activeBranch.shortCode} visitors by name, pass, company, host...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Branch Visitors"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logbook_search_input")
            )

            // Time & Visit Type Filters for Active Branch
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedTimeRange == LogTimeRange.ALL_TIME,
                        onClick = { onTimeRangeChange(LogTimeRange.ALL_TIME) },
                        label = { Text("All (${branchVisitors.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTimeRange == LogTimeRange.TODAY,
                        onClick = { onTimeRangeChange(LogTimeRange.TODAY) },
                        label = { Text("Today ($branchToday)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTimeRange == LogTimeRange.LAST_7_DAYS,
                        onClick = { onTimeRangeChange(LogTimeRange.LAST_7_DAYS) },
                        label = { Text("Last 7 Days") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Walk-In",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Walk-In") "ALL" else "Walk-In")
                        },
                        label = { Text("Walk-In") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Appointment",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Appointment") "ALL" else "Appointment")
                        },
                        label = { Text("Appointment") }
                    )
                }
            }

            // Branch Department Filter Chips
            if (availableDepartments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedDepartment == "ALL",
                            onClick = { onDepartmentChange("ALL") },
                            label = { Text("All ${activeBranch.shortCode} Depts") }
                        )
                    }
                    items(availableDepartments, key = { it }) { dept ->
                        FilterChip(
                            selected = selectedDepartment.equals(dept, ignoreCase = true),
                            onClick = {
                                onDepartmentChange(
                                    if (selectedDepartment.equals(dept, ignoreCase = true)) "ALL" else dept
                                )
                            },
                            label = { Text(dept) }
                        )
                    }
                }
            }

            if (displayedVisitorsCount != branchTotal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing $displayedVisitorsCount of $branchTotal records for ${activeBranch.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onResetFilters,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear Filters", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// SHARED VISITOR RECORD CARD & EMPTY STATE
// =========================================================================================

@Composable
private fun VisitorLogCard(
    visitor: VisitorEntity,
    isAdmin: Boolean,
    isFirebaseSignedIn: Boolean,
    showBranchBadge: Boolean,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visitor_card_${visitor.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Pass Number Pill
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Pass",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = visitor.passNumber,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Branch Pill (shown in Admin mode so multi-branch records are immediately identifiable)
                    if (showBranchBadge && visitor.branchName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = "Branch",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = visitor.branchCode.ifBlank { visitor.branchName },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Cloud Sync Status + Visit Type + Admin Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (visitor.isSyncedToGoogleForm) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = if (visitor.isSyncedToGoogleForm) "Synced to Cloud" else "Pending Cloud Sync",
                        modifier = Modifier.size(18.dp),
                        tint = if (visitor.isSyncedToGoogleForm) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    )

                    Surface(
                        color = if (visitor.visitType.equals("Appointment", ignoreCase = true)) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        },
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (visitor.visitType.equals("Appointment", ignoreCase = true)) {
                                    Icons.Default.EventAvailable
                                } else {
                                    Icons.Default.VerifiedUser
                                },
                                contentDescription = visitor.visitType,
                                tint = if (visitor.visitType.equals("Appointment", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = visitor.visitType,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (visitor.visitType.equals("Appointment", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onDeleteClick,
                            enabled = isFirebaseSignedIn,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_visitor_btn_${visitor.id}")
                        ) {
                            Icon(
                                imageVector = if (isFirebaseSignedIn) Icons.Default.DeleteOutline else Icons.Default.Lock,
                                contentDescription = if (isFirebaseSignedIn) "Delete Visitor Record" else "Sign in to Firebase to delete",
                                tint = if (isFirebaseSignedIn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visitor Name & Company
            Text(
                text = visitor.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Company",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visitor.companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Mobile",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visitor.mobileNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (visitor.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email ID",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visitor.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Department, Purpose, Host & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = visitor.department,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayPurpose = if (visitor.purpose == "Other" && visitor.customPurpose.isNotBlank()) {
                            visitor.customPurpose
                        } else {
                            visitor.purpose
                        }
                        Text(
                            text = "• $displayPurpose",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (visitor.personToMeet.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Host",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Host: ${visitor.personToMeet}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Check-In Time",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Registered: ${visitor.checkInFormattedTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLogbookState(
    isSearching: Boolean,
    branchName: String,
    onResetFilters: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Empty Logbook",
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isSearching) "No Matching Visitor Records" else "No Visitor Records Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isSearching) {
                "No visitors matched your current filters in $branchName."
            } else {
                "Visitors registered at $branchName will appear here automatically."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onResetFilters != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResetFilters,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAltOff,
                    contentDescription = "Reset Filters",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset All Filters")
            }
        }
    }
}
