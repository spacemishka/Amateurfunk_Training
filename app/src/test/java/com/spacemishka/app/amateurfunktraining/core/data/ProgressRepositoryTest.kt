package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.leitner.LeitnerCalculator
import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressRepositoryTest {

    private lateinit var repository: ProgressRepository

    @Before
    fun setUp() {
        repository = InMemoryProgressRepository()
    }

    @Test
    fun testRecordAnswerAndStreakStream() = runTest {
        repository.recordAnswer("Q1", isCorrect = true, timestamp = 1000L)

        val progress = repository.getProgressForQuestion("Q1")
        assertNotNull(progress)
        assertEquals("Q1", progress?.questionId)
        assertEquals(2, progress?.leitnerBox)
        assertEquals(0, progress?.errorCount)

        val streak = repository.getStreakStream().first()
        assertTrue(streak >= 1)
    }

    @Test
    fun testToggleBookmark() = runTest {
        val initiallyBookmarked = repository.toggleBookmark("Q1")
        assertTrue(initiallyBookmarked)

        val progress = repository.getProgressForQuestion("Q1")
        assertTrue(progress?.isBookmarked == true)

        val bookmarks = repository.getBookmarkedQuestionIds()
        assertEquals(listOf("Q1"), bookmarks)

        val toggledOff = repository.toggleBookmark("Q1")
        assertFalse(toggledOff)
        assertTrue(repository.getBookmarkedQuestionIds().isEmpty())
    }

    @Test
    fun testProblemQuestionsFiltering() = runTest {
        // Answer Q1 incorrectly twice
        repository.recordAnswer("Q1", isCorrect = false, timestamp = 1000L)
        repository.recordAnswer("Q1", isCorrect = false, timestamp = 2000L)

        // Answer Q2 correctly
        repository.recordAnswer("Q2", isCorrect = true, timestamp = 1500L)

        // Answer Q3 incorrectly once
        repository.recordAnswer("Q3", isCorrect = false, timestamp = 1800L)

        val problemIds = repository.getProblemQuestionIds()
        assertEquals(listOf("Q1"), problemIds)
    }

    @Test
    fun testDueLeitnerQuestions() = runTest {
        val now = 10_000_000_000L
        // Q1 was answered 2 days ago in Box 1 (interval 1 day -> DUE)
        repository.recordAnswer("Q1", isCorrect = false, timestamp = now - 2 * LeitnerCalculator.ONE_DAY_MS)

        // Q2 was answered 1 hour ago in Box 1 (NOT DUE)
        repository.recordAnswer("Q2", isCorrect = false, timestamp = now - 3600_000L)

        val dueIds = repository.getDueLeitnerQuestionIds(now)
        assertTrue(dueIds.contains("Q1"))
        assertFalse(dueIds.contains("Q2"))
    }
}
