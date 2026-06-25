package com.example.data

import android.content.Context
import android.content.SharedPreferences

class LockerPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_FILE_NAME = "app_locker_preferences"
        private const val KEY_LOCKER_ENABLED = "pref_locker_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "pref_biometric_enabled"
        private const val KEY_RELOCK_TIMING = "pref_relock_timing"
        private const val KEY_PIN_HASH = "pref_pin_hash"
        private const val KEY_SETUP_COMPLETED = "pref_is_setup_completed"
        private const val KEY_USE_ACCESSIBILITY = "pref_use_accessibility"
        private const val KEY_FAILED_ATTEMPTS = "pref_failed_attempts"
        private const val KEY_LOCKOUT_TIME = "pref_lockout_time"
    }

    var isLockerEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCKER_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var relockTimingMode: String
        get() = prefs.getString(KEY_RELOCK_TIMING, "immediate") ?: "immediate"
        set(value) = prefs.edit().putString(KEY_RELOCK_TIMING, value).apply()

    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var isAccessibilityServicePrimary: Boolean
        get() = prefs.getBoolean(KEY_USE_ACCESSIBILITY, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_ACCESSIBILITY, value).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    var lockoutTime: Long
        get() = prefs.getLong(KEY_LOCKOUT_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_TIME, value).apply()

    fun resetLockout() {
        failedAttempts = 0
        lockoutTime = 0L
    }
}
