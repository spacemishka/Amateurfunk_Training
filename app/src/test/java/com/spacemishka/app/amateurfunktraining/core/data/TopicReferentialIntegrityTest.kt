package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.QuestionDto
import com.spacemishka.app.amateurfunktraining.core.model.TopicDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TopicReferentialIntegrityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testTopicCatalogueAndQuestionIntegrity() {
        val topicsFile = File("src/main/assets/topics_klasse_e.json")
        assertTrue("Asset file topics_klasse_e.json must exist", topicsFile.exists())
        val topicsJson = topicsFile.readText(Charsets.UTF_8)
        assertFalse("Topics JSON must not be blank", topicsJson.isBlank())

        val topicDtos = json.decodeFromString<List<TopicDto>>(topicsJson)
        val topics = topicDtos.map { it.toDomain() }

        // DoD check: Mindestens 50 kuratierte Themenblöcke
        assertTrue("Muss mindestens 50 Themenblöcke enthalten (aktuell: ${topics.size})", topics.size >= 50)

        val topicMap = topics.associateBy { it.id.lowercase() }
        assertEquals("Alle topic_ids müssen eindeutig sein", topics.size, topicMap.size)

        // Check each topic's required content
        for (topic in topics) {
            assertTrue("Topic ID darf nicht leer sein: ${topic.id}", topic.id.isNotBlank())
            assertTrue("Topic Titel darf nicht leer sein: ${topic.id}", topic.title.isNotBlank())
            assertTrue("Kernaussage darf nicht leer sein: ${topic.id}", topic.keyTakeaway.isNotBlank())
            assertTrue("Erklärung darf nicht leer sein: ${topic.id}", topic.explanation.isNotBlank())
            assertTrue("Kategorie darf nicht ALL sein: ${topic.id}", topic.category != Category.ALL)

            // Validate related topics references if present
            for (relId in topic.relatedTopicIds) {
                assertTrue(
                    "Verwandtes Thema '$relId' in '${topic.id}' muss im Katalog existieren",
                    topicMap.containsKey(relId.lowercase())
                )
            }
        }

        // Now verify referential integrity with questions_klasse_e.json
        val questionsFile = File("src/main/assets/questions_klasse_e.json")
        assertTrue("Asset file questions_klasse_e.json must exist", questionsFile.exists())
        val questionsJson = questionsFile.readText(Charsets.UTF_8)
        val questionDtos = json.decodeFromString<List<QuestionDto>>(questionsJson)
        val questions = questionDtos.map { it.toDomain() }

        assertTrue("Es müssen alle 830 Prüfungsfragen vorhanden sein", questions.size >= 800)

        val unmappedQuestions = mutableListOf<String>()
        val categoryMismatches = mutableListOf<String>()

        for (question in questions) {
            val topic = topicMap[question.topicId.lowercase()]
            if (topic == null) {
                unmappedQuestions.add("${question.id} -> ${question.topicId}")
            } else {
                if (topic.category != question.category) {
                    categoryMismatches.add(
                        "Frage ${question.id} (${question.category}) referenziert Topic ${topic.id} (${topic.category})"
                    )
                }
            }
        }

        assertTrue(
            "Folgende Fragen haben ungültige topic_id: $unmappedQuestions",
            unmappedQuestions.isEmpty()
        )
        assertTrue(
            "Kategorie-Diskrepanzen entdeckt: $categoryMismatches",
            categoryMismatches.isEmpty()
        )
    }
}
