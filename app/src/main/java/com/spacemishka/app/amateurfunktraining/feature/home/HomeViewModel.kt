package com.spacemishka.app.amateurfunktraining.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.data.ProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.CategoryProgress
import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val categoryCounts: Map<Category, Int> = emptyMap(),
    val categoryProgresses: Map<Category, CategoryProgress> = emptyMap(),
    val streakDays: Int = 0,
    val dueLeitnerCount: Int = 0,
    val bookmarkCount: Int = 0,
    val problemCount: Int = 0,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe real-time progress updates and streak updates
        progressRepository.getAllProgressStream()
            .onEach { refreshCalculatedProgress(it) }
            .launchIn(viewModelScope)

        progressRepository.getStreakStream()
            .onEach { streak ->
                _uiState.update { it.copy(streakDays = streak) }
            }
            .launchIn(viewModelScope)

        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val counts = questionRepository.getCategoryCounts()
                val dueCount = progressRepository.getDueLeitnerQuestionIds().size
                val bookmarkCount = progressRepository.getBookmarkedQuestionIds().size
                val problemCount = progressRepository.getProblemQuestionIds().size

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categoryCounts = counts,
                        dueLeitnerCount = dueCount,
                        bookmarkCount = bookmarkCount,
                        problemCount = problemCount
                    )
                }

                // Trigger category progress calculation
                val allProgress = progressRepository.getAllProgressStream()
                // initial calculation will be triggered by onEach
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    private suspend fun refreshCalculatedProgress(allProgressList: List<QuestionProgress>) {
        try {
            val progressMap = allProgressList.associateBy { it.questionId }
            val allQuestions = questionRepository.getAllQuestions()

            val progressByCategory = mutableMapOf<Category, CategoryProgress>()

            listOf(Category.TECHNIK, Category.BETRIEB, Category.VORSCHRIFTEN, Category.ALL).forEach { cat ->
                val catQuestions = if (cat == Category.ALL) allQuestions else allQuestions.filter { it.category == cat }
                val total = catQuestions.size

                var newCount = 0
                var inProgressCount = 0
                var masteredCount = 0

                catQuestions.forEach { q ->
                    val progress = progressMap[q.id]
                    when {
                        progress == null || progress.status == ProgressStatus.NEU -> newCount++
                        progress.isMastered -> masteredCount++
                        else -> inProgressCount++
                    }
                }

                progressByCategory[cat] = CategoryProgress(
                    category = cat,
                    totalCount = total,
                    newCount = newCount,
                    inProgressCount = inProgressCount,
                    masteredCount = masteredCount
                )
            }

            val dueCount = progressRepository.getDueLeitnerQuestionIds().size
            val bookmarkCount = progressRepository.getBookmarkedQuestionIds().size
            val problemCount = progressRepository.getProblemQuestionIds().size

            _uiState.update {
                it.copy(
                    categoryProgresses = progressByCategory,
                    dueLeitnerCount = dueCount,
                    bookmarkCount = bookmarkCount,
                    problemCount = problemCount
                )
            }
        } catch (_: Exception) {
            // Non-critical background calculation
        }
    }
}
