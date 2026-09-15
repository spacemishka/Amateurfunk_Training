package com.spacemishka.app.amateurfunktraining.core.exam

import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryEntry
import com.spacemishka.app.amateurfunktraining.core.model.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamReadinessTest {

    private fun createEntry(
        id: String,
        technikRichtig: Int,
        betriebRichtig: Int,
        vorschriftenRichtig: Int,
        passed: Boolean
    ): ExamHistoryEntry {
        return ExamHistoryEntry(
            sessionId = id,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 3600L,
            technikRichtig = technikRichtig,
            technikGesamt = 34,
            betriebRichtig = betriebRichtig,
            betriebGesamt = 25,
            vorschriftenRichtig = vorschriftenRichtig,
            vorschriftenGesamt = 25,
            isOverallPassed = passed
        )
    }

    @Test
    fun testEmptyHistory_NotReady() {
        val readiness = ExamReadinessCalculator.calculateReadiness(emptyList())
        assertEquals(ReadinessLevel.NOT_READY, readiness.level)
        assertEquals(0, readiness.totalSimulations)
        assertEquals(0, readiness.passedSimulations)
    }

    @Test
    fun testOneOrTwoExamsPassed_PartiallyReady() {
        val exam1 = createEntry("e1", 30, 23, 23, true)
        val readiness1 = ExamReadinessCalculator.calculateReadiness(listOf(exam1))
        assertEquals(ReadinessLevel.PARTIALLY_READY, readiness1.level)
        assertEquals(1, readiness1.passedSimulations)

        val exam2 = createEntry("e2", 28, 20, 21, true)
        val readiness2 = ExamReadinessCalculator.calculateReadiness(listOf(exam2, exam1))
        assertEquals(ReadinessLevel.PARTIALLY_READY, readiness2.level)
        assertEquals(2, readiness2.passedSimulations)
    }

    @Test
    fun testThreePassedExamsWithHighScores_Ready() {
        val exam1 = createEntry("e1", 30, 23, 23, true) // ~90%
        val exam2 = createEntry("e2", 29, 22, 22, true) // ~87%
        val exam3 = createEntry("e3", 28, 22, 22, true) // ~86%

        val readiness = ExamReadinessCalculator.calculateReadiness(listOf(exam3, exam2, exam1))
        assertEquals(ReadinessLevel.READY, readiness.level)
        assertEquals(3, readiness.totalSimulations)
        assertEquals(3, readiness.passedSimulations)
    }

    @Test
    fun testFailedExamInRecentThree_NotReady() {
        val exam1 = createEntry("e1", 30, 23, 23, true)
        val exam2 = createEntry("e2", 20, 20, 20, false) // Technik failed (< 26)
        val exam3 = createEntry("e3", 30, 24, 24, true)

        val readiness = ExamReadinessCalculator.calculateReadiness(listOf(exam3, exam2, exam1))
        assertEquals(ReadinessLevel.NOT_READY, readiness.level)
    }
}
