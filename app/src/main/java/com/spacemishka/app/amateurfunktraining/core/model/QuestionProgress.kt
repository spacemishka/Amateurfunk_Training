package com.spacemishka.app.amateurfunktraining.core.model

enum class ProgressStatus {
    NEU,
    IN_BEARBEITUNG,
    GEMEISTERT
}

data class QuestionProgress(
    val questionId: String,
    val status: ProgressStatus = ProgressStatus.NEU,
    val errorCount: Int = 0,
    val lastAnsweredTimestamp: Long = 0L,
    val leitnerBox: Int = 1,
    val isBookmarked: Boolean = false
) {
    val isMastered: Boolean get() = leitnerBox >= 5 || status == ProgressStatus.GEMEISTERT
    val isProblemQuestion: Boolean get() = errorCount >= 2
}

data class CategoryProgress(
    val category: Category,
    val totalCount: Int,
    val newCount: Int,
    val inProgressCount: Int,
    val masteredCount: Int
) {
    val masteredPercentage: Float
        get() = if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f

    val learnedPercentage: Float
        get() = if (totalCount > 0) (inProgressCount + masteredCount).toFloat() / totalCount else 0f
}
