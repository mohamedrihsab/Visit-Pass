package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.liquidButtonGlass
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.liquidModalGlass
import com.example.ui.theme.liquidPressEffect

@Composable
fun AdminPinDialog(
    adminPin: String,
    receptionistPin: String,
    targetRoleOnly: String? = null,
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
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.90f)
                .liquidModalGlass(
                    shape = RoundedCornerShape(28.dp),
                    cornerRadius = 28.dp,
                    strokeWidth = 1.6.dp
                )
                .testTag("dialog_admin_pin")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                            imageVector = if (targetRoleOnly == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (targetRoleOnly == "ADMIN") "Admin Verification" else "Role Access",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (targetRoleOnly == "ADMIN")
                        "Enter Admin PIN"
                    else
                        "Enter Receptionist or Admin PIN",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 6 Animated PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    repeat(6) { index ->
                        val isFilled = index < enteredPin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.18f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "pinDotScale_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Numeric Keypad with comfortable spacing to prevent mistouches
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
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .liquidPressEffect(pressedScale = 0.92f)
                                    .liquidGlass(
                                        shape = CircleShape,
                                        cornerRadius = 28.dp,
                                        elevated = key !in listOf("C", "DEL")
                                    )
                                    .clickable {
                                        when (key) {
                                            "C" -> {
                                                enteredPin = ""
                                                errorMessage = null
                                            }
                                            "DEL" -> onBackspace()
                                            else -> onDigitPress(key)
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (enteredPin.length in 4..5) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { verifyPin(enteredPin) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.80f)
                            .height(42.dp)
                            .liquidButtonGlass(
                                shape = RoundedCornerShape(12.dp),
                                cornerRadius = 12.dp
                            )
                            .testTag("btn_verify_pin"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
