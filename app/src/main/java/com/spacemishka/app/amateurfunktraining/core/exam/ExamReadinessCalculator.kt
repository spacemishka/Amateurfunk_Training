package com.spacemishka.app.amateurfunktraining.core.exam

import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryEntry
import com.spacemishka.app.amateurfunktraining.core.model.ExamReadiness
import com.spacemishka.app.amateurfunktraining.core.model.ReadinessLevel

object ExamReadinessCalculator {

    fun calculateReadiness(history: List<ExamHistoryEntry>): ExamReadiness {
        if (history.isEmpty()) {
            return ExamReadiness(
                level = ReadinessLevel.NOT_READY,
                averageScorePercentage = 0f,
                totalSimulations = 0,
                passedSimulations = 0,
                description = "Noch keine Prüfungssimulation absolviert"
            )
        }

        val recentExams = history.take(3)
        val passedCount = recentExams.count { it.isOverallPassed }
        val avgScore = recentExams.map { it.overallPercentage }.average().toFloat()

        return when {
            recentExams.size >= 3 && passedCount == 3 && avgScore >= 0.80f -> {
                ExamReadiness(
                    level = ReadinessLevel.READY,
                    averageScorePercentage = avgScore,
                    totalSimulations = history.size,
                    passedSimulations = history.count { it.isOverallPassed },
                    description = "Hervorragend vorbereitet (Prüfungsbereit)"
                )
            }

            recentExams.size >= 3 && passedCount == 3 -> {
                ExamReadiness(
                    level = ReadinessLevel.PARTIALLY_READY,
                    averageScorePercentage = avgScore,
                    totalSimulations = history.size,
                    passedSimulations = history.count { it.isOverallPassed },
                    description = "Solide Vorbereitung (Bestehensgrenze erreicht)"
                )
            }

            recentExams.size < 3 && passedCount > 0 -> {
                ExamReadiness(
                    level = ReadinessLevel.PARTIALLY_READY,
                    averageScorePercentage = avgScore,
                    totalSimulations = history.size,
                    passedSimulations = history.count { it.isOverallPassed },
                    description = "${recentExams.size} von 3 empfohlenen Probeprüfungen absolviert"
                )
            }

            else -> {
                ExamReadiness(
                    level = ReadinessLevel.NOT_READY,
                    averageScorePercentage = avgScore,
                    totalSimulations = history.size,
                    passedSimulations = history.count { it.isOverallPassed },
                    description = "Prüfungssimulation nicht bestanden – Fehlertraining empfohlen"
                )
            }
        }
    }
}
