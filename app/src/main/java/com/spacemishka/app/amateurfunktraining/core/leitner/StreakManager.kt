package com.spacemishka.app.amateurfunktraining.core.leitner

import java.time.LocalDate
import java.time.format.DateTimeParseException

object StreakManager {

    data class StreakUpdateResult(
        val newStreak: Int,
        val newDateString: String,
        val hasChanged: Boolean
    )

    fun onPracticeCompleted(
        lastPracticeDateString: String?,
        currentStreak: Int,
        nowDate: LocalDate = LocalDate.now()
    ): StreakUpdateResult {
        val todayStr = nowDate.toString()

        if (lastPracticeDateString.isNullOrBlank()) {
            return StreakUpdateResult(newStreak = 1, newDateString = todayStr, hasChanged = true)
        }

        val lastDate = try {
            LocalDate.parse(lastPracticeDateString)
        } catch (_: DateTimeParseException) {
            return StreakUpdateResult(newStreak = 1, newDateString = todayStr, hasChanged = true)
        }

        return when {
            lastDate == nowDate -> {
                StreakUpdateResult(
                    newStreak = currentStreak.coerceAtLeast(1),
                    newDateString = todayStr,
                    hasChanged = false
                )
            }
            lastDate == nowDate.minusDays(1) -> {
                StreakUpdateResult(
                    newStreak = currentStreak + 1,
                    newDateString = todayStr,
                    hasChanged = true
                )
            }
            else -> {
                // Streak broken (missed one or more days)
                StreakUpdateResult(
                    newStreak = 1,
                    newDateString = todayStr,
                    hasChanged = true
                )
            }
        }
    }

    fun computeDisplayStreak(
        lastPracticeDateString: String?,
        storedStreak: Int,
        nowDate: LocalDate = LocalDate.now()
    ): Int {
        if (lastPracticeDateString.isNullOrBlank() || storedStreak <= 0) return 0
        val lastDate = try {
            LocalDate.parse(lastPracticeDateString)
        } catch (_: DateTimeParseException) {
            return 0
        }

        return when {
            lastDate == nowDate -> storedStreak
            lastDate == nowDate.minusDays(1) -> storedStreak // Streak still active today, awaiting practice
            else -> 0 // Expired
        }
    }
}
