package com.spacemishka.app.amateurfunktraining.feature.practice

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.core.model.Topic

data class PracticeUiState(
    val isLoading: Boolean = true,
    val practiceMode: PracticeMode = PracticeMode.CategoryMode(Category.ALL),
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val isAnswerConfirmed: Boolean = false,
    val isCorrect: Boolean? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isFinished: Boolean = false,
    val errorMessage: String? = null,
    val isBookmarked: Boolean = false,
    val currentLeitnerBox: Int = 1,
    val currentErrorCount: Int = 0,
    val currentTopic: Topic? = null
) {
    val category: Category
        get() = (practiceMode as? PracticeMode.CategoryMode)?.category ?: Category.ALL

    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)

    val totalCount: Int
        get() = questions.size

    val progressFraction: Float
        get() = if (totalCount > 0) (currentIndex + 1).toFloat() / totalCount.toFloat() else 0f
}
