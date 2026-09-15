package com.spacemishka.app.amateurfunktraining.core.data

import android.content.Context
import com.spacemishka.app.amateurfunktraining.core.model.BandPlanEntry
import com.spacemishka.app.amateurfunktraining.core.model.CountryPrefix
import com.spacemishka.app.amateurfunktraining.core.model.Formula
import com.spacemishka.app.amateurfunktraining.core.model.FormulaCategory
import com.spacemishka.app.amateurfunktraining.core.model.FormulaDto
import com.spacemishka.app.amateurfunktraining.core.model.PhoneticEntry
import com.spacemishka.app.amateurfunktraining.core.model.QGroup
import com.spacemishka.app.amateurfunktraining.core.model.ReferenceDataDto
import com.spacemishka.app.amateurfunktraining.core.model.RegulationItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

interface ReferenceRepository {
    suspend fun getAllFormulas(): List<Formula>
    suspend fun searchFormulas(query: String, category: FormulaCategory? = null): List<Formula>
    suspend fun getQGroups(): List<QGroup>
    suspend fun searchQGroups(query: String): List<QGroup>
    suspend fun getCountryPrefixes(): List<CountryPrefix>
    suspend fun searchCountryPrefixes(query: String): List<CountryPrefix>
    suspend fun getBandPlan(): List<BandPlanEntry>
    suspend fun getRegulations(): List<RegulationItem>
    suspend fun getItuAlphabet(): List<PhoneticEntry>
}

class AssetReferenceRepository(
    private val context: Context? = null,
    private val formulasJsonProvider: (() -> String)? = null,
    private val referenceJsonProvider: (() -> String)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReferenceRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var cachedFormulas: List<Formula>? = null

    @Volatile
    private var cachedReferenceData: ReferenceDataDto? = null

    override suspend fun getAllFormulas(): List<Formula> = withContext(ioDispatcher) {
        cachedFormulas ?: loadFormulas().also { cachedFormulas = it }
    }

    override suspend fun searchFormulas(query: String, category: FormulaCategory?): List<Formula> = withContext(ioDispatcher) {
        val all = getAllFormulas()
        val filteredByCategory = if (category != null) {
            all.filter { it.category == category }
        } else {
            all
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext filteredByCategory

        filteredByCategory.filter { f ->
            f.title.contains(trimmed, ignoreCase = true) ||
                    f.formula.contains(trimmed, ignoreCase = true) ||
                    f.description.contains(trimmed, ignoreCase = true) ||
                    f.example.contains(trimmed, ignoreCase = true) ||
                    f.examTip.contains(trimmed, ignoreCase = true) ||
                    f.variables.any { it.name.contains(trimmed, ignoreCase = true) || it.symbol.contains(trimmed, ignoreCase = true) }
        }
    }

    override suspend fun getQGroups(): List<QGroup> = withContext(ioDispatcher) {
        getReferenceData().qGruppen
    }

    override suspend fun searchQGroups(query: String): List<QGroup> = withContext(ioDispatcher) {
        val all = getQGroups()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext all

        all.filter { q ->
            q.code.contains(trimmed, ignoreCase = true) ||
                    q.frage.contains(trimmed, ignoreCase = true) ||
                    q.antwort.contains(trimmed, ignoreCase = true) ||
                    q.bedeutung.contains(trimmed, ignoreCase = true) ||
                    q.kategorie.contains(trimmed, ignoreCase = true)
        }
    }

    override suspend fun getCountryPrefixes(): List<CountryPrefix> = withContext(ioDispatcher) {
        getReferenceData().landeskenner
    }

    override suspend fun searchCountryPrefixes(query: String): List<CountryPrefix> = withContext(ioDispatcher) {
        val all = getCountryPrefixes()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext all

        all.filter { cp ->
            cp.praefix.contains(trimmed, ignoreCase = true) ||
                    cp.land.contains(trimmed, ignoreCase = true) ||
                    cp.kontinent.contains(trimmed, ignoreCase = true)
        }
    }

    override suspend fun getBandPlan(): List<BandPlanEntry> = withContext(ioDispatcher) {
        getReferenceData().bandplanKlasseE
    }

    override suspend fun getRegulations(): List<RegulationItem> = withContext(ioDispatcher) {
        getReferenceData().gesetzeUndVorschriften
    }

    override suspend fun getItuAlphabet(): List<PhoneticEntry> = withContext(ioDispatcher) {
        getReferenceData().ituAlphabet
    }

    private fun loadFormulas(): List<Formula> {
        val jsonString = formulasJsonProvider?.invoke() ?: run {
            val inputStream: InputStream = context!!.assets.open("formulas_klasse_e.json")
            inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val dtos: List<FormulaDto> = json.decodeFromString(jsonString)
        return dtos.map { it.toDomain() }
    }

    private fun getReferenceData(): ReferenceDataDto {
        return cachedReferenceData ?: synchronized(this) {
            cachedReferenceData ?: run {
                val jsonString = referenceJsonProvider?.invoke() ?: run {
                    val inputStream: InputStream = context!!.assets.open("reference_data.json")
                    inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                val data: ReferenceDataDto = json.decodeFromString(jsonString)
                cachedReferenceData = data
                data
            }
        }
    }
}
