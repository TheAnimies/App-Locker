package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.LockerPreferenceManager
import com.example.service.UsageStatsMonitorService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted. Restoring App Locker hooks if necessary.")
            val prefManager = LockerPreferenceManager(context)

            // If the user utilizes the Usage Stats polling mechanism, restore it
            if (prefManager.isLockerEnabled && !prefManager.isAccessibilityServicePrimary) {
                try {
                    val serviceIntent = Intent(context, UsageStatsMonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(TAG, "UsageStatsMonitorService launched after reboot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start UsageStatsMonitorService on boot: ${e.message}")
                }
            }
        }
    }
}
