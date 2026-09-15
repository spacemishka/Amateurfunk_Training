package com.spacemishka.app.amateurfunktraining.feature.calculator

import androidx.lifecycle.ViewModel
import com.spacemishka.app.amateurfunktraining.core.calculator.CalculatorEngine
import com.spacemishka.app.amateurfunktraining.core.calculator.PresetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CalculatorTab(val title: String) {
    BASIC("Taschenrechner"),
    PRESETS("Prüfungs-Formeln")
}

data class CalculatorUiState(
    val tab: CalculatorTab = CalculatorTab.PRESETS,
    // Basic calculator state
    val expression: String = "",
    val basicResult: String = "",
    val basicError: String? = null,

    // Preset calculator state
    val selectedPreset: PresetType = PresetType.OHM,
    val ohmTarget: String = "U", // U, R, or I
    val ohmU: String = "12",
    val ohmR: String = "100",
    val ohmI: String = "0.12",

    val powerTarget: String = "P", // P, U, I
    val powerP: String = "",
    val powerU: String = "13.8",
    val powerI: String = "5",
    val powerR: String = "",

    val waveTarget: String = "LAMBDA", // LAMBDA or FREQUENCY
    val waveFreqMHz: String = "145",
    val waveLambdaMeters: String = "2.07",

    val periodTarget: String = "T", // T or F
    val periodFreqHz: String = "1000",
    val periodSeconds: String = "",

    val dbIsPower: Boolean = true,
    val dbInVal1: String = "5",
    val dbOutVal2: String = "10",
    val dbValue: String = "",

    val dipoleFreqMHz: String = "28.5",

    val presetResult: CalculatorEngine.PresetResult? = null
)

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        runCurrentPresetCalculation()
    }

    fun setTab(tab: CalculatorTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    // --- Basic Calculator Actions ---

    fun onKey(key: String) {
        _uiState.update { state ->
            val expr = state.expression + key
            state.copy(
                expression = expr,
                basicError = null
            )
        }
    }

    fun onClear() {
        _uiState.update {
            it.copy(
                expression = "",
                basicResult = "",
                basicError = null
            )
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.expression.isNotEmpty()) {
                state.copy(
                    expression = state.expression.dropLast(1),
                    basicError = null
                )
            } else {
                state
            }
        }
    }

    fun onEvaluate() {
        val expr = _uiState.value.expression
        when (val res = CalculatorEngine.evaluateBasic(expr)) {
            is CalculatorEngine.EvalResult.Success -> {
                _uiState.update { it.copy(basicResult = res.formatted, basicError = null) }
            }
            is CalculatorEngine.EvalResult.Error -> {
                _uiState.update { it.copy(basicError = res.message) }
            }
        }
    }

    // --- Preset Actions ---

    fun selectPreset(type: PresetType) {
        _uiState.update { it.copy(selectedPreset = type) }
        runCurrentPresetCalculation()
    }

    fun updateOhm(target: String? = null, u: String? = null, r: String? = null, i: String? = null) {
        _uiState.update {
            it.copy(
                ohmTarget = target ?: it.ohmTarget,
                ohmU = u ?: it.ohmU,
                ohmR = r ?: it.ohmR,
                ohmI = i ?: it.ohmI
            )
        }
        runCurrentPresetCalculation()
    }

    fun updatePower(target: String? = null, p: String? = null, u: String? = null, i: String? = null, r: String? = null) {
        _uiState.update {
            it.copy(
                powerTarget = target ?: it.powerTarget,
                powerP = p ?: it.powerP,
                powerU = u ?: it.powerU,
                powerI = i ?: it.powerI,
                powerR = r ?: it.powerR
            )
        }
        runCurrentPresetCalculation()
    }

    fun updateWavelength(target: String? = null, fMHz: String? = null, lambda: String? = null) {
        _uiState.update {
            it.copy(
                waveTarget = target ?: it.waveTarget,
                waveFreqMHz = fMHz ?: it.waveFreqMHz,
                waveLambdaMeters = lambda ?: it.waveLambdaMeters
            )
        }
        runCurrentPresetCalculation()
    }

    fun updatePeriod(target: String? = null, fHz: String? = null, tSec: String? = null) {
        _uiState.update {
            it.copy(
                periodTarget = target ?: it.periodTarget,
                periodFreqHz = fHz ?: it.periodFreqHz,
                periodSeconds = tSec ?: it.periodSeconds
            )
        }
        runCurrentPresetCalculation()
    }

    fun updateDecibel(isPower: Boolean? = null, val1: String? = null, val2: String? = null, db: String? = null) {
        _uiState.update {
            it.copy(
                dbIsPower = isPower ?: it.dbIsPower,
                dbInVal1 = val1 ?: it.dbInVal1,
                dbOutVal2 = val2 ?: it.dbOutVal2,
                dbValue = db ?: it.dbValue
            )
        }
        runCurrentPresetCalculation()
    }

    fun updateDipole(fMHz: String) {
        _uiState.update { it.copy(dipoleFreqMHz = fMHz) }
        runCurrentPresetCalculation()
    }

    private fun runCurrentPresetCalculation() {
        val state = _uiState.value
        val result = when (state.selectedPreset) {
            PresetType.OHM -> {
                CalculatorEngine.calculateOhm(
                    target = state.ohmTarget,
                    uVolts = state.ohmU.toDoubleOrNull(),
                    rOhms = state.ohmR.toDoubleOrNull(),
                    iAmperes = state.ohmI.toDoubleOrNull()
                )
            }
            PresetType.POWER -> {
                CalculatorEngine.calculatePower(
                    target = state.powerTarget,
                    pWatts = state.powerP.toDoubleOrNull(),
                    uVolts = state.powerU.toDoubleOrNull(),
                    iAmperes = state.powerI.toDoubleOrNull(),
                    rOhms = state.powerR.toDoubleOrNull()
                )
            }
            PresetType.WAVELENGTH -> {
                val fHz = state.waveFreqMHz.toDoubleOrNull()?.let { it * 1_000_000.0 }
                val lMeters = state.waveLambdaMeters.toDoubleOrNull()
                CalculatorEngine.calculateWavelength(
                    target = state.waveTarget,
                    frequencyHz = fHz,
                    wavelengthMeters = lMeters
                )
            }
            PresetType.PERIOD -> {
                CalculatorEngine.calculatePeriod(
                    target = state.periodTarget,
                    periodSeconds = state.periodSeconds.toDoubleOrNull(),
                    frequencyHz = state.periodFreqHz.toDoubleOrNull()
                )
            }
            PresetType.DECIBEL -> {
                CalculatorEngine.calculateDecibel(
                    isPower = state.dbIsPower,
                    inVal1 = state.dbInVal1.toDoubleOrNull(),
                    outVal2 = state.dbOutVal2.toDoubleOrNull(),
                    dbValue = state.dbValue.toDoubleOrNull()
                )
            }
            PresetType.DIPOLE -> {
                CalculatorEngine.calculateDipole(state.dipoleFreqMHz.toDoubleOrNull())
            }
        }
        _uiState.update { it.copy(presetResult = result) }
    }
}
