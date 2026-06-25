package com.example.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import androidx.biometric.BiometricManager

object PermissionHelper {

    /**
     * Checks if the Accessibility Service is active.
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${serviceClass.name}"
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(settingValue)
        while (colonSplitter.hasNext()) {
            val enabledService = colonSplitter.next()
            if (enabledService.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Launch Accessibility setting panel.
     */
    fun launchAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Checks if Usage Access permission is granted.
     */
    @Suppress("DEPRECATION")
    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Launch Usage settings panel.
     */
    fun launchUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for some older devices or custom skins where standard Uri data is rejected
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    /**
     * Checks if System Overlay (Draw over other apps) permission is granted.
     */
    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Launch System Overlay settings panel.
     */
    fun launchOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    /**
     * Checks if the device has enrolled biometrics.
     */
    fun getBiometricStatus(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return getBiometricStatus(context) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
