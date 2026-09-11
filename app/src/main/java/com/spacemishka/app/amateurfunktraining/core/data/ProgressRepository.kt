package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getProgressStream(questionId: String): Flow<QuestionProgress?>
    fun getAllProgressStream(): Flow<List<QuestionProgress>>
    suspend fun getProgressForQuestion(questionId: String): QuestionProgress?
    suspend fun recordAnswer(questionId: String, isCorrect: Boolean, timestamp: Long = System.currentTimeMillis())
    suspend fun toggleBookmark(questionId: String): Boolean
    suspend fun getProblemQuestionIds(): List<String>
    suspend fun getBookmarkedQuestionIds(): List<String>
    suspend fun getDueLeitnerQuestionIds(currentTimestamp: Long = System.currentTimeMillis()): List<String>
    fun getStreakStream(): Flow<Int>
}
