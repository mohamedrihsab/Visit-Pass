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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Branch
import com.example.data.UserRole
import com.example.data.VisitorEntity
import com.example.ui.VisitorViewModel
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.liquidAppearAnimation
import com.example.ui.theme.liquidButtonGlass
import com.example.ui.theme.liquidFilterChipColors
import com.example.ui.theme.liquidInnerGlass
import com.example.ui.theme.liquidPressEffect
import com.example.ui.theme.liquidTextFieldColors
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

    val startOfTodayMillis = remember(allVisitors.size) { getStartOfTodayMillis() }
    val sevenDaysAgoMillis = remember(allVisitors.size) { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val thirtyDaysAgoMillis = remember(allVisitors.size) { System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 }

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
                    visitor.visitType.startsWith(selectedVisitType, ignoreCase = true)

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
                            val scopeBranchName = if (logBranchFilter == "ALL") "All_Branches" else activeBranch.name
                            com.example.util.GoogleDriveExportHelper.shareCsvFile(
                                context = context,
                                csvContent = csv,
                                branchName = scopeBranchName
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
        scopedVisitors.count { it.visitType.startsWith("Walk-In", ignoreCase = true) }
    }
    val appointmentCount = remember(scopedVisitors) {
        scopedVisitors.count { it.visitType.startsWith("Appointment", ignoreCase = true) }
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

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .liquidAppearAnimation(delayMillis = 0)
            .testTag("admin_comprehensive_dashboard"),
        cornerRadius = 22.dp,
        elevated = true
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Executive Visitor Intelligence",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Scope: $scopeLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .liquidPressEffect()
                            .testTag("admin_logbook_export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableView,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export CSV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .liquidPressEffect(pressedScale = 0.90f)
                            .liquidInnerGlass(shape = CircleShape, cornerRadius = 13.dp)
                            .clickable(onClick = onToggleExpanded)
                            .testTag("toggle_admin_analytics_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Detailed Charts" else "Expand Detailed Charts",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
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
                    accentColor = if (pendingSyncCount == 0) StatusSuccess else MaterialTheme.colorScheme.error,
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

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
    Box(
        modifier = modifier.liquidInnerGlass(
            shape = RoundedCornerShape(10.dp),
            cornerRadius = 10.dp,
            accentColor = accentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidInnerGlass(shape = RoundedCornerShape(16.dp), cornerRadius = 16.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = "Branch Traffic",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Branch Traffic Distribution",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap to filter",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidPressEffect(pressedScale = 0.985f)
                        .liquidInnerGlass(
                            shape = RoundedCornerShape(12.dp),
                            cornerRadius = 12.dp,
                            selected = isSelected
                        )
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
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
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
                                        color = StatusSuccess.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "+$branchToday today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
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
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
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
        Box(
            modifier = Modifier
                .weight(1f)
                .liquidInnerGlass(shape = RoundedCornerShape(16.dp), cornerRadius = 16.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            )
                        }
                    }
                }
            }
        }

        // Purpose Breakdown Card
        Box(
            modifier = Modifier
                .weight(1f)
                .liquidInnerGlass(shape = RoundedCornerShape(16.dp), cornerRadius = 16.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidInnerGlass(shape = RoundedCornerShape(16.dp), cornerRadius = 16.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (topCompanies.isEmpty()) {
                Text(
                    text = "No organization data yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topCompanies, key = { it.key }) { (company, count) ->
                        Row(
                            modifier = Modifier
                                .liquidInnerGlass(shape = RoundedCornerShape(12.dp), cornerRadius = 12.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .liquidAppearAnimation(delayMillis = 45),
        cornerRadius = 20.dp,
        elevated = false
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
                shape = RoundedCornerShape(14.dp),
                colors = liquidTextFieldColors(),
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
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier
                            .liquidPressEffect()
                            .testTag("filter_branch_all")
                    )
                }
                item {
                    val activeCount = allVisitors.count { it.matchesBranch(activeBranch) }
                    FilterChip(
                        selected = logBranchFilter == "CURRENT",
                        onClick = { onBranchFilterChange("CURRENT") },
                        label = { Text("${activeBranch.shortCode} (Active: $activeCount)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier
                            .liquidPressEffect()
                            .testTag("filter_branch_current")
                    )
                }
                items(branches.filter { it.id != activeBranch.id }, key = { it.id }) { branch ->
                    val bCount = allVisitors.count { it.matchesBranch(branch) }
                    FilterChip(
                        selected = logBranchFilter.equals(branch.name, ignoreCase = true) ||
                                logBranchFilter.equals(branch.shortCode, ignoreCase = true),
                        onClick = { onBranchFilterChange(branch.name) },
                        label = { Text("${branch.shortCode} ($bCount)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier.liquidPressEffect()
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
                        label = { Text(range.label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(useSecondaryAccent = true),
                        modifier = Modifier.liquidPressEffect()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Walk-In",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Walk-In") "ALL" else "Walk-In")
                        },
                        label = { Text("Walk-In Only") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier.liquidPressEffect()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Appointment",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Appointment") "ALL" else "Appointment")
                        },
                        label = { Text("Appointments Only") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(useSecondaryAccent = true),
                        modifier = Modifier.liquidPressEffect()
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
                            label = { Text("All Depts") },
                            shape = RoundedCornerShape(12.dp),
                            colors = liquidFilterChipColors(),
                            modifier = Modifier.liquidPressEffect()
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
                            label = { Text(dept) },
                            shape = RoundedCornerShape(12.dp),
                            colors = liquidFilterChipColors(),
                            modifier = Modifier.liquidPressEffect()
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
                        modifier = Modifier
                            .liquidPressEffect()
                            .testTag("admin_reset_filters_btn")
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
        branchVisitors.count { it.visitType.startsWith("Walk-In", ignoreCase = true) }
    }
    val branchAppointments = remember(branchVisitors) {
        branchVisitors.count { it.visitType.startsWith("Appointment", ignoreCase = true) }
    }
    val branchSynced = remember(branchVisitors) {
        branchVisitors.count { it.isSyncedToGoogleForm }
    }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .liquidAppearAnimation(delayMillis = 0)
            .testTag("receptionist_branch_logbook_header"),
        cornerRadius = 22.dp,
        elevated = true
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
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(34.dp)
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
                            Text(
                                text = activeBranch.shortCode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "${activeBranch.name} Visitor Logbook",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Facility Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = activeBranch.displayFacilityLocation,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .liquidInnerGlass(
                            shape = RoundedCornerShape(8.dp),
                            cornerRadius = 8.dp,
                            accentColor = MaterialTheme.colorScheme.secondary
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${activeBranch.city} Only",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
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
                shape = RoundedCornerShape(14.dp),
                colors = liquidTextFieldColors(),
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
                        label = { Text("All (${branchVisitors.size})") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier.liquidPressEffect()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTimeRange == LogTimeRange.TODAY,
                        onClick = { onTimeRangeChange(LogTimeRange.TODAY) },
                        label = { Text("Today ($branchToday)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier.liquidPressEffect()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTimeRange == LogTimeRange.LAST_7_DAYS,
                        onClick = { onTimeRangeChange(LogTimeRange.LAST_7_DAYS) },
                        label = { Text("Last 7 Days") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(),
                        modifier = Modifier.liquidPressEffect()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Walk-In",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Walk-In") "ALL" else "Walk-In")
                        },
                        label = { Text("Walk-In") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(useSecondaryAccent = true),
                        modifier = Modifier.liquidPressEffect()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedVisitType == "Appointment",
                        onClick = {
                            onVisitTypeChange(if (selectedVisitType == "Appointment") "ALL" else "Appointment")
                        },
                        label = { Text("Appointment") },
                        shape = RoundedCornerShape(12.dp),
                        colors = liquidFilterChipColors(useSecondaryAccent = true),
                        modifier = Modifier.liquidPressEffect()
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
                            label = { Text("All ${activeBranch.shortCode} Depts") },
                            shape = RoundedCornerShape(12.dp),
                            colors = liquidFilterChipColors(),
                            modifier = Modifier.liquidPressEffect()
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
                            label = { Text(dept) },
                            shape = RoundedCornerShape(12.dp),
                            colors = liquidFilterChipColors(),
                            modifier = Modifier.liquidPressEffect()
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.liquidPressEffect()
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
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .liquidAppearAnimation(delayMillis = 20)
            .testTag("visitor_card_${visitor.id}"),
        cornerRadius = 16.dp,
        elevated = false
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Pass Pill + Branch Pill on left, Sync + Visit Type + Delete on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Pass Number Refractive Glass Pill
                    Row(
                        modifier = Modifier
                            .liquidInnerGlass(
                                shape = RoundedCornerShape(7.dp),
                                cornerRadius = 7.dp,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Pass",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = visitor.passNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Cloud Sync Status + Visit Type + Admin Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (visitor.isSyncedToGoogleForm) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = if (visitor.isSyncedToGoogleForm) "Synced to Cloud" else "Pending Cloud Sync",
                        modifier = Modifier.size(15.dp),
                        tint = if (visitor.isSyncedToGoogleForm) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isAppointment = visitor.visitType.startsWith("Appointment", ignoreCase = true)
                    val isQrEntry = visitor.visitType.contains("(QR)", ignoreCase = true)
                    val visitAccent = if (isAppointment) {
                        MaterialTheme.colorScheme.tertiary
                    } else if (isQrEntry) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Row(
                        modifier = Modifier
                            .liquidInnerGlass(
                                shape = CircleShape,
                                cornerRadius = 12.dp,
                                accentColor = visitAccent
                            )
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAppointment) {
                                Icons.Default.EventAvailable
                            } else {
                                Icons.Default.VerifiedUser
                            },
                            contentDescription = visitor.visitType,
                            tint = visitAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = visitor.visitType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = visitAccent,
                            maxLines = 1
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onDeleteClick,
                            enabled = isFirebaseSignedIn,
                            modifier = Modifier
                                .size(28.dp)
                                .liquidPressEffect(enabled = isFirebaseSignedIn)
                                .testTag("delete_visitor_btn_${visitor.id}")
                        ) {
                            Icon(
                                imageVector = if (isFirebaseSignedIn) Icons.Default.DeleteOutline else Icons.Default.Lock,
                                contentDescription = if (isFirebaseSignedIn) "Delete Visitor Record" else "Sign in to Firebase to delete",
                                tint = if (isFirebaseSignedIn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Visitor Name
            Text(
                text = visitor.fullName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Company & Phone row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Company",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visitor.companyName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Mobile",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visitor.mobileNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (visitor.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email ID",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visitor.email,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            // Department, Purpose, Host & Timestamp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .liquidInnerGlass(
                                shape = RoundedCornerShape(6.dp),
                                cornerRadius = 6.dp,
                                accentColor = MaterialTheme.colorScheme.secondary
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = visitor.department,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val displayPurpose = if (visitor.purpose == "Other" && visitor.customPurpose.isNotBlank()) {
                        visitor.customPurpose
                    } else {
                        visitor.purpose
                    }
                    Text(
                        text = "• $displayPurpose",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (visitor.personToMeet.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Host",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Host: ${visitor.personToMeet}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Check-In Time",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Registered: ${visitor.checkInFormattedTime}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .liquidInnerGlass(
                        shape = CircleShape,
                        cornerRadius = 38.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Empty Logbook",
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
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
                    "No visitors matched your filters in $branchName."
                } else {
                    "No visitor entries recorded for $branchName."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onResetFilters != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.liquidPressEffect()
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
}
