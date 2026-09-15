package com.spacemishka.app.amateurfunktraining.feature.exam

import com.spacemishka.app.amateurfunktraining.core.data.InMemoryExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamConfig
import com.spacemishka.app.amateurfunktraining.core.model.ExamPartConfig
import com.spacemishka.app.amateurfunktraining.core.model.Question
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var questionRepository: FakeQuestionRepository
    private lateinit var examRepository: InMemoryExamRepository
    private lateinit var viewModel: ExamViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        questionRepository = FakeQuestionRepository()
        examRepository = InMemoryExamRepository()
        viewModel = ExamViewModel(questionRepository, examRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartExamAndQuestionNavigation() = runTest {
        viewModel.startExam()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.questions.isNotEmpty())
        assertEquals(0, state.currentIndex)

        // Select answer
        viewModel.selectAnswer(2)
        assertEquals(2, viewModel.uiState.value.currentQuestionState?.selectedAnswerIndex)
        assertTrue(viewModel.uiState.value.currentQuestionState?.isAnswered == true)

        // Mark for review
        viewModel.toggleMarkForReview()
        assertTrue(viewModel.uiState.value.currentQuestionState?.isMarkedForReview == true)
        viewModel.toggleMarkForReview()
        assertFalse(viewModel.uiState.value.currentQuestionState?.isMarkedForReview == true)

        // Navigation
        viewModel.nextQuestion()
        assertEquals(1, viewModel.uiState.value.currentIndex)

        viewModel.previousQuestion()
        assertEquals(0, viewModel.uiState.value.currentIndex)

        viewModel.goToQuestion(3)
        assertEquals(3, viewModel.uiState.value.currentIndex)
    }

    @Test
    fun testSubmitExamLifecycle() = runTest {
        val testConfig = ExamConfig(
            parts = listOf(
                ExamPartConfig(Category.TECHNIK, 2),
                ExamPartConfig(Category.BETRIEB, 1),
                ExamPartConfig(Category.VORSCHRIFTEN, 1)
            ),
            durationMinutes = 10
        )

        viewModel.startExam(testConfig)
        advanceUntilIdle()

        // Answer questions
        val total = viewModel.uiState.value.questions.size
        for (i in 0 until total) {
            viewModel.goToQuestion(i)
            // answer correctly (correct answer is index 0 in fake repo)
            viewModel.selectAnswer(0)
        }

        viewModel.submitExam()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("isFinished muss true sein", finalState.isFinished)
        assertNotNull("examResult darf nicht null sein", finalState.examResult)
        assertTrue("Prüfung muss bestanden sein", finalState.examResult?.isOverallPassed == true)
        assertEquals(0, finalState.examResult?.wrongQuestions?.size)

        // Check persistence
        val history = examRepository.getRecentExamResults(5)
        assertEquals(1, history.size)
        assertTrue(history[0].isOverallPassed)
    }

    private class FakeQuestionRepository : QuestionRepository {
        private val dummyQuestions = listOf(
            Question("T-1", Category.TECHNIK, "top", "Tech 1", listOf("A", "B", "C", "D"), 0, "Ex"),
            Question("T-2", Category.TECHNIK, "top", "Tech 2", listOf("A", "B", "C", "D"), 0, "Ex"),
            Question("T-3", Category.TECHNIK, "top", "Tech 3", listOf("A", "B", "C", "D"), 0, "Ex"),
            Question("B-1", Category.BETRIEB, "top", "Betrieb 1", listOf("A", "B", "C", "D"), 0, "Ex"),
            Question("B-2", Category.BETRIEB, "top", "Betrieb 2", listOf("A", "B", "C", "D"), 0, "Ex"),
            Question("V-1", Category.VORSCHRIFTEN, "top", "Vorschrift 1", listOf("A", "B", "C", "D"), 0, "Ex"),
            Question("V-2", Category.VORSCHRIFTEN, "top", "Vorschrift 2", listOf("A", "B", "C", "D"), 0, "Ex")
        )

        override suspend fun getAllQuestions(): List<Question> = dummyQuestions

        override suspend fun getQuestionsByCategory(category: Category): List<Question> =
            if (category == Category.ALL) dummyQuestions else dummyQuestions.filter { it.category == category }

        override suspend fun getQuestionById(id: String): Question? = dummyQuestions.find { it.id == id }

        override suspend fun getCategoryCounts(): Map<Category, Int> = mapOf(
            Category.TECHNIK to 3,
            Category.BETRIEB to 2,
            Category.VORSCHRIFTEN to 2,
            Category.ALL to 7
        )
    }
}
