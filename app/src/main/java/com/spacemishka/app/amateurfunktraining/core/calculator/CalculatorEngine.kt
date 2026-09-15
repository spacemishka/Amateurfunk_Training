package com.spacemishka.app.amateurfunktraining.core.calculator

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class PresetType(val displayName: String, val formulaStr: String) {
    OHM("Ohmsches Gesetz", "U = R · I"),
    POWER("Elektrische Leistung", "P = U · I"),
    WAVELENGTH("Wellenlänge / Frequenz", "λ = c / f"),
    PERIOD("Periodendauer", "T = 1 / f"),
    DECIBEL("Dezibel (dB)", "dB = 10·lg(P₂/P₁)"),
    DIPOLE("λ/2-Dipol", "L ≈ 142,5 / f [MHz]")
}

object CalculatorEngine {

    // --- Standard Basic Calculator Eval ---

    sealed interface EvalResult {
        data class Success(val value: Double, val formatted: String) : EvalResult
        data class Error(val message: String) : EvalResult
    }

    fun evaluateBasic(expression: String): EvalResult {
        return try {
            val cleaned = expression.replace(" ", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace(",", ".")
            if (cleaned.isBlank()) return EvalResult.Success(0.0, "0")

            val result = SimpleParser(cleaned).parse()
            if (result.isNaN() || result.isInfinite()) {
                EvalResult.Error("Undefiniert / Division durch 0")
            } else {
                EvalResult.Success(result, formatNumber(result))
            }
        } catch (e: Exception) {
            EvalResult.Error(e.message ?: "Ungültiger Ausdruck")
        }
    }

    // --- Preset Calculations ---

    // 1. Ohmsches Gesetz: target is what we want to calculate ("U", "R", "I")
    fun calculateOhm(
        target: String,
        uVolts: Double?,
        rOhms: Double?,
        iAmperes: Double?
    ): PresetResult {
        return when (target.uppercase(Locale.ROOT)) {
            "U" -> {
                if (rOhms == null || iAmperes == null) return PresetResult.Error("Bitte R und I eingeben.")
                val u = rOhms * iAmperes
                PresetResult.Success(
                    resultValue = u,
                    resultUnit = "V",
                    explanation = "U = R · I = ${formatNumber(rOhms)} Ω · ${formatNumber(iAmperes)} A = ${formatNumber(u)} V"
                )
            }
            "R" -> {
                if (uVolts == null || iAmperes == null) return PresetResult.Error("Bitte U und I eingeben.")
                if (iAmperes == 0.0) return PresetResult.Error("Stromstärke darf nicht 0 sein (Division durch 0).")
                val r = uVolts / iAmperes
                PresetResult.Success(
                    resultValue = r,
                    resultUnit = "Ω",
                    explanation = "R = U / I = ${formatNumber(uVolts)} V / ${formatNumber(iAmperes)} A = ${formatNumber(r)} Ω"
                )
            }
            "I" -> {
                if (uVolts == null || rOhms == null) return PresetResult.Error("Bitte U und R eingeben.")
                if (rOhms == 0.0) return PresetResult.Error("Widerstand darf nicht 0 sein (Kurzschluss).")
                val i = uVolts / rOhms
                PresetResult.Success(
                    resultValue = i,
                    resultUnit = "A",
                    explanation = "I = U / R = ${formatNumber(uVolts)} V / ${formatNumber(rOhms)} Ω = ${formatNumber(i)} A"
                )
            }
            else -> PresetResult.Error("Unbekannte Zielgröße: $target")
        }
    }

    // 2. Elektrische Leistung: target is "P", "U", "I", or "R"
    fun calculatePower(
        target: String,
        pWatts: Double?,
        uVolts: Double?,
        iAmperes: Double?,
        rOhms: Double?
    ): PresetResult {
        return when (target.uppercase(Locale.ROOT)) {
            "P" -> {
                if (uVolts != null && iAmperes != null) {
                    val p = uVolts * iAmperes
                    PresetResult.Success(p, "W", "P = U · I = ${formatNumber(uVolts)} V · ${formatNumber(iAmperes)} A = ${formatNumber(p)} W")
                } else if (iAmperes != null && rOhms != null) {
                    val p = iAmperes * iAmperes * rOhms
                    PresetResult.Success(p, "W", "P = I² · R = (${formatNumber(iAmperes)} A)² · ${formatNumber(rOhms)} Ω = ${formatNumber(p)} W")
                } else if (uVolts != null && rOhms != null) {
                    if (rOhms == 0.0) return PresetResult.Error("R darf nicht 0 sein.")
                    val p = (uVolts * uVolts) / rOhms
                    PresetResult.Success(p, "W", "P = U² / R = (${formatNumber(uVolts)} V)² / ${formatNumber(rOhms)} Ω = ${formatNumber(p)} W")
                } else {
                    PresetResult.Error("Mindestens 2 Werte aus (U, I, R) eingeben.")
                }
            }
            "U" -> {
                if (pWatts != null && iAmperes != null) {
                    if (iAmperes == 0.0) return PresetResult.Error("I darf nicht 0 sein.")
                    val u = pWatts / iAmperes
                    PresetResult.Success(u, "V", "U = P / I = ${formatNumber(pWatts)} W / ${formatNumber(iAmperes)} A = ${formatNumber(u)} V")
                } else if (pWatts != null && rOhms != null) {
                    if (pWatts * rOhms < 0) return PresetResult.Error("Wurzel aus negativer Zahl nicht möglich.")
                    val u = sqrt(pWatts * rOhms)
                    PresetResult.Success(u, "V", "U = √(P · R) = √(${formatNumber(pWatts)} · ${formatNumber(rOhms)}) = ${formatNumber(u)} V")
                } else {
                    PresetResult.Error("Bitte P und (I oder R) eingeben.")
                }
            }
            "I" -> {
                if (pWatts != null && uVolts != null) {
                    if (uVolts == 0.0) return PresetResult.Error("U darf nicht 0 sein.")
                    val i = pWatts / uVolts
                    PresetResult.Success(i, "A", "I = P / U = ${formatNumber(pWatts)} W / ${formatNumber(uVolts)} V = ${formatNumber(i)} A")
                } else if (pWatts != null && rOhms != null) {
                    if (rOhms == 0.0) return PresetResult.Error("R darf nicht 0 sein.")
                    val valInside = pWatts / rOhms
                    if (valInside < 0) return PresetResult.Error("Wurzel aus negativer Zahl nicht möglich.")
                    val i = sqrt(valInside)
                    PresetResult.Success(i, "A", "I = √(P / R) = √(${formatNumber(pWatts)} / ${formatNumber(rOhms)}) = ${formatNumber(i)} A")
                } else {
                    PresetResult.Error("Bitte P und (U oder R) eingeben.")
                }
            }
            else -> PresetResult.Error("Ungültige Zielgröße für Leistung: $target")
        }
    }

    // 3. Wellenlänge & Frequenz: c = 299792458 m/s (ideal) bzw. 3.0e8 m/s
    fun calculateWavelength(
        target: String, // "LAMBDA" or "FREQUENCY"
        frequencyHz: Double?,
        wavelengthMeters: Double?
    ): PresetResult {
        val c = 299_792_458.0
        return when (target.uppercase(Locale.ROOT)) {
            "LAMBDA" -> {
                if (frequencyHz == null || frequencyHz <= 0.0) return PresetResult.Error("Frequenz muss > 0 Hz sein.")
                val lambda = c / frequencyHz
                val approxMHz = frequencyHz / 1_000_000.0
                val approxFormula = if (approxMHz > 0.01) 300.0 / approxMHz else lambda
                PresetResult.Success(
                    resultValue = lambda,
                    resultUnit = "m",
                    explanation = "λ = c / f ≈ 300 000 km/s / ${formatNumber(frequencyHz)} Hz = ${formatNumber(lambda)} m (Faustformel 300 / f[MHz] ≈ ${formatNumber(approxFormula)} m)"
                )
            }
            "FREQUENCY" -> {
                if (wavelengthMeters == null || wavelengthMeters <= 0.0) return PresetResult.Error("Wellenlänge muss > 0 m sein.")
                val f = c / wavelengthMeters
                val fMHz = f / 1_000_000.0
                PresetResult.Success(
                    resultValue = f,
                    resultUnit = "Hz",
                    explanation = "f = c / λ = 3·10⁸ m/s / ${formatNumber(wavelengthMeters)} m = ${formatNumber(fMHz)} MHz (${formatNumber(f)} Hz)"
                )
            }
            else -> PresetResult.Error("Unbekannte Zielgröße: $target")
        }
    }

    // 4. Periodendauer: T = 1 / f
    fun calculatePeriod(
        target: String, // "T" or "F"
        periodSeconds: Double?,
        frequencyHz: Double?
    ): PresetResult {
        return when (target.uppercase(Locale.ROOT)) {
            "T" -> {
                if (frequencyHz == null || frequencyHz <= 0.0) return PresetResult.Error("Frequenz muss > 0 Hz sein.")
                val t = 1.0 / frequencyHz
                PresetResult.Success(
                    resultValue = t,
                    resultUnit = "s",
                    explanation = "T = 1 / f = 1 / ${formatNumber(frequencyHz)} Hz = ${formatNumber(t)} s (${formatPeriodUnit(t)})"
                )
            }
            "F" -> {
                if (periodSeconds == null || periodSeconds <= 0.0) return PresetResult.Error("Periodendauer muss > 0 s sein.")
                val f = 1.0 / periodSeconds
                PresetResult.Success(
                    resultValue = f,
                    resultUnit = "Hz",
                    explanation = "f = 1 / T = 1 / ${formatNumber(periodSeconds)} s = ${formatNumber(f)} Hz (${formatFreqUnit(f)})"
                )
            }
            else -> PresetResult.Error("Unbekannte Zielgröße: $target")
        }
    }

    // 5. Dezibel: isPower = true (10 log) vs false (20 log)
    fun calculateDecibel(
        isPower: Boolean,
        inVal1: Double?, // P1 or U1 (Eingang)
        outVal2: Double?, // P2 or U2 (Ausgang)
        dbValue: Double? // Wenn dB gegeben und Verhältnis/P2 gesucht
    ): PresetResult {
        val factor = if (isPower) 10.0 else 20.0
        val typeStr = if (isPower) "Leistungspegel (10 · lg)" else "Spannungspegel (20 · lg)"

        if (inVal1 != null && outVal2 != null) {
            if (inVal1 <= 0.0 || outVal2 <= 0.0) return PresetResult.Error("Werte müssen > 0 sein.")
            val ratio = outVal2 / inVal1
            val db = factor * log10(ratio)
            return PresetResult.Success(
                resultValue = db,
                resultUnit = "dB",
                explanation = "$typeStr: a = $factor · lg(${formatNumber(outVal2)} / ${formatNumber(inVal1)}) = ${formatNumber(db)} dB (Faktor: ${formatNumber(ratio)})"
            )
        } else if (inVal1 != null && dbValue != null) {
            val ratio = 10.0.pow(dbValue / factor)
            val outVal = inVal1 * ratio
            return PresetResult.Success(
                resultValue = outVal,
                resultUnit = if (isPower) "W" else "V",
                explanation = "Ausgangswert = Eingangswert · 10^(${formatNumber(dbValue)} / $factor) = ${formatNumber(inVal1)} · ${formatNumber(ratio)} = ${formatNumber(outVal)}"
            )
        }
        return PresetResult.Error("Bitte entweder (Eingang & Ausgang) oder (Eingang & dB) eingeben.")
    }

    // 6. Dipollänge: L = (c / (2 * f)) * 0.95 ≈ 142.5 / f[MHz]
    fun calculateDipole(frequencyMHz: Double?): PresetResult {
        if (frequencyMHz == null || frequencyMHz <= 0.0) return PresetResult.Error("Frequenz muss > 0 MHz sein.")
        val totalLength = 142.5 / frequencyMHz
        val legLength = totalLength / 2.0
        return PresetResult.Success(
            resultValue = totalLength,
            resultUnit = "m",
            explanation = "Gesamtlänge L ≈ 142,5 / ${formatNumber(frequencyMHz)} MHz = ${formatNumber(totalLength)} m (je Schenkel: ${formatNumber(legLength)} m)"
        )
    }

    // --- Helper Formatting ---

    fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble() && Math.abs(value) < 1e9) {
            value.toLong().toString()
        } else if (Math.abs(value) < 0.001 || Math.abs(value) >= 1e6) {
            String.format(Locale.US, "%.4e", value)
        } else {
            val formatted = String.format(Locale.US, "%.4f", value)
            formatted.trimEnd('0').trimEnd('.')
        }
    }

    private fun formatPeriodUnit(seconds: Double): String {
        return when {
            seconds >= 1.0 -> "${formatNumber(seconds)} s"
            seconds >= 1e-3 -> "${formatNumber(seconds * 1e3)} ms"
            seconds >= 1e-6 -> "${formatNumber(seconds * 1e6)} µs"
            else -> "${formatNumber(seconds * 1e9)} ns"
        }
    }

    private fun formatFreqUnit(hz: Double): String {
        return when {
            hz >= 1e9 -> "${formatNumber(hz / 1e9)} GHz"
            hz >= 1e6 -> "${formatNumber(hz / 1e6)} MHz"
            hz >= 1e3 -> "${formatNumber(hz / 1e3)} kHz"
            else -> "${formatNumber(hz)} Hz"
        }
    }

    sealed interface PresetResult {
        data class Success(val resultValue: Double, val resultUnit: String, val explanation: String) : PresetResult
        data class Error(val message: String) : PresetResult
    }

    // Minimal recursive-descent math parser
    private class SimpleParser(private val text: String) {
        private var pos = 0

        fun parse(): Double {
            val v = parseExpression()
            if (pos < text.length) throw IllegalArgumentException("Unerwartetes Zeichen: '${text[pos]}'")
            return v
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (pos < text.length) {
                when (text[pos]) {
                    '+' -> { pos++; x += parseTerm() }
                    '-' -> { pos++; x -= parseTerm() }
                    else -> return x
                }
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (pos < text.length) {
                when (text[pos]) {
                    '*' -> { pos++; x *= parseFactor() }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division durch 0")
                        x /= divisor
                    }
                    else -> return x
                }
            }
            return x
        }

        private fun parseFactor(): Double {
            var x = parsePrimary()
            while (pos < text.length && text[pos] == '^') {
                pos++
                val exponent = parsePrimary()
                x = x.pow(exponent)
            }
            return x
        }

        private fun parsePrimary(): Double {
            if (pos >= text.length) throw IllegalArgumentException("Unerwartetes Ende der Formel")

            if (text[pos] == '+') { pos++; return parsePrimary() }
            if (text[pos] == '-') { pos++; return -parsePrimary() }

            if (text[pos] == '(') {
                pos++
                val res = parseExpression()
                if (pos < text.length && text[pos] == ')') {
                    pos++
                } else {
                    throw IllegalArgumentException("Fehlende schließende Klammer ')'")
                }
                return res
            }

            // Functions: sqrt, log, ln
            if (text.startsWith("sqrt(", pos) || text.startsWith("√( ", pos) || text.startsWith("√(", pos)) {
                val offset = if (text.startsWith("sqrt(", pos)) 5 else if (text.startsWith("√( ", pos)) 3 else 2
                pos += offset
                val inside = parseExpression()
                if (pos < text.length && text[pos] == ')') pos++
                if (inside < 0) throw IllegalArgumentException("Wurzel aus negativer Zahl")
                return sqrt(inside)
            }

            if (text.startsWith("log(", pos) || text.startsWith("lg(", pos)) {
                val offset = if (text.startsWith("log(", pos)) 4 else 3
                pos += offset
                val inside = parseExpression()
                if (pos < text.length && text[pos] == ')') pos++
                if (inside <= 0) throw IllegalArgumentException("Logarithmus von Wert <= 0 nicht definiert")
                return log10(inside)
            }

            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) {
                pos++
            }
            if (start == pos) {
                throw IllegalArgumentException("Zahl erwartet bei Position $pos ('${text.substring(pos)}')")
            }
            return text.substring(start, pos).toDouble()
        }
    }
}
