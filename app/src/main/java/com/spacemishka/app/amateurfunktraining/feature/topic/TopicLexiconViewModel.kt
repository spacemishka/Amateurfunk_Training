package com.spacemishka.app.amateurfunktraining.feature.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.data.QuestionRepository
import com.spacemishka.app.amateurfunktraining.core.data.TopicRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TopicItem(
    val topic: Topic,
    val questionCount: Int
)

data class TopicLexiconUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: Category = Category.ALL,
    val topics: List<TopicItem> = emptyList(),
    val recentTopics: List<Topic> = emptyList(),
    val totalTopicsCount: Int = 0
)

class TopicLexiconViewModel(
    private val topicRepository: TopicRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicLexiconUiState())
    val uiState: StateFlow<TopicLexiconUiState> = _uiState.asStateFlow()

    private var allTopicItems: List<TopicItem> = emptyList()

    init {
        loadData()
        topicRepository.getRecentlyViewedTopicsStream()
            .onEach { recentList ->
                _uiState.update { it.copy(recentTopics = recentList) }
            }
            .launchIn(viewModelScope)
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val topics = topicRepository.getAllTopics()
                val questions = questionRepository.getAllQuestions()

                val questionCountMap = questions.groupingBy { it.topicId.lowercase() }.eachCount()

                allTopicItems = topics.map { topic ->
                    TopicItem(
                        topic = topic,
                        questionCount = questionCountMap[topic.id.lowercase()] ?: 0
                    )
                }

                recomputeFilteredTopics()
            } finally {
                _uiState.update { it.copy(isLoading = false, totalTopicsCount = allTopicItems.size) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        recomputeFilteredTopics()
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
        recomputeFilteredTopics()
    }

    private fun recomputeFilteredTopics() {
        val currentState = _uiState.value
        val query = currentState.searchQuery.trim().lowercase()
        val category = currentState.selectedCategory

        val filtered = allTopicItems.filter { item ->
            val matchesCat = (category == Category.ALL || item.topic.category == category)
            val matchesQuery = if (query.isEmpty()) true else {
                item.topic.title.lowercase().contains(query) ||
                        item.topic.keyTakeaway.lowercase().contains(query) ||
                        item.topic.explanation.lowercase().contains(query) ||
                        (item.topic.mnemonic?.lowercase()?.contains(query) == true) ||
                        (item.topic.formula?.lowercase()?.contains(query) == true)
            }
            matchesCat && matchesQuery
        }

        _uiState.update { it.copy(topics = filtered) }
    }

    fun markTopicViewed(topicId: String) {
        viewModelScope.launch {
            topicRepository.markTopicAsViewed(topicId)
        }
    }
}
