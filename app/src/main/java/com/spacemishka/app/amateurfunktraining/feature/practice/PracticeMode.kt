package com.spacemishka.app.amateurfunktraining.feature.practice

import com.spacemishka.app.amateurfunktraining.core.model.Category

sealed interface PracticeMode {
    val title: String

    data class CategoryMode(val category: Category) : PracticeMode {
        override val title: String = category.displayName
    }

    data object DueLeitner : PracticeMode {
        override val title: String = "Fällige Wiederholungen"
    }

    data object Bookmarks : PracticeMode {
        override val title: String = "Lesezeichen"
    }

    data object ProblemQuestions : PracticeMode {
        override val title: String = "Problemfragen"
    }

    data class ExamMistakes(
        val questionIds: List<String>,
        override val title: String = "Fehlertraining (Prüfung)"
    ) : PracticeMode

    data class TopicQuestions(
        val topicId: String,
        val topicTitle: String,
        override val title: String = "Thema: $topicTitle"
    ) : PracticeMode
}
