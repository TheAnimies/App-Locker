package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.data.LockedAppRepository
import com.example.data.LockerDatabase
import com.example.data.LockerPreferenceManager
import com.example.security.PinManager
import com.example.service.LockCoordinator

class AppLockerApplication : Application() {

    lateinit var preferenceManager: LockerPreferenceManager
        private set
    lateinit var database: LockerDatabase
        private set
    lateinit var repository: LockedAppRepository
        private set
    lateinit var pinManager: PinManager
        private set
    lateinit var coordinator: LockCoordinator
        private set

    companion object {
        private const val TAG = "AppLockerApplication"
        lateinit var instance: AppLockerApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "Application onCreate - initializing locks.")

        // 1. Core managers (Simple Service Locator pattern)
        preferenceManager = LockerPreferenceManager(this)
        database = LockerDatabase.getDatabase(this)
        repository = LockedAppRepository(database.lockedAppDao())
        pinManager = PinManager(this, preferenceManager)
        coordinator = LockCoordinator.getInstance(this)

        // 2. Dynamic BroadcastReceiver for power/screen on/off transitions
        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    coordinator.handleScreenOff()
                }
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, intentFilter)
    }
}
