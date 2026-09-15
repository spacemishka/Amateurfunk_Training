package com.spacemishka.app.amateurfunktraining.core.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TopicHistoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun createRepository(): AssetTopicRepository {
        val json = File("src/main/assets/topics_klasse_e.json").readText(Charsets.UTF_8)
        return AssetTopicRepository(
            jsonContentProvider = { json },
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testMarkTopicAsViewedAndOrdering() = runTest(testDispatcher) {
        val repo = createRepository()

        // Initial history should be empty
        val initialHistory = repo.getRecentlyViewedTopicsStream().first()
        assertTrue(initialHistory.isEmpty())

        // View topic 1
        repo.markTopicAsViewed("top_technik_ea1")
        val history1 = repo.getRecentlyViewedTopicsStream().first()
        assertEquals(1, history1.size)
        assertEquals("top_technik_ea1", history1[0].id)

        // View topic 2
        repo.markTopicAsViewed("top_technik_eb3")
        val history2 = repo.getRecentlyViewedTopicsStream().first()
        assertEquals(2, history2.size)
        assertEquals("top_technik_eb3", history2[0].id)
        assertEquals("top_technik_ea1", history2[1].id)

        // View topic 3
        repo.markTopicAsViewed("top_betrieb_ba1")
        val history3 = repo.getRecentlyViewedTopicsStream().first()
        assertEquals(3, history3.size)
        assertEquals("top_betrieb_ba1", history3[0].id)
        assertEquals("top_technik_eb3", history3[1].id)
        assertEquals("top_technik_ea1", history3[2].id)

        // Re-view topic 1: Must move to top without duplicating
        repo.markTopicAsViewed("top_technik_ea1")
        val history4 = repo.getRecentlyViewedTopicsStream().first()
        assertEquals(3, history4.size)
        assertEquals("top_technik_ea1", history4[0].id)
        assertEquals("top_betrieb_ba1", history4[1].id)
        assertEquals("top_technik_eb3", history4[2].id)
    }
}
