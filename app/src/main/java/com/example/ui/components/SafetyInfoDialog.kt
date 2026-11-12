package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Branch
import com.example.ui.theme.liquidButtonGlass
import com.example.ui.theme.liquidInnerGlass
import com.example.ui.theme.liquidModalGlass

@Composable
fun SafetyInfoDialog(
    branch: Branch,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .liquidModalGlass(
                    shape = RoundedCornerShape(28.dp),
                    cornerRadius = 28.dp,
                    strokeWidth = 1.6.dp
                )
                .testTag("dialog_safety_info")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
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
                        CompanyBrandLogoBadge(
                            modifier = Modifier
                                .width(88.dp)
                                .height(38.dp),
                            logoUri = branch.logoUri,
                            branchName = branch.name,
                            testTag = "company_logo_safety_dialog"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = branch.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Facility Location: ${branch.displayFacilityLocation}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_safety_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidInnerGlass(
                            shape = RoundedCornerShape(16.dp),
                            cornerRadius = 16.dp
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Facility Safety Regulations (QHSE)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = branch.safetyGuidelines,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidInnerGlass(
                            shape = RoundedCornerShape(16.dp),
                            cornerRadius = 16.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Emergency & Security Helpline",
                                    fontSize = 13.sp,
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Emergency Exit & Assembly Point Map",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (!branch.emergencyExitDrawingUri.isNullOrBlank()) {
                    AsyncImage(
                        model = branch.emergencyExitDrawingUri,
                        contentDescription = "Emergency Exit Plan",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    EmergencyEvacuationPlanCanvas()
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(44.dp)
                        .liquidButtonGlass(
                            shape = RoundedCornerShape(14.dp),
                            cornerRadius = 14.dp,
                            baseColor = MaterialTheme.colorScheme.primary
                        )
                        .testTag("btn_dismiss_safety_dialog"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text("I Acknowledge & Understand", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun EmergencyEvacuationPlanCanvas() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val w = size.width
                val h = size.height

                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.65f),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(w * 0.15f, h * 0.26f),
                    size = Size(w * 0.42f, h * 0.57f)
                )

                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(w * 0.60f, h * 0.26f),
                    size = Size(w * 0.25f, h * 0.57f)
                )

                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                drawLine(
                    color = Color(0xFF34D399),
                    start = Offset(w * 0.72f, h * 0.55f),
                    end = Offset(w * 0.72f, h * 0.16f),
                    strokeWidth = 4f,
                    pathEffect = pathEffect
                )
                drawLine(
                    color = Color(0xFF34D399),
                    start = Offset(w * 0.35f, h * 0.55f),
                    end = Offset(w * 0.35f, h * 0.16f),
                    strokeWidth = 4f,
                    pathEffect = pathEffect
                )

                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(w * 0.30f, h * 0.20f),
                    size = Size(w * 0.10f, h * 0.04f)
                )
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(w * 0.67f, h * 0.20f),
                    size = Size(w * 0.10f, h * 0.04f)
                )

                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 24f,
                    center = Offset(w * 0.50f, h * 0.10f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981)
                ) {
                    Text(
                        text = "★ EMERGENCY ASSEMBLY POINT 1 (EXTERNAL YARD)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "EXIT A (WORKSHOP)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = "EXIT B (OFFICE/LOBBY)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "OPERATIONAL ZONE",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "MAIN RECEPTION",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
