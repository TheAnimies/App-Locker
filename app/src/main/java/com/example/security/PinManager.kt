package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.data.LockerPreferenceManager
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class PinManager(context: Context, private val prefManager: LockerPreferenceManager) {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "PinManager"
        private const val KEY_ALIAS = "AppLockerSecurePinKey"
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 60000L // 1 minute lockout
    }

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                )
                keyGenerator.generateKey()
                Log.d(TAG, "Keystore AES Key created successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Keystore Key: ${e.message}", e)
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypts and saves the user's PIN.
     */
    fun setPin(pin: String): Boolean {
        if (pin.length != 4) return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            
            val encryptedBytes = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            val ivString = Base64.encodeToString(iv, Base64.DEFAULT)

            // Combine cipher text and IV with a delimiter
            val storedValue = "$encryptedString:$ivString"
            prefManager.pinHash = storedValue
            prefManager.isSetupCompleted = true
            prefManager.resetLockout()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save PIN: ${e.message}", e)
            false
        }
    }

    /**
     * Checks if a PIN has been set up.
     */
    fun isPinSet(): Boolean {
        return prefManager.pinHash != null
    }

    /**
     * Decrypts and verifies the input PIN.
     */
    fun verifyPin(enteredPin: String): Boolean {
        if (isLockedOut()) return false

        val storedValue = prefManager.pinHash ?: return false
        try {
            val parts = storedValue.split(":")
            if (parts.size != 2) return false

            val encryptedBytes = Base64.decode(parts[0], Base64.DEFAULT)
            val iv = Base64.decode(parts[1], Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val decryptedPin = String(decryptedBytes, Charsets.UTF_8)

            val isCorrect = enteredPin == decryptedPin
            if (isCorrect) {
                prefManager.resetLockout()
            } else {
                recordFailedAttempt()
            }
            return isCorrect
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify PIN: ${e.message}", e)
            recordFailedAttempt()
            return false
        }
    }

    /**
     * Failed attempts & lockout logs
     */
    private fun recordFailedAttempt() {
        val currentAttempts = prefManager.failedAttempts + 1
        prefManager.failedAttempts = currentAttempts
        if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
            prefManager.lockoutTime = System.currentTimeMillis()
            Log.w(TAG, "User locked out due to $currentAttempts consecutive failed attempts.")
        }
    }

    fun isLockedOut(): Boolean {
        val lockoutTime = prefManager.lockoutTime
        if (lockoutTime == 0L) return false
        
        if (System.currentTimeMillis() - lockoutTime > LOCKOUT_DURATION_MS) {
            prefManager.resetLockout()
            return false
        }
        return true
    }

    fun getRemainingLockoutTimeSeconds(): Long {
        val lockoutTime = prefManager.lockoutTime
        if (lockoutTime == 0L) return 0L
        val diff = LOCKOUT_DURATION_MS - (System.currentTimeMillis() - lockoutTime)
        return if (diff < 0L) 0L else diff / 1000
    }
}
