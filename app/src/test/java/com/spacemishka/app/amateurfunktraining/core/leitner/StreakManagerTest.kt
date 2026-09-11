package com.spacemishka.app.amateurfunktraining.core.leitner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakManagerTest {

    @Test
    fun testFirstPracticeStartsStreakAtOne() {
        val today = LocalDate.of(2026, 9, 11)
        val result = StreakManager.onPracticeCompleted(
            lastPracticeDateString = null,
            currentStreak = 0,
            nowDate = today
        )

        assertEquals(1, result.newStreak)
        assertEquals("2026-09-11", result.newDateString)
        assertTrue(result.hasChanged)
    }

    @Test
    fun testPracticeOnSameDayKeepsStreakUnchanged() {
        val today = LocalDate.of(2026, 9, 11)
        val result = StreakManager.onPracticeCompleted(
            lastPracticeDateString = "2026-09-11",
            currentStreak = 5,
            nowDate = today
        )

        assertEquals(5, result.newStreak)
        assertEquals("2026-09-11", result.newDateString)
        assertFalse(result.hasChanged)
    }

    @Test
    fun testPracticeOnConsecutiveDayIncrementsStreak() {
        val today = LocalDate.of(2026, 9, 12)
        val result = StreakManager.onPracticeCompleted(
            lastPracticeDateString = "2026-09-11",
            currentStreak = 5,
            nowDate = today
        )

        assertEquals(6, result.newStreak)
        assertEquals("2026-09-12", result.newDateString)
        assertTrue(result.hasChanged)
    }

    @Test
    fun testPracticeAfterMissedDayResetsStreakToOne() {
        val today = LocalDate.of(2026, 9, 14) // 3 days later
        val result = StreakManager.onPracticeCompleted(
            lastPracticeDateString = "2026-09-11",
            currentStreak = 10,
            nowDate = today
        )

        assertEquals(1, result.newStreak)
        assertEquals("2026-09-14", result.newDateString)
        assertTrue(result.hasChanged)
    }

    @Test
    fun testComputeDisplayStreak() {
        val today = LocalDate.of(2026, 9, 12)

        // Practiced today
        assertEquals(4, StreakManager.computeDisplayStreak("2026-09-12", 4, today))

        // Practiced yesterday (streak still active, waiting for today's practice)
        assertEquals(4, StreakManager.computeDisplayStreak("2026-09-11", 4, today))

        // Practiced 2 days ago (streak broken)
        assertEquals(0, StreakManager.computeDisplayStreak("2026-09-10", 4, today))

        // No practice ever
        assertEquals(0, StreakManager.computeDisplayStreak(null, 0, today))
    }
}
