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

    suspend fun searchQuestions(query: String, category: Category? = null): List<Question> {
        val trimmed = query.trim()
        val base = if (category != null && category != Category.ALL) {
            getQuestionsByCategory(category)
        } else {
            getAllQuestions()
        }
        if (trimmed.isEmpty()) return base

        val lowerQuery = trimmed.lowercase()
        val tokens = lowerQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        val exactId = mutableListOf<Question>()
        val prefixId = mutableListOf<Question>()
        val containsId = mutableListOf<Question>()
        val textMatches = mutableListOf<Question>()
        val otherMatches = mutableListOf<Question>()

        fun matchesToken(target: String, token: String): Boolean {
            return if (token.length <= 2) {
                Regex("\\b${Regex.escape(token)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(target)
            } else {
                target.lowercase().contains(token)
            }
        }

        for (q in base) {
            val qIdLower = q.id.lowercase()

            if (qIdLower == lowerQuery) {
                exactId.add(q)
            } else if (qIdLower.startsWith(lowerQuery)) {
                prefixId.add(q)
            } else if (qIdLower.contains(lowerQuery)) {
                containsId.add(q)
            } else if (tokens.all { matchesToken(q.text, it) }) {
                textMatches.add(q)
            } else if (tokens.all { token ->
                matchesToken(q.text, token) ||
                q.answers.any { matchesToken(it, token) } ||
                matchesToken(q.explanation, token)
            }) {
                otherMatches.add(q)
            }
        }

        return exactId + prefixId + containsId + textMatches + otherMatches
    }
}
