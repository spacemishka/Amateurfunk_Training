package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TopicSearchTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun createRepository(): AssetTopicRepository {
        val json = File("src/main/assets/topics_klasse_e.json").readText(Charsets.UTF_8)
        return AssetTopicRepository(
            jsonContentProvider = { json },
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testSearchByTitleAndExplanation() = runTest(testDispatcher) {
        val repo = createRepository()

        val resultsOhm = repo.searchTopics("ohm", null)
        assertFalse("Muss Themen zu 'ohm' finden", resultsOhm.isEmpty())
        assertTrue("Widerstände müssen enthalten sein", resultsOhm.any { it.id.contains("ec1") || it.id.contains("eb5") })

        val resultsWelle = repo.searchTopics("Wellenlänge", null)
        assertFalse("Muss Themen zu 'Wellenlänge' finden", resultsWelle.isEmpty())
        assertTrue(resultsWelle.any { it.id == "top_technik_eb3" })

        val resultsRelais = repo.searchTopics("Relais", null)
        assertFalse("Muss Themen zu 'Relais' finden", resultsRelais.isEmpty())
        assertTrue(resultsRelais.any { it.id == "top_betrieb_be2" || it.id == "top_vorschriften_vd5" })
    }

    @Test
    fun testSearchInsideFormulaAndMnemonic() = runTest(testDispatcher) {
        val repo = createRepository()

        // Formula search
        val formulaResults = repo.searchTopics("300 / f", null)
        assertFalse(formulaResults.isEmpty())
        assertTrue(formulaResults.any { it.formula?.contains("300 / f") == true })

        // Mnemonic search
        val mnemonicResults = repo.searchTopics("kleine Antenne", null)
        assertFalse(mnemonicResults.isEmpty())
        assertTrue(mnemonicResults.any { it.mnemonic?.contains("kleine Antenne") == true })
    }

    @Test
    fun testSearchWithCategoryFilter() = runTest(testDispatcher) {
        val repo = createRepository()

        val allResults = repo.searchTopics("", Category.ALL)
        assertTrue(allResults.size >= 50)

        val technikOnly = repo.searchTopics("", Category.TECHNIK)
        assertTrue(technikOnly.all { it.category == Category.TECHNIK })
        assertFalse(technikOnly.isEmpty())

        val betriebOnly = repo.searchTopics("", Category.BETRIEB)
        assertTrue(betriebOnly.all { it.category == Category.BETRIEB })
        assertFalse(betriebOnly.isEmpty())

        val vorschriftenOnly = repo.searchTopics("", Category.VORSCHRIFTEN)
        assertTrue(vorschriftenOnly.all { it.category == Category.VORSCHRIFTEN })
        assertFalse(vorschriftenOnly.isEmpty())

        // Combined query + category
        val vorschriftenLeistung = repo.searchTopics("Leistung", Category.VORSCHRIFTEN)
        assertTrue(vorschriftenLeistung.all { it.category == Category.VORSCHRIFTEN })
        assertTrue(vorschriftenLeistung.any { it.id == "top_vorschriften_vd7" })
    }

    @Test
    fun testGetTopicById() = runTest(testDispatcher) {
        val repo = createRepository()

        val topic = repo.getTopicById("TOP_TECHNIK_EB3") // Case insensitive check
        assertNotNull(topic)
        assertEquals("top_technik_eb3", topic?.id)
        assertEquals(Category.TECHNIK, topic?.category)

        val nonExistent = repo.getTopicById("non_existent_topic_id")
        org.junit.Assert.assertNull(nonExistent)
    }
}
