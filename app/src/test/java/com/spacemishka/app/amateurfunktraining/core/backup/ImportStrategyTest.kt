package com.spacemishka.app.amateurfunktraining.core.backup

import com.spacemishka.app.amateurfunktraining.core.data.InMemoryExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.InMemoryProgressRepository
import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgressBackupDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportStrategyTest {

    private lateinit var progressRepo: InMemoryProgressRepository
    private lateinit var examRepo: InMemoryExamRepository

    @Before
    fun setup() {
        progressRepo = InMemoryProgressRepository()
        examRepo = InMemoryExamRepository()
    }

    @Test
    fun testMergeStrategyKeepsHigherLeitnerBoxAndErrorCount() = runBlocking {
        // Device state: Q1 in box 2 with 3 errors
        progressRepo.importProgress(
            listOf(
                QuestionProgressBackupDto(
                    frageId = "Q-001",
                    status = "IN_BEARBEITUNG",
                    fehlerzaehler = 3,
                    letzteAntwort = 1000L,
                    leitnerBox = 2,
                    istLesezeichen = false
                )
            ),
            mode = ImportMode.OVERWRITE
        )

        // Incoming backup from Web/second device: Q1 in box 4 with 1 error and bookmarked
        val backupItems = listOf(
            QuestionProgressBackupDto(
                frageId = "Q-001",
                status = "IN_BEARBEITUNG",
                fehlerzaehler = 1,
                letzteAntwort = 2000L,
                leitnerBox = 4,
                istLesezeichen = true
            ),
            QuestionProgressBackupDto(
                frageId = "Q-002",
                status = "GEMEISTERT",
                fehlerzaehler = 0,
                letzteAntwort = 2500L,
                leitnerBox = 5,
                istLesezeichen = false
            )
        )

        progressRepo.importProgress(backupItems, mode = ImportMode.MERGE)

        val mergedQ1 = progressRepo.getProgressForQuestion("Q-001")
        assertNotNull(mergedQ1)
        assertEquals(4, mergedQ1!!.leitnerBox) // Highest box wins
        assertEquals(3, mergedQ1.errorCount) // Highest errors win
        assertEquals(2000L, mergedQ1.lastAnsweredTimestamp) // Latest timestamp wins
        assertTrue(mergedQ1.isBookmarked) // Logical OR for bookmark

        val mergedQ2 = progressRepo.getProgressForQuestion("Q-002")
        assertNotNull(mergedQ2)
        assertEquals(5, mergedQ2!!.leitnerBox)
        assertEquals(ProgressStatus.GEMEISTERT, mergedQ2.status)
    }

    @Test
    fun testMergeStrategyDeduplicatesExamSessions() = runBlocking {
        // Device state has session 1
        examRepo.importExamHistory(
            listOf(
                ExamHistoryBackupDto(
                    sessionId = "session_1",
                    timestamp = 1000L,
                    durationSeconds = 1200L,
                    technikRichtig = 30,
                    technikGesamt = 34,
                    betriebRichtig = 22,
                    betriebGesamt = 25,
                    vorschriftenRichtig = 24,
                    vorschriftenGesamt = 25,
                    gesamtBestanden = true
                )
            ),
            mode = ImportMode.OVERWRITE
        )

        // Backup has session 1 (duplicate) and session 2 (new)
        val backupExams = listOf(
            ExamHistoryBackupDto(
                sessionId = "session_1",
                timestamp = 1000L,
                durationSeconds = 1200L,
                technikRichtig = 30,
                technikGesamt = 34,
                betriebRichtig = 22,
                betriebGesamt = 25,
                vorschriftenRichtig = 24,
                vorschriftenGesamt = 25,
                gesamtBestanden = true
            ),
            ExamHistoryBackupDto(
                sessionId = "session_2",
                timestamp = 2000L,
                durationSeconds = 1400L,
                technikRichtig = 18,
                technikGesamt = 34,
                betriebRichtig = 19,
                betriebGesamt = 25,
                vorschriftenRichtig = 19,
                vorschriftenGesamt = 25,
                gesamtBestanden = false
            )
        )

        examRepo.importExamHistory(backupExams, mode = ImportMode.MERGE)

        val allExams = examRepo.getAllExamHistory()
        assertEquals(2, allExams.size) // No duplicate session_1
        assertEquals("session_2", allExams[0].sessionId) // Sorted descending by timestamp
        assertEquals("session_1", allExams[1].sessionId)
    }

    @Test
    fun testOverwriteStrategyReplacesCompletely() = runBlocking {
        // Device state has Q-OLD
        progressRepo.importProgress(
            listOf(
                QuestionProgressBackupDto(
                    frageId = "Q-OLD",
                    status = "IN_BEARBEITUNG",
                    fehlerzaehler = 2,
                    letzteAntwort = 100L,
                    leitnerBox = 3,
                    istLesezeichen = true
                )
            ),
            mode = ImportMode.OVERWRITE
        )

        // Backup has only Q-NEW
        progressRepo.importProgress(
            listOf(
                QuestionProgressBackupDto(
                    frageId = "Q-NEW",
                    status = "NEU",
                    fehlerzaehler = 0,
                    letzteAntwort = 0L,
                    leitnerBox = 1,
                    istLesezeichen = false
                )
            ),
            mode = ImportMode.OVERWRITE
        )

        val allProgress = progressRepo.getAllProgress()
        assertEquals(1, allProgress.size)
        assertEquals("Q-NEW", allProgress[0].questionId)
        assertFalse(allProgress.any { it.questionId == "Q-OLD" })
    }
}
