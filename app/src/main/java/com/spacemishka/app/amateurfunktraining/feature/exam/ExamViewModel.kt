package com.spacemishka.app.amateurfunktraining.feature.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.data.ExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.exam.ExamEvaluationEngine
import com.spacemishka.app.amateurfunktraining.core.exam.ExamGenerator
import com.spacemishka.app.amateurfunktraining.core.exam.ExamTimerManager
import com.spacemishka.app.amateurfunktraining.core.model.ExamConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ExamViewModel(
    private val questionRepository: QuestionRepository,
    private val examRepository: ExamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private val timerManager = ExamTimerManager(viewModelScope)
    private var currentSessionId: String = ""
    private var activeConfig: ExamConfig = ExamConfig.DEFAULT_KLASSE_E
    private var examStartTime: Long = 0L

    init {
        viewModelScope.launch {
            timerManager.remainingSeconds.collect { sec ->
                _uiState.update {
                    it.copy(
                        remainingSeconds = sec,
                        isWarningZone = timerManager.isWarningZone
                    )
                }
            }
        }
    }

    fun startExam(config: ExamConfig = ExamConfig.DEFAULT_KLASSE_E) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isFinished = false) }
            try {
                activeConfig = config
                currentSessionId = UUID.randomUUID().toString()
                examStartTime = System.currentTimeMillis()

                val allQuestions = questionRepository.getAllQuestions()
                val examQuestions = ExamGenerator.generateExamQuestions(allQuestions, config)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = examQuestions,
                        currentIndex = 0,
                        showSubmitDialog = false,
                        showCancelDialog = false,
                        isFinished = false,
                        examResult = null
                    )
                }

                timerManager.start(config.durationMinutes * 60L) {
                    submitExam()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Fehler beim Starten der Prüfung: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        val curIndex = currentState.currentIndex
        val curQuestionState = currentState.questions.getOrNull(curIndex) ?: return

        val updatedQuestions = currentState.questions.toMutableList()
        updatedQuestions[curIndex] = curQuestionState.copy(selectedAnswerIndex = answerIndex)

        _uiState.update { it.copy(questions = updatedQuestions) }
    }

    fun toggleMarkForReview() {
        val currentState = _uiState.value
        val curIndex = currentState.currentIndex
        val curQuestionState = currentState.questions.getOrNull(curIndex) ?: return

        val updatedQuestions = currentState.questions.toMutableList()
        updatedQuestions[curIndex] = curQuestionState.copy(
            isMarkedForReview = !curQuestionState.isMarkedForReview
        )

        _uiState.update { it.copy(questions = updatedQuestions) }
    }

    fun goToQuestion(index: Int) {
        if (index in 0 until _uiState.value.questions.size) {
            _uiState.update { it.copy(currentIndex = index) }
        }
    }

    fun nextQuestion() {
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex < _uiState.value.questions.size) {
            _uiState.update { it.copy(currentIndex = nextIndex) }
        }
    }

    fun previousQuestion() {
        val prevIndex = _uiState.value.currentIndex - 1
        if (prevIndex >= 0) {
            _uiState.update { it.copy(currentIndex = prevIndex) }
        }
    }

    fun setShowSubmitDialog(show: Boolean) {
        _uiState.update { it.copy(showSubmitDialog = show) }
    }

    fun setShowCancelDialog(show: Boolean) {
        _uiState.update { it.copy(showCancelDialog = show) }
    }

    fun submitExam() {
        timerManager.stop()
        val currentState = _uiState.value
        val durationSeconds = ((System.currentTimeMillis() - examStartTime) / 1000L).coerceAtLeast(1L)

        val result = ExamEvaluationEngine.evaluateExam(
            sessionId = currentSessionId,
            questionStates = currentState.questions,
            durationSeconds = durationSeconds,
            config = activeConfig
        )

        viewModelScope.launch {
            try {
                examRepository.recordExamResult(result)
            } catch (_: Exception) {
                // Ignore DB logging failure in testing/mock environments
            }

            _uiState.update {
                it.copy(
                    isFinished = true,
                    examResult = result,
                    showSubmitDialog = false
                )
            }
        }
    }

    fun cancelExam() {
        timerManager.stop()
        _uiState.update {
            it.copy(
                showCancelDialog = false,
                isFinished = false,
                questions = emptyList()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.stop()
    }
}
