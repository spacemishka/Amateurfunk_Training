package com.spacemishka.app.amateurfunktraining.core.exam

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamConfig
import com.spacemishka.app.amateurfunktraining.core.model.ExamQuestionState
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.ExamResultPart
import com.spacemishka.app.amateurfunktraining.core.model.Question

object ExamEvaluationEngine {

    fun evaluateExam(
        sessionId: String,
        questionStates: List<ExamQuestionState>,
        durationSeconds: Long,
        config: ExamConfig = ExamConfig.DEFAULT_KLASSE_E,
        timestamp: Long = System.currentTimeMillis()
    ): ExamResult {
        val partResults = mutableListOf<ExamResultPart>()
        val wrongQuestions = mutableListOf<Question>()
        val selectedAnswers = mutableMapOf<String, Int>()

        for (state in questionStates) {
            state.selectedAnswerIndex?.let { selectedIndex ->
                selectedAnswers[state.question.id] = selectedIndex
            }
            if (state.selectedAnswerIndex != state.question.correctAnswerIndex) {
                wrongQuestions.add(state.question)
            }
        }

        for (partConfig in config.parts) {
            val partQuestions = questionStates.filter { it.question.category == partConfig.category }
            val totalInPart = partQuestions.size
            val correctInPart = partQuestions.count { it.selectedAnswerIndex == it.question.correctAnswerIndex }
            val percentage = if (totalInPart > 0) correctInPart.toFloat() / totalInPart else 0f
            val isPassed = percentage >= partConfig.passThresholdPercentage

            partResults.add(
                ExamResultPart(
                    category = partConfig.category,
                    totalQuestions = totalInPart,
                    correctAnswers = correctInPart,
                    percentage = percentage,
                    isPassed = isPassed
                )
            )
        }

        // BNetzA Rule: All parts must have >= 75% for the overall exam to be passed!
        val isOverallPassed = partResults.isNotEmpty() && partResults.all { it.isPassed }

        return ExamResult(
            sessionId = sessionId,
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            partResults = partResults,
            isOverallPassed = isOverallPassed,
            wrongQuestions = wrongQuestions,
            selectedAnswers = selectedAnswers
        )
    }
}
