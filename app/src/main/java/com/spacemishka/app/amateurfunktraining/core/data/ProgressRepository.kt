package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgressBackupDto
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

    // MVP 6: Import / Export & Data Portability
    suspend fun getAllProgress(): List<QuestionProgress>
    suspend fun importProgress(items: List<QuestionProgressBackupDto>, mode: ImportMode)
    suspend fun getStreakData(): Pair<String?, Int>
    suspend fun setStreakData(lastDate: String?, streak: Int, mode: ImportMode)
    suspend fun clearAllProgress()
}

