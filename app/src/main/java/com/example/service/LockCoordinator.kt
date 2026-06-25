package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.LockedAppRepository
import com.example.data.LockerDatabase
import com.example.data.LockerPreferenceManager
import com.example.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LockCoordinator private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefManager = LockerPreferenceManager(appContext)
    private val database = LockerDatabase.getDatabase(appContext)
    private val repository = LockedAppRepository(database.lockedAppDao())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "LockCoordinator"
        
        @Volatile
        private var INSTANCE: LockCoordinator? = null

        fun getInstance(context: Context): LockCoordinator {
            return INSTANCE ?: synchronized(this) {
                val instance = LockCoordinator(context)
                INSTANCE = instance
                instance
            }
        }
    }

    // List of active unlocked session packages and the timestamp when the app was exited
    private val unlockedApps = mutableMapOf<String, Long>() // PackageName -> LastActiveTimestamp
    private val exitTimestamps = mutableMapOf<String, Long>() // PackageName -> ExitTimestamp

    // Live state tracking of current foreground and lock screens
    private var lastForegroundApp: String? = null
    var currentForegroundApp: String? = null
        private set

    // Control variable to prevent spamming lock activities
    private var activeLockScreenPackage: String? = null

    init {
        // Log setup
        Log.d(TAG, "LockCoordinator initialized.")
    }

    /**
     * Checks if a package is locked in the database and requires an authentication overlay.
     */
    fun shouldLockPackage(packageName: String): Boolean {
        // App Locker is disabled globally
        if (!prefManager.isLockerEnabled) return false
        
        // Never lock ourselves, the home screen launcher, or common system UI packages
        if (packageName == appContext.packageName) return false
        if (isSystemLauncherOrSystemUi(packageName)) return false

        // Run checking
        var isLockedInDatabase = false
        val job = scope.launch {
            isLockedInDatabase = repository.isAppLocked(packageName)
        }
        // Block temporarily for db sync (safe within low-latency accessibility threads)
        while (!job.isCompleted) {
            Thread.sleep(2)
        }

        if (!isLockedInDatabase) return false

        // Check if there is an active unlocked session
        if (unlockedApps.containsKey(packageName)) {
            // Check if background away timing has exceeded rules
            val exitTime = exitTimestamps[packageName]
            if (exitTime != null) {
                val timeSpentInBackgroundMs = System.currentTimeMillis() - exitTime
                val thresholdMs = getRelockThresholdMs()
                if (thresholdMs != -1L && timeSpentInBackgroundMs > thresholdMs) {
                    // Session expired, remove from active unlocked and trigger lock
                    unlockedApps.remove(packageName)
                    exitTimestamps.remove(packageName)
                    Log.d(TAG, "Session expired for $packageName. Relocking.")
                    return true
                }
            }
            return false // Session still valid
        }

        return true
    }

    /**
     * Called by Accessibility/UsageStats trackers when a package moves to foreground.
     */
    fun onForegroundAppChanged(packageName: String) {
        if (packageName.isBlank()) return
        if (packageName == appContext.packageName) return

        val previousApp = currentForegroundApp
        currentForegroundApp = packageName
        
        Log.d(TAG, "Foreground transition: $previousApp -> $packageName")

        // Handle leaving a locked app
        if (previousApp != null && previousApp != packageName) {
            scope.launch {
                if (repository.isAppLocked(previousApp)) {
                    exitTimestamps[previousApp] = System.currentTimeMillis()
                    Log.d(TAG, "Recorded exit timestamp for $previousApp")
                }
            }
        }

        // Handle entering a locked app
        if (shouldLockPackage(packageName)) {
            // Prevent showing duplicated lock screens on the exact same target
            if (activeLockScreenPackage != packageName) {
                triggerLock(packageName)
            }
        } else {
            // If the package is marked unlocked, update last active
            if (unlockedApps.containsKey(packageName)) {
                unlockedApps[packageName] = System.currentTimeMillis()
                exitTimestamps.remove(packageName) // we are in foreground now
            }
        }

        lastForegroundApp = packageName
    }

    /**
     * Launches the Lock Screen activity.
     */
    private fun triggerLock(packageName: String) {
        synchronized(this) {
            activeLockScreenPackage = packageName
            Log.d(TAG, "Triggering lock for: $packageName")
            try {
                val intent = Intent(appContext, LockActivity::class.java).apply {
                    putExtra(LockActivity.EXTRA_PACKAGE_NAME, packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
                appContext.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LockActivity: ${e.message}", e)
                activeLockScreenPackage = null
            }
        }
    }

    /**
     * Mark app as temporarily unlocked for current session.
     */
    fun onPackageUnlocked(packageName: String) {
        synchronized(this) {
            unlockedApps[packageName] = System.currentTimeMillis()
            exitTimestamps.remove(packageName)
            if (activeLockScreenPackage == packageName) {
                activeLockScreenPackage = null
            }
            Log.d(TAG, "Successfully unlocked: $packageName")
        }
    }

    fun onLockScreenDismissed(packageName: String) {
        synchronized(this) {
            if (activeLockScreenPackage == packageName) {
                activeLockScreenPackage = null
            }
        }
    }

    /**
     * Invalidate all sessions. Invoked on screen off.
     */
    fun handleScreenOff() {
        synchronized(this) {
            unlockedApps.clear()
            exitTimestamps.clear()
            activeLockScreenPackage = null
            Log.d(TAG, "Screen off event processed. Invalidated all unlocked sessions.")
        }
    }

    private fun getRelockThresholdMs(): Long {
        return when (prefManager.relockTimingMode) {
            "immediate" -> 0L
            "30s" -> 30000L
            "1m" -> 60000L
            "5m" -> 300000L
            "screen_off" -> -1L // Only relocks on screen off
            else -> 0L
        }
    }

    private fun isSystemLauncherOrSystemUi(packageName: String): Boolean {
        val commonLaunchers = setOf(
            "com.sec.android.app.launcher", // Samsung One UI Home
            "com.google.android.apps.nexuslauncher", // Pixel launcher
            "com.android.launcher", // Stock launcher
            "com.android.launcher3",
            "com.android.systemui" // System Bar, Quick settings
        )
        return commonLaunchers.contains(packageName)
    }
}
