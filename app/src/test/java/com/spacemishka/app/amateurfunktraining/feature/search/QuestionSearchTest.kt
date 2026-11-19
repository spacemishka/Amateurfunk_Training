package com.spacemishka.app.amateurfunktraining.feature.search

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionSearchTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleQuestions = listOf(
        Question(
            id = "EI502",
            category = Category.TECHNIK,
            topicId = "top_messtechnik",
            text = "Das Bild stellt die Anzeige eines Frequenzzählers dar. Welchen Stellenwert hat die mit X gekennzeichnete Ziffer?",
            answers = listOf("hundert Hertz", "ein Hertz", "ein Kilohertz", "zehn Hertz"),
            correctAnswerIndex = 0,
            explanation = "Bei der Anzeige in MHz entspricht die zweite Nachkommastelle 10 kHz...",
            imageSvg = "ei502.svg"
        ),
        Question(
            id = "EI503",
            category = Category.TECHNIK,
            topicId = "top_messtechnik",
            text = "Wie misst man Hochfrequenzströme?",
            answers = listOf("Thermo-Umformer", "Dreheisen", "Drehspul", "Weicheisen"),
            correctAnswerIndex = 0,
            explanation = "Thermo-Umformer eignen sich für HF-Ströme."
        ),
        Question(
            id = "BA101",
            category = Category.BETRIEB,
            topicId = "top_betriebstechnik",
            text = "Was bedeutet das Q-Signal QTH?",
            answers = listOf("Mein Standort ist...", "Wie ist mein Standort?", "Ich gebe Standort", "Standortwechsel"),
            correctAnswerIndex = 0,
            explanation = "QTH steht für den Standort."
        ),
        Question(
            id = "VA201",
            category = Category.VORSCHRIFTEN,
            topicId = "top_vorschriften",
            text = "Wer darf eine Amateurfunkstelle betreiben?",
            answers = listOf("Funkamateure mit Zulassung", "Jedermann", "Nur Vereine", "CB-Funker"),
            correctAnswerIndex = 0,
            explanation = "Gemäß AFuG bedarf es einer Zulassung."
        )
    )

    private val fakeRepo = object : QuestionRepository {
        override suspend fun getAllQuestions(): List<Question> = sampleQuestions
        override suspend fun getQuestionsByCategory(category: Category): List<Question> =
            sampleQuestions.filter { it.category == category }
        override suspend fun getQuestionById(id: String): Question? =
            sampleQuestions.firstOrNull { it.id.equals(id, ignoreCase = true) }
        override suspend fun getQuestionsByTopic(topicId: String): List<Question> =
            sampleQuestions.filter { it.topicId == topicId }
        override suspend fun getCategoryCounts(): Map<Category, Int> =
            sampleQuestions.groupBy { it.category }.mapValues { it.value.size }
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
    fun `searchQuestions ranks exact ID match first`() = runTest {
        val results = fakeRepo.searchQuestions("EI502")
        assertEquals(1, results.size)
        assertEquals("EI502", results.first().id)
    }

    @Test
    fun `searchQuestions finds questions by prefix ID`() = runTest {
        val results = fakeRepo.searchQuestions("EI")
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == "EI502" })
        assertTrue(results.any { it.id == "EI503" })
    }

    @Test
    fun `searchQuestions finds questions by text tokens`() = runTest {
        val results = fakeRepo.searchQuestions("Frequenzzähler Ziffer")
        assertEquals(1, results.size)
        assertEquals("EI502", results.first().id)
    }

    @Test
    fun `searchQuestions filters by category`() = runTest {
        val results = fakeRepo.searchQuestions(query = "", category = Category.BETRIEB)
        assertEquals(1, results.size)
        assertEquals("BA101", results.first().id)
    }

    @Test
    fun `QuestionSearchViewModel updates results and toggles filters`() = runTest {
        val viewModel = QuestionSearchViewModel(fakeRepo, progressRepository)
        advanceUntilIdle()

        // Initially empty query returns all questions
        assertEquals(4, viewModel.uiState.value.results.size)
        assertEquals(4, viewModel.uiState.value.totalFoundCount)

        // Query EI
        viewModel.onQueryChanged("EI")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.results.size)

        // Filter by image only
        viewModel.toggleOnlyWithImage()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.size)
        assertEquals("EI502", viewModel.uiState.value.results.first().question.id)

        // Toggle card expansion
        assertFalse(viewModel.uiState.value.results.first().isExpanded)
        viewModel.toggleCardExpanded("EI502")
        assertTrue(viewModel.uiState.value.results.first().isExpanded)

        // Bookmark question
        viewModel.toggleBookmark("EI502")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.results.first().progress?.isBookmarked == true)

        // Clear query
        viewModel.onQueryChanged("")
        advanceUntilIdle()
        // Only EI502 has an image, so with image filter active count is 1
        assertEquals(1, viewModel.uiState.value.results.size)

        // Clear all filters
        viewModel.clearFilters()
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.results.size)
    }
}
