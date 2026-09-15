package com.spacemishka.app.amateurfunktraining.core.model

data class ExamHistoryEntry(
    val sessionId: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val technikRichtig: Int,
    val technikGesamt: Int,
    val betriebRichtig: Int,
    val betriebGesamt: Int,
    val vorschriftenRichtig: Int,
    val vorschriftenGesamt: Int,
    val isOverallPassed: Boolean
) {
    val totalCorrect: Int
        get() = technikRichtig + betriebRichtig + vorschriftenRichtig

    val totalQuestions: Int
        get() = technikGesamt + betriebGesamt + vorschriftenGesamt

    val overallPercentage: Float
        get() = if (totalQuestions > 0) totalCorrect.toFloat() / totalQuestions else 0f

    val technikPercentage: Float
        get() = if (technikGesamt > 0) technikRichtig.toFloat() / technikGesamt else 0f

    val betriebPercentage: Float
        get() = if (betriebGesamt > 0) betriebRichtig.toFloat() / betriebGesamt else 0f

    val vorschriftenPercentage: Float
        get() = if (vorschriftenGesamt > 0) vorschriftenRichtig.toFloat() / vorschriftenGesamt else 0f

    val technikPassed: Boolean
        get() = technikPercentage >= 0.75f

    val betriebPassed: Boolean
        get() = betriebPercentage >= 0.75f

    val vorschriftenPassed: Boolean
        get() = vorschriftenPercentage >= 0.75f
}
