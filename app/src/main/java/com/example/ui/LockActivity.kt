package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.AppLockerApplication
import com.example.service.LockCoordinator
import com.example.ui.screens.LockScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.concurrent.Executor

class LockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val TAG = "LockActivity"
    }

    private lateinit var targetPackage: String
    private lateinit var coordinator: LockCoordinator
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Secure visual screen states (Disable screenshots/recents previews)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        coordinator = LockCoordinator.getInstance(this)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        if (targetPackage.isBlank()) {
            finish()
            return
        }

        Log.d(TAG, "Lock Screen active for package: $targetPackage")

        // 2. Back press Interception: Reroute target to standard Home Launcher (Prevent bypass)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitToHomeScreen()
            }
        })

        // 3. Optional layout setup for Biometrics
        val requestManager = (application as AppLockerApplication).preferenceManager
        if (requestManager.isBiometricEnabled) {
            setupBiometricPrompt()
        }

        setContent {
            MyApplicationTheme {
                LockScreen(
                    packageName = targetPackage,
                    onUnlockSuccess = {
                        coordinator.onPackageUnlocked(targetPackage)
                        finish()
                    },
                    onBackPress = {
                        exitToHomeScreen()
                    },
                    onTriggerBiometric = {
                        if (requestManager.isBiometricEnabled) {
                            showBiometricPrompt()
                        }
                    }
                )
            }
        }

        // Auto trigger biometric scan right on opening if enabled
        if (requestManager.isBiometricEnabled) {
            // Give layout 300ms to load and show biometric prompt safely
            window.decorView.postDelayed({
                showBiometricPrompt()
            }, 300)
        }
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d(TAG, "Biometric error: $errString ($errorCode)")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Biometric authentication succeeded.")
                    coordinator.onPackageUnlocked(targetPackage)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d(TAG, "Biometric authentication failed.")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock protected app")
            .setSubtitle("Confirm biological scan to proceed")
            .setNegativeButtonText("Use alternative PIN")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    private fun showBiometricPrompt() {
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed initiating biometrics: ${e.message}")
        }
    }

    private fun exitToHomeScreen() {
        coordinator.onLockScreenDismissed(targetPackage)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
