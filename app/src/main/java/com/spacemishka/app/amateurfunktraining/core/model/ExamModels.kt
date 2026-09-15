package com.spacemishka.app.amateurfunktraining.core.model

enum class ExamStatus {
    PASSED,
    FAILED
}

data class ExamPartConfig(
    val category: Category,
    val questionCount: Int,
    val passThresholdPercentage: Float = 0.75f
) {
    val minimumCorrectToPass: Int
        get() = kotlin.math.ceil(questionCount * passThresholdPercentage).toInt()
}

data class ExamConfig(
    val parts: List<ExamPartConfig>,
    val durationMinutes: Int
) {
    val totalQuestions: Int
        get() = parts.sumOf { it.questionCount }

    companion object {
        val DEFAULT_KLASSE_E = ExamConfig(
            parts = listOf(
                ExamPartConfig(
                    category = Category.TECHNIK,
                    questionCount = 34,
                    passThresholdPercentage = 0.75f
                ),
                ExamPartConfig(
                    category = Category.BETRIEB,
                    questionCount = 25,
                    passThresholdPercentage = 0.75f
                ),
                ExamPartConfig(
                    category = Category.VORSCHRIFTEN,
                    questionCount = 25,
                    passThresholdPercentage = 0.75f
                )
            ),
            durationMinutes = 150
        )
    }
}

data class ExamQuestionState(
    val question: Question,
    val selectedAnswerIndex: Int? = null,
    val isMarkedForReview: Boolean = false
) {
    val isAnswered: Boolean
        get() = selectedAnswerIndex != null
}

data class ExamResultPart(
    val category: Category,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Float,
    val isPassed: Boolean
)

data class ExamResult(
    val sessionId: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val partResults: List<ExamResultPart>,
    val isOverallPassed: Boolean,
    val wrongQuestions: List<Question>,
    val selectedAnswers: Map<String, Int> = emptyMap()
) {
    val totalQuestions: Int
        get() = partResults.sumOf { it.totalQuestions }

    val totalCorrectAnswers: Int
        get() = partResults.sumOf { it.correctAnswers }

    val overallPercentage: Float
        get() = if (totalQuestions > 0) totalCorrectAnswers.toFloat() / totalQuestions else 0f
}

enum class ReadinessLevel {
    NOT_READY,
    PARTIALLY_READY,
    READY
}

data class ExamReadiness(
    val level: ReadinessLevel,
    val averageScorePercentage: Float,
    val totalSimulations: Int,
    val passedSimulations: Int,
    val description: String
)
