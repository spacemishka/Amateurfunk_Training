package com.spacemishka.app.amateurfunktraining.core.backup

import com.spacemishka.app.amateurfunktraining.core.data.InMemoryExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.InMemoryProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.InMemorySettingsRepository
import com.spacemishka.app.amateurfunktraining.core.data.TopicRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupCompatibilityTest {

    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        val dummyTopicRepo = object : TopicRepository {
            override suspend fun getAllTopics(): List<Topic> = emptyList()
            override suspend fun getTopicById(id: String): Topic? = null
            override suspend fun searchTopics(query: String, category: Category?): List<Topic> = emptyList()
            override fun getRecentlyViewedTopicsStream(): Flow<List<Topic>> = emptyFlow()
            override suspend fun markTopicAsViewed(topicId: String) {}
            override suspend fun getAllReadTopicIds(): List<String> = emptyList()
            override suspend fun importReadTopicIds(topicIds: List<String>, mode: ImportMode) {}
            override suspend fun clearTopicHistory() {}
        }

        backupManager = BackupManager(
            progressRepository = InMemoryProgressRepository(),
            examRepository = InMemoryExamRepository(),
            topicRepository = dummyTopicRepo,
            settingsRepository = InMemorySettingsRepository()
        )
    }

    @Test
    fun testRejectsIncompatibleMajorSchemaVersion() {
        val futureJson = """
            {
              "schema_version": "99.0",
              "app_version": "99.0.0",
              "exportiert_am": "2030-01-01T00:00:00Z",
              "client_platform": "Web",
              "lernstand": []
            }
        """.trimIndent()

        val result = backupManager.validateBackupJson(futureJson)
        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Inkompatible Schema-Version") == true)
    }

    @Test
    fun testRejectsMalformedJson() {
        val malformedJson = "{ this is not valid json: true"

        val result = backupManager.validateBackupJson(malformedJson)
        assertFalse(result.isSuccess)
    }

    @Test
    fun testRejectsEmptySchemaVersion() {
        val emptySchemaJson = """
            {
              "schema_version": "",
              "app_version": "1.0.0",
              "exportiert_am": "2026-09-15T00:00:00Z",
              "client_platform": "Android",
              "lernstand": []
            }
        """.trimIndent()

        val result = backupManager.validateBackupJson(emptySchemaJson)
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Fehlende Schema-Version") == true)
    }

    @Test
    fun testAcceptsForwardCompatibleUnknownFields() {
        val jsonWithUnknownFields = """
            {
              "schema_version": "1.0",
              "app_version": "1.6.0",
              "exportiert_am": "2026-09-15T12:00:00Z",
              "client_platform": "WasmJs",
              "unbekanntes_neues_feld": 12345,
              "lernstand": [
                {
                  "frage_id": "TA101",
                  "status": "GEMEISTERT",
                  "fehlerzaehler": 0,
                  "letzte_antwort": 1726000000000,
                  "leitner_box": 5,
                  "ist_lesezeichen": false,
                  "future_field_extra": "foo"
                }
              ]
            }
        """.trimIndent()

        val result = backupManager.validateBackupJson(jsonWithUnknownFields)
        assertTrue(result.isSuccess)
        val (dto, summary) = result.getOrThrow()
        assertEquals(1, summary.progressCount)
        assertEquals("WasmJs", summary.clientPlatform)
        assertEquals("TA101", dto.lernstand[0].frageId)
    }
}
