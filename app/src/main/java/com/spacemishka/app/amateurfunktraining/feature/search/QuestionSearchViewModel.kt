package com.spacemishka.app.amateurfunktraining.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.data.ProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuestionSearchViewModel(
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionSearchUiState(isLoading = true))
    val uiState: StateFlow<QuestionSearchUiState> = _uiState.asStateFlow()

    private var allQuestionsCache: List<Question> = emptyList()
    private var progressMap: Map<String, QuestionProgress> = emptyMap()

    init {
        loadData()
        observeProgress()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            allQuestionsCache = questionRepository.getAllQuestions()
            applyFilterAndSearch()
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            progressRepository.getAllProgressStream().collect { progressList ->
                progressMap = progressList.associateBy { it.questionId }
                applyFilterAndSearch()
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        applyFilterAndSearch()
    }

    fun onCategorySelected(category: Category?) {
        _uiState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
        applyFilterAndSearch()
    }

    fun toggleOnlyBookmarked() {
        _uiState.update { it.copy(onlyBookmarked = !it.onlyBookmarked) }
        applyFilterAndSearch()
    }

    fun toggleOnlyMistakes() {
        _uiState.update { it.copy(onlyMistakes = !it.onlyMistakes) }
        applyFilterAndSearch()
    }

    fun toggleOnlyWithImage() {
        _uiState.update { it.copy(onlyWithImage = !it.onlyWithImage) }
        applyFilterAndSearch()
    }

    fun onLeitnerBoxSelected(box: Int?) {
        _uiState.update {
            it.copy(selectedLeitnerBox = if (it.selectedLeitnerBox == box) null else box)
        }
        applyFilterAndSearch()
    }

    fun toggleCardExpanded(questionId: String) {
        _uiState.update { state ->
            val newExpanded = if (questionId in state.expandedQuestionIds) {
                state.expandedQuestionIds - questionId
            } else {
                state.expandedQuestionIds + questionId
            }
            val updatedResults = state.results.map { item ->
                if (item.question.id == questionId) {
                    item.copy(isExpanded = questionId in newExpanded)
                } else {
                    item
                }
            }
            state.copy(expandedQuestionIds = newExpanded, results = updatedResults)
        }
    }

    fun toggleBookmark(questionId: String) {
        viewModelScope.launch {
            progressRepository.toggleBookmark(questionId)
        }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                query = "",
                selectedCategory = null,
                onlyBookmarked = false,
                onlyMistakes = false,
                onlyWithImage = false,
                selectedLeitnerBox = null
            )
        }
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val currentState = _uiState.value
        val query = currentState.query.trim()
        val cat = currentState.selectedCategory

        viewModelScope.launch {
            // Search base from question repository
            val searched = questionRepository.searchQuestions(query, cat)

            // Apply additional filter toggles
            val filtered = searched.filter { q ->
                val progress = progressMap[q.id]

                val matchesBookmark = !currentState.onlyBookmarked || (progress?.isBookmarked == true)
                val matchesMistakes = !currentState.onlyMistakes || ((progress?.errorCount ?: 0) >= 1)
                val matchesImage = !currentState.onlyWithImage || (!q.imageSvg.isNullOrBlank())
                val matchesLeitner = currentState.selectedLeitnerBox == null || ((progress?.leitnerBox ?: 1) == currentState.selectedLeitnerBox)

                matchesBookmark && matchesMistakes && matchesImage && matchesLeitner
            }

            // Category counts for current query and active filters (excluding category selection)
            val baseForCategoryCounts = questionRepository.searchQuestions(query, null).filter { q ->
                val progress = progressMap[q.id]
                val matchesBookmark = !currentState.onlyBookmarked || (progress?.isBookmarked == true)
                val matchesMistakes = !currentState.onlyMistakes || ((progress?.errorCount ?: 0) >= 1)
                val matchesImage = !currentState.onlyWithImage || (!q.imageSvg.isNullOrBlank())
                val matchesLeitner = currentState.selectedLeitnerBox == null || ((progress?.leitnerBox ?: 1) == currentState.selectedLeitnerBox)
                matchesBookmark && matchesMistakes && matchesImage && matchesLeitner
            }

            val counts = mutableMapOf<Category?, Int>()
            counts[null] = baseForCategoryCounts.size
            counts[Category.TECHNIK] = baseForCategoryCounts.count { it.category == Category.TECHNIK }
            counts[Category.BETRIEB] = baseForCategoryCounts.count { it.category == Category.BETRIEB }
            counts[Category.VORSCHRIFTEN] = baseForCategoryCounts.count { it.category == Category.VORSCHRIFTEN }

            val resultItems = filtered.map { q ->
                QuestionSearchResultItem(
                    question = q,
                    progress = progressMap[q.id],
                    isExpanded = q.id in currentState.expandedQuestionIds
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = resultItems,
                    totalFoundCount = resultItems.size,
                    categoryCounts = counts
                )
            }
        }
    }
}
