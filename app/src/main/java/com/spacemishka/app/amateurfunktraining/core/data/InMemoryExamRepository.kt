package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.exam.ExamReadinessCalculator
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryEntry
import com.spacemishka.app.amateurfunktraining.core.model.ExamReadiness
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryExamRepository : ExamRepository {

    private val historyList = mutableListOf<ExamHistoryEntry>()
    private val _historyFlow = MutableStateFlow<List<ExamHistoryEntry>>(emptyList())
    private val sessionWrongQuestions = mutableMapOf<String, List<String>>()
    private val wrongQuestionsChronological = mutableListOf<String>()

    override fun getExamHistoryStream(): Flow<List<ExamHistoryEntry>> = _historyFlow.asStateFlow()

    override suspend fun recordExamResult(result: ExamResult) {
        val technikPart = result.partResults.find { it.category == Category.TECHNIK }
        val betriebPart = result.partResults.find { it.category == Category.BETRIEB }
        val vorschriftenPart = result.partResults.find { it.category == Category.VORSCHRIFTEN }

        val entry = ExamHistoryEntry(
            sessionId = result.sessionId,
            timestamp = result.timestamp,
            durationSeconds = result.durationSeconds,
            technikRichtig = technikPart?.correctAnswers ?: 0,
            technikGesamt = technikPart?.totalQuestions ?: 0,
            betriebRichtig = betriebPart?.correctAnswers ?: 0,
            betriebGesamt = betriebPart?.totalQuestions ?: 0,
            vorschriftenRichtig = vorschriftenPart?.correctAnswers ?: 0,
            vorschriftenGesamt = vorschriftenPart?.totalQuestions ?: 0,
            isOverallPassed = result.isOverallPassed
        )

        historyList.add(0, entry)
        _historyFlow.value = historyList.toList()

        val wrongIds = result.wrongQuestions.map { it.id }
        sessionWrongQuestions[result.sessionId] = wrongIds
        for (wid in wrongIds) {
            if (!wrongQuestionsChronological.contains(wid)) {
                wrongQuestionsChronological.add(0, wid)
            }
        }
    }

    override suspend fun getRecentExamResults(limit: Int): List<ExamHistoryEntry> {
        return historyList.take(limit)
    }

    override suspend fun getLatestWrongQuestionIds(limit: Int): List<String> {
        return wrongQuestionsChronological.take(limit)
    }

    override suspend fun getWrongQuestionIdsForSession(sessionId: String): List<String> {
        return sessionWrongQuestions[sessionId] ?: emptyList()
    }

    override suspend fun getReadiness(): ExamReadiness {
        return ExamReadinessCalculator.calculateReadiness(historyList)
    }

    override suspend fun getAllExamHistory(): List<ExamHistoryEntry> {
        return historyList.toList()
    }

    override suspend fun importExamHistory(
        entries: List<com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryBackupDto>,
        mode: com.spacemishka.app.amateurfunktraining.core.model.ImportMode
    ) {
        val converted = entries.map { dto ->
            ExamHistoryEntry(
                sessionId = dto.sessionId,
                timestamp = dto.timestamp,
                durationSeconds = dto.durationSeconds,
                technikRichtig = dto.technikRichtig,
                technikGesamt = dto.technikGesamt,
                betriebRichtig = dto.betriebRichtig,
                betriebGesamt = dto.betriebGesamt,
                vorschriftenRichtig = dto.vorschriftenRichtig,
                vorschriftenGesamt = dto.vorschriftenGesamt,
                isOverallPassed = dto.gesamtBestanden
            )
        }

        if (mode == com.spacemishka.app.amateurfunktraining.core.model.ImportMode.OVERWRITE) {
            historyList.clear()
            historyList.addAll(converted.sortedByDescending { it.timestamp })
            sessionWrongQuestions.clear()
            wrongQuestionsChronological.clear()
        } else {
            val existingIds = historyList.map { it.sessionId }.toSet()
            for (entry in converted) {
                if (!existingIds.contains(entry.sessionId)) {
                    historyList.add(entry)
                }
            }
            historyList.sortByDescending { it.timestamp }
        }
        _historyFlow.value = historyList.toList()
    }

    override suspend fun clearExamHistory() {
        historyList.clear()
        sessionWrongQuestions.clear()
        wrongQuestionsChronological.clear()
        _historyFlow.value = emptyList()
    }
}

