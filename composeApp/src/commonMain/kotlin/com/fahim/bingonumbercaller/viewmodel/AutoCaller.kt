package com.fahim.bingonumbercaller.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AutoCallIntervals {
    /** Choices offered to the caller. 3s leaves room for the spoken call to finish. */
    val OPTIONS_SECONDS: List<Int> = listOf(3, 5, 8, 12)
    const val DEFAULT_SECONDS: Int = 5
}

/**
 * Fires [onTick] on a fixed cadence. Only a timer: whether a draw is allowed, and whether it
 * succeeds, stays with the ViewModel and the authoritative server.
 */
internal class AutoCaller(
    private val scope: CoroutineScope,
    private val onTick: () -> Unit
) {
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    /** @param tickImmediately true to draw now (starting); false to wait a full interval (retiming). */
    fun start(intervalMillis: Long, tickImmediately: Boolean) {
        stop()
        job = scope.launch {
            if (!tickImmediately) delay(intervalMillis)
            while (isActive) {
                onTick()
                delay(intervalMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
