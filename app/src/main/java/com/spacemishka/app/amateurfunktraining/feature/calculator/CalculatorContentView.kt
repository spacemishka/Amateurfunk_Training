package com.spacemishka.app.amateurfunktraining.feature.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.core.calculator.CalculatorEngine
import com.spacemishka.app.amateurfunktraining.core.calculator.PresetType
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed

@Composable
fun CalculatorContentView(
    uiState: CalculatorUiState,
    onTabSelected: (CalculatorTab) -> Unit,
    onKey: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit,
    onSelectPreset: (PresetType) -> Unit,
    onUpdateOhm: (target: String?, u: String?, r: String?, i: String?) -> Unit,
    onUpdatePower: (target: String?, p: String?, u: String?, i: String?, r: String?) -> Unit,
    onUpdateWavelength: (target: String?, fMHz: String?, lambda: String?) -> Unit,
    onUpdatePeriod: (target: String?, fHz: String?, tSec: String?) -> Unit,
    onUpdateDecibel: (isPower: Boolean?, val1: String?, val2: String?, db: String?) -> Unit,
    onUpdateDipole: (fMHz: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PrimaryTabRow(
            selectedTabIndex = uiState.tab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RadioBlue
        ) {
            CalculatorTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.tab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (uiState.tab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (uiState.tab) {
            CalculatorTab.PRESETS -> {
                PresetCalculatorView(
                    uiState = uiState,
                    onSelectPreset = onSelectPreset,
                    onUpdateOhm = onUpdateOhm,
                    onUpdatePower = onUpdatePower,
                    onUpdateWavelength = onUpdateWavelength,
                    onUpdatePeriod = onUpdatePeriod,
                    onUpdateDecibel = onUpdateDecibel,
                    onUpdateDipole = onUpdateDipole
                )
            }
            CalculatorTab.BASIC -> {
                BasicCalculatorView(
                    expression = uiState.expression,
                    result = uiState.basicResult,
                    error = uiState.basicError,
                    onKey = onKey,
                    onClear = onClear,
                    onBackspace = onBackspace,
                    onEvaluate = onEvaluate
                )
            }
        }
    }
}

@Composable
private fun PresetCalculatorView(
    uiState: CalculatorUiState,
    onSelectPreset: (PresetType) -> Unit,
    onUpdateOhm: (target: String?, u: String?, r: String?, i: String?) -> Unit,
    onUpdatePower: (target: String?, p: String?, u: String?, i: String?, r: String?) -> Unit,
    onUpdateWavelength: (target: String?, fMHz: String?, lambda: String?) -> Unit,
    onUpdatePeriod: (target: String?, fHz: String?, tSec: String?) -> Unit,
    onUpdateDecibel: (isPower: Boolean?, val1: String?, val2: String?, db: String?) -> Unit,
    onUpdateDipole: (fMHz: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Preset selector chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetType.entries.forEach { preset ->
                    FilterChip(
                        selected = uiState.selectedPreset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadioBlue.copy(alpha = 0.2f),
                            selectedLabelColor = RadioBlue
                        )
                    )
                }
            }
        }

        // Selected Preset Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${uiState.selectedPreset.displayName} (${uiState.selectedPreset.formulaStr})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    when (uiState.selectedPreset) {
                        PresetType.OHM -> {
                            TargetSelector(
                                options = listOf("U" to "Spannung (U)", "R" to "Widerstand (R)", "I" to "Strom (I)"),
                                selected = uiState.ohmTarget,
                                onSelect = { onUpdateOhm(it, null, null, null) }
                            )

                            if (uiState.ohmTarget != "U") {
                                PresetInputField(label = "Spannung U [V]", value = uiState.ohmU) {
                                    onUpdateOhm(null, it, null, null)
                                }
                            }
                            if (uiState.ohmTarget != "R") {
                                PresetInputField(label = "Widerstand R [Ω]", value = uiState.ohmR) {
                                    onUpdateOhm(null, null, it, null)
                                }
                            }
                            if (uiState.ohmTarget != "I") {
                                PresetInputField(label = "Stromstärke I [A]", value = uiState.ohmI) {
                                    onUpdateOhm(null, null, null, it)
                                }
                            }
                        }

                        PresetType.POWER -> {
                            TargetSelector(
                                options = listOf("P" to "Leistung (P)", "U" to "Spannung (U)", "I" to "Strom (I)"),
                                selected = uiState.powerTarget,
                                onSelect = { onUpdatePower(it, null, null, null, null) }
                            )

                            if (uiState.powerTarget != "P") {
                                PresetInputField(label = "Leistung P [W]", value = uiState.powerP) {
                                    onUpdatePower(null, it, null, null, null)
                                }
                            }
                            PresetInputField(label = "Spannung U [V]", value = uiState.powerU) {
                                onUpdatePower(null, null, it, null, null)
                            }
                            PresetInputField(label = "Stromstärke I [A]", value = uiState.powerI) {
                                onUpdatePower(null, null, null, it, null)
                            }
                        }

                        PresetType.WAVELENGTH -> {
                            TargetSelector(
                                options = listOf("LAMBDA" to "Wellenlänge λ [m]", "FREQUENCY" to "Frequenz f [MHz]"),
                                selected = uiState.waveTarget,
                                onSelect = { onUpdateWavelength(it, null, null) }
                            )

                            if (uiState.waveTarget == "LAMBDA") {
                                PresetInputField(label = "Frequenz f [MHz]", value = uiState.waveFreqMHz) {
                                    onUpdateWavelength(null, it, null)
                                }
                            } else {
                                PresetInputField(label = "Wellenlänge λ [m]", value = uiState.waveLambdaMeters) {
                                    onUpdateWavelength(null, null, it)
                                }
                            }
                        }

                        PresetType.PERIOD -> {
                            TargetSelector(
                                options = listOf("T" to "Periodendauer T [s]", "F" to "Frequenz f [Hz]"),
                                selected = uiState.periodTarget,
                                onSelect = { onUpdatePeriod(it, null, null) }
                            )

                            if (uiState.periodTarget == "T") {
                                PresetInputField(label = "Frequenz f [Hz]", value = uiState.periodFreqHz) {
                                    onUpdatePeriod(null, it, null)
                                }
                            } else {
                                PresetInputField(label = "Periodendauer T [s]", value = uiState.periodSeconds) {
                                    onUpdatePeriod(null, null, it)
                                }
                            }
                        }

                        PresetType.DECIBEL -> {
                            TargetSelector(
                                options = listOf("true" to "Leistung (10·lg)", "false" to "Spannung (20·lg)"),
                                selected = uiState.dbIsPower.toString(),
                                onSelect = { onUpdateDecibel(it.toBoolean(), null, null, null) }
                            )

                            PresetInputField(
                                label = if (uiState.dbIsPower) "Eingangsleistung P1 [W]" else "Eingangsspannung U1 [V]",
                                value = uiState.dbInVal1
                            ) {
                                onUpdateDecibel(null, it, null, null)
                            }

                            PresetInputField(
                                label = if (uiState.dbIsPower) "Ausgangsleistung P2 [W]" else "Ausgangsspannung U2 [V]",
                                value = uiState.dbOutVal2
                            ) {
                                onUpdateDecibel(null, null, it, null)
                            }
                        }

                        PresetType.DIPOLE -> {
                            PresetInputField(label = "Frequenz f [MHz]", value = uiState.dipoleFreqMHz) {
                                onUpdateDipole(it)
                            }
                        }
                    }
                }
            }
        }

        // Result Box
        item {
            when (val res = uiState.presetResult) {
                is CalculatorEngine.PresetResult.Success -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Berechnungsergebnis:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "${CalculatorEngine.formatNumber(res.resultValue)} ${res.resultUnit}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = res.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                is CalculatorEngine.PresetResult.Error -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WrongRed.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ ${res.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WrongRed,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                null -> {}
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BasicCalculatorView(
    expression: String,
    result: String,
    error: String?,
    onKey: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                if (result.isNotEmpty()) {
                    Text(
                        text = "= $result",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = RadioBlue
                        )
                    )
                }

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = WrongRed
                    )
                }
            }
        }

        // Keypad Grid
        val buttonRows = listOf(
            listOf("C", "DEL", "(", ")"),
            listOf("sqrt(", "^", "log(", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "±", "=")
        )

        buttonRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { label ->
                    val isAction = label in listOf("C", "DEL", "=")
                    val isOp = label in listOf("÷", "×", "-", "+", "(", ")", "^", "sqrt(", "log(")

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    label == "=" -> RadioBlue
                                    isAction -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    isOp -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                            .clickable {
                                when (label) {
                                    "C" -> onClear()
                                    "DEL" -> onBackspace()
                                    "=" -> onEvaluate()
                                    "±" -> onKey("-")
                                    else -> onKey(label)
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = when (label) {
                                    "C" -> "Löschen"
                                    "DEL" -> "Rücktaste"
                                    "=" -> "Ist gleich"
                                    "÷" -> "Geteilt durch"
                                    "×" -> "Mal"
                                    "sqrt(" -> "Wurzel"
                                    "log(" -> "Logarithmus"
                                    else -> label
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (label == "DEL") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (label.length > 2) 14.sp else 18.sp
                                ),
                                color = if (label == "=") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetSelector(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Gesuchte Zielgröße:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = selected.equals(key, ignoreCase = true)
                Button(
                    onClick = { onSelect(key) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) RadioBlue else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    )
}
