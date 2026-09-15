package com.spacemishka.app.amateurfunktraining.core.exam

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamTimerTest {

    @Test
    fun testTimeFormatting() {
        assertEquals("02:30:00", ExamTimerManager.formatTime(9000))
        assertEquals("01:01:01", ExamTimerManager.formatTime(3661))
        assertEquals("00:09:59", ExamTimerManager.formatTime(599))
        assertEquals("00:00:00", ExamTimerManager.formatTime(0))
        assertEquals("00:00:00", ExamTimerManager.formatTime(-5))
    }

    @Test
    fun testTimerCountdownAndWarningZone() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val timer = ExamTimerManager(scope, testDispatcher)

        var timeoutCalled = false
        // Start with 602 seconds (just outside 600s warning zone)
        timer.start(602L) {
            timeoutCalled = true
        }

        assertTrue("Timer muss laufen", timer.isRunning.value)
        assertEquals(602L, timer.remainingSeconds.value)
        assertFalse("Bei 602s noch keine Warnzone", timer.isWarningZone)

        // Advance 2 seconds -> 600s (enters warning zone)
        testScheduler.advanceTimeBy(2001L)
        assertEquals(600L, timer.remainingSeconds.value)
        assertTrue("Bei 600s in der Warnzone", timer.isWarningZone)

        // Pause
        timer.pause()
        assertFalse(timer.isRunning.value)
        testScheduler.advanceTimeBy(5000L)
        assertEquals("Während Pause darf Zeit nicht weiterlaufen", 600L, timer.remainingSeconds.value)

        // Resume and advance to zero
        timer.resume()
        assertTrue(timer.isRunning.value)
        testScheduler.advanceTimeBy(601_000L)

        assertEquals(0L, timer.remainingSeconds.value)
        assertFalse(timer.isRunning.value)
        assertTrue("Timeout-Callback muss nach Ablauf ausgelöst worden sein", timeoutCalled)
    }
}
