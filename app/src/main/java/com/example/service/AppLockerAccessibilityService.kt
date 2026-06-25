package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppLockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLockerAccessibility"
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected.")
        isRunning = true

        // Configure programmatic service settings for wide Android compatibility
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50L
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                LockCoordinator.getInstance(this).onForegroundAppChanged(packageName)
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Accessibility Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility Service Destroyed.")
        isRunning = false
    }
}
