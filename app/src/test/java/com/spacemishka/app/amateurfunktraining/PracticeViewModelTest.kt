package com.spacemishka.app.amateurfunktraining

import com.spacemishka.app.amateurfunktraining.core.data.InMemoryProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeMode
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleQuestions = listOf(
        Question(
            id = "Q1",
            category = Category.TECHNIK,
            topicId = "topic1",
            text = "Was ist U = R * I?",
            answers = listOf("Ohmsches Gesetz", "Coulomb", "Kirchhoff", "Lenz"),
            correctAnswerIndex = 0,
            explanation = "U = R * I ist das Ohmsche Gesetz."
        ),
        Question(
            id = "Q2",
            category = Category.TECHNIK,
            topicId = "topic2",
            text = "Einheit der Frequenz?",
            answers = listOf("Watt", "Hertz", "Volt", "Ampere"),
            correctAnswerIndex = 1,
            explanation = "Frequenz wird in Hertz gemessen."
        )
    )

    private val fakeRepository = object : QuestionRepository {
        override suspend fun getAllQuestions(): List<Question> = sampleQuestions
        override suspend fun getQuestionsByCategory(category: Category): List<Question> = sampleQuestions
        override suspend fun getQuestionById(id: String): Question? = sampleQuestions.firstOrNull { it.id == id }
        override suspend fun getCategoryCounts(): Map<Category, Int> = mapOf(Category.TECHNIK to 2)
    }

    private lateinit var progressRepository: InMemoryProgressRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        progressRepository = InMemoryProgressRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartPracticeLoadsQuestions() = runTest(testDispatcher) {
        val viewModel = PracticeViewModel(fakeRepository, progressRepository)

        viewModel.startPractice(Category.TECHNIK, shuffle = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.totalCount)
        assertEquals(0, state.currentIndex)
        assertNotNull(state.currentQuestion)
        assertEquals("Q1", state.currentQuestion?.id)
        assertNull(state.selectedAnswerIndex)
        assertFalse(state.isAnswerConfirmed)
    }

    @Test
    fun testSelectCorrectAnswerRecordsProgress() = runTest(testDispatcher) {
        val viewModel = PracticeViewModel(fakeRepository, progressRepository)
        viewModel.startPractice(Category.TECHNIK, shuffle = false)
        advanceUntilIdle()

        // Q1 correct answer is 0
        viewModel.selectAnswer(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAnswerConfirmed)
        assertEquals(true, state.isCorrect)
        assertEquals(0, state.selectedAnswerIndex)
        assertEquals(1, state.correctCount)
        assertEquals(0, state.wrongCount)
        assertEquals(2, state.currentLeitnerBox) // Advanced to Box 2

        val savedProgress = progressRepository.getProgressForQuestion("Q1")
        assertNotNull(savedProgress)
        assertEquals(2, savedProgress?.leitnerBox)
    }

    @Test
    fun testSelectWrongAnswerResetsBoxTo1() = runTest(testDispatcher) {
        // Prepopulate Q1 in Box 3
        progressRepository.recordAnswer("Q1", isCorrect = true)
        progressRepository.recordAnswer("Q1", isCorrect = true)

        val viewModel = PracticeViewModel(fakeRepository, progressRepository)
        viewModel.startPractice(Category.TECHNIK, shuffle = false)
        advanceUntilIdle()

        // Q1 correct answer is 0, choose 1 (wrong)
        viewModel.selectAnswer(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAnswerConfirmed)
        assertEquals(false, state.isCorrect)
        assertEquals(0, state.correctCount)
        assertEquals(1, state.wrongCount)
        assertEquals(1, state.currentLeitnerBox) // Reset to Box 1
        assertEquals(1, state.currentErrorCount)
    }

    @Test
    fun testToggleBookmark() = runTest(testDispatcher) {
        val viewModel = PracticeViewModel(fakeRepository, progressRepository)
        viewModel.startPractice(Category.TECHNIK, shuffle = false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBookmarked)

        viewModel.toggleBookmark()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBookmarked)

        val bookmarks = progressRepository.getBookmarkedQuestionIds()
        assertEquals(listOf("Q1"), bookmarks)

        viewModel.toggleBookmark()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBookmarked)
    }

    @Test
    fun testPracticeBookmarksMode() = runTest(testDispatcher) {
        progressRepository.toggleBookmark("Q2")

        val viewModel = PracticeViewModel(fakeRepository, progressRepository)
        viewModel.startPractice(PracticeMode.Bookmarks, shuffle = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals("Q2", state.currentQuestion?.id)
        assertTrue(state.isBookmarked)
    }

    @Test
    fun testPracticeProblemQuestionsMode() = runTest(testDispatcher) {
        // Fail Q2 twice
        progressRepository.recordAnswer("Q2", isCorrect = false)
        progressRepository.recordAnswer("Q2", isCorrect = false)

        val viewModel = PracticeViewModel(fakeRepository, progressRepository)
        viewModel.startPractice(PracticeMode.ProblemQuestions, shuffle = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals("Q2", state.currentQuestion?.id)
        assertEquals(2, state.currentErrorCount)
    }

    @Test
    fun testNextQuestionAndFinish() = runTest(testDispatcher) {
        val viewModel = PracticeViewModel(fakeRepository, progressRepository)
        viewModel.startPractice(Category.TECHNIK, shuffle = false)
        advanceUntilIdle()

        viewModel.selectAnswer(0)
        advanceUntilIdle()
        viewModel.nextQuestion()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertEquals("Q2", state.currentQuestion?.id)
        assertFalse(state.isAnswerConfirmed)
        assertFalse(state.isFinished)

        viewModel.selectAnswer(1) // Q2 correct is 1
        advanceUntilIdle()
        viewModel.nextQuestion()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.isFinished)
        assertEquals(2, state.correctCount)
        assertEquals(0, state.wrongCount)
    }
}
