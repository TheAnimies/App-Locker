package com.example.ui.screens

import android.widget.ImageView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AppLockerApplication
import com.example.viewmodel.LockViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockScreen(
    packageName: String,
    onUnlockSuccess: () -> Unit,
    onBackPress: () -> Unit,
    onTriggerBiometric: () -> Unit
) {
    val context = LocalContext.current
    val appInstance = context.applicationContext as AppLockerApplication
    
    // Instantiate local LockViewModel
    val lockViewModel: LockViewModel = viewModel(
        factory = LockViewModel.Factory(appInstance)
    )

    val pinInput by lockViewModel.pinInput.collectAsState()
    val isVibratingError by lockViewModel.isVibratingError.collectAsState()
    val lockoutSecondsLeft by lockViewModel.lockoutSecondsLeft.collectAsState()

    // Observe local authentication events
    LaunchedEffect(key1 = true) {
        lockViewModel.unlockSuccessEvent.collect {
            onUnlockSuccess()
        }
    }

    // Dynamic recovery of the locked Application name details
    val targetAppName = remember(packageName) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            "Secure App"
        }
    }

    // Shake offset animation during wrong pin input
    val shakeOffset by animateFloatAsState(
        targetValue = if (isVibratingError) 15f else 0f,
        animationSpec = keyframes {
            durationMillis = 350
            0f at 0
            -15f at 50
            15f at 100
            -15f at 150
            15f at 200
            -15f at 250
            0f at 350
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Identity Header (Centered, beautiful Samsung look)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Application Icon wrapper of the target app
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        try {
                            val iconDrawable = ctx.packageManager.getApplicationIcon(packageName)
                            setImageDrawable(iconDrawable)
                        } catch (e: Exception) {
                            setImageResource(android.R.drawable.sym_def_app_icon)
                        }
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = targetAppName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "App Locked by App Locker",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Subtitle state (Handles lockout blocks gracefully)
            if (lockoutSecondsLeft > 0) {
                Text(
                    text = "Too many failed attempts. Try again in $lockoutSecondsLeft seconds.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                Text(
                    text = if (isVibratingError) "Incorrect PIN. Please try again." else "Enter security PIN to check identity",
                    fontSize = 14.sp,
                    color = if (isVibratingError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = if (isVibratingError) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input indicator circles (with shake layout modifier)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset(x = shakeOffset.dp)
            ) {
                repeat(4) { idx ->
                    val isEntered = idx < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = when {
                                    isVibratingError -> MaterialTheme.colorScheme.error
                                    isEntered -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        // Action keypad region at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 320.dp)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
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
                        // Determine custom visual style of bottom corner key selectors
                        val isActionKey = char == null || char == '⌫'
                        
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        lockoutSecondsLeft > 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                        char == null -> Color.Transparent // Bottom left customized area
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (char != null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable(enabled = lockoutSecondsLeft <= 0) {
                                    if (char == '⌫') {
                                        lockViewModel.onDeletePress()
                                    } else if (char != null) {
                                        lockViewModel.onKeyPress(char)
                                    } else {
                                        // Left auxiliary key - trigger biometric scanning if clicked
                                        onTriggerBiometric()
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
                            } else if (char == null) {
                                // Place visual Quick Biometric lock icon
                                if (appInstance.preferenceManager.isBiometricEnabled) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Scan fingerprint",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = char.toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = if (lockoutSecondsLeft > 0) 0.3f else 1.0f
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Cancel actions (Allows returning directly to system Home launcher)
            TextButton(
                onClick = onBackPress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel & Exit to Home",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
