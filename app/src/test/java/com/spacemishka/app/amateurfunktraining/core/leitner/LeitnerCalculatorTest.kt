package com.spacemishka.app.amateurfunktraining.core.leitner

import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitnerCalculatorTest {

    @Test
    fun testInitialCorrectAnswerMovesToBox2() {
        val result = LeitnerCalculator.calculateNextState(
            questionId = "Q1",
            current = null,
            isCorrect = true,
            timestamp = 1000L
        )

        assertEquals("Q1", result.questionId)
        assertEquals(2, result.leitnerBox)
        assertEquals(0, result.errorCount)
        assertEquals(ProgressStatus.IN_BEARBEITUNG, result.status)
        assertEquals(1000L, result.lastAnsweredTimestamp)
        assertFalse(result.isMastered)
    }

    @Test
    fun testConsecutiveCorrectAnswersAscendToBox5Mastered() {
        var current: QuestionProgress? = null
        val expectedBoxes = listOf(2, 3, 4, 5, 5)

        for (expectedBox in expectedBoxes) {
            current = LeitnerCalculator.calculateNextState(
                questionId = "Q1",
                current = current,
                isCorrect = true,
                timestamp = 1000L
            )
            assertEquals(expectedBox, current.leitnerBox)
        }

        assertEquals(5, current!!.leitnerBox)
        assertEquals(ProgressStatus.GEMEISTERT, current.status)
        assertTrue(current.isMastered)
    }

    @Test
    fun testWrongAnswerResetsToBox1AndIncrementsErrorCount() {
        val inBox4 = QuestionProgress(
            questionId = "Q1",
            status = ProgressStatus.IN_BEARBEITUNG,
            errorCount = 1,
            lastAnsweredTimestamp = 500L,
            leitnerBox = 4,
            isBookmarked = true
        )

        val result = LeitnerCalculator.calculateNextState(
            questionId = "Q1",
            current = inBox4,
            isCorrect = false,
            timestamp = 2000L
        )

        assertEquals(1, result.leitnerBox)
        assertEquals(2, result.errorCount)
        assertTrue(result.isProblemQuestion)
        assertTrue(result.isBookmarked) // Bookmark retained
        assertEquals(2000L, result.lastAnsweredTimestamp)
    }

    @Test
    fun testDueIntervalCalculations() {
        val baseTime = 1_000_000_000L

        val box1 = QuestionProgress(questionId = "Q1", leitnerBox = 1, lastAnsweredTimestamp = baseTime)
        // 1 day = 86_400_000 ms
        assertFalse(LeitnerCalculator.isDue(box1, baseTime + LeitnerCalculator.ONE_DAY_MS - 1000L))
        assertTrue(LeitnerCalculator.isDue(box1, baseTime + LeitnerCalculator.ONE_DAY_MS))

        val box2 = QuestionProgress(questionId = "Q2", leitnerBox = 2, lastAnsweredTimestamp = baseTime)
        assertFalse(LeitnerCalculator.isDue(box2, baseTime + LeitnerCalculator.THREE_DAYS_MS - 1000L))
        assertTrue(LeitnerCalculator.isDue(box2, baseTime + LeitnerCalculator.THREE_DAYS_MS))

        val box3 = QuestionProgress(questionId = "Q3", leitnerBox = 3, lastAnsweredTimestamp = baseTime)
        assertFalse(LeitnerCalculator.isDue(box3, baseTime + LeitnerCalculator.SEVEN_DAYS_MS - 1000L))
        assertTrue(LeitnerCalculator.isDue(box3, baseTime + LeitnerCalculator.SEVEN_DAYS_MS))

        val box4 = QuestionProgress(questionId = "Q4", leitnerBox = 4, lastAnsweredTimestamp = baseTime)
        assertFalse(LeitnerCalculator.isDue(box4, baseTime + LeitnerCalculator.FOURTEEN_DAYS_MS - 1000L))
        assertTrue(LeitnerCalculator.isDue(box4, baseTime + LeitnerCalculator.FOURTEEN_DAYS_MS))

        // Box 5 is mastered and never due in regular cycle
        val box5 = QuestionProgress(questionId = "Q5", leitnerBox = 5, lastAnsweredTimestamp = baseTime)
        assertFalse(LeitnerCalculator.isDue(box5, baseTime + 100 * LeitnerCalculator.ONE_DAY_MS))
    }
}
