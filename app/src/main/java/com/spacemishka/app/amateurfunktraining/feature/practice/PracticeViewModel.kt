package com.spacemishka.app.amateurfunktraining.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.data.ProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.leitner.LeitnerCalculator
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PracticeViewModel(
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val wrongQuestionsPool = mutableListOf<Question>()

    fun startPractice(category: Category, shuffle: Boolean = true) {
        startPractice(PracticeMode.CategoryMode(category), shuffle)
    }

    fun startPractice(mode: PracticeMode, shuffle: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, practiceMode = mode, errorMessage = null) }
            try {
                val allQuestions = questionRepository.getAllQuestions()
                val loadedQuestions = when (mode) {
                    is PracticeMode.CategoryMode -> questionRepository.getQuestionsByCategory(mode.category)
                    is PracticeMode.DueLeitner -> {
                        val dueIds = progressRepository.getDueLeitnerQuestionIds().toSet()
                        allQuestions.filter { it.id in dueIds }
                    }
                    is PracticeMode.Bookmarks -> {
                        val bookmarkIds = progressRepository.getBookmarkedQuestionIds().toSet()
                        allQuestions.filter { it.id in bookmarkIds }
                    }
                    is PracticeMode.ProblemQuestions -> {
                        val problemIds = progressRepository.getProblemQuestionIds().toSet()
                        allQuestions.filter { it.id in problemIds }
                    }
                }

                val preparedQuestions = if (shuffle && mode !is PracticeMode.ProblemQuestions) {
                    loadedQuestions.shuffled()
                } else {
                    loadedQuestions
                }

                wrongQuestionsPool.clear()

                val firstQuestion = preparedQuestions.firstOrNull()
                val firstProgress = firstQuestion?.let { progressRepository.getProgressForQuestion(it.id) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = preparedQuestions,
                        currentIndex = 0,
                        selectedAnswerIndex = null,
                        isAnswerConfirmed = false,
                        isCorrect = null,
                        correctCount = 0,
                        wrongCount = 0,
                        isFinished = false,
                        isBookmarked = firstProgress?.isBookmarked ?: false,
                        currentLeitnerBox = firstProgress?.leitnerBox ?: 1,
                        currentErrorCount = firstProgress?.errorCount ?: 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Fehler beim Laden der Fragen: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectAnswer(index: Int) {
        val currentState = _uiState.value
        if (currentState.isAnswerConfirmed || currentState.currentQuestion == null) return

        val question = currentState.currentQuestion ?: return
        val isCorrect = (index == question.correctAnswerIndex)

        if (!isCorrect) {
            wrongQuestionsPool.add(question)
        }

        viewModelScope.launch {
            progressRepository.recordAnswer(question.id, isCorrect)
            val updatedProgress = progressRepository.getProgressForQuestion(question.id)

            _uiState.update {
                it.copy(
                    selectedAnswerIndex = index,
                    isAnswerConfirmed = true,
                    isCorrect = isCorrect,
                    correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                    wrongCount = if (!isCorrect) it.wrongCount + 1 else it.wrongCount,
                    currentLeitnerBox = updatedProgress?.leitnerBox ?: if (isCorrect) (it.currentLeitnerBox + 1).coerceAtMost(5) else 1,
                    currentErrorCount = updatedProgress?.errorCount ?: if (!isCorrect) it.currentErrorCount + 1 else it.currentErrorCount
                )
            }
        }
    }

    fun toggleBookmark() {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            val newStatus = progressRepository.toggleBookmark(question.id)
            _uiState.update { it.copy(isBookmarked = newStatus) }
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentIndex + 1

        if (nextIndex < currentState.questions.size) {
            val nextQuestion = currentState.questions[nextIndex]
            viewModelScope.launch {
                val nextProgress = progressRepository.getProgressForQuestion(nextQuestion.id)
                _uiState.update {
                    it.copy(
                        currentIndex = nextIndex,
                        selectedAnswerIndex = null,
                        isAnswerConfirmed = false,
                        isCorrect = null,
                        isBookmarked = nextProgress?.isBookmarked ?: false,
                        currentLeitnerBox = nextProgress?.leitnerBox ?: 1,
                        currentErrorCount = nextProgress?.errorCount ?: 0
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    isFinished = true,
                    selectedAnswerIndex = null,
                    isAnswerConfirmed = false
                )
            }
        }
    }

    fun retryWrongQuestions() {
        if (wrongQuestionsPool.isEmpty()) return
        val retryList = wrongQuestionsPool.toList().shuffled()
        wrongQuestionsPool.clear()

        viewModelScope.launch {
            val firstQuestion = retryList.firstOrNull()
            val firstProgress = firstQuestion?.let { progressRepository.getProgressForQuestion(it.id) }

            _uiState.update {
                it.copy(
                    questions = retryList,
                    currentIndex = 0,
                    selectedAnswerIndex = null,
                    isAnswerConfirmed = false,
                    isCorrect = null,
                    correctCount = 0,
                    wrongCount = 0,
                    isFinished = false,
                    isBookmarked = firstProgress?.isBookmarked ?: false,
                    currentLeitnerBox = firstProgress?.leitnerBox ?: 1,
                    currentErrorCount = firstProgress?.errorCount ?: 0
                )
            }
        }
    }
}
