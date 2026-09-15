package com.spacemishka.app.amateurfunktraining.feature.exam

import com.spacemishka.app.amateurfunktraining.core.model.ExamQuestionState
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.Question

data class ExamUiState(
    val isLoading: Boolean = false,
    val questions: List<ExamQuestionState> = emptyList(),
    val currentIndex: Int = 0,
    val remainingSeconds: Long = 0L,
    val isWarningZone: Boolean = false,
    val showSubmitDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val isFinished: Boolean = false,
    val examResult: ExamResult? = null,
    val errorMessage: String? = null
) {
    val currentQuestionState: ExamQuestionState?
        get() = questions.getOrNull(currentIndex)

    val currentQuestion: Question?
        get() = currentQuestionState?.question

    val totalCount: Int
        get() = questions.size

    val answeredCount: Int
        get() = questions.count { it.isAnswered }

    val unansweredCount: Int
        get() = questions.size - answeredCount

    val markedForReviewCount: Int
        get() = questions.count { it.isMarkedForReview }
}
