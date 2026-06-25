package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PermissionHelper
import com.example.viewmodel.AppLockerViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: AppLockerViewModel,
    onCompleted: () -> Unit
) {
    val permissionState by viewModel.permissionState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Determine lock screen preparedness (Accessibility or Usage Access + Overlay)
    val isAppReady = permissionState.isAccessibilityGranted || 
                     (permissionState.isUsageAccessGranted && permissionState.isOverlayGranted)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large title region (classic Samsung One UI spacing)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to App Locker",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Protect your privacy by blocking access to your confidential apps. Please authorize key integrations below to initialize protection.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Card List for Permission Enforcements
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Accessibility Service check
            PermissionRow(
                title = "Accessibility Monitoring",
                desc = "The primary and most battery-safe mechanism allowed on Samsung devices to block locked apps instantly.",
                isGranted = permissionState.isAccessibilityGranted,
                onGrantClick = {
                    PermissionHelper.launchAccessibilitySettings(context)
                }
            )

            // 2. Overlay Permission Check
            PermissionRow(
                title = "Draw Over Other Apps",
                desc = "Required to display the secure, full-screen lock screen activity on top of protected applications.",
                isGranted = permissionState.isOverlayGranted,
                onGrantClick = {
                    PermissionHelper.launchOverlaySettings(context)
                }
            )

            // 3. Usage Statistics Access Check
            PermissionRow(
                title = "Usage Access (Backup Tracker)",
                desc = "Used as a robust alternative monitor to ensure app protection remains active under restricted conditions.",
                isGranted = permissionState.isUsageAccessGranted,
                onGrantClick = {
                    PermissionHelper.launchUsageAccessSettings(context)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // Next Phase Action CTA
        Button(
            onClick = {
                if (isAppReady) {
                    onCompleted()
                } else {
                    // Quick fallback permission nudge
                    PermissionHelper.launchAccessibilitySettings(context)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAppReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isAppReady) "Launch Locker" else "Provide Required Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAppReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
