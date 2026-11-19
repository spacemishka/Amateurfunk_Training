package com.spacemishka.app.amateurfunktraining.feature.search

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress

data class QuestionSearchResultItem(
    val question: Question,
    val progress: QuestionProgress?,
    val isExpanded: Boolean = false
)

data class QuestionSearchUiState(
    val query: String = "",
    val selectedCategory: Category? = null,
    val onlyBookmarked: Boolean = false,
    val onlyMistakes: Boolean = false,
    val onlyWithImage: Boolean = false,
    val selectedLeitnerBox: Int? = null,
    val results: List<QuestionSearchResultItem> = emptyList(),
    val expandedQuestionIds: Set<String> = emptySet(),
    val totalFoundCount: Int = 0,
    val categoryCounts: Map<Category?, Int> = emptyMap(),
    val isLoading: Boolean = false
)
