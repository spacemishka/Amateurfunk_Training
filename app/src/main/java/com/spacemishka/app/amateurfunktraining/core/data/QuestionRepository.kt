package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question

interface QuestionRepository {
    suspend fun getAllQuestions(): List<Question>
    suspend fun getQuestionsByCategory(category: Category): List<Question>
    suspend fun getQuestionById(id: String): Question?
    suspend fun getQuestionsByTopic(topicId: String): List<Question> {
        val target = topicId.lowercase()
        return getAllQuestions().filter { it.topicId.equals(target, ignoreCase = true) }
    }
    suspend fun getCategoryCounts(): Map<Category, Int>
}
