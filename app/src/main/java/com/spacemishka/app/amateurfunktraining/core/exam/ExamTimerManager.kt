package com.spacemishka.app.amateurfunktraining.core.exam

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExamTimerManager(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null
    private var onTimeoutCallback: (() -> Unit)? = null

    val isWarningZone: Boolean
        get() = _remainingSeconds.value in 1..600 // <= 10 minutes

    fun start(initialSeconds: Long, onTimeout: () -> Unit) {
        stop()
        _remainingSeconds.value = initialSeconds
        onTimeoutCallback = onTimeout
        startCountdown()
    }

    fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun resume() {
        if (!_isRunning.value && _remainingSeconds.value > 0) {
            startCountdown()
        }
    }

    fun stop() {
        _isRunning.value = false
        timerJob?.cancel()
        timerJob = null
        onTimeoutCallback = null
    }

    private fun startCountdown() {
        _isRunning.value = true
        timerJob = scope.launch(dispatcher) {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1000L)
                if (!isActive) break
                val newRemaining = _remainingSeconds.value - 1
                _remainingSeconds.value = newRemaining
                if (newRemaining <= 0) {
                    _isRunning.value = false
                    onTimeoutCallback?.invoke()
                    break
                }
            }
        }
    }

    companion object {
        fun formatTime(seconds: Long): String {
            val s = seconds.coerceAtLeast(0)
            val hours = s / 3600
            val minutes = (s % 3600) / 60
            val remainingSec = s % 60
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSec)
        }
    }
}
