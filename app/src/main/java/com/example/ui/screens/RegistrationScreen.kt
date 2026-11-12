package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.data.CountryDialOption
import com.example.data.UserRole
import com.example.ui.VisitorViewModel
import com.example.ui.components.CompanyBrandLogoBadge
import com.example.ui.components.SafetyInfoDialog
import com.example.ui.components.TouchlessKioskView
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassSegmentedControl
import com.example.ui.theme.liquidAppearAnimation
import com.example.ui.theme.liquidButtonGlass
import com.example.ui.theme.liquidFilterChipColors
import com.example.ui.theme.liquidHeroGlass
import com.example.ui.theme.liquidInnerGlass
import com.example.ui.theme.liquidModalGlass
import com.example.ui.theme.liquidPressEffect
import com.example.ui.theme.liquidTextFieldColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    viewModel: VisitorViewModel,
    contentPadding: PaddingValues,
    onNavigateToSafety: () -> Unit = {}
) {
    val formState by viewModel.formState.collectAsState()
    val activeBranch by viewModel.activeBranch.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val visitPurposes by viewModel.visitPurposes.collectAsState()
    val isKioskMode by viewModel.isKioskMode.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isReceptionistMode = userRole == UserRole.RECEPTIONIST
    val isTouchlessKioskMode by viewModel.isTouchlessKioskMode.collectAsState()

    var showSafetyDialog by remember { mutableStateOf(false) }
    var safetyRulesAccepted by remember { mutableStateOf(true) }

    if (showSafetyDialog) {
        SafetyInfoDialog(
            branch = activeBranch,
            onDismiss = { showSafetyDialog = false }
        )
    }

    AnimatedContent(
        targetState = isKioskMode && isTouchlessKioskMode,
        transitionSpec = {
            (fadeIn(animationSpec = tween(260)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 10 }
                )).togetherWith(
                fadeOut(animationSpec = tween(180)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(180)) +
                    slideOutVertically(targetOffsetY = { -it / 12 })
            )
        },
        label = "kioskModeTransition"
    ) { showTouchlessQr ->
        if (showTouchlessQr) {
            TouchlessKioskView(
                viewModel = viewModel,
                activeBranch = activeBranch,
                onSwitchToTouchscreen = { viewModel.setTouchlessKioskMode(false) },
                modifier = Modifier.padding(contentPadding)
            )
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("registration_screen")
            ) {
                HeroHeader(
                    activeBranch = activeBranch,
                    isKioskMode = isKioskMode,
                    isReceptionistMode = isReceptionistMode,
                    modifier = Modifier.liquidAppearAnimation(delayMillis = 0)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Touchless Mobile Check-In Option Card (only the explicit button triggers QR mode)
                if (isKioskMode) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .liquidAppearAnimation(delayMillis = 80)
                            .testTag("card_touchless_kiosk_prompt"),
                        cornerRadius = 20.dp,
                        elevated = false
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .liquidInnerGlass(
                                            shape = RoundedCornerShape(12.dp),
                                            cornerRadius = 12.dp,
                                            accentColor = MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Touchless Mobile Check-In",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Scan QR with your phone camera",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { viewModel.setTouchlessKioskMode(true) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .liquidButtonGlass(
                                        shape = RoundedCornerShape(12.dp),
                                        cornerRadius = 12.dp
                                    )
                                    .testTag("btn_open_touchless_mode")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }

                // Multi-Branch Quick Selector (Visible ONLY in Admin Mode)
                if (!isKioskMode && !isReceptionistMode) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .liquidAppearAnimation(delayMillis = 90),
                        cornerRadius = 20.dp,
                        elevated = false
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active Branch / Company Location",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                branches.forEach { branch ->
                                    val selected = branch.id == activeBranch.id
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.setBranch(branch) },
                                        label = {
                                            Text(
                                                text = branch.shortCode,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
                                        colors = liquidFilterChipColors(),
                                        modifier = Modifier
                                            .liquidPressEffect()
                                            .testTag("chip_branch_${branch.shortCode}")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Main Registration Form Card
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidAppearAnimation(delayMillis = 120)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    cornerRadius = 26.dp,
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .liquidInnerGlass(
                                            shape = RoundedCornerShape(11.dp),
                                            cornerRadius = 11.dp,
                                            accentColor = MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Visitor Entry Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${activeBranch.name} (${activeBranch.city})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // 1. Visitor Full Name
                        OutlinedTextField(
                            value = formState.fullName,
                            onValueChange = { viewModel.onFullNameChange(it) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            label = { Text("Full Name *", fontSize = 12.sp) },
                            placeholder = { Text("Enter your full name", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = liquidTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_full_name")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Mobile Number with Country Code Selector (Enforce LTR direction to prevent inversion on Arabic/RTL systems)
                        var showCountryPickerDialog by remember { mutableStateOf(false) }
                        val allCountryOptions = BranchRegistry.COUNTRY_DIAL_OPTIONS
                        val selectedCountryOption = remember(formState.countryCode, allCountryOptions) {
                            allCountryOptions.find { it.dialCode == formState.countryCode }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            if (showCountryPickerDialog) {
                                CountryCodePickerDialog(
                                    options = allCountryOptions,
                                    selectedDialCode = formState.countryCode,
                                    onSelect = { option ->
                                        viewModel.onCountryCodeChange(option.dialCode)
                                        showCountryPickerDialog = false
                                    },
                                    onDismiss = { showCountryPickerDialog = false }
                                )
                            }

                            OutlinedTextField(
                                value = formState.mobileNumber,
                                onValueChange = { viewModel.onMobileNumberChange(it) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.5.sp),
                                label = { Text("Mobile Number *", fontSize = 12.sp) },
                                placeholder = { Text("50 123 4567", fontSize = 12.sp) },
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { showCountryPickerDialog = true }
                                            .padding(start = 10.dp, end = 6.dp)
                                            .testTag("dropdown_country_code")
                                    ) {
                                        Text(
                                            text = if (selectedCountryOption != null) {
                                                "${selectedCountryOption.flagEmoji} ${formState.countryCode}"
                                            } else {
                                                formState.countryCode
                                            },
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Country Code",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        VerticalDivider(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .width(1.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = liquidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_mobile")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Company Name
                        OutlinedTextField(
                            value = formState.companyName,
                            onValueChange = { viewModel.onCompanyNameChange(it) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            label = { Text("Company / Organization Name *", fontSize = 12.sp) },
                            placeholder = { Text("Enter your company name", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = liquidTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_company")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Visitor Email ID
                        val isEmailInvalid = formState.email.isNotBlank() && formState.emailError != null
                        OutlinedTextField(
                            value = formState.email,
                            onValueChange = { viewModel.onEmailChange(it) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            label = { Text("Email ID *", fontSize = 12.sp) },
                            placeholder = { Text("visitor@company.com", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            isError = isEmailInvalid,
                            supportingText = if (isEmailInvalid) {
                                {
                                    Text(
                                        text = "Please enter a valid email address",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = liquidTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_email")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Type of Visit (Walk-In / Appointment)
                        Text(
                            text = "Type of Visit *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LiquidGlassSegmentedControl(
                            options = listOf("Walk-In", "Appointment"),
                            selectedOption = formState.visitType,
                            onOptionSelected = { viewModel.onVisitTypeChange(it) },
                            icons = mapOf(
                                "Walk-In" to Icons.Default.DirectionsWalk,
                                "Appointment" to Icons.Default.EventAvailable
                            ),
                            testTagPrefix = "chip_visit_type"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 6. Department to Visit (Dropdown Selector)
                        var departmentDropdownExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = formState.department,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                label = { Text("Department to Visit (${activeBranch.shortCode}) *", fontSize = 12.sp) },
                                placeholder = { Text("Select department", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Department",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = liquidTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { departmentDropdownExpanded = true }
                                    .testTag("dropdown_department")
                            )

                            DropdownMenu(
                                expanded = departmentDropdownExpanded,
                                onDismissRequest = { departmentDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                activeBranch.defaultDepartments.forEach { dept ->
                                    val selected = formState.department == dept
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = dept,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (selected) Icons.Default.Check else Icons.Default.Apartment,
                                                contentDescription = null,
                                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            viewModel.onDepartmentChange(dept)
                                            departmentDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("dept_item_${dept.replace(" ", "_")}")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7. Purpose of Visit (Dropdown Selector)
                        var purposeDropdownExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = formState.purpose,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                label = { Text("Purpose of Visit *", fontSize = 12.sp) },
                                placeholder = { Text("Select purpose of visit", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Purpose of Visit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = liquidTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { purposeDropdownExpanded = true }
                                    .testTag("dropdown_purpose")
                            )

                            DropdownMenu(
                                expanded = purposeDropdownExpanded,
                                onDismissRequest = { purposeDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                visitPurposes.forEach { purposeOption ->
                                    val selected = formState.purpose == purposeOption
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = purposeOption,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (selected) Icons.Default.Check else Icons.Default.EditNote,
                                                contentDescription = null,
                                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            viewModel.onPurposeChange(purposeOption)
                                            purposeDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("purpose_item_${purposeOption.replace(" ", "_")}")
                                    )
                                }
                            }
                        }

                        // Conditional Custom Purpose field if "Other" is selected
                        AnimatedVisibility(visible = formState.purpose == "Other") {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = formState.customPurpose,
                                    onValueChange = { viewModel.onCustomPurposeChange(it) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    label = { Text("Please Specify Purpose *", fontSize = 12.sp) },
                                    placeholder = { Text("Describe the reason for your visit", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = liquidTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_custom_purpose")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 8. Person to Meet (Optional)
                        OutlinedTextField(
                            value = formState.personToMeet,
                            onValueChange = { viewModel.onPersonToMeetChange(it) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            label = { Text("Person to Meet (Optional)", fontSize = 12.sp) },
                            placeholder = { Text("Host employee name (if known)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = liquidTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_person_to_meet")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 9. Mandatory Safety & Emergency Guidelines Acknowledgment Checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidInnerGlass(
                                    shape = RoundedCornerShape(16.dp),
                                    cornerRadius = 16.dp,
                                    selected = safetyRulesAccepted
                                )
                                .clickable {
                                    safetyRulesAccepted = !safetyRulesAccepted
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("card_safety_acknowledgment"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = safetyRulesAccepted,
                                onCheckedChange = { safetyRulesAccepted = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("checkbox_safety_rules")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "I acknowledge and agree to comply with ${activeBranch.name} QHSE Safety Guidelines & Emergency Evacuation rules *",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { showSafetyDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .liquidPressEffect()
                                        .testTag("btn_inline_safety_rules")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Read Safety Rules",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Submit Button (Flexible heightIn with padding to prevent clipping and mistouches)
                        Button(
                            onClick = { viewModel.submitRegistration() },
                            enabled = formState.isValid && safetyRulesAccepted && !formState.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .heightIn(min = 48.dp)
                                .liquidButtonGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    cornerRadius = 14.dp,
                                    enabled = formState.isValid && safetyRulesAccepted && !formState.isSubmitting
                                )
                                .testTag("btn_submit_registration"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.White.copy(alpha = 0.72f)
                            )
                        ) {
                            if (formState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Generating Official Pass...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Check-In & Generate Pass",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun HeroHeader(
    activeBranch: Branch,
    isKioskMode: Boolean,
    isReceptionistMode: Boolean,
    modifier: Modifier = Modifier
) {
    val roleSubtitle = when {
        isKioskMode -> "SELF-SERVICE CHECK-IN TERMINAL"
        isReceptionistMode -> "RECEPTIONIST DESK ENTRY"
        else -> "ADMINISTRATION & ENTRY TERMINAL"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidHeroGlass(
                shape = RoundedCornerShape(20.dp),
                cornerRadius = 20.dp
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompanyBrandLogoBadge(
                    modifier = Modifier
                        .width(76.dp)
                        .height(34.dp),
                    logoUri = activeBranch.logoUri,
                    branchName = activeBranch.name,
                    testTag = "company_logo_header"
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SAIT GROUP",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = activeBranch.city,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Text(
                        text = activeBranch.name,
                        color = Color(0xFFE0F2FE),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = roleSubtitle,
                        color = Color(0xFF7DD3FC),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Full Company Address Display in the Box below Top Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Facility Address",
                    tint = Color(0xFF7DD3FC),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeBranch.displayFacilityLocation,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
                    colors = liquidTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_country")
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
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
                                            Color.White
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
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
