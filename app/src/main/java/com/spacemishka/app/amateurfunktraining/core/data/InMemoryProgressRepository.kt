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
}
