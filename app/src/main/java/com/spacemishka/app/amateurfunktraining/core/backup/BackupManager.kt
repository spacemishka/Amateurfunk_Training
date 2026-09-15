package com.spacemishka.app.amateurfunktraining.core.backup

import com.spacemishka.app.amateurfunktraining.core.data.ExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.ProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.SettingsRepository
import com.spacemishka.app.amateurfunktraining.core.data.TopicRepository
import com.spacemishka.app.amateurfunktraining.core.model.AppBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.BackupSummary
import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgressBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.StreakBackupDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BackupManager(
    private val progressRepository: ProgressRepository,
    private val examRepository: ExamRepository,
    private val topicRepository: TopicRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun createBackupJson(
        appVersion: String = "1.0.0",
        platform: String = "Android"
    ): String = withContext(ioDispatcher) {
        val progressList = progressRepository.getAllProgress()
        val examList = examRepository.getAllExamHistory()
        val readTopics = topicRepository.getAllReadTopicIds()
        val settingsDto = settingsRepository.exportSettings()
        val (lastDate, streak) = progressRepository.getStreakData()

        val progressDtos = progressList.map { p ->
            QuestionProgressBackupDto(
                frageId = p.questionId,
                status = p.status.name,
                fehlerzaehler = p.errorCount,
                letzteAntwort = p.lastAnsweredTimestamp,
                leitnerBox = p.leitnerBox,
                istLesezeichen = p.isBookmarked
            )
        }

        val examDtos = examList.map { e ->
            ExamHistoryBackupDto(
                sessionId = e.sessionId,
                timestamp = e.timestamp,
                durationSeconds = e.durationSeconds,
                technikRichtig = e.technikRichtig,
                technikGesamt = e.technikGesamt,
                betriebRichtig = e.betriebRichtig,
                betriebGesamt = e.betriebGesamt,
                vorschriftenRichtig = e.vorschriftenRichtig,
                vorschriftenGesamt = e.vorschriftenGesamt,
                gesamtBestanden = e.isOverallPassed
            )
        }

        val streakDto = StreakBackupDto(
            aktuellerStreak = streak,
            letzterLerntag = lastDate
        )

        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val exportTimestamp = isoFormatter.format(Date())

        val backupDto = AppBackupDto(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appVersion = appVersion,
            exportiertAm = exportTimestamp,
            clientPlatform = platform,
            lernstand = progressDtos,
            pruefungshistorie = examDtos,
            einstellungen = settingsDto,
            geleseneThemen = readTopics,
            streak = streakDto
        )

        json.encodeToString(AppBackupDto.serializer(), backupDto)
    }

    fun validateBackupJson(jsonString: String): Result<Pair<AppBackupDto, BackupSummary>> {
        return try {
            val dto = json.decodeFromString(AppBackupDto.serializer(), jsonString)

            if (dto.schemaVersion.isBlank()) {
                return Result.failure(IllegalArgumentException("Ungültiges Backup-Format: Fehlende Schema-Version."))
            }

            val majorVersion = dto.schemaVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
            if (majorVersion > SUPPORTED_MAJOR_VERSION) {
                return Result.failure(
                    IllegalArgumentException(
                        "Inkompatible Schema-Version: ${dto.schemaVersion}. Unterstützt wird maximal Version $CURRENT_SCHEMA_VERSION."
                    )
                )
            }

            val summary = BackupSummary(
                exportiertAm = dto.exportiertAm,
                clientPlatform = dto.clientPlatform,
                progressCount = dto.lernstand.size,
                examCount = dto.pruefungshistorie.size,
                topicsCount = dto.geleseneThemen.size,
                streakDays = dto.streak?.aktuellerStreak ?: 0
            )

            Result.success(Pair(dto, summary))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Fehler beim Lesen der Backup-Datei: ${e.message ?: "Ungültiges JSON-Format"}", e))
        }
    }

    suspend fun restoreBackup(
        backup: AppBackupDto,
        mode: ImportMode
    ): Result<String> = withContext(ioDispatcher) {
        try {
            // 1. Progress
            progressRepository.importProgress(backup.lernstand, mode)

            // 2. Streak
            if (backup.streak != null) {
                progressRepository.setStreakData(
                    lastDate = backup.streak.letzterLerntag,
                    streak = backup.streak.aktuellerStreak,
                    mode = mode
                )
            }

            // 3. Exam History
            examRepository.importExamHistory(backup.pruefungshistorie, mode)

            // 4. Topic History
            topicRepository.importReadTopicIds(backup.geleseneThemen, mode)

            // 5. Settings
            if (backup.einstellungen != null) {
                settingsRepository.importSettings(backup.einstellungen)
            }

            val modeText = if (mode == ImportMode.MERGE) "zusammengeführt" else "wiederhergestellt"
            Result.success("${backup.lernstand.size} Lernstände und ${backup.pruefungshistorie.size} Prüfungen erfolgreich $modeText.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = "1.0"
        const val SUPPORTED_MAJOR_VERSION = 1
    }
}
