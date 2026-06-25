package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.AppLockerAccessibilityService
import com.example.util.PermissionHelper
import com.example.viewmodel.AppLockerViewModel

object AppDestinations {
    const val ONBOARDING = "onboarding"
    const val PIN_SETUP = "pin_setup"
    const val APP_LIST = "app_list"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    viewModel: AppLockerViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // 1. Determine safe default start destination
    val isPinSet = viewModel.isPinSet()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context, AppLockerAccessibilityService::class.java)
    val isUsageGranted = PermissionHelper.isUsageAccessGranted(context)
    val isOverlayGranted = PermissionHelper.isOverlayGranted(context)

    // Ready if Accessibility OK, or both Usage stats and Overlay OK
    val isPermissionsReady = isAccessibilityGranted || (isUsageGranted && isOverlayGranted)

    val startDestination = try {
        when {
            !isPinSet -> AppDestinations.PIN_SETUP
            !isPermissionsReady -> AppDestinations.ONBOARDING
            else -> AppDestinations.APP_LIST
        }
    } catch (e: Exception) {
        AppDestinations.PIN_SETUP
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.ONBOARDING) {
            OnboardingScreen(
                viewModel = viewModel,
                onCompleted = {
                    navController.navigate(AppDestinations.APP_LIST) {
                        popUpTo(AppDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.PIN_SETUP) {
            PinSetupScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    val currentPermissionsReady = PermissionHelper.isAccessibilityServiceEnabled(context, AppLockerAccessibilityService::class.java) || 
                                                 (PermissionHelper.isUsageAccessGranted(context) && PermissionHelper.isOverlayGranted(context))
                    
                    val nextDest = if (currentPermissionsReady) AppDestinations.APP_LIST else AppDestinations.ONBOARDING
                    navController.navigate(nextDest) {
                        popUpTo(AppDestinations.PIN_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.APP_LIST) {
            AppListScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(AppDestinations.SETTINGS)
                }
            )
        }

        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onChangePin = {
                    navController.navigate(AppDestinations.PIN_SETUP)
                }
            )
        }
    }
}
