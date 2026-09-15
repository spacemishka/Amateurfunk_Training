package com.spacemishka.app.amateurfunktraining.feature.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.data.ReferenceRepository
import com.spacemishka.app.amateurfunktraining.core.model.BandPlanEntry
import com.spacemishka.app.amateurfunktraining.core.model.CountryPrefix
import com.spacemishka.app.amateurfunktraining.core.model.Formula
import com.spacemishka.app.amateurfunktraining.core.model.FormulaCategory
import com.spacemishka.app.amateurfunktraining.core.model.QGroup
import com.spacemishka.app.amateurfunktraining.core.model.RegulationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReferenceTab(val title: String) {
    FORMULAS("Formeln"),
    Q_GROUPS("Q-Codes"),
    PREFIXES("Landeskenner"),
    BANDPLAN("Bandplan"),
    REGULATIONS("Vorschriften")
}

data class ReferenceHubUiState(
    val currentTab: ReferenceTab = ReferenceTab.FORMULAS,
    val searchQuery: String = "",
    val selectedFormulaCategory: FormulaCategory? = null,
    val qGroupShowAsQuestion: Boolean = true,
    val formulas: List<Formula> = emptyList(),
    val qGroups: List<QGroup> = emptyList(),
    val prefixes: List<CountryPrefix> = emptyList(),
    val bandPlan: List<BandPlanEntry> = emptyList(),
    val regulations: List<RegulationItem> = emptyList(),
    val isLoading: Boolean = false
)

class ReferenceHubViewModel(
    private val referenceRepository: ReferenceRepository,
    initialTab: ReferenceTab = ReferenceTab.FORMULAS
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferenceHubUiState(currentTab = initialTab))
    val uiState: StateFlow<ReferenceHubUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setTab(tab: ReferenceTab) {
        _uiState.update { it.copy(currentTab = tab) }
        loadCurrentTabData()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadCurrentTabData()
    }

    fun setFormulaCategory(category: FormulaCategory?) {
        _uiState.update { it.copy(selectedFormulaCategory = category) }
        loadFormulas()
    }

    fun toggleQGroupQuestionMode() {
        _uiState.update { it.copy(qGroupShowAsQuestion = !it.qGroupShowAsQuestion) }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allFormulas = referenceRepository.getAllFormulas()
            val allQ = referenceRepository.getQGroups()
            val allPrefixes = referenceRepository.getCountryPrefixes()
            val allBands = referenceRepository.getBandPlan()
            val allRegs = referenceRepository.getRegulations()

            _uiState.update {
                it.copy(
                    formulas = allFormulas,
                    qGroups = allQ,
                    prefixes = allPrefixes,
                    bandPlan = allBands,
                    regulations = allRegs,
                    isLoading = false
                )
            }
        }
    }

    private fun loadCurrentTabData() {
        when (_uiState.value.currentTab) {
            ReferenceTab.FORMULAS -> loadFormulas()
            ReferenceTab.Q_GROUPS -> loadQGroups()
            ReferenceTab.PREFIXES -> loadPrefixes()
            ReferenceTab.BANDPLAN -> loadBandPlan()
            ReferenceTab.REGULATIONS -> loadRegulations()
        }
    }

    private fun loadFormulas() {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val cat = _uiState.value.selectedFormulaCategory
            val result = referenceRepository.searchFormulas(query, cat)
            _uiState.update { it.copy(formulas = result) }
        }
    }

    private fun loadQGroups() {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val result = referenceRepository.searchQGroups(query)
            _uiState.update { it.copy(qGroups = result) }
        }
    }

    private fun loadPrefixes() {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val result = referenceRepository.searchCountryPrefixes(query)
            _uiState.update { it.copy(prefixes = result) }
        }
    }

    private fun loadBandPlan() {
        viewModelScope.launch {
            val all = referenceRepository.getBandPlan()
            val q = _uiState.value.searchQuery.trim()
            val filtered = if (q.isEmpty()) all else all.filter {
                it.band.contains(q, ignoreCase = true) ||
                        it.frequenzbereich.contains(q, ignoreCase = true) ||
                        it.bemerkung.contains(q, ignoreCase = true)
            }
            _uiState.update { it.copy(bandPlan = filtered) }
        }
    }

    private fun loadRegulations() {
        viewModelScope.launch {
            val all = referenceRepository.getRegulations()
            val q = _uiState.value.searchQuery.trim()
            val filtered = if (q.isEmpty()) all else all.filter {
                it.titel.contains(q, ignoreCase = true) ||
                        it.paragraph.contains(q, ignoreCase = true) ||
                        it.inhalt.contains(q, ignoreCase = true)
            }
            _uiState.update { it.copy(regulations = filtered) }
        }
    }
}
