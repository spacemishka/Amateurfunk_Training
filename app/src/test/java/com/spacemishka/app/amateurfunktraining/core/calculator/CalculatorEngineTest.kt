package com.spacemishka.app.amateurfunktraining.core.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun testBasicArithmeticEvaluation() {
        val res1 = CalculatorEngine.evaluateBasic("12 + 34")
        assertTrue(res1 is CalculatorEngine.EvalResult.Success)
        assertEquals(46.0, (res1 as CalculatorEngine.EvalResult.Success).value, 0.001)

        val res2 = CalculatorEngine.evaluateBasic("100 / 4 - 5 * 2")
        assertTrue(res2 is CalculatorEngine.EvalResult.Success)
        assertEquals(15.0, (res2 as CalculatorEngine.EvalResult.Success).value, 0.001)

        val res3 = CalculatorEngine.evaluateBasic("2 ^ 3")
        assertTrue(res3 is CalculatorEngine.EvalResult.Success)
        assertEquals(8.0, (res3 as CalculatorEngine.EvalResult.Success).value, 0.001)

        val res4 = CalculatorEngine.evaluateBasic("sqrt(144)")
        assertTrue(res4 is CalculatorEngine.EvalResult.Success)
        assertEquals(12.0, (res4 as CalculatorEngine.EvalResult.Success).value, 0.001)

        val res5 = CalculatorEngine.evaluateBasic("log(1000)")
        assertTrue(res5 is CalculatorEngine.EvalResult.Success)
        assertEquals(3.0, (res5 as CalculatorEngine.EvalResult.Success).value, 0.001)
    }

    @Test
    fun testDivisionByZeroHandled() {
        val divZero = CalculatorEngine.evaluateBasic("12 / 0")
        assertTrue("Division durch 0 muss einen Fehler liefern", divZero is CalculatorEngine.EvalResult.Error)
    }

    @Test
    fun testOhmPresetCalculations() {
        // U = R * I: 100 Ω * 0.12 A = 12 V
        val resU = CalculatorEngine.calculateOhm("U", null, 100.0, 0.12)
        assertTrue(resU is CalculatorEngine.PresetResult.Success)
        assertEquals(12.0, (resU as CalculatorEngine.PresetResult.Success).resultValue, 0.001)

        // R = U / I: 12 V / 0.05 A = 240 Ω
        val resR = CalculatorEngine.calculateOhm("R", 12.0, null, 0.05)
        assertTrue(resR is CalculatorEngine.PresetResult.Success)
        assertEquals(240.0, (resR as CalculatorEngine.PresetResult.Success).resultValue, 0.001)

        // I = U / R: 13.8 V / 4.6 Ω = 3.0 A
        val resI = CalculatorEngine.calculateOhm("I", 13.8, 4.6, null)
        assertTrue(resI is CalculatorEngine.PresetResult.Success)
        assertEquals(3.0, (resI as CalculatorEngine.PresetResult.Success).resultValue, 0.001)

        // Error on zero resistance for I
        val errI = CalculatorEngine.calculateOhm("I", 12.0, 0.0, null)
        assertTrue(errI is CalculatorEngine.PresetResult.Error)
    }

    @Test
    fun testPowerPresetCalculations() {
        // P = U * I: 13.8 V * 5 A = 69 W
        val resP1 = CalculatorEngine.calculatePower("P", null, 13.8, 5.0, null)
        assertTrue(resP1 is CalculatorEngine.PresetResult.Success)
        assertEquals(69.0, (resP1 as CalculatorEngine.PresetResult.Success).resultValue, 0.001)

        // P = I^2 * R: (2 A)^2 * 50 Ω = 200 W
        val resP2 = CalculatorEngine.calculatePower("P", null, null, 2.0, 50.0)
        assertTrue(resP2 is CalculatorEngine.PresetResult.Success)
        assertEquals(200.0, (resP2 as CalculatorEngine.PresetResult.Success).resultValue, 0.001)

        // U from P and R: sqrt(100 W * 50 Ω) = sqrt(5000) ≈ 70.71 V
        val resU = CalculatorEngine.calculatePower("U", 100.0, null, null, 50.0)
        assertTrue(resU is CalculatorEngine.PresetResult.Success)
        assertEquals(70.7106, (resU as CalculatorEngine.PresetResult.Success).resultValue, 0.01)
    }

    @Test
    fun testWavelengthFrequencyPresetCalculations() {
        // 145 MHz -> lambda ≈ 2.0675 m (c / 145e6)
        val resLambda = CalculatorEngine.calculateWavelength("LAMBDA", 145_000_000.0, null)
        assertTrue(resLambda is CalculatorEngine.PresetResult.Success)
        val lambda = (resLambda as CalculatorEngine.PresetResult.Success).resultValue
        assertTrue(lambda in 2.06..2.07)

        // 2m band -> frequency ≈ 150 MHz (approx c / 2)
        val resFreq = CalculatorEngine.calculateWavelength("FREQUENCY", null, 2.0)
        assertTrue(resFreq is CalculatorEngine.PresetResult.Success)
        val freq = (resFreq as CalculatorEngine.PresetResult.Success).resultValue
        assertTrue(freq in 149_000_000.0..151_000_000.0)
    }

    @Test
    fun testPeriodPresetCalculations() {
        // 50 Hz -> 0.02 s (20 ms)
        val resT = CalculatorEngine.calculatePeriod("T", null, 50.0)
        assertTrue(resT is CalculatorEngine.PresetResult.Success)
        assertEquals(0.02, (resT as CalculatorEngine.PresetResult.Success).resultValue, 0.0001)

        // 1 µs -> 1 MHz
        val resF = CalculatorEngine.calculatePeriod("F", 0.000001, null)
        assertTrue(resF is CalculatorEngine.PresetResult.Success)
        assertEquals(1_000_000.0, (resF as CalculatorEngine.PresetResult.Success).resultValue, 1.0)
    }

    @Test
    fun testDecibelPresetCalculations() {
        // Power ratio: P1 = 5W, P2 = 10W -> +3.01 dB
        val resDb = CalculatorEngine.calculateDecibel(isPower = true, inVal1 = 5.0, outVal2 = 10.0, dbValue = null)
        assertTrue(resDb is CalculatorEngine.PresetResult.Success)
        assertEquals(3.0103, (resDb as CalculatorEngine.PresetResult.Success).resultValue, 0.01)

        // Voltage ratio: U1 = 1V, U2 = 10V -> +20 dB
        val resDbU = CalculatorEngine.calculateDecibel(isPower = false, inVal1 = 1.0, outVal2 = 10.0, dbValue = null)
        assertTrue(resDbU is CalculatorEngine.PresetResult.Success)
        assertEquals(20.0, (resDbU as CalculatorEngine.PresetResult.Success).resultValue, 0.01)

        // Inverse power calculation: P1 = 10W, +6 dB -> P2 ≈ 39.81 W (approx 4x)
        val resOutP = CalculatorEngine.calculateDecibel(isPower = true, inVal1 = 10.0, outVal2 = null, dbValue = 6.0)
        assertTrue(resOutP is CalculatorEngine.PresetResult.Success)
        assertEquals(39.81, (resOutP as CalculatorEngine.PresetResult.Success).resultValue, 0.1)
    }

    @Test
    fun testDipoleCalculation() {
        // 28.5 MHz -> L ≈ 142.5 / 28.5 = 5.00 m
        val resDipole = CalculatorEngine.calculateDipole(28.5)
        assertTrue(resDipole is CalculatorEngine.PresetResult.Success)
        assertEquals(5.0, (resDipole as CalculatorEngine.PresetResult.Success).resultValue, 0.01)
    }
}
