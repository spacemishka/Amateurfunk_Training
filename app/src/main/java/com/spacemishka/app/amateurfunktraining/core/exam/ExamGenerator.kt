package com.spacemishka.app.amateurfunktraining.core.exam

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamConfig
import com.spacemishka.app.amateurfunktraining.core.model.ExamQuestionState
import com.spacemishka.app.amateurfunktraining.core.model.Question
import kotlin.random.Random

object ExamGenerator {

    fun generateExamQuestions(
        allQuestions: List<Question>,
        config: ExamConfig = ExamConfig.DEFAULT_KLASSE_E,
        random: Random = Random.Default
    ): List<ExamQuestionState> {
        val selectedQuestions = mutableListOf<Question>()

        for (partConfig in config.parts) {
            val categoryPool = allQuestions
                .filter { it.category == partConfig.category }
                .distinctBy { it.id }

            val shuffledPool = categoryPool.shuffled(random)
            val takeCount = partConfig.questionCount.coerceAtMost(shuffledPool.size)
            selectedQuestions.addAll(shuffledPool.take(takeCount))
        }

        return selectedQuestions.map { ExamQuestionState(question = it) }
    }
}
