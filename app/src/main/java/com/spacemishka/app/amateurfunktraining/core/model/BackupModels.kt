package com.spacemishka.app.amateurfunktraining.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppBackupDto(
    @SerialName("schema_version")
    val schemaVersion: String = "1.0",
    @SerialName("app_version")
    val appVersion: String = "1.0.0",
    @SerialName("exportiert_am")
    val exportiertAm: String,
    @SerialName("client_platform")
    val clientPlatform: String = "Android",
    @SerialName("lernstand")
    val lernstand: List<QuestionProgressBackupDto> = emptyList(),
    @SerialName("pruefungshistorie")
    val pruefungshistorie: List<ExamHistoryBackupDto> = emptyList(),
    @SerialName("einstellungen")
    val einstellungen: SettingsBackupDto? = null,
    @SerialName("gelesene_themen")
    val geleseneThemen: List<String> = emptyList(),
    @SerialName("streak")
    val streak: StreakBackupDto? = null
)

@Serializable
data class QuestionProgressBackupDto(
    @SerialName("frage_id")
    val frageId: String,
    @SerialName("status")
    val status: String,
    @SerialName("fehlerzaehler")
    val fehlerzaehler: Int = 0,
    @SerialName("letzte_antwort")
    val letzteAntwort: Long = 0L,
    @SerialName("leitner_box")
    val leitnerBox: Int = 1,
    @SerialName("ist_lesezeichen")
    val istLesezeichen: Boolean = false
)

@Serializable
data class ExamHistoryBackupDto(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("duration_seconds")
    val durationSeconds: Long = 0L,
    @SerialName("technik_richtig")
    val technikRichtig: Int,
    @SerialName("technik_gesamt")
    val technikGesamt: Int,
    @SerialName("betrieb_richtig")
    val betriebRichtig: Int,
    @SerialName("betrieb_gesamt")
    val betriebGesamt: Int,
    @SerialName("vorschriften_richtig")
    val vorschriftenRichtig: Int,
    @SerialName("vorschriften_gesamt")
    val vorschriftenGesamt: Int,
    @SerialName("gesamt_bestanden")
    val gesamtBestanden: Boolean
)

@Serializable
data class SettingsBackupDto(
    @SerialName("dunkelmodus")
    val dunkelmodus: String = "SYSTEM",
    @SerialName("schriftgroesse")
    val schriftgroesse: String = "NORMAL",
    @SerialName("pruefungs_timer_minuten")
    val pruefungsTimerMinuten: Int = 150
)

@Serializable
data class StreakBackupDto(
    @SerialName("aktueller_streak")
    val aktuellerStreak: Int = 0,
    @SerialName("letzter_lerntag")
    val letzterLerntag: String? = null
)

data class BackupSummary(
    val exportiertAm: String,
    val clientPlatform: String,
    val progressCount: Int,
    val examCount: Int,
    val topicsCount: Int,
    val streakDays: Int
)

enum class ImportMode(val displayName: String, val description: String) {
    MERGE(
        displayName = "Intelligent zusammenführen (Empfohlen)",
        description = "Behält bestehende Lernerfolge. Höhere Leitner-Boxen und neue Prüfungssitzungen werden ergänzt."
    ),
    OVERWRITE(
        displayName = "Bestehende Daten überschreiben",
        description = "Löscht den aktuellen lokalen Lernstand vollständig und stellt das Backup 1:1 wieder her."
    )
}
