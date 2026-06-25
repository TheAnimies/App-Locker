package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.LockerPreferenceManager
import kotlinx.coroutines.*

class UsageStatsMonitorService : Service() {

    private var pollingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var prefManager: LockerPreferenceManager

    companion object {
        private const val TAG = "UsageStatsMonitor"
        private const val CHANNEL_ID = "app_locker_monitor_channel"
        private const val NOTIFICATION_ID = 112233
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        prefManager = LockerPreferenceManager(this)
        createNotificationChannel()
        startTrackingForeground()
        isRunning = true
        Log.d(TAG, "Usage Stats Service Created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start polling coroutine loop when service is started
        if (pollingJob == null || pollingJob?.isCancelled == true) {
            pollingJob = serviceScope.launch {
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                var lastActivePackage: String? = null

                while (isActive) {
                    try {
                        val currentTime = System.currentTimeMillis()
                        val stats = usageStatsManager.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY,
                            currentTime - 1000 * 5, // query last 5 seconds of active stats
                            currentTime
                        )

                        if (!stats.isNullOrEmpty()) {
                            val activeStat = stats.maxByOrNull { it.lastTimeUsed }
                            val activePackage = activeStat?.packageName

                            if (activePackage != null && activePackage != lastActivePackage) {
                                lastActivePackage = activePackage
                                withContext(Dispatchers.Main) {
                                    LockCoordinator.getInstance(this@UsageStatsMonitorService)
                                        .onForegroundAppChanged(activePackage)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking foreground app in polling loop: ${e.message}")
                    }
                    delay(800L) // Poll every 800ms for stable power consumption on Samsung
                }
            }
        }
        return START_STICKY
    }

    private fun startTrackingForeground() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Protection Active")
            .setContentText("Monitoring launched applications in background.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Protection Service"
            val descriptionText = "Ensures background apps can be locked securely"
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        serviceScope.cancel()
        isRunning = false
        Log.d(TAG, "Usage Stats Service Destroyed.")
    }
}
