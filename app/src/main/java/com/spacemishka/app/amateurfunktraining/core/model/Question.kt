package com.spacemishka.app.amateurfunktraining.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestionDto(
    @SerialName("id")
    val id: String,
    @SerialName("kategorie")
    val kategorie: String,
    @SerialName("topic_id")
    val topicId: String,
    @SerialName("frage_text")
    val frageText: String,
    @SerialName("antworten")
    val antworten: List<String>,
    @SerialName("richtige_antwort")
    val richtigeAntwort: Int,
    @SerialName("erklaerung")
    val erklaerung: String,
    @SerialName("bild_svg")
    val bildSvg: String? = null
) {
    fun toDomain(): Question {
        return Question(
            id = id,
            category = Category.fromKey(kategorie),
            topicId = topicId,
            text = frageText,
            answers = antworten,
            correctAnswerIndex = richtigeAntwort,
            explanation = erklaerung,
            imageSvg = bildSvg
        )
    }
}

data class Question(
    val id: String,
    val category: Category,
    val topicId: String,
    val text: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val imageSvg: String? = null
)
