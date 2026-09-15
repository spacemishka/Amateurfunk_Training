package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryEntry
import com.spacemishka.app.amateurfunktraining.core.model.ExamReadiness
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    fun getExamHistoryStream(): Flow<List<ExamHistoryEntry>>
    suspend fun recordExamResult(result: ExamResult)
    suspend fun getRecentExamResults(limit: Int = 20): List<ExamHistoryEntry>
    suspend fun getLatestWrongQuestionIds(limit: Int = 100): List<String>
    suspend fun getWrongQuestionIdsForSession(sessionId: String): List<String>
    suspend fun getReadiness(): ExamReadiness

    // MVP 6: Import / Export & Data Portability
    suspend fun getAllExamHistory(): List<ExamHistoryEntry>
    suspend fun importExamHistory(entries: List<ExamHistoryBackupDto>, mode: ImportMode)
    suspend fun clearExamHistory()
}

