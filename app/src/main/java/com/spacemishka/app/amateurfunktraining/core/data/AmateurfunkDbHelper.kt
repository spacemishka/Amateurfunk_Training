package com.spacemishka.app.amateurfunktraining.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AmateurfunkDbHelper(
    context: Context,
    dbName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // QuestionProgress & UserMeta from MVP 2
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS QuestionProgress (
                frage_id TEXT NOT NULL PRIMARY KEY,
                status TEXT NOT NULL,
                fehlerzaehler INTEGER NOT NULL DEFAULT 0,
                letzte_antwort INTEGER NOT NULL DEFAULT 0,
                leitner_box INTEGER NOT NULL DEFAULT 1,
                ist_lesezeichen INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS UserMeta (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_progress_fehler ON QuestionProgress(fehlerzaehler)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_progress_lesezeichen ON QuestionProgress(ist_lesezeichen)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_progress_box ON QuestionProgress(leitner_box)")

        // ExamHistory & ExamWrongQuestions for MVP 3
        createExamTables(db)

        // TopicHistory for MVP 4
        createTopicTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createExamTables(db)
        }
        if (oldVersion < 3) {
            createTopicTables(db)
        }
    }

    private fun createExamTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ExamHistory (
                session_id TEXT NOT NULL PRIMARY KEY,
                timestamp INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                technik_richtig INTEGER NOT NULL,
                technik_gesamt INTEGER NOT NULL,
                betrieb_richtig INTEGER NOT NULL,
                betrieb_gesamt INTEGER NOT NULL,
                vorschriften_richtig INTEGER NOT NULL,
                vorschriften_gesamt INTEGER NOT NULL,
                gesamt_bestanden INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ExamWrongQuestions (
                session_id TEXT NOT NULL,
                frage_id TEXT NOT NULL,
                PRIMARY KEY(session_id, frage_id),
                FOREIGN KEY(session_id) REFERENCES ExamHistory(session_id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exam_history_time ON ExamHistory(timestamp DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exam_wrong_session ON ExamWrongQuestions(session_id)")
    }

    private fun createTopicTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS TopicHistory (
                topic_id TEXT NOT NULL PRIMARY KEY,
                gelesen_am INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_topic_history_gelesen ON TopicHistory(gelesen_am DESC)")
    }

    companion object {
        const val DATABASE_NAME = "amateurfunk_progress.db"
        const val DATABASE_VERSION = 3
    }
}
