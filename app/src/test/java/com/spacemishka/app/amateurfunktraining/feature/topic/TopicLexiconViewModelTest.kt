package com.spacemishka.app.amateurfunktraining.feature.topic

import com.spacemishka.app.amateurfunktraining.core.data.AssetQuestionRepository
import com.spacemishka.app.amateurfunktraining.core.data.AssetTopicRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TopicLexiconViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TopicLexiconViewModel {
        val topicsJson = File("src/main/assets/topics_klasse_e.json").readText(Charsets.UTF_8)
        val questionsJson = File("src/main/assets/questions_klasse_e.json").readText(Charsets.UTF_8)

        val topicRepo = AssetTopicRepository(
            jsonContentProvider = { topicsJson },
            ioDispatcher = testDispatcher
        )
        val questionRepo = AssetQuestionRepository(
            jsonContentProvider = { questionsJson },
            ioDispatcher = testDispatcher
        )

        return TopicLexiconViewModel(
            topicRepository = topicRepo,
            questionRepository = questionRepo
        )
    }

    @Test
    fun testInitialLoadingAndQuestionCounts() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.totalTopicsCount >= 50)
        assertEquals(state.totalTopicsCount, state.topics.size)

        // Verify that question counts are populated
        val firstItem = state.topics.first()
        assertTrue("Fragenanzahl muss für jedes Thema > 0 sein", firstItem.questionCount > 0)
    }

    @Test
    fun testFilteringByCategoryAndQuery() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Filter by Technik
        viewModel.onCategorySelected(Category.TECHNIK)
        advanceUntilIdle()

        val technikState = viewModel.uiState.value
        assertEquals(Category.TECHNIK, technikState.selectedCategory)
        assertTrue(technikState.topics.all { it.topic.category == Category.TECHNIK })

        // Search query
        viewModel.onSearchQueryChanged("Dipol")
        advanceUntilIdle()

        val searchState = viewModel.uiState.value
        assertTrue(searchState.topics.isNotEmpty())
        assertTrue(searchState.topics.any { it.topic.id == "top_technik_eg1" })
    }
}
