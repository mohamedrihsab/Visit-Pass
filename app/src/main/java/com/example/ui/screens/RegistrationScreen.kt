package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.data.CountryDialOption
import com.example.data.UserRole
import com.example.data.VisitorEntity
import com.example.ui.VisitorViewModel
import com.example.ui.components.SafetyInfoDialog
import com.example.ui.theme.SaitDarkBackground
import com.example.ui.theme.SaitPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    viewModel: VisitorViewModel,
    contentPadding: PaddingValues
) {
    val formState by viewModel.formState.collectAsState()
    val isKioskMode by viewModel.isKioskMode.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val activeBranch by viewModel.activeBranch.collectAsState()
    val recentVisitors by viewModel.allVisitorsList.collectAsState()
    val purposes by viewModel.visitPurposes.collectAsState()
    val isTouchlessKioskMode by viewModel.isTouchlessKioskMode.collectAsState()
    var showSafetyDialog by remember { mutableStateOf(false) }

    // Ensure the default country code selected for mobile number matches the selected branch
    LaunchedEffect(activeBranch.id, activeBranch.countryCode) {
        val branchDialCode = activeBranch.countryCode.ifBlank {
            BranchRegistry.inferCountryCode(activeBranch.city, activeBranch.name)
        }
        viewModel.onCountryCodeChange(branchDialCode)
    }

    // Ensure selected purpose stays valid if Admin edits the Purpose of Visit options
    LaunchedEffect(purposes) {
        if (purposes.isNotEmpty() && formState.purpose !in purposes) {
            viewModel.onPurposeChange(purposes.first())
        }
    }

    val departments = activeBranch.defaultDepartments

    // If Kiosk is set to Touchless Station mode, display the Touchless QR scanning interface
    if (userRole == UserRole.VISITOR_KIOSK && isTouchlessKioskMode) {
        com.example.ui.components.TouchlessKioskView(
            viewModel = viewModel,
            activeBranch = activeBranch,
            onSwitchToTouchscreen = { viewModel.setTouchlessKioskMode(false) },
            modifier = Modifier.padding(contentPadding)
        )
        return
    }

    if (showSafetyDialog) {
        SafetyInfoDialog(
            branch = activeBranch,
            onDismiss = { showSafetyDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .testTag("registration_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Header with company logo
        item {
            HeroHeader(
                activeBranch = activeBranch,
                userRole = userRole
            )
        }

        // Safety Information & Guidelines Quick Access Button (Visible to ALL users: Visitor, Receptionist, Admin)
        item {
            OutlinedButton(
                onClick = { showSafetyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("btn_view_safety_info"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${activeBranch.name} Safety Information & Emergency Exit Plan",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        // In Kiosk Mode, show prominent Touchless Mobile Check-In card
        if (userRole == UserRole.VISITOR_KIOSK) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { viewModel.setTouchlessKioskMode(true) }
                        .testTag("card_touchless_kiosk_prompt"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📱 Prefer Touchless Check-in?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Scan QR with your phone to register without touching the screen.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { viewModel.setTouchlessKioskMode(true) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_open_touchless_mode")
                        ) {
                            Text("Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // In Non-Kiosk Mode (Admin or Receptionist), show repeat visitor quick-fill
        if (userRole != UserRole.VISITOR_KIOSK && recentVisitors.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Quick-Fill from Recent Visitors",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentVisitors.distinctBy { it.fullName }.take(5)) { visitor ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.prefillForRepeatVisitor(visitor) }
                                    .testTag("quickfill_${visitor.fullName.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${visitor.fullName} (${visitor.companyName})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Registration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visitor Registration",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 1. Full Name
                    OutlinedTextField(
                        value = formState.fullName,
                        onValueChange = { viewModel.onFullNameChange(it) },
                        label = { Text("Full Name *") },
                        placeholder = { Text("Enter visitor full name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (formState.fullName.isNotBlank()) {
                                IconButton(onClick = { viewModel.onFullNameChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        isError = formState.fullNameError != null,
                        supportingText = {
                            if (formState.fullNameError != null) {
                                Text(formState.fullNameError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_full_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 2. Mobile Number with Aligned All-Countries Selector
                    var showCountryPickerDialog by remember { mutableStateOf(false) }
                    var selectedCountryName by remember(activeBranch.id) { mutableStateOf<String?>(null) }
                    val branchDefaultCode = activeBranch.countryCode.ifBlank { "+971" }
                    val countryOptions = remember(branchDefaultCode, activeBranch.city) {
                        val base = BranchRegistry.COUNTRY_DIAL_OPTIONS
                        if (base.any { it.dialCode == branchDefaultCode }) {
                            base
                        } else {
                            listOf(
                                CountryDialOption(
                                    dialCode = branchDefaultCode,
                                    label = "🌐 ${activeBranch.city.take(10)}",
                                    countryName = activeBranch.city
                                )
                            ) + base
                        }
                    }
                    val selectedCountryOption = remember(countryOptions, formState.countryCode, selectedCountryName) {
                        if (selectedCountryName != null) {
                            countryOptions.find { it.countryName == selectedCountryName && it.dialCode == formState.countryCode }
                                ?: countryOptions.find { it.dialCode == formState.countryCode }
                        } else {
                            countryOptions.find { it.dialCode == formState.countryCode }
                        }
                    }

                    if (showCountryPickerDialog) {
                        CountryCodePickerDialog(
                            options = countryOptions,
                            selectedDialCode = formState.countryCode,
                            selectedCountryName = selectedCountryOption?.countryName,
                            onSelect = { option ->
                                selectedCountryName = option.countryName
                                viewModel.onCountryCodeChange(option.dialCode)
                                showCountryPickerDialog = false
                            },
                            onDismiss = { showCountryPickerDialog = false }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Country Code Selector Box (Aligned OutlinedTextField)
                        Box(
                            modifier = Modifier.width(138.dp)
                        ) {
                            OutlinedTextField(
                                value = if (selectedCountryOption != null) {
                                    "${selectedCountryOption.flagEmoji} ${formState.countryCode}"
                                } else {
                                    "🌐 ${formState.countryCode}"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Country *") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Country Code",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                supportingText = {
                                    Text(
                                        text = selectedCountryOption?.countryName ?: "Select country",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Transparent clickable overlay covering the text field box
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 8.dp, bottom = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCountryPickerDialog = true }
                                    .testTag("dropdown_country_code")
                            )
                        }

                        // Mobile Number Entry Box (Aligned OutlinedTextField)
                        OutlinedTextField(
                            value = formState.mobileNumber,
                            onValueChange = { viewModel.onMobileNumberChange(it) },
                            label = { Text("Mobile Number *") },
                            placeholder = { Text("501234567") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (formState.mobileNumber.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onMobileNumberChange("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = formState.mobileError != null,
                            supportingText = {
                                Text(
                                    text = formState.mobileError ?: "Enter digits only",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (formState.mobileError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_mobile"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 3. Visitor Email ID (Mandatory)
                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email ID *") },
                        placeholder = { Text("visitor@company.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (formState.email.isNotBlank()) {
                                IconButton(onClick = { viewModel.onEmailChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email
                        ),
                        isError = formState.emailError != null,
                        supportingText = {
                            Text(
                                text = formState.emailError ?: "Your digital pass and safety guidelines will be emailed automatically",
                                color = if (formState.emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 4. Company Name
                    OutlinedTextField(
                        value = formState.companyName,
                        onValueChange = { viewModel.onCompanyNameChange(it) },
                        label = { Text("Company Name *") },
                        placeholder = { Text("Company or organization name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (formState.companyName.isNotBlank()) {
                                IconButton(onClick = { viewModel.onCompanyNameChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        isError = formState.companyError != null,
                        supportingText = {
                            if (formState.companyError != null) {
                                Text(formState.companyError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_company"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 4. Type of Visit
                    Column {
                        Text(
                            text = "Type of Visit *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Walk-In", "Appointment").forEach { type ->
                                val selected = formState.visitType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.onVisitTypeChange(type) },
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
                                        .height(44.dp)
                                        .testTag("chip_visit_${type.lowercase().replace("-", "")}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // 5. Department to Visit
                    Column {
                        Text(
                            text = "Department to Visit *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            departments.forEach { dept ->
                                val selected = formState.department == dept
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.onDepartmentChange(dept) },
                                    label = {
                                        Text(
                                            text = dept,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("chip_dept_${dept.replace(" ", "_").replace("&", "and")}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // 6. Purpose of Visit
                    Column {
                        Text(
                            text = "Purpose of Visit *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            purposes.forEach { purpose ->
                                val selected = formState.purpose == purpose
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.onPurposeChange(purpose) },
                                    label = {
                                        Text(
                                            text = purpose,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("chip_purpose_${purpose.lowercase().replace(" ", "_")}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }

                        // Animated Other Purpose text field
                        AnimatedVisibility(
                            visible = formState.purpose == "Other",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = formState.customPurpose,
                                onValueChange = { viewModel.onCustomPurposeChange(it) },
                                label = { Text("Specify Other Purpose *") },
                                placeholder = { Text("Describe reason for visit") },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    keyboardType = KeyboardType.Text
                                ),
                                isError = formState.customPurposeError != null,
                                supportingText = {
                                    if (formState.customPurposeError != null) {
                                        Text(formState.customPurposeError!!, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .testTag("input_custom_purpose"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 7. Person to Meet (Optional)
                    OutlinedTextField(
                        value = formState.personToMeet,
                        onValueChange = { viewModel.onPersonToMeetChange(it) },
                        label = { Text("Person to Meet (Optional)") },
                        placeholder = { Text("Host name or employee to see") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (formState.personToMeet.isNotBlank()) {
                                IconButton(onClick = { viewModel.onPersonToMeetChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_person_to_meet"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = { viewModel.submitRegistration() },
                        enabled = !formState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_submit_registration"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Registering Visit Online...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Register & Get Visitor Pass",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Subtitle Note
                    Text(
                        text = "Once submitted, your visit is synced directly to ${activeBranch.name} and your official pass & safety guidelines are emailed to your inbox.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HeroHeader(
    activeBranch: Branch,
    userRole: UserRole
) {
    val roleSubtitle = when (userRole) {
        UserRole.VISITOR_KIOSK -> "VISITOR REGISTRATION"
        UserRole.RECEPTIONIST -> "RECEPTION MANUAL ENTRY DESK"
        UserRole.ADMIN -> "ADMIN VISITOR REGISTRATION"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SaitPrimary,
                        Color(0xFF034A6E)
                    )
                )
            )
            .padding(top = 20.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Company Logo or Security Badge (Always Solid White Background)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("company_logo_header")
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
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.sait_logo),
                                contentDescription = "SAIT Group Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .padding(6.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SAIT GROUP",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = activeBranch.city,
                                    color = Color(0xFFE0F2FE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = activeBranch.name,
                            color = Color(0xFFBAE6FD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Facility Location: ${activeBranch.displayFacilityLocation}",
                            color = Color(0xFFE0F2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = roleSubtitle,
                            color = Color(0xFF7DD3FC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Text Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Welcome to ${activeBranch.name}! Kindly fill below details",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CountryCodePickerDialog(
    options: List<CountryDialOption>,
    selectedDialCode: String,
    selectedCountryName: String? = null,
    onSelect: (CountryDialOption) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isBlank()) {
            options
        } else {
            val q = searchQuery.trim()
            options.filter {
                it.countryName.contains(q, ignoreCase = true) ||
                    it.label.contains(q, ignoreCase = true) ||
                    it.dialCode.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Country & Dial Code",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country or dial code (e.g. UAE, +971)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Country"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_country")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${filteredOptions.size} countries available",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(
                        items = filteredOptions,
                        key = { "${it.countryName}_${it.dialCode}" }
                    ) { option ->
                        val isSelected = if (selectedCountryName != null) {
                            option.countryName == selectedCountryName && option.dialCode == selectedDialCode
                        } else {
                            option.dialCode == selectedDialCode
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .testTag("country_opt_${option.countryName.replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = option.flagEmoji,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = option.countryName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = option.dialCode,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

