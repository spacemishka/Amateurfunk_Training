package com.spacemishka.app.amateurfunktraining.feature.home

import com.spacemishka.app.amateurfunktraining.core.data.InMemoryProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleQuestions = listOf(
        Question(
            id = "Q1",
            category = Category.TECHNIK,
            topicId = "t1",
            text = "Frage 1",
            answers = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 0,
            explanation = "Erklärung"
        ),
        Question(
            id = "Q2",
            category = Category.BETRIEB,
            topicId = "t2",
            text = "Frage 2",
            answers = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 1,
            explanation = "Erklärung"
        )
    )

    private val fakeRepository = object : QuestionRepository {
        override suspend fun getAllQuestions(): List<Question> = sampleQuestions
        override suspend fun getQuestionsByCategory(category: Category): List<Question> =
            sampleQuestions.filter { it.category == category }
        override suspend fun getQuestionById(id: String): Question? = sampleQuestions.firstOrNull { it.id == id }
        override suspend fun getCategoryCounts(): Map<Category, Int> = mapOf(
            Category.ALL to 2,
            Category.TECHNIK to 1,
            Category.BETRIEB to 1,
            Category.VORSCHRIFTEN to 0
        )
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
    fun testLoadDataPopulatesCategoryCountsAndProgress() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, progressRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.categoryCounts[Category.ALL])
        assertEquals(1, state.categoryCounts[Category.TECHNIK])
        assertEquals(1, state.categoryCounts[Category.BETRIEB])

        assertNotNull(state.categoryProgresses[Category.TECHNIK])
        assertEquals(1, state.categoryProgresses[Category.TECHNIK]?.totalCount)
        assertEquals(1, state.categoryProgresses[Category.TECHNIK]?.newCount)
        assertEquals(0, state.categoryProgresses[Category.TECHNIK]?.masteredCount)
    }

    @Test
    fun testReactiveProgressUpdateWhenAnswerRecorded() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, progressRepository)
        advanceUntilIdle()

        // Record Q1 as answered correctly 4 times to reach Box 5 (mastered)
        repeat(4) {
            progressRepository.recordAnswer("Q1", isCorrect = true)
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val technikProgress = state.categoryProgresses[Category.TECHNIK]
        assertEquals(1, technikProgress?.masteredCount)
        assertEquals(0, technikProgress?.newCount)
        assertTrue(state.streakDays >= 1)
    }

    @Test
    fun testBookmarkAndProblemCounts() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, progressRepository)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.bookmarkCount)
        assertEquals(0, viewModel.uiState.value.problemCount)

        progressRepository.toggleBookmark("Q1")
        progressRepository.recordAnswer("Q2", isCorrect = false)
        progressRepository.recordAnswer("Q2", isCorrect = false)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.bookmarkCount)
        assertEquals(1, viewModel.uiState.value.problemCount)
    }
}
