package com.example.jarvis.core

import android.os.SystemClock

data class PerformanceMetrics(
    val coldStartTimeMs: Long = 0L,
    val warmStartTimeMs: Long = 0L,
    val sttLatencyMs: Long = 0L,
    val intentLatencyMs: Long = 0L,
    val slmLatencyMs: Long = 0L,
    val toolLatencyMs: Long = 0L,
    val ttsLatencyMs: Long = 0L,
    val peakRamMb: Long = 0L,
    val lastOperationTotalMs: Long = 0L
)

class PerformanceTracker {

    private var appStartTime: Long = SystemClock.elapsedRealtime()
    private var isColdStart: Boolean = true

    @Volatile
    private var currentMetrics = PerformanceMetrics()

    fun recordAppStart() {
        val now = SystemClock.elapsedRealtime()
        val duration = now - appStartTime
        currentMetrics = if (isColdStart) {
            isColdStart = false
            currentMetrics.copy(coldStartTimeMs = duration)
        } else {
            currentMetrics.copy(warmStartTimeMs = duration)
        }
    }

    fun recordIntentLatency(durationMs: Long) {
        currentMetrics = currentMetrics.copy(intentLatencyMs = durationMs)
    }

    fun recordSlmLatency(durationMs: Long) {
        currentMetrics = currentMetrics.copy(slmLatencyMs = durationMs)
    }

    fun recordToolLatency(durationMs: Long) {
        currentMetrics = currentMetrics.copy(toolLatencyMs = durationMs)
    }

    fun recordTtsLatency(durationMs: Long) {
        currentMetrics = currentMetrics.copy(ttsLatencyMs = durationMs)
    }

    fun recordSttLatency(durationMs: Long) {
        currentMetrics = currentMetrics.copy(sttLatencyMs = durationMs)
    }

    fun recordPeakRam(ramMb: Long) {
        if (ramMb > currentMetrics.peakRamMb) {
            currentMetrics = currentMetrics.copy(peakRamMb = ramMb)
        }
    }

    fun recordOperationTotal(totalMs: Long) {
        currentMetrics = currentMetrics.copy(lastOperationTotalMs = totalMs)
    }

    fun getMetrics(): PerformanceMetrics = currentMetrics

    fun reset() {
        currentMetrics = PerformanceMetrics()
    }
}
