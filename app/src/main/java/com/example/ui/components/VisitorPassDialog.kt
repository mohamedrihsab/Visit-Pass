package com.example.ui.components

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.VisitorEntity
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.SaitDarkBackground
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.liquidButtonGlass
import com.example.ui.theme.liquidHeroGlass
import com.example.ui.theme.liquidInnerGlass
import com.example.ui.theme.liquidModalGlass
import com.example.ui.theme.liquidPressEffect

@Composable
fun VisitorPassDialog(
    visitor: VisitorEntity,
    branchLogoUri: String? = null,
    countdownSeconds: Int = 0,
    totalCountdownSeconds: Int = 0,
    isKioskMode: Boolean = false,
    onDismiss: () -> Unit,
    onCheckOut: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val isCheckedIn = visitor.status == "CHECKED_IN"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.94f)
                .liquidModalGlass(
                    shape = RoundedCornerShape(28.dp),
                    cornerRadius = 28.dp,
                    strokeWidth = 1.6.dp
                )
                .testTag("visitor_pass_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(36.dp))

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close pass",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    elevated = false
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidHeroGlass(
                                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                                    cornerRadius = 22.dp
                                )
                                .padding(vertical = 16.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CompanyBrandLogoBadge(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(40.dp),
                                    logoUri = branchLogoUri,
                                    branchName = visitor.branchName,
                                    testTag = "company_logo_pass"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SAIT GROUP",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = visitor.branchName.uppercase(),
                                    color = Color(0xFFE0F2FE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "OFFICIAL VISITOR PASS",
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCheckedIn) {
                                    StatusSuccess.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCheckedIn) Icons.Default.CheckCircle else Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = if (isCheckedIn) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCheckedIn) "ACTIVE ON PREMISES" else "CHECKED OUT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isCheckedIn) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = visitor.fullName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = visitor.companyName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))

                            PassDetailRow(label = "Branch", value = visitor.branchName)
                            PassDetailRow(label = "Pass ID", value = visitor.passNumber, isMono = true)
                            if (visitor.email.isNotBlank()) {
                                PassDetailRow(label = "Email ID", value = visitor.email)
                            }
                            PassDetailRow(label = "Visit Type", value = visitor.visitType)
                            PassDetailRow(label = "Department", value = visitor.department)
                            PassDetailRow(label = "Purpose", value = visitor.purpose)
                            if (visitor.personToMeet.isNotBlank()) {
                                PassDetailRow(label = "Person to Meet", value = visitor.personToMeet)
                            }
                            PassDetailRow(label = "Registered At", value = visitor.checkInFormattedTime)

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Pass QR Code",
                                        tint = SaitDarkBackground,
                                        modifier = Modifier.size(76.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = visitor.passNumber,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isKioskMode && countdownSeconds > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .liquidInnerGlass(
                                shape = RoundedCornerShape(12.dp),
                                cornerRadius = 12.dp,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kiosk resets in ${countdownSeconds}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isKioskMode) {
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
                            .testTag("btn_kiosk_next_visitor"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Next Visitor Registration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "SAIT Group Visitor Pass - ${visitor.fullName}")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        """
                                        SAIT GROUP - VISITOR PASS
                                        Pass ID: ${visitor.passNumber}
                                        Visitor: ${visitor.fullName}
                                        Email: ${visitor.email}
                                        Company: ${visitor.companyName}
                                        Branch: ${visitor.branchName}
                                        Department: ${visitor.department}
                                        Purpose: ${visitor.purpose}
                                        Person to Meet: ${if (visitor.personToMeet.isNotBlank()) visitor.personToMeet else "Reception"}
                                        Registered: ${visitor.checkInFormattedTime}
                                        """.trimIndent()
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Pass Details"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .liquidPressEffect()
                                .testTag("share_pass_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .liquidButtonGlass(
                                    shape = RoundedCornerShape(12.dp),
                                    cornerRadius = 12.dp,
                                    baseColor = MaterialTheme.colorScheme.primary
                                )
                                .testTag("done_pass_button")
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassDetailRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
