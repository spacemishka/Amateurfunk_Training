package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.leitner.LeitnerCalculator
import com.spacemishka.app.amateurfunktraining.core.leitner.StreakManager
import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryProgressRepository : ProgressRepository {

    private val progressMap = MutableStateFlow<Map<String, QuestionProgress>>(emptyMap())
    private val streakFlow = MutableStateFlow(0)
    private var lastPracticeDateStr: String? = null
    private var storedStreak: Int = 0

    override fun getProgressStream(questionId: String): Flow<QuestionProgress?> {
        return progressMap.map { it[questionId] }
    }

    override fun getAllProgressStream(): Flow<List<QuestionProgress>> {
        return progressMap.map { it.values.toList() }
    }

    override suspend fun getProgressForQuestion(questionId: String): QuestionProgress? {
        return progressMap.value[questionId]
    }

    override suspend fun recordAnswer(
        questionId: String,
        isCorrect: Boolean,
        timestamp: Long
    ) {
        val current = progressMap.value[questionId]
        val updated = LeitnerCalculator.calculateNextState(
            questionId = questionId,
            current = current,
            isCorrect = isCorrect,
            timestamp = timestamp
        )

        progressMap.update { it + (questionId to updated) }

        val streakResult = StreakManager.onPracticeCompleted(
            lastPracticeDateString = lastPracticeDateStr,
            currentStreak = storedStreak
        )
        lastPracticeDateStr = streakResult.newDateString
        storedStreak = streakResult.newStreak
        streakFlow.value = streakResult.newStreak
    }

    override suspend fun toggleBookmark(questionId: String): Boolean {
        val current = progressMap.value[questionId]
        val newBookmark = !(current?.isBookmarked ?: false)
        val updated = current?.copy(isBookmarked = newBookmark)
            ?: QuestionProgress(
                questionId = questionId,
                status = ProgressStatus.NEU,
                isBookmarked = newBookmark
            )

        progressMap.update { it + (questionId to updated) }
        return newBookmark
    }

    override suspend fun getProblemQuestionIds(): List<String> {
        return progressMap.value.values
            .filter { it.isProblemQuestion }
            .sortedByDescending { it.errorCount }
            .map { it.questionId }
    }

    override suspend fun getBookmarkedQuestionIds(): List<String> {
        return progressMap.value.values
            .filter { it.isBookmarked }
            .map { it.questionId }
    }

    override suspend fun getDueLeitnerQuestionIds(currentTimestamp: Long): List<String> {
        return progressMap.value.values
            .filter { LeitnerCalculator.isDue(it, currentTimestamp) }
            .map { it.questionId }
    }

    override fun getStreakStream(): Flow<Int> {
        return streakFlow.asStateFlow()
    }

    override suspend fun getAllProgress(): List<QuestionProgress> {
        return progressMap.value.values.toList()
    }

    override suspend fun importProgress(items: List<com.spacemishka.app.amateurfunktraining.core.model.QuestionProgressBackupDto>, mode: com.spacemishka.app.amateurfunktraining.core.model.ImportMode) {
        if (mode == com.spacemishka.app.amateurfunktraining.core.model.ImportMode.OVERWRITE) {
            val newMap = items.associate { dto ->
                dto.frageId to QuestionProgress(
                    questionId = dto.frageId,
                    status = try { ProgressStatus.valueOf(dto.status) } catch (_: Exception) { ProgressStatus.NEU },
                    errorCount = dto.fehlerzaehler,
                    lastAnsweredTimestamp = dto.letzteAntwort,
                    leitnerBox = dto.leitnerBox,
                    isBookmarked = dto.istLesezeichen
                )
            }
            progressMap.value = newMap
        } else {
            val current = progressMap.value.toMutableMap()
            for (dto in items) {
                val existing = current[dto.frageId]
                if (existing == null) {
                    current[dto.frageId] = QuestionProgress(
                        questionId = dto.frageId,
                        status = try { ProgressStatus.valueOf(dto.status) } catch (_: Exception) { ProgressStatus.NEU },
                        errorCount = dto.fehlerzaehler,
                        lastAnsweredTimestamp = dto.letzteAntwort,
                        leitnerBox = dto.leitnerBox,
                        isBookmarked = dto.istLesezeichen
                    )
                } else {
                    val mergedBox = maxOf(existing.leitnerBox, dto.leitnerBox)
                    val mergedErrors = maxOf(existing.errorCount, dto.fehlerzaehler)
                    val mergedTime = maxOf(existing.lastAnsweredTimestamp, dto.letzteAntwort)
                    val mergedBookmark = existing.isBookmarked || dto.istLesezeichen
                    val mergedStatus = if (mergedBox >= 5) {
                        ProgressStatus.GEMEISTERT
                    } else if (mergedBox > 1 || mergedErrors > 0 || mergedTime > 0) {
                        ProgressStatus.IN_BEARBEITUNG
                    } else {
                        ProgressStatus.NEU
                    }
                    current[dto.frageId] = QuestionProgress(
                        questionId = dto.frageId,
                        status = mergedStatus,
                        errorCount = mergedErrors,
                        lastAnsweredTimestamp = mergedTime,
                        leitnerBox = mergedBox,
                        isBookmarked = mergedBookmark
                    )
                }
            }
            progressMap.value = current
        }
    }

    override suspend fun getStreakData(): Pair<String?, Int> {
        return Pair(lastPracticeDateStr, storedStreak)
    }

    override suspend fun setStreakData(lastDate: String?, streak: Int, mode: com.spacemishka.app.amateurfunktraining.core.model.ImportMode) {
        if (mode == com.spacemishka.app.amateurfunktraining.core.model.ImportMode.OVERWRITE) {
            lastPracticeDateStr = lastDate
            storedStreak = streak
        } else {
            if (streak > storedStreak || (lastPracticeDateStr == null && lastDate != null)) {
                lastPracticeDateStr = lastDate
                storedStreak = maxOf(storedStreak, streak)
            }
        }
        streakFlow.value = StreakManager.computeDisplayStreak(lastPracticeDateStr, storedStreak)
    }

    override suspend fun clearAllProgress() {
        progressMap.value = emptyMap()
        lastPracticeDateStr = null
        storedStreak = 0
        streakFlow.value = 0
    }
}

