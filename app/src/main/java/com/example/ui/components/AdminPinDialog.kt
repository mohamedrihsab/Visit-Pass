package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AdminPinDialog(
    adminPin: String,
    receptionistPin: String,
    targetRoleOnly: String? = null, // "ADMIN" if strictly prompting for Admin
    onDismiss: () -> Unit,
    onAdminSuccess: () -> Unit,
    onReceptionistSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun verifyPin(pin: String) {
        if (targetRoleOnly == "ADMIN") {
            if (pin == adminPin) {
                onAdminSuccess()
            } else {
                errorMessage = "Incorrect Admin PIN"
                enteredPin = ""
            }
        } else {
            when (pin) {
                adminPin -> onAdminSuccess()
                receptionistPin -> onReceptionistSuccess()
                else -> {
                    errorMessage = "Incorrect PIN"
                    enteredPin = ""
                }
            }
        }
    }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < 6) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorMessage = null

            // Check auto-success if exact match
            if (targetRoleOnly == "ADMIN") {
                if (newPin == adminPin) {
                    onAdminSuccess()
                    return
                } else if (newPin.length == 6) {
                    verifyPin(newPin)
                }
            } else {
                if (newPin == adminPin) {
                    onAdminSuccess()
                    return
                } else if (newPin == receptionistPin) {
                    onReceptionistSuccess()
                    return
                } else if (newPin.length == 6) {
                    verifyPin(newPin)
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.92f)
                .testTag("dialog_admin_pin"),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (targetRoleOnly == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (targetRoleOnly == "ADMIN") "Admin Verification" else "Role Access Authentication",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (targetRoleOnly == "ADMIN")
                        "Enter 4 to 6 digit Admin PIN to access system settings"
                    else
                        "Enter 4 to 6 digit PIN for Receptionist or Admin access",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // 6 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    repeat(6) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        text = "Authorized Personnel Only (4-6 digits)",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Numeric Keypad
                val keypadButtons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keypadButtons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            Surface(
                                shape = CircleShape,
                                color = when (key) {
                                    "C", "DEL" -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                },
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .testTag("keypad_$key"),
                                onClick = {
                                    when (key) {
                                        "C" -> {
                                            enteredPin = ""
                                            errorMessage = null
                                        }
                                        "DEL" -> onBackspace()
                                        else -> onDigitPress(key)
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // If user entered 4 or 5 digits, show convenient "Verify PIN" button
                if (enteredPin.length in 4..5) {
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.Button(
                        onClick = { verifyPin(enteredPin) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(44.dp)
                            .testTag("btn_verify_pin"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify PIN (${enteredPin.length} digits)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
