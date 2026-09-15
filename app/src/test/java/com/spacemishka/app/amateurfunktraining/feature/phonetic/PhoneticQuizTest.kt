package com.spacemishka.app.amateurfunktraining.feature.phonetic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhoneticQuizTest {

    @Test
    fun testItuEntriesCompleteness() {
        assertEquals("Das ITU-Alphabet muss alle 26 Buchstaben A-Z enthalten", 26, PhoneticQuizViewModel.ITU_ENTRIES.size)
        val letters = PhoneticQuizViewModel.ITU_ENTRIES.map { it.buchstabe }.toSet()
        ('A'..'Z').forEach { char ->
            assertTrue("Buchstabe $char muss enthalten sein", letters.contains(char.toString()))
        }
    }

    @Test
    fun testQuestionGenerationHasUniqueFourOptions() {
        val vm = PhoneticQuizViewModel(random = Random(42))
        vm.startQuiz(QuizMode.LETTER_TO_WORD)
        val state = vm.uiState.value

        assertEquals("Quiz muss 10 Runden haben", 10, state.questions.size)

        state.questions.forEach { q ->
            assertEquals("Jede Frage muss 4 Optionen haben", 4, q.options.size)
            assertEquals("Alle 4 Optionen müssen eindeutig sein", 4, q.options.distinct().size)
            assertTrue("Korrekter Index muss im Bereich 0..3 liegen", q.correctIndex in 0..3)
            val correctWord = q.options[q.correctIndex]
            assertTrue("Erklärung muss korrekte Option erwähnen", q.explanation.contains(correctWord))
        }
    }

    @Test
    fun testQuizWorkflowAndScoring() {
        val vm = PhoneticQuizViewModel(random = Random(123))
        vm.startQuiz(QuizMode.LETTER_TO_WORD)

        // Question 1: Select correct option and submit
        val q1 = vm.uiState.value.currentQuestion
        assertNotNull(q1)
        vm.selectOption(q1!!.correctIndex)
        vm.submitAnswer()

        assertTrue(vm.uiState.value.isSubmitted)
        assertEquals(1, vm.uiState.value.score)

        // Advance
        vm.nextQuestion()
        assertFalse(vm.uiState.value.isSubmitted)
        assertEquals(1, vm.uiState.value.currentIndex)

        // Question 2: Select wrong option and submit
        val q2 = vm.uiState.value.currentQuestion
        assertNotNull(q2)
        val wrongIdx = (q2!!.correctIndex + 1) % 4
        vm.selectOption(wrongIdx)
        vm.submitAnswer()

        assertTrue(vm.uiState.value.isSubmitted)
        assertEquals("Score darf sich bei Falschantwort nicht erhöhen", 1, vm.uiState.value.score)
    }

    @Test
    fun testCallsignQuizMode() {
        val vm = PhoneticQuizViewModel(random = Random(999))
        vm.startQuiz(QuizMode.CALLSIGN_SPELLING)
        val state = vm.uiState.value

        assertEquals(10, state.questions.size)
        state.questions.forEach { q ->
            assertTrue("Prompt muss ein Rufzeichen sein", q.prompt.length >= 4)
            assertEquals(4, q.options.size)
        }
    }
}
