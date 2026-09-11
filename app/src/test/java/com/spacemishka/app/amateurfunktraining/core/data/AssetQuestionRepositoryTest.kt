package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.Category
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AssetQuestionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleJson = """
        [
          {
            "id": "T1",
            "kategorie": "Technik",
            "topic_id": "top_technik_1",
            "frage_text": "Frage 1 Technik",
            "antworten": ["A1", "A2", "A3", "A4"],
            "richtige_antwort": 0,
            "erklaerung": "Erklärung 1"
          },
          {
            "id": "T2",
            "kategorie": "Technik",
            "topic_id": "top_technik_2",
            "frage_text": "Frage 2 Technik",
            "antworten": ["B1", "B2", "B3", "B4"],
            "richtige_antwort": 1,
            "erklaerung": "Erklärung 2"
          },
          {
            "id": "B1",
            "kategorie": "Betrieb",
            "topic_id": "top_betrieb_1",
            "frage_text": "Frage 1 Betrieb",
            "antworten": ["C1", "C2", "C3", "C4"],
            "richtige_antwort": 2,
            "erklaerung": "Erklärung 3"
          },
          {
            "id": "V1",
            "kategorie": "Vorschriften",
            "topic_id": "top_recht_1",
            "frage_text": "Frage 1 Vorschriften",
            "antworten": ["D1", "D2", "D3", "D4"],
            "richtige_antwort": 3,
            "erklaerung": "Erklärung 4"
          }
        ]
    """.trimIndent()

    @Test
    fun testLoadAndFilterByCategory() = runTest(testDispatcher) {
        val repo = AssetQuestionRepository(
            jsonContentProvider = { sampleJson },
            ioDispatcher = testDispatcher
        )

        val all = repo.getAllQuestions()
        assertEquals(4, all.size)

        val technik = repo.getQuestionsByCategory(Category.TECHNIK)
        assertEquals(2, technik.size)
        assertEquals("T1", technik[0].id)
        assertEquals("T2", technik[1].id)

        val betrieb = repo.getQuestionsByCategory(Category.BETRIEB)
        assertEquals(1, betrieb.size)
        assertEquals("B1", betrieb[0].id)

        val vorschriften = repo.getQuestionsByCategory(Category.VORSCHRIFTEN)
        assertEquals(1, vorschriften.size)
        assertEquals("V1", vorschriften[0].id)

        val allFilter = repo.getQuestionsByCategory(Category.ALL)
        assertEquals(4, allFilter.size)
    }

    @Test
    fun testGetQuestionById() = runTest(testDispatcher) {
        val repo = AssetQuestionRepository(
            jsonContentProvider = { sampleJson },
            ioDispatcher = testDispatcher
        )

        val q = repo.getQuestionById("b1") // Case insensitive check
        assertNotNull(q)
        assertEquals("B1", q?.id)
        assertEquals(Category.BETRIEB, q?.category)

        val notFound = repo.getQuestionById("UNKNOWN")
        assertNull(notFound)
    }

    @Test
    fun testCategoryCounts() = runTest(testDispatcher) {
        val repo = AssetQuestionRepository(
            jsonContentProvider = { sampleJson },
            ioDispatcher = testDispatcher
        )

        val counts = repo.getCategoryCounts()
        assertEquals(4, counts[Category.ALL])
        assertEquals(2, counts[Category.TECHNIK])
        assertEquals(1, counts[Category.BETRIEB])
        assertEquals(1, counts[Category.VORSCHRIFTEN])
    }
}
