package com.spacemishka.app.amateurfunktraining.core.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.spacemishka.app.amateurfunktraining.core.leitner.LeitnerCalculator
import com.spacemishka.app.amateurfunktraining.core.leitner.StreakManager
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.ProgressStatus
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgressBackupDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class SqliteProgressRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    databaseName: String = "amateurfunk_progress.db"
) : ProgressRepository {

    private val dbHelper = AmateurfunkDbHelper(context, databaseName)
    private val _allProgressFlow = MutableStateFlow<Map<String, QuestionProgress>>(emptyMap())
    private val _streakFlow = MutableStateFlow(0)

    init {
        // Initial load on launch
        loadInitialData()
    }

    private fun loadInitialData() {
        try {
            val loadedMap = readAllProgressFromDb()
            _allProgressFlow.value = loadedMap
            val (lastDate, storedStreak) = readStreakFromDb()
            _streakFlow.value = StreakManager.computeDisplayStreak(lastDate, storedStreak)
        } catch (_: Exception) {
            // Fallback for non-Android / early init environments
        }
    }

    override fun getProgressStream(questionId: String): Flow<QuestionProgress?> {
        return _allProgressFlow.map { it[questionId] }
    }

    override fun getAllProgressStream(): Flow<List<QuestionProgress>> {
        return _allProgressFlow.map { it.values.toList() }
    }

    override suspend fun getProgressForQuestion(questionId: String): QuestionProgress? = withContext(ioDispatcher) {
        _allProgressFlow.value[questionId] ?: readSingleProgressFromDb(questionId)
    }

    override suspend fun recordAnswer(
        questionId: String,
        isCorrect: Boolean,
        timestamp: Long
    ) = withContext(ioDispatcher) {
        val currentProgress = getProgressForQuestion(questionId)
        val updatedProgress = LeitnerCalculator.calculateNextState(
            questionId = questionId,
            current = currentProgress,
            isCorrect = isCorrect,
            timestamp = timestamp
        )

        saveProgressToDb(updatedProgress)

        // Update streak
        val (lastDate, storedStreak) = readStreakFromDb()
        val streakUpdate = StreakManager.onPracticeCompleted(
            lastPracticeDateString = lastDate,
            currentStreak = storedStreak
        )

        if (streakUpdate.hasChanged || lastDate != streakUpdate.newDateString) {
            saveStreakToDb(streakUpdate.newDateString, streakUpdate.newStreak)
            _streakFlow.value = streakUpdate.newStreak
        } else {
            _streakFlow.value = streakUpdate.newStreak
        }

        _allProgressFlow.update { currentMap ->
            currentMap + (questionId to updatedProgress)
        }
    }

    override suspend fun toggleBookmark(questionId: String): Boolean = withContext(ioDispatcher) {
        val current = getProgressForQuestion(questionId)
        val currentBookmark = current?.isBookmarked ?: false
        val newBookmark = !currentBookmark

        val updated = current?.copy(isBookmarked = newBookmark)
            ?: QuestionProgress(
                questionId = questionId,
                status = ProgressStatus.NEU,
                isBookmarked = newBookmark
            )

        saveProgressToDb(updated)

        _allProgressFlow.update { currentMap ->
            currentMap + (questionId to updated)
        }
        newBookmark
    }

    override suspend fun getProblemQuestionIds(): List<String> = withContext(ioDispatcher) {
        _allProgressFlow.value.values
            .filter { it.isProblemQuestion }
            .sortedByDescending { it.errorCount }
            .map { it.questionId }
    }

    override suspend fun getBookmarkedQuestionIds(): List<String> = withContext(ioDispatcher) {
        _allProgressFlow.value.values
            .filter { it.isBookmarked }
            .map { it.questionId }
    }

    override suspend fun getDueLeitnerQuestionIds(currentTimestamp: Long): List<String> = withContext(ioDispatcher) {
        _allProgressFlow.value.values
            .filter { LeitnerCalculator.isDue(it, currentTimestamp) }
            .map { it.questionId }
    }

    override fun getStreakStream(): Flow<Int> {
        return _streakFlow.asStateFlow()
    }

    // --- Database Helper Methods ---

    private fun readAllProgressFromDb(): Map<String, QuestionProgress> {
        val db = dbHelper.readableDatabase
        val map = mutableMapOf<String, QuestionProgress>()
        val cursor = db.rawQuery("SELECT * FROM QuestionProgress", null)
        cursor.use {
            while (it.moveToNext()) {
                val progress = cursorToQuestionProgress(it)
                map[progress.questionId] = progress
            }
        }
        return map
    }

    private fun readSingleProgressFromDb(questionId: String): QuestionProgress? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM QuestionProgress WHERE frage_id = ?", arrayOf(questionId))
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToQuestionProgress(it)
            }
        }
        return null
    }

    private fun saveProgressToDb(progress: QuestionProgress) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("frage_id", progress.questionId)
            put("status", progress.status.name)
            put("fehlerzaehler", progress.errorCount)
            put("letzte_antwort", progress.lastAnsweredTimestamp)
            put("leitner_box", progress.leitnerBox)
            put("ist_lesezeichen", if (progress.isBookmarked) 1 else 0)
        }
        db.insertWithOnConflict("QuestionProgress", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun readStreakFromDb(): Pair<String?, Int> {
        val db = dbHelper.readableDatabase
        var lastDate: String? = null
        var streak = 0

        val cursor = db.rawQuery("SELECT key, value FROM UserMeta WHERE key IN (?, ?)", arrayOf(KEY_LAST_PRACTICE_DATE, KEY_STREAK))
        cursor.use {
            while (it.moveToNext()) {
                val key = it.getString(0)
                val value = it.getString(1)
                when (key) {
                    KEY_LAST_PRACTICE_DATE -> lastDate = value
                    KEY_STREAK -> streak = value.toIntOrNull() ?: 0
                }
            }
        }
        return Pair(lastDate, streak)
    }

    private fun saveStreakToDb(dateStr: String, streak: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val v1 = ContentValues().apply {
                put("key", KEY_LAST_PRACTICE_DATE)
                put("value", dateStr)
            }
            db.insertWithOnConflict("UserMeta", null, v1, SQLiteDatabase.CONFLICT_REPLACE)

            val v2 = ContentValues().apply {
                put("key", KEY_STREAK)
                put("value", streak.toString())
            }
            db.insertWithOnConflict("UserMeta", null, v2, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun cursorToQuestionProgress(cursor: Cursor): QuestionProgress {
        val questionId = cursor.getString(cursor.getColumnIndexOrThrow("frage_id"))
        val statusStr = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        val errorCount = cursor.getInt(cursor.getColumnIndexOrThrow("fehlerzaehler"))
        val lastTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("letzte_antwort"))
        val leitnerBox = cursor.getInt(cursor.getColumnIndexOrThrow("leitner_box"))
        val isBookmarked = cursor.getInt(cursor.getColumnIndexOrThrow("ist_lesezeichen")) == 1

        val status = try {
            ProgressStatus.valueOf(statusStr)
        } catch (_: Exception) {
            ProgressStatus.IN_BEARBEITUNG
        }

        return QuestionProgress(
            questionId = questionId,
            status = status,
            errorCount = errorCount,
            lastAnsweredTimestamp = lastTimestamp,
            leitnerBox = leitnerBox,
            isBookmarked = isBookmarked
        )
    }

    override suspend fun getAllProgress(): List<QuestionProgress> = withContext(ioDispatcher) {
        _allProgressFlow.value.values.toList()
    }

    override suspend fun importProgress(
        items: List<QuestionProgressBackupDto>,
        mode: ImportMode
    ): Unit = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            if (mode == ImportMode.OVERWRITE) {
                db.delete("QuestionProgress", null, null)
                for (item in items) {
                    val values = ContentValues().apply {
                        put("frage_id", item.frageId)
                        put("status", item.status)
                        put("fehlerzaehler", item.fehlerzaehler)
                        put("letzte_antwort", item.letzteAntwort)
                        put("leitner_box", item.leitnerBox)
                        put("ist_lesezeichen", if (item.istLesezeichen) 1 else 0)
                    }
                    db.insertWithOnConflict("QuestionProgress", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            } else {
                val currentMap = readAllProgressFromDb()
                for (item in items) {
                    val existing = currentMap[item.frageId]
                    val mergedBox = if (existing != null) maxOf(existing.leitnerBox, item.leitnerBox) else item.leitnerBox
                    val mergedErrors = if (existing != null) maxOf(existing.errorCount, item.fehlerzaehler) else item.fehlerzaehler
                    val mergedTime = if (existing != null) maxOf(existing.lastAnsweredTimestamp, item.letzteAntwort) else item.letzteAntwort
                    val mergedBookmark = (existing?.isBookmarked == true) || item.istLesezeichen
                    val mergedStatus = if (mergedBox >= 5) {
                        ProgressStatus.GEMEISTERT.name
                    } else if (mergedBox > 1 || mergedErrors > 0 || mergedTime > 0) {
                        ProgressStatus.IN_BEARBEITUNG.name
                    } else {
                        item.status
                    }

                    val values = ContentValues().apply {
                        put("frage_id", item.frageId)
                        put("status", mergedStatus)
                        put("fehlerzaehler", mergedErrors)
                        put("letzte_antwort", mergedTime)
                        put("leitner_box", mergedBox)
                        put("ist_lesezeichen", if (mergedBookmark) 1 else 0)
                    }
                    db.insertWithOnConflict("QuestionProgress", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        _allProgressFlow.value = readAllProgressFromDb()
    }

    override suspend fun getStreakData(): Pair<String?, Int> = withContext(ioDispatcher) {
        readStreakFromDb()
    }

    override suspend fun setStreakData(
        lastDate: String?,
        streak: Int,
        mode: ImportMode
    ): Unit = withContext(ioDispatcher) {
        val (currentDate, currentStreak) = readStreakFromDb()
        val (targetDate, targetStreak) = if (mode == ImportMode.OVERWRITE) {
            Pair(lastDate, streak)
        } else {
            if (streak > currentStreak || (currentDate == null && lastDate != null)) {
                Pair(lastDate, maxOf(currentStreak, streak))
            } else {
                Pair(currentDate, currentStreak)
            }
        }
        if (targetDate != null) {
            saveStreakToDb(targetDate, targetStreak)
        }
        _streakFlow.value = StreakManager.computeDisplayStreak(targetDate, targetStreak)
    }

    override suspend fun clearAllProgress(): Unit = withContext(ioDispatcher) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("QuestionProgress", null, null)
            db.delete("UserMeta", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _allProgressFlow.value = emptyMap()
        _streakFlow.value = 0
    }

    companion object {
        private const val KEY_LAST_PRACTICE_DATE = "last_practice_date"
        private const val KEY_STREAK = "user_streak"
    }
}
