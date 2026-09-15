package com.spacemishka.app.amateurfunktraining.core.exam

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamConfig
import com.spacemishka.app.amateurfunktraining.core.model.ExamQuestionState
import com.spacemishka.app.amateurfunktraining.core.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamEvaluationTest {

    private fun createQuestionsForPart(category: Category, total: Int, correctCount: Int): List<ExamQuestionState> {
        return (1..total).map { i ->
            val isCorrect = i <= correctCount
            val q = Question(
                id = "${category.name}-$i",
                category = category,
                topicId = "top_${category.name.lowercase()}",
                text = "Frage $i",
                answers = listOf("A", "B", "C", "D"),
                correctAnswerIndex = 0,
                explanation = "Lösung $i"
            )
            ExamQuestionState(
                question = q,
                selectedAnswerIndex = if (isCorrect) 0 else 1
            )
        }
    }

    @Test
    fun testAllPartsPassed_OverallPassed() {
        // Technik: 26/34 (76.5%), Betrieb: 19/25 (76.0%), Vorschriften: 19/25 (76.0%)
        val technik = createQuestionsForPart(Category.TECHNIK, 34, 26)
        val betrieb = createQuestionsForPart(Category.BETRIEB, 25, 19)
        val vorschriften = createQuestionsForPart(Category.VORSCHRIFTEN, 25, 19)
        val states = technik + betrieb + vorschriften

        val result = ExamEvaluationEngine.evaluateExam(
            sessionId = "test-session-1",
            questionStates = states,
            durationSeconds = 3600L,
            config = ExamConfig.DEFAULT_KLASSE_E
        )

        assertTrue("Prüfung muss bestanden sein", result.isOverallPassed)
        assertEquals(3, result.partResults.size)
        assertTrue("Technik muss bestanden sein", result.partResults.first { it.category == Category.TECHNIK }.isPassed)
        assertTrue("Betrieb muss bestanden sein", result.partResults.first { it.category == Category.BETRIEB }.isPassed)
        assertTrue("Vorschriften muss bestanden sein", result.partResults.first { it.category == Category.VORSCHRIFTEN }.isPassed)
    }

    @Test
    fun testTechnikFailsThreshold_OverallFails() {
        // Technik: 25/34 (73.5% -> NICHT BESTANDEN), Betrieb: 25/25 (100%), Vorschriften: 25/25 (100%)
        val technik = createQuestionsForPart(Category.TECHNIK, 34, 25)
        val betrieb = createQuestionsForPart(Category.BETRIEB, 25, 25)
        val vorschriften = createQuestionsForPart(Category.VORSCHRIFTEN, 25, 25)
        val states = technik + betrieb + vorschriften

        val result = ExamEvaluationEngine.evaluateExam(
            sessionId = "test-session-2",
            questionStates = states,
            durationSeconds = 3600L,
            config = ExamConfig.DEFAULT_KLASSE_E
        )

        assertFalse("Prüfung darf nicht bestanden sein, wenn Technik < 75%", result.isOverallPassed)
        assertFalse("Technik muss durchgefallen sein", result.partResults.first { it.category == Category.TECHNIK }.isPassed)
        assertTrue("Betrieb muss bestanden sein", result.partResults.first { it.category == Category.BETRIEB }.isPassed)
        assertTrue("Vorschriften muss bestanden sein", result.partResults.first { it.category == Category.VORSCHRIFTEN }.isPassed)
        assertEquals("Technik-Fehleranzahl muss 9 sein (34 - 25)", 9, result.wrongQuestions.size)
    }

    @Test
    fun testVorschriftenFailsThreshold_OverallFails() {
        // Technik: 34/34 (100%), Betrieb: 25/25 (100%), Vorschriften: 18/25 (72.0% -> NICHT BESTANDEN)
        val technik = createQuestionsForPart(Category.TECHNIK, 34, 34)
        val betrieb = createQuestionsForPart(Category.BETRIEB, 25, 25)
        val vorschriften = createQuestionsForPart(Category.VORSCHRIFTEN, 25, 18)
        val states = technik + betrieb + vorschriften

        val result = ExamEvaluationEngine.evaluateExam(
            sessionId = "test-session-3",
            questionStates = states,
            durationSeconds = 3600L,
            config = ExamConfig.DEFAULT_KLASSE_E
        )

        assertFalse("Prüfung darf nicht bestanden sein, wenn Vorschriften < 75%", result.isOverallPassed)
        assertFalse("Vorschriften muss durchgefallen sein", result.partResults.first { it.category == Category.VORSCHRIFTEN }.isPassed)
    }

    @Test
    fun testUnansweredQuestionsCountAsWrong() {
        val q = Question(
            id = "T-1",
            category = Category.TECHNIK,
            topicId = "top_t",
            text = "Test",
            answers = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 0,
            explanation = "Expl"
        )
        val states = listOf(ExamQuestionState(question = q, selectedAnswerIndex = null))

        val result = ExamEvaluationEngine.evaluateExam(
            sessionId = "test-unanswered",
            questionStates = states,
            durationSeconds = 100L,
            config = ExamConfig(
                parts = listOf(com.spacemishka.app.amateurfunktraining.core.model.ExamPartConfig(Category.TECHNIK, 1)),
                durationMinutes = 10
            )
        )

        assertEquals("Unbeantwortete Frage muss als Fehler zählen", 1, result.wrongQuestions.size)
        assertEquals(0, result.partResults.first().correctAnswers)
        assertFalse(result.isOverallPassed)
    }
}
