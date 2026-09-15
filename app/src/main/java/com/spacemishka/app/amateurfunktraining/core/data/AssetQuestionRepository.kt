package com.spacemishka.app.amateurfunktraining.core.data

import android.content.Context
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.core.model.QuestionDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetQuestionRepository(
    private val context: Context? = null,
    private val jsonContentProvider: (() -> String)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : QuestionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedQuestions: List<Question>? = null
    private val mutex = Mutex()

    private suspend fun loadQuestions(): List<Question> = withContext(ioDispatcher) {
        mutex.withLock {
            cachedQuestions?.let { return@withContext it }

            val rawJson = when {
                jsonContentProvider != null -> jsonContentProvider.invoke()
                context != null -> {
                    context.assets.open("questions_klasse_e.json")
                        .bufferedReader()
                        .use { it.readText() }
                }
                else -> error("Neither Context nor jsonContentProvider provided to AssetQuestionRepository")
            }

            val dtoList = json.decodeFromString<List<QuestionDto>>(rawJson)
            val domainList = dtoList.map { it.toDomain() }
            cachedQuestions = domainList
            domainList
        }
    }

    override suspend fun getAllQuestions(): List<Question> {
        return loadQuestions()
    }

    override suspend fun getQuestionsByCategory(category: Category): List<Question> {
        val all = loadQuestions()
        return if (category == Category.ALL) {
            all
        } else {
            all.filter { it.category == category }
        }
    }

    override suspend fun getQuestionById(id: String): Question? {
        return loadQuestions().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    override suspend fun getQuestionsByTopic(topicId: String): List<Question> {
        val target = topicId.lowercase()
        return loadQuestions().filter { it.topicId.equals(target, ignoreCase = true) }
    }

    override suspend fun getCategoryCounts(): Map<Category, Int> {
        val all = loadQuestions()
        return mapOf(
            Category.ALL to all.size,
            Category.TECHNIK to all.count { it.category == Category.TECHNIK },
            Category.BETRIEB to all.count { it.category == Category.BETRIEB },
            Category.VORSCHRIFTEN to all.count { it.category == Category.VORSCHRIFTEN }
        )
    }
}
