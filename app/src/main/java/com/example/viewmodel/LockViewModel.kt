package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppLockerApplication
import com.example.security.PinManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LockViewModel(
    private val pinManager: PinManager,
    context: Context
) : ViewModel() {

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _isVibratingError = MutableStateFlow(false)
    val isVibratingError: StateFlow<Boolean> = _isVibratingError.asStateFlow()

    private val _lockoutSecondsLeft = MutableStateFlow(0L)
    val lockoutSecondsLeft: StateFlow<Long> = _lockoutSecondsLeft.asStateFlow()

    private val _unlockSuccessEvent = MutableSharedFlow<Unit>()
    val unlockSuccessEvent: SharedFlow<Unit> = _unlockSuccessEvent.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        checkLockoutStatus()
    }

    fun onKeyPress(char: Char) {
        if (isLockedOut()) return
        val current = _pinInput.value
        if (current.length < 4) {
            val newVal = current + char
            _pinInput.value = newVal
            if (newVal.length == 4) {
                // Instantly trigger validation when 4 digits are completed
                verifyInput(newVal)
            }
        }
    }

    fun onDeletePress() {
        if (isLockedOut()) return
        val current = _pinInput.value
        if (current.isNotEmpty()) {
            _pinInput.value = current.dropLast(1)
        }
    }

    fun onClearPress() {
        if (isLockedOut()) return
        _pinInput.value = ""
    }

    private fun verifyInput(input: String) {
        viewModelScope.launch {
            delay(150) // Give short visual delay for the 4th indicator selection
            if (pinManager.verifyPin(input)) {
                _unlockSuccessEvent.emit(Unit)
            } else {
                // PIN is incorrect, trigger vibration shake feedback
                _pinInput.value = ""
                _isVibratingError.value = true
                delay(400)
                _isVibratingError.value = false
                checkLockoutStatus()
            }
        }
    }

    private fun checkLockoutStatus() {
        if (pinManager.isLockedOut()) {
            _pinInput.value = ""
            startCountdown()
        }
    }

    fun isLockedOut(): Boolean {
        return pinManager.isLockedOut()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (pinManager.isLockedOut()) {
                _lockoutSecondsLeft.value = pinManager.getRemainingLockoutTimeSeconds()
                delay(1000L)
            }
            _lockoutSecondsLeft.value = 0L
        }
    }

    class Factory(private val app: AppLockerApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LockViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LockViewModel(app.pinManager, app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
