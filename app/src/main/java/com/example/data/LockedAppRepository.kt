package com.example.data

import kotlinx.coroutines.flow.Flow

class LockedAppRepository(private val dao: LockedAppDao) {
    val lockedApps: Flow<List<LockedApp>> = dao.getAllLockedApps()
    val lockedPackages: Flow<List<String>> = dao.getAllLockedPackages()

    suspend fun getLockedPackagesSync(): List<String> {
        return dao.getAllLockedPackagesSync()
    }

    suspend fun lockApp(packageName: String, appName: String) {
        dao.insertLockedApp(LockedApp(packageName, appName))
    }

    suspend fun unlockApp(packageName: String) {
        dao.deleteLockedApp(packageName)
    }

    suspend fun isAppLocked(packageName: String): Boolean {
        return dao.isAppLocked(packageName)
    }
}
