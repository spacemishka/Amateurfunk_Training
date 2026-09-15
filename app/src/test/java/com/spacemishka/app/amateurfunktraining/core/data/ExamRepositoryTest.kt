package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.ExamResultPart
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.core.model.ReadinessLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRepositoryTest {

    @Test
    fun testInMemoryExamRepository_RecordAndQuery() = runTest {
        val repo = InMemoryExamRepository()

        assertTrue(repo.getRecentExamResults(10).isEmpty())
        assertEquals(ReadinessLevel.NOT_READY, repo.getReadiness().level)

        val wrongQ1 = Question("E-1", Category.TECHNIK, "top", "Frage 1", listOf("A", "B", "C", "D"), 0, "Ex")
        val wrongQ2 = Question("B-1", Category.BETRIEB, "top", "Frage 2", listOf("A", "B", "C", "D"), 0, "Ex")

        val result = ExamResult(
            sessionId = "sess-1",
            timestamp = 1000L,
            durationSeconds = 1200L,
            partResults = listOf(
                ExamResultPart(Category.TECHNIK, 34, 30, 0.88f, true),
                ExamResultPart(Category.BETRIEB, 25, 24, 0.96f, true),
                ExamResultPart(Category.VORSCHRIFTEN, 25, 23, 0.92f, true)
            ),
            isOverallPassed = true,
            wrongQuestions = listOf(wrongQ1, wrongQ2)
        )

        repo.recordExamResult(result)

        val recent = repo.getRecentExamResults(10)
        assertEquals(1, recent.size)
        assertEquals("sess-1", recent[0].sessionId)
        assertTrue(recent[0].isOverallPassed)
        assertEquals(77, recent[0].totalCorrect)

        val wrongIds = repo.getLatestWrongQuestionIds(10)
        assertEquals(2, wrongIds.size)
        assertTrue(wrongIds.contains("E-1"))
        assertTrue(wrongIds.contains("B-1"))

        val sessionWrongIds = repo.getWrongQuestionIdsForSession("sess-1")
        assertEquals(2, sessionWrongIds.size)

        val stream = repo.getExamHistoryStream().first()
        assertEquals(1, stream.size)
    }
}
