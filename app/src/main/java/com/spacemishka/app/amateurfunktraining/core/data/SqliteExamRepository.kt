package com.spacemishka.app.amateurfunktraining.core.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.spacemishka.app.amateurfunktraining.core.exam.ExamReadinessCalculator
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.ExamHistoryEntry
import com.spacemishka.app.amateurfunktraining.core.model.ExamReadiness
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SqliteExamRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    databaseName: String = AmateurfunkDbHelper.DATABASE_NAME
) : ExamRepository {

    private val dbHelper = AmateurfunkDbHelper(context, databaseName)
    private val _historyFlow = MutableStateFlow<List<ExamHistoryEntry>>(emptyList())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        try {
            _historyFlow.value = readExamHistoryFromDb(limit = 20)
        } catch (_: Exception) {
            // Fallback for mock/test environments
        }
    }

    override fun getExamHistoryStream(): Flow<List<ExamHistoryEntry>> = _historyFlow.asStateFlow()

    override suspend fun recordExamResult(result: ExamResult): Unit = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val technikPart = result.partResults.find { it.category == Category.TECHNIK }
            val betriebPart = result.partResults.find { it.category == Category.BETRIEB }
            val vorschriftenPart = result.partResults.find { it.category == Category.VORSCHRIFTEN }

            val historyValues = ContentValues().apply {
                put("session_id", result.sessionId)
                put("timestamp", result.timestamp)
                put("duration_seconds", result.durationSeconds)
                put("technik_richtig", technikPart?.correctAnswers ?: 0)
                put("technik_gesamt", technikPart?.totalQuestions ?: 0)
                put("betrieb_richtig", betriebPart?.correctAnswers ?: 0)
                put("betrieb_gesamt", betriebPart?.totalQuestions ?: 0)
                put("vorschriften_richtig", vorschriftenPart?.correctAnswers ?: 0)
                put("vorschriften_gesamt", vorschriftenPart?.totalQuestions ?: 0)
                put("gesamt_bestanden", if (result.isOverallPassed) 1 else 0)
            }
            db.insertWithOnConflict("ExamHistory", null, historyValues, SQLiteDatabase.CONFLICT_REPLACE)

            for (wrongQ in result.wrongQuestions) {
                val wrongValues = ContentValues().apply {
                    put("session_id", result.sessionId)
                    put("frage_id", wrongQ.id)
                }
                db.insertWithOnConflict("ExamWrongQuestions", null, wrongValues, SQLiteDatabase.CONFLICT_IGNORE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val updatedHistory = readExamHistoryFromDb(limit = 20)
        _historyFlow.value = updatedHistory
    }

    override suspend fun getRecentExamResults(limit: Int): List<ExamHistoryEntry> = withContext(ioDispatcher) {
        readExamHistoryFromDb(limit)
    }

    override suspend fun getLatestWrongQuestionIds(limit: Int): List<String> = withContext(ioDispatcher) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<String>()
        val query = "SELECT DISTINCT frage_id FROM ExamWrongQuestions ORDER BY rowid DESC LIMIT ?"
        val cursor = db.rawQuery(query, arrayOf(limit.toString()))
        cursor.use {
            val idx = it.getColumnIndexOrThrow("frage_id")
            while (it.moveToNext()) {
                list.add(it.getString(idx))
            }
        }
        list
    }

    override suspend fun getWrongQuestionIdsForSession(sessionId: String): List<String> = withContext(ioDispatcher) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<String>()
        val query = "SELECT frage_id FROM ExamWrongQuestions WHERE session_id = ?"
        val cursor = db.rawQuery(query, arrayOf(sessionId))
        cursor.use {
            val idx = it.getColumnIndexOrThrow("frage_id")
            while (it.moveToNext()) {
                list.add(it.getString(idx))
            }
        }
        list
    }

    override suspend fun getReadiness(): ExamReadiness = withContext(ioDispatcher) {
        val history = readExamHistoryFromDb(limit = 10)
        ExamReadinessCalculator.calculateReadiness(history)
    }

    private fun readExamHistoryFromDb(limit: Int): List<ExamHistoryEntry> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<ExamHistoryEntry>()
        val query = "SELECT * FROM ExamHistory ORDER BY timestamp DESC LIMIT ?"
        val cursor = db.rawQuery(query, arrayOf(limit.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToExamHistoryEntry(it))
            }
        }
        return list
    }

    private fun cursorToExamHistoryEntry(cursor: Cursor): ExamHistoryEntry {
        return ExamHistoryEntry(
            sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
            durationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("duration_seconds")),
            technikRichtig = cursor.getInt(cursor.getColumnIndexOrThrow("technik_richtig")),
            technikGesamt = cursor.getInt(cursor.getColumnIndexOrThrow("technik_gesamt")),
            betriebRichtig = cursor.getInt(cursor.getColumnIndexOrThrow("betrieb_richtig")),
            betriebGesamt = cursor.getInt(cursor.getColumnIndexOrThrow("betrieb_gesamt")),
            vorschriftenRichtig = cursor.getInt(cursor.getColumnIndexOrThrow("vorschriften_richtig")),
            vorschriftenGesamt = cursor.getInt(cursor.getColumnIndexOrThrow("vorschriften_gesamt")),
            isOverallPassed = cursor.getInt(cursor.getColumnIndexOrThrow("gesamt_bestanden")) == 1
        )
    }

    override suspend fun getAllExamHistory(): List<ExamHistoryEntry> = withContext(ioDispatcher) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<ExamHistoryEntry>()
        val query = "SELECT * FROM ExamHistory ORDER BY timestamp DESC"
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToExamHistoryEntry(it))
            }
        }
        list
    }

    override suspend fun importExamHistory(
        entries: List<ExamHistoryBackupDto>,
        mode: ImportMode
    ): Unit = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            if (mode == ImportMode.OVERWRITE) {
                db.delete("ExamWrongQuestions", null, null)
                db.delete("ExamHistory", null, null)
            }
            for (dto in entries) {
                val values = ContentValues().apply {
                    put("session_id", dto.sessionId)
                    put("timestamp", dto.timestamp)
                    put("duration_seconds", dto.durationSeconds)
                    put("technik_richtig", dto.technikRichtig)
                    put("technik_gesamt", dto.technikGesamt)
                    put("betrieb_richtig", dto.betriebRichtig)
                    put("betrieb_gesamt", dto.betriebGesamt)
                    put("vorschriften_richtig", dto.vorschriftenRichtig)
                    put("vorschriften_gesamt", dto.vorschriftenGesamt)
                    put("gesamt_bestanden", if (dto.gesamtBestanden) 1 else 0)
                }
                val conflictAlgorithm = if (mode == ImportMode.OVERWRITE) {
                    SQLiteDatabase.CONFLICT_REPLACE
                } else {
                    SQLiteDatabase.CONFLICT_IGNORE
                }
                db.insertWithOnConflict("ExamHistory", null, values, conflictAlgorithm)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _historyFlow.value = readExamHistoryFromDb(limit = 20)
    }

    override suspend fun clearExamHistory(): Unit = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("ExamWrongQuestions", null, null)
            db.delete("ExamHistory", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _historyFlow.value = emptyList()
    }
}
