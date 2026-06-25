package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppLockerViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinSetupScreen(
    viewModel: AppLockerViewModel,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1 = Create, 2 = Confirm
    var pinFirst by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }

    val currentInput = if (step == 1) pinFirst else pinConfirm

    val headingText = if (step == 1) "Create security PIN" else "Confirm your PIN"
    val subtitleText = if (step == 1) 
        "Set a 4-digit PIN to lock and protect apps." 
    else 
        "Enter the exact same 4 digits again."

    // Core input callback
    val onKeyPress: (Char) -> Unit = { char ->
        if (currentInput.length < 4) {
            val newVal = currentInput + char
            if (step == 1) {
                pinFirst = newVal
                if (newVal.length == 4) {
                    step = 2 // Progress immediately to confirmation
                }
            } else {
                pinConfirm = newVal
                if (newVal.length == 4) {
                    if (pinFirst == newVal) {
                        val saved = viewModel.savePin(newVal)
                        if (saved) {
                            viewModel.setSetupCompleted()
                            Toast.makeText(context, "PIN successfully set!", Toast.LENGTH_SHORT).show()
                            onSetupComplete()
                        } else {
                            Toast.makeText(context, "Failed saving PIN in secure Keystore.", Toast.LENGTH_LONG).show()
                            step = 1
                            pinFirst = ""
                            pinConfirm = ""
                        }
                    } else {
                        Toast.makeText(context, "PINs did not match. Please restart.", Toast.LENGTH_SHORT).show()
                        step = 1
                        pinFirst = ""
                        pinConfirm = ""
                    }
                }
            }
        }
    }

    val onDeletePress: () -> Unit = {
        if (currentInput.isNotEmpty()) {
            if (step == 1) {
                pinFirst = pinFirst.dropLast(1)
            } else {
                pinConfirm = pinConfirm.dropLast(1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Samsung elegant layout)
            Spacer(modifier = Modifier.height(40.dp))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = headingText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Pin Digits Indicators (M3 Style Pills)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < currentInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Large comfortable keypad (Samsung One UI feel)
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val buttonRows = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf(null, '0', '⌫')
                )

                buttonRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (char != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.background
                                    )
                                    .clickable(enabled = char != null) {
                                        if (char == '⌫') {
                                            onDeletePress()
                                        } else if (char != null) {
                                            onKeyPress(char)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (char == '⌫') {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                } else if (char != null) {
                                    Text(
                                        text = char.toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
