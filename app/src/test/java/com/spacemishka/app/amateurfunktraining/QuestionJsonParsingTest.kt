package com.spacemishka.app.amateurfunktraining

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.QuestionDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuestionJsonParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testQuestionsCatalogValidity() {
        val assetFile = File("src/main/assets/questions_klasse_e.json")
        assertTrue("Asset file questions_klasse_e.json must exist", assetFile.exists())

        val rawJson = assetFile.readText()
        assertFalse("JSON content must not be empty", rawJson.isBlank())

        val dtoList = json.decodeFromString<List<QuestionDto>>(rawJson)
        assertTrue("There must be at least 15 questions in the baseline catalog", dtoList.size >= 15)

        val seenIds = mutableSetOf<String>()
        val categoryCounts = mutableMapOf<Category, Int>()

        for (dto in dtoList) {
            val question = dto.toDomain()

            // Unique ID check
            assertFalse("Duplicate question ID: ${question.id}", seenIds.contains(question.id))
            seenIds.add(question.id)

            // Non-empty fields
            assertTrue("Question ID must not be blank", question.id.isNotBlank())
            assertTrue("Question text must not be blank for ${question.id}", question.text.isNotBlank())
            assertTrue("Explanation must not be blank for ${question.id}", question.explanation.isNotBlank())

            // Exactly 4 options
            assertEquals("Question ${question.id} must have exactly 4 answer options", 4, question.answers.size)
            for (answer in question.answers) {
                assertTrue("Answer option must not be blank for ${question.id}", answer.isNotBlank())
            }

            // Correct answer index in 0..3
            assertTrue("Correct answer index out of bounds for ${question.id}", question.correctAnswerIndex in 0..3)

            // Category must not be ALL
            assertNotNull("Category must be parsed", question.category)
            assertTrue("Category must be Technik, Betrieb or Vorschriften", question.category != Category.ALL)

            categoryCounts[question.category] = (categoryCounts[question.category] ?: 0) + 1
        }

        // Verify representation of all 3 required exam sections
        assertTrue("Technik must have at least 5 questions", (categoryCounts[Category.TECHNIK] ?: 0) >= 5)
        assertTrue("Betrieb must have at least 5 questions", (categoryCounts[Category.BETRIEB] ?: 0) >= 5)
        assertTrue("Vorschriften must have at least 5 questions", (categoryCounts[Category.VORSCHRIFTEN] ?: 0) >= 5)
    }
}
