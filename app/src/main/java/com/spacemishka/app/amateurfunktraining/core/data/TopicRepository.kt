package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    suspend fun getAllTopics(): List<Topic>
    suspend fun getTopicById(id: String): Topic?
    suspend fun searchTopics(query: String, category: Category? = null): List<Topic>
    fun getRecentlyViewedTopicsStream(): Flow<List<Topic>>
    suspend fun markTopicAsViewed(topicId: String)

    // MVP 6: Import / Export & Data Portability
    suspend fun getAllReadTopicIds(): List<String>
    suspend fun importReadTopicIds(topicIds: List<String>, mode: ImportMode)
    suspend fun clearTopicHistory()
}

