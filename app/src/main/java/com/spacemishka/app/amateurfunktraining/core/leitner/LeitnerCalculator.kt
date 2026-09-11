package com.spacemishka.app.amateurfunktraining.core.leitner

import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress

object LeitnerCalculator {

    const val ONE_DAY_MS = 86_400_000L
    const val THREE_DAYS_MS = 3 * ONE_DAY_MS
    const val SEVEN_DAYS_MS = 7 * ONE_DAY_MS
    const val FOURTEEN_DAYS_MS = 14 * ONE_DAY_MS

    fun getIntervalForBox(box: Int): Long {
        return when (box) {
            1 -> ONE_DAY_MS
            2 -> THREE_DAYS_MS
            3 -> SEVEN_DAYS_MS
            4 -> FOURTEEN_DAYS_MS
            else -> Long.MAX_VALUE
        }
    }

    fun calculateNextState(
        questionId: String,
        current: QuestionProgress?,
        isCorrect: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): QuestionProgress {
        val currentBox = current?.leitnerBox ?: 1
        val currentErrorCount = current?.errorCount ?: 0
        val isBookmarked = current?.isBookmarked ?: false

        return if (isCorrect) {
            val nextBox = (currentBox + 1).coerceAtMost(5)
            val nextStatus = if (nextBox >= 5) ProgressStatus.GEMEISTERT else ProgressStatus.IN_BEARBEITUNG
            QuestionProgress(
                questionId = questionId,
                status = nextStatus,
                errorCount = currentErrorCount,
                lastAnsweredTimestamp = timestamp,
                leitnerBox = nextBox,
                isBookmarked = isBookmarked
            )
        } else {
            QuestionProgress(
                questionId = questionId,
                status = ProgressStatus.IN_BEARBEITUNG,
                errorCount = currentErrorCount + 1,
                lastAnsweredTimestamp = timestamp,
                leitnerBox = 1,
                isBookmarked = isBookmarked
            )
        }
    }

    fun isDue(progress: QuestionProgress, currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        if (progress.leitnerBox >= 5) return false
        if (progress.lastAnsweredTimestamp == 0L) return true
        val interval = getIntervalForBox(progress.leitnerBox)
        return (currentTimestamp - progress.lastAnsweredTimestamp) >= interval
    }
}
