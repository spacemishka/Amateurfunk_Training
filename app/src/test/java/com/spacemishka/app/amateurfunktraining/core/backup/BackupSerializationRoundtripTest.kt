package com.spacemishka.app.amateurfunktraining.core.backup

import com.spacemishka.app.amateurfunktraining.core.data.InMemoryExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.InMemoryProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.InMemorySettingsRepository
import com.spacemishka.app.amateurfunktraining.core.data.ThemeMode
import com.spacemishka.app.amateurfunktraining.core.data.TopicRepository
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.ExamResultPart
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupSerializationRoundtripTest {

    private lateinit var progressRepo: InMemoryProgressRepository
    private lateinit var examRepo: InMemoryExamRepository
    private lateinit var topicRepo: MockTopicRepository
    private lateinit var settingsRepo: InMemorySettingsRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        progressRepo = InMemoryProgressRepository()
        examRepo = InMemoryExamRepository()
        topicRepo = MockTopicRepository()
        settingsRepo = InMemorySettingsRepository()
        backupManager = BackupManager(progressRepo, examRepo, topicRepo, settingsRepo)
    }

    @Test
    fun testExportAndRestoreRoundtrip() = runBlocking {
        // 1. Populate initial data
        progressRepo.recordAnswer("TE-001", isCorrect = true, timestamp = 1726000000000L)
        progressRepo.recordAnswer("TE-002", isCorrect = false, timestamp = 1726000010000L)
        progressRepo.toggleBookmark("TE-002")

        examRepo.recordExamResult(
            ExamResult(
                sessionId = "exam_session_1",
                timestamp = 1726055000000L,
                durationSeconds = 3600L,
                partResults = listOf(
                    ExamResultPart(Category.TECHNIK, 34, 28, 28f / 34f, true),
                    ExamResultPart(Category.BETRIEB, 25, 20, 20f / 25f, true),
                    ExamResultPart(Category.VORSCHRIFTEN, 25, 22, 22f / 25f, true)
                ),
                isOverallPassed = true,
                wrongQuestions = emptyList()
            )
        )

        topicRepo.markTopicAsViewed("topic_ohmsches_gesetz")
        topicRepo.markTopicAsViewed("topic_wellenlaenge")

        settingsRepo.setThemeMode(ThemeMode.DARK)

        // 2. Export to JSON
        val exportedJson = backupManager.createBackupJson(appVersion = "1.6.0", platform = "Android")
        assertTrue(exportedJson.contains("\"schema_version\": \"1.0\""))
        assertTrue(exportedJson.contains("\"frage_id\": \"TE-001\""))
        assertTrue(exportedJson.contains("\"session_id\": \"exam_session_1\""))
        assertTrue(exportedJson.contains("\"dunkelmodus\": \"DARK\""))

        // 3. Validate JSON
        val validation = backupManager.validateBackupJson(exportedJson)
        assertTrue(validation.isSuccess)
        val (dto, summary) = validation.getOrThrow()
        assertEquals(2, summary.progressCount)
        assertEquals(1, summary.examCount)
        assertEquals(2, summary.topicsCount)
        assertEquals("Android", summary.clientPlatform)

        // 4. Restore into clean repositories
        val freshProgress = InMemoryProgressRepository()
        val freshExam = InMemoryExamRepository()
        val freshTopic = MockTopicRepository()
        val freshSettings = InMemorySettingsRepository()
        val freshManager = BackupManager(freshProgress, freshExam, freshTopic, freshSettings)

        val restoreResult = freshManager.restoreBackup(dto, ImportMode.OVERWRITE)
        assertTrue(restoreResult.isSuccess)

        // 5. Verify restored state
        val p1 = freshProgress.getProgressForQuestion("TE-001")
        assertNotNull(p1)
        assertEquals(2, p1!!.leitnerBox)
        assertEquals(0, p1.errorCount)

        val p2 = freshProgress.getProgressForQuestion("TE-002")
        assertNotNull(p2)
        assertEquals(1, p2!!.leitnerBox)
        assertEquals(1, p2.errorCount)
        assertTrue(p2.isBookmarked)

        val restoredExams = freshExam.getAllExamHistory()
        assertEquals(1, restoredExams.size)
        assertEquals("exam_session_1", restoredExams[0].sessionId)
        assertTrue(restoredExams[0].isOverallPassed)
        assertEquals(28, restoredExams[0].technikRichtig)

        val restoredTopics = freshTopic.getAllReadTopicIds()
        assertEquals(2, restoredTopics.size)
        assertTrue(restoredTopics.contains("topic_ohmsches_gesetz"))

        assertEquals(ThemeMode.DARK, freshSettings.settingsFlow.value.themeMode)
    }

    private class MockTopicRepository : TopicRepository {
        private val readTopics = mutableListOf<String>()
        private val _flow = MutableStateFlow<List<Topic>>(emptyList())

        override suspend fun getAllTopics(): List<Topic> = emptyList()
        override suspend fun getTopicById(id: String): Topic? = null
        override suspend fun searchTopics(query: String, category: Category?): List<Topic> = emptyList()
        override fun getRecentlyViewedTopicsStream(): Flow<List<Topic>> = _flow.asStateFlow()

        override suspend fun markTopicAsViewed(topicId: String) {
            if (!readTopics.contains(topicId)) readTopics.add(0, topicId)
        }

        override suspend fun getAllReadTopicIds(): List<String> = readTopics.toList()

        override suspend fun importReadTopicIds(topicIds: List<String>, mode: ImportMode) {
            if (mode == ImportMode.OVERWRITE) readTopics.clear()
            for (t in topicIds) {
                if (!readTopics.contains(t)) readTopics.add(t)
            }
        }

        override suspend fun clearTopicHistory() {
            readTopics.clear()
        }
    }
}
