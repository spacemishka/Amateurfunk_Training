package com.spacemishka.app.amateurfunktraining.core.exam

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamConfig
import com.spacemishka.app.amateurfunktraining.core.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExamGeneratorTest {

    private fun createDummyQuestions(category: Category, count: Int, prefix: String): List<Question> {
        return (1..count).map { i ->
            Question(
                id = "$prefix-$i",
                category = category,
                topicId = "top_${prefix.lowercase()}",
                text = "Frage $prefix $i",
                answers = listOf("A", "B", "C", "D"),
                correctAnswerIndex = 0,
                explanation = "Lösung $i"
            )
        }
    }

    @Test
    fun testGenerateExamQuestionsExactCounts() {
        val technikList = createDummyQuestions(Category.TECHNIK, 50, "T")
        val betriebList = createDummyQuestions(Category.BETRIEB, 40, "B")
        val vorschriftenList = createDummyQuestions(Category.VORSCHRIFTEN, 40, "V")
        val allQuestions = technikList + betriebList + vorschriftenList

        val examQuestions = ExamGenerator.generateExamQuestions(allQuestions, ExamConfig.DEFAULT_KLASSE_E)

        assertEquals("Gesamtfragenanzahl muss genau 84 sein", 84, examQuestions.size)

        val technikCount = examQuestions.count { it.question.category == Category.TECHNIK }
        val betriebCount = examQuestions.count { it.question.category == Category.BETRIEB }
        val vorschriftenCount = examQuestions.count { it.question.category == Category.VORSCHRIFTEN }

        assertEquals("Technik-Fragen müssen genau 34 sein", 34, technikCount)
        assertEquals("Betriebs-Fragen müssen genau 25 sein", 25, betriebCount)
        assertEquals("Vorschriften-Fragen müssen genau 25 sein", 25, vorschriftenCount)

        // Verify zero duplicates
        val seenIds = mutableSetOf<String>()
        for (qState in examQuestions) {
            assertFalse("Doppelte Frage-ID entdeckt: ${qState.question.id}", seenIds.contains(qState.question.id))
            seenIds.add(qState.question.id)
            assertFalse("Initial darf Frage nicht als beantwortet gelten", qState.isAnswered)
            assertFalse("Initial darf Frage nicht für Review markiert sein", qState.isMarkedForReview)
        }
    }

    @Test
    fun testGenerateExamQuestionsWithFewerAvailableQuestions() {
        // Edge case: smaller pool than 84 questions (e.g. mock test environment)
        val technikList = createDummyQuestions(Category.TECHNIK, 10, "T")
        val betriebList = createDummyQuestions(Category.BETRIEB, 10, "B")
        val vorschriftenList = createDummyQuestions(Category.VORSCHRIFTEN, 10, "V")
        val allQuestions = technikList + betriebList + vorschriftenList

        val examQuestions = ExamGenerator.generateExamQuestions(allQuestions, ExamConfig.DEFAULT_KLASSE_E)

        assertEquals("Muss alle verfügbaren Fragen nehmen, ohne abzustürzen", 30, examQuestions.size)
    }
}
