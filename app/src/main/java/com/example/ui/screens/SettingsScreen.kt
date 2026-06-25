package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PermissionHelper
import com.example.viewmodel.AppLockerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppLockerViewModel,
    onNavigateBack: () -> Unit,
    onChangePin: () -> Unit
) {
    val context = LocalContext.current
    val isEnabled by viewModel.isLockerEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val relockTimingMode by viewModel.relockTimingMode.collectAsState()
    val isAccessibilityPrimary by viewModel.isAccessibilityServicePrimary.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    val scrollState = rememberScrollState()

    // Query system constraints on resume
    LaunchedEffect(key1 = true) {
        viewModel.refreshPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 1. Master Lock Activation Switch Card (One UI feel)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) {
                         MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                         MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.Security else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "App Protection Active",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Global switch to activate lock screens",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleMasterLocker(it) }
                    )
                }
            }

            // 2. Authentication Parameters Group
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Security Configuration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Change PIN Row
                        SettingsClickableRow(
                            icon = Icons.Default.Password,
                            title = "Change PIN Code",
                            desc = "Redefine the 4-digit lockout code",
                            onClick = onChangePin
                        )

                        Divider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        // Toggle Biometrics Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Biometric Verification",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (permissionState.isBiometricAvailable) "Use Fingerprint or Face Scan quick shortcuts" else "Unavailable (Not enrolled on device)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometric(it) },
                                enabled = permissionState.isBiometricAvailable
                            )
                        }
                    }
                }
            }

            // 3. Relock rules and Background Tracker Configurations
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Relocking Parameters",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "When should the app ask for PIN again?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        val optionsList = listOf(
                            "immediate" to "Every direct launch",
                            "30s" to "If away for 30 seconds",
                            "1m" to "If away for 1 minute",
                            "5m" to "If away for 5 minutes",
                            "screen_off" to "Only on screen off"
                        )

                        optionsList.forEach { (mode, label) ->
                            val isSelected = relockTimingMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.setRelockTimingMode(mode) }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setRelockTimingMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            // 4. Primary Foreground Tracking Engine Switch
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Foreground Detection System",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Use Accessibility Service",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isAccessibilityPrimary) "Active tracking (Instant response, recommended)" else "Using UsageStats background fallback",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isAccessibilityPrimary,
                                onCheckedChange = { viewModel.toggleAccessibilityAsPrimary(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Permission Badges and clicks
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PermissionStatusIndicator(
                                name = "Accessibility System service",
                                isGranted = permissionState.isAccessibilityGranted,
                                onClick = { PermissionHelper.launchAccessibilitySettings(context) }
                            )
                            PermissionStatusIndicator(
                                name = "Apps Draw Overlay capability",
                                isGranted = permissionState.isOverlayGranted,
                                onClick = { PermissionHelper.launchOverlaySettings(context) }
                            )
                            PermissionStatusIndicator(
                                name = "Usage Access statistics permission",
                                isGranted = permissionState.isUsageAccessGranted,
                                onClick = { PermissionHelper.launchUsageAccessSettings(context) }
                            )
                        }
                    }
                }
            }

            // 5. Samsung-Specific OS Management Instructions (Requirement #7)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Samsung One UI Optimizations",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "To guarantee Samsung One UI 8.5 does not aggressively terminate protections in the background, set the App Locker battery constraint to 'Unrestricted':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    instructionsBullet(number = 1, text = "Long press the App Locker icon on Home Screen and tap 'App Info' (the circle Info icon).")
                    instructionsBullet(number = 2, text = "Scroll and tap on 'Battery'.")
                    instructionsBullet(number = 3, text = "Change selection from 'Optimized' to 'Unrestricted'.")
                    instructionsBullet(number = 4, text = "For maximum stability, also search settings for 'Never Sleeping Apps' and add App Locker.")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PermissionStatusIndicator(
    name: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isGranted) "Authorized" else "Authorize",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Settings,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun instructionsBullet(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number. ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
