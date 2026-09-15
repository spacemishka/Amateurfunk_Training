package com.spacemishka.app.amateurfunktraining.core.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import com.spacemishka.app.amateurfunktraining.core.model.TopicDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetTopicRepository(
    private val context: Context? = null,
    private val dbHelper: AmateurfunkDbHelper? = if (context != null) AmateurfunkDbHelper(context) else null,
    private val jsonContentProvider: (() -> String)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TopicRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedTopics: List<Topic>? = null
    private var cachedTopicMap: Map<String, Topic>? = null
    private val mutex = Mutex()

    private val recentTopicsFlow = MutableStateFlow<List<Topic>>(emptyList())
    private val inMemoryHistory = mutableMapOf<String, Long>()

    private suspend fun loadTopics(): List<Topic> = withContext(ioDispatcher) {
        mutex.withLock {
            cachedTopics?.let { return@withContext it }

            val rawJson = when {
                jsonContentProvider != null -> jsonContentProvider.invoke()
                context != null -> {
                    context.assets.open("topics_klasse_e.json")
                        .bufferedReader()
                        .use { it.readText() }
                }
                else -> error("Neither Context nor jsonContentProvider provided to AssetTopicRepository")
            }

            val dtoList = json.decodeFromString<List<TopicDto>>(rawJson)
            val domainList = dtoList.map { it.toDomain() }
            cachedTopics = domainList
            cachedTopicMap = domainList.associateBy { it.id.lowercase() }

            refreshRecentTopicsInternal()

            domainList
        }
    }

    private suspend fun getTopicMap(): Map<String, Topic> {
        if (cachedTopicMap == null) {
            loadTopics()
        }
        return cachedTopicMap ?: emptyMap()
    }

    override suspend fun getAllTopics(): List<Topic> {
        return loadTopics()
    }

    override suspend fun getTopicById(id: String): Topic? {
        val map = getTopicMap()
        return map[id.lowercase()]
    }

    override suspend fun searchTopics(query: String, category: Category?): List<Topic> {
        val all = loadTopics()
        val trimmed = query.trim().lowercase()

        return all.filter { topic ->
            val matchesCategory = category == null || category == Category.ALL || topic.category == category
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                topic.title.lowercase().contains(trimmed) ||
                        topic.keyTakeaway.lowercase().contains(trimmed) ||
                        topic.explanation.lowercase().contains(trimmed) ||
                        (topic.mnemonic?.lowercase()?.contains(trimmed) == true) ||
                        (topic.formula?.lowercase()?.contains(trimmed) == true)
            }
            matchesCategory && matchesQuery
        }
    }

    override fun getRecentlyViewedTopicsStream(): Flow<List<Topic>> {
        return recentTopicsFlow.asStateFlow()
    }

    private var lastHistoryTimestamp = 0L

    override suspend fun markTopicAsViewed(topicId: String) = withContext(ioDispatcher) {
        val now = synchronized(this@AssetTopicRepository) {
            val current = System.currentTimeMillis()
            val ts = if (current <= lastHistoryTimestamp) lastHistoryTimestamp + 1 else current
            lastHistoryTimestamp = ts
            ts
        }
        if (dbHelper != null) {
            try {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("topic_id", topicId)
                    put("gelesen_am", now)
                }
                db.insertWithOnConflict("TopicHistory", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            } catch (e: Exception) {
                // Fallback to in-memory if DB fails
                inMemoryHistory[topicId] = now
            }
        } else {
            inMemoryHistory[topicId] = now
        }

        refreshRecentTopicsInternal()
    }

    private suspend fun refreshRecentTopicsInternal() {
        val topicMap = getTopicMap()
        val recentIds = mutableListOf<String>()

        if (dbHelper != null) {
            try {
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery(
                    "SELECT topic_id FROM TopicHistory ORDER BY gelesen_am DESC LIMIT 10",
                    null
                )
                cursor.use {
                    while (it.moveToNext()) {
                        recentIds.add(it.getString(0))
                    }
                }
            } catch (e: Exception) {
                recentIds.clear()
                recentIds.addAll(
                    inMemoryHistory.entries.sortedByDescending { it.value }.map { it.key }.take(10)
                )
            }
        } else {
            recentIds.addAll(
                inMemoryHistory.entries.sortedByDescending { it.value }.map { it.key }.take(10)
            )
        }

        val recentTopics = recentIds.mapNotNull { topicMap[it.lowercase()] }
        recentTopicsFlow.value = recentTopics
    }

    override suspend fun getAllReadTopicIds(): List<String> = withContext(ioDispatcher) {
        val ids = mutableListOf<String>()
        if (dbHelper != null) {
            try {
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery("SELECT topic_id FROM TopicHistory ORDER BY gelesen_am DESC", null)
                cursor.use {
                    while (it.moveToNext()) {
                        ids.add(it.getString(0))
                    }
                }
            } catch (_: Exception) {
                ids.addAll(inMemoryHistory.keys)
            }
        } else {
            ids.addAll(inMemoryHistory.keys)
        }
        ids
    }

    override suspend fun importReadTopicIds(
        topicIds: List<String>,
        mode: ImportMode
    ): Unit = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        if (dbHelper != null) {
            try {
                val db = dbHelper.writableDatabase
                db.beginTransaction()
                try {
                    if (mode == ImportMode.OVERWRITE) {
                        db.delete("TopicHistory", null, null)
                        inMemoryHistory.clear()
                    }
                    for (topicId in topicIds) {
                        val values = ContentValues().apply {
                            put("topic_id", topicId)
                            put("gelesen_am", now)
                        }
                        db.insertWithOnConflict(
                            "TopicHistory",
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_IGNORE
                        )
                        inMemoryHistory[topicId] = now
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (_: Exception) {
                if (mode == ImportMode.OVERWRITE) inMemoryHistory.clear()
                for (topicId in topicIds) inMemoryHistory[topicId] = now
            }
        } else {
            if (mode == ImportMode.OVERWRITE) inMemoryHistory.clear()
            for (topicId in topicIds) inMemoryHistory[topicId] = now
        }
        refreshRecentTopicsInternal()
    }

    override suspend fun clearTopicHistory(): Unit = withContext(ioDispatcher) {
        if (dbHelper != null) {
            try {
                val db = dbHelper.writableDatabase
                db.beginTransaction()
                try {
                    db.delete("TopicHistory", null, null)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (_: Exception) {}
        }
        inMemoryHistory.clear()
        recentTopicsFlow.value = emptyList()
    }
}
