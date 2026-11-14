package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.ui.components.CompanyBrandLogoBadge
import com.example.ui.components.EmergencyEvacuationPlanCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.liquidAppearAnimation
import com.example.ui.theme.liquidInnerGlass

@Composable
fun SafetyInfoScreen(
    contentPadding: PaddingValues,
    branch: Branch = BranchRegistry.INITIAL_BRANCHES[1]
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag("safety_info_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Company & Facility Header Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidAppearAnimation(delayMillis = 0),
                cornerRadius = 24.dp,
                elevated = true
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanyBrandLogoBadge(
                            modifier = Modifier
                                .width(88.dp)
                                .height(38.dp),
                            logoUri = branch.logoUri,
                            branchName = branch.name,
                            testTag = "company_logo_safety"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = branch.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = branch.displayFacilityLocation,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    FacilityInfoRow(
                        icon = Icons.Default.LocationOn,
                        title = "Facility Location",
                        subtitle = branch.displayFacilityLocation
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FacilityInfoRow(
                        icon = Icons.Default.HealthAndSafety,
                        title = "QHSE Compliance",
                        subtitle = "Quality, Health, Safety & Environment standards"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FacilityInfoRow(
                        icon = Icons.Default.Call,
                        title = "Emergency & Security Helpline",
                        subtitle = if (branch.safetyContactName.isNotBlank()) {
                            "${branch.safetyContactName} • ${branch.safetyHelpline}"
                        } else {
                            branch.safetyHelpline
                        }
                    )
                }
            }
        }

        // Emergency Helpline Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidAppearAnimation(delayMillis = 50),
                cornerRadius = 22.dp,
                elevated = false
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "24/7 Emergency Helpline (${branch.shortCode})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (branch.safetyContactName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Contact Person: ${branch.safetyContactName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = branch.safetyHelpline,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Custom Branch Safety Guidelines Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidAppearAnimation(delayMillis = 100),
                cornerRadius = 24.dp,
                elevated = true
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${branch.name} — Safety Guidelines (QHSE)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = branch.safetyGuidelines,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SafetyRuleItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Visitor Badge Must Be Visible",
                        description = "Wear your visitor pass at all times on company premises."
                    )

                    SafetyRuleItem(
                        icon = Icons.Default.Construction,
                        title = "Mandatory PPE in Operational Areas",
                        description = "Safety shoes, high-visibility vest, and hard hat are required in factory & project zones."
                    )

                    SafetyRuleItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Photography & Filming Restricted",
                        description = "Authorization required before taking photos or videos in operational areas."
                    )

                    SafetyRuleItem(
                        icon = Icons.Default.SmokeFree,
                        title = "Designated Smoking Areas",
                        description = "Smoking is permitted only in marked outdoor shelters."
                    )

                    SafetyRuleItem(
                        icon = Icons.Default.Emergency,
                        title = "Emergency & Evacuation",
                        description = "Follow green exit routes to Emergency Assembly Point 1."
                    )
                }
            }
        }

        // Emergency Exit Drawing Card (Optional — shown only if admin added the image for it)
        if (!branch.emergencyExitDrawingUri.isNullOrBlank()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidAppearAnimation(delayMillis = 150),
                    cornerRadius = 24.dp,
                    elevated = true
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Emergency Exit & Evacuation Drawing",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        AsyncImage(
                            model = branch.emergencyExitDrawingUri,
                            contentDescription = "Emergency Exit Plan",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FacilityInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SafetyRuleItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .liquidInnerGlass(
                    shape = RoundedCornerShape(10.dp),
                    cornerRadius = 10.dp,
                    tint = MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
