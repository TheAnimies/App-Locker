package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppLockerApplication
import com.example.data.LockedAppRepository
import com.example.data.LockerPreferenceManager
import com.example.security.PinManager
import com.example.service.AppLockerAccessibilityService
import com.example.service.UsageStatsMonitorService
import com.example.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppInfo(
    val packageName: String,
    val appName: String,
    var isLocked: Boolean,
    val isSystem: Boolean = false
)

enum class SortOption {
    ALPHA_ASC,
    ALPHA_DESC,
    LOCKED_FIRST
}

enum class AppFilter {
    ALL,
    LOCKED,
    SYSTEM
}

data class PermissionState(
    val isAccessibilityGranted: Boolean = false,
    val isUsageAccessGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isBiometricAvailable: Boolean = false
)

class AppLockerViewModel(
    private val repository: LockedAppRepository,
    private val prefManager: LockerPreferenceManager,
    private val pinManager: PinManager,
    private val context: Context
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.ALPHA_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _appFilter = MutableStateFlow(AppFilter.ALL)
    val appFilter: StateFlow<AppFilter> = _appFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Preferences wrapped in Flows for UI observation
    private val _isLockerEnabled = MutableStateFlow(prefManager.isLockerEnabled)
    val isLockerEnabled: StateFlow<Boolean> = _isLockerEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefManager.isBiometricEnabled)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _relockTimingMode = MutableStateFlow(prefManager.relockTimingMode)
    val relockTimingMode: StateFlow<String> = _relockTimingMode.asStateFlow()

    private val _isAccessibilityServicePrimary = MutableStateFlow(prefManager.isAccessibilityServicePrimary)
    val isAccessibilityServicePrimary: StateFlow<Boolean> = _isAccessibilityServicePrimary.asStateFlow()

    // Local system permissions live tracking state
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // Expose filtered & sorted app list
    val appList: StateFlow<List<AppInfo>> = combine(
        _installedApps,
        _searchQuery,
        _sortOption,
        _appFilter,
        repository.lockedPackages
    ) { RawApps, Query, Sort, Filter, LockedPackages ->
        // 1. Sync lock status from the DB live flow
        val syncedApps = RawApps.map { app ->
            app.copy(isLocked = LockedPackages.contains(app.packageName))
        }

        // 2. Perform Category filters
        val categoryFiltered = when (Filter) {
            AppFilter.ALL -> syncedApps
            AppFilter.LOCKED -> syncedApps.filter { it.isLocked }
            AppFilter.SYSTEM -> syncedApps.filter { it.isSystem }
        }

        // 3. Perform search filters
        val searched = if (Query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter {
                it.appName.contains(Query, ignoreCase = true) || 
                it.packageName.contains(Query, ignoreCase = true)
            }
        }

        // 4. Perform sorting
        when (Sort) {
            SortOption.ALPHA_ASC -> searched.sortedBy { it.appName.lowercase() }
            SortOption.ALPHA_DESC -> searched.sortedByDescending { it.appName.lowercase() }
            SortOption.LOCKED_FIRST -> searched.sortedWith(
                compareByDescending<AppInfo> { it.isLocked }
                .thenBy { it.appName.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
        refreshPermissions()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val packageManager = context.packageManager
                val installedAppsList = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                val apps = installedAppsList.mapNotNull { appInfo ->
                    val pName = appInfo.packageName
                    if (pName == context.packageName) return@mapNotNull null // hide ourselves
                    val aName = appInfo.loadLabel(packageManager).toString()
                    val isSys = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    AppInfo(packageName = pName, appName = aName, isLocked = false, isSystem = isSys)
                }.distinctBy { it.packageName }

                _installedApps.value = apps
            } catch (e: Exception) {
                Log.e("AppLockerViewModel", "Error fetching installed apps: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setAppFilter(filter: AppFilter) {
        _appFilter.value = filter
    }

    /**
     * Toggles the locked status in Room DB
     */
    fun toggleAppLock(app: AppInfo) {
        viewModelScope.launch {
            if (app.isLocked) {
                repository.unlockApp(app.packageName)
            } else {
                repository.lockApp(app.packageName, app.appName)
            }
        }
    }

    fun toggleMasterLocker(enabled: Boolean) {
        prefManager.isLockerEnabled = enabled
        _isLockerEnabled.value = enabled
        Log.d("AppLockerViewModel", "Master Switch: $enabled")
        
        // Sync the Usage Stats monitoring background service
        syncUsageStatsService()
    }

    fun toggleBiometric(enabled: Boolean) {
        prefManager.isBiometricEnabled = enabled
        _isBiometricEnabled.value = enabled
    }

    fun setRelockTimingMode(mode: String) {
        prefManager.relockTimingMode = mode
        _relockTimingMode.value = mode
    }

    fun toggleAccessibilityAsPrimary(useAccessibility: Boolean) {
        prefManager.isAccessibilityServicePrimary = useAccessibility
        _isAccessibilityServicePrimary.value = useAccessibility
        Log.d("AppLockerViewModel", "Primary Service shifted. useAccessibility=$useAccessibility")
        
        // If switched to accessibility as primary, immediately turn off usage stats service
        syncUsageStatsService()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context, AppLockerAccessibilityService::class.java)
            val hasUsage = PermissionHelper.isUsageAccessGranted(context)
            val hasOverlay = PermissionHelper.isOverlayGranted(context)
            val biometricAvail = PermissionHelper.isBiometricAvailable(context)

            _permissionState.value = PermissionState(
                isAccessibilityGranted = hasAccessibility,
                isUsageAccessGranted = hasUsage,
                isOverlayGranted = hasOverlay,
                isBiometricAvailable = biometricAvail
            )

            // Dynamic launch configuration or synchronization
            syncUsageStatsService()
        }
    }

    private fun syncUsageStatsService() {
        val enabled = _isLockerEnabled.value
        val primaryIsAccessibility = _isAccessibilityServicePrimary.value
        val hasUsagePerms = PermissionHelper.isUsageAccessGranted(context)

        val serviceIntent = Intent(context, UsageStatsMonitorService::class.java)
        if (enabled && !primaryIsAccessibility && hasUsagePerms) {
            if (!UsageStatsMonitorService.isRunning) {
                try {
                    context.startService(serviceIntent)
                    Log.d("AppLockerViewModel", "UsageStatsMonitorService started.")
                } catch (e: Exception) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            context.startForegroundService(serviceIntent)
                        } catch (ex: Exception) {
                            Log.e("AppLockerViewModel", "Failed starting usage stats service: ${ex.message}")
                        }
                    }
                }
            }
        } else {
            if (UsageStatsMonitorService.isRunning) {
                context.stopService(serviceIntent)
                Log.d("AppLockerViewModel", "UsageStatsMonitorService stopped.")
            }
        }
    }

    fun setSetupCompleted() {
        prefManager.isSetupCompleted = true
    }

    fun isSetupCompleted(): Boolean {
        return prefManager.isSetupCompleted
    }

    fun isPinSet(): Boolean {
        return pinManager.isPinSet()
    }

    fun savePin(pin: String): Boolean {
        return pinManager.setPin(pin)
    }

    class Factory(private val app: AppLockerApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppLockerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppLockerViewModel(app.repository, app.preferenceManager, app.pinManager, app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
