package com.spacemishka.app.amateurfunktraining.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicDto(
    @SerialName("topic_id")
    val topicId: String,
    @SerialName("kategorie")
    val kategorie: String,
    @SerialName("titel")
    val titel: String,
    @SerialName("kernaussage")
    val kernaussage: String,
    @SerialName("erklaerung")
    val erklaerung: String,
    @SerialName("merksatz")
    val merksatz: String? = null,
    @SerialName("formel")
    val formel: String? = null,
    @SerialName("verwandte_themen")
    val verwandteThemen: List<String> = emptyList()
) {
    fun toDomain(): Topic {
        return Topic(
            id = topicId,
            category = Category.fromKey(kategorie),
            title = titel,
            keyTakeaway = kernaussage,
            explanation = erklaerung,
            mnemonic = merksatz,
            formula = formel,
            relatedTopicIds = verwandteThemen
        )
    }
}

data class Topic(
    val id: String,
    val category: Category,
    val title: String,
    val keyTakeaway: String,
    val explanation: String,
    val mnemonic: String? = null,
    val formula: String? = null,
    val relatedTopicIds: List<String> = emptyList()
)
