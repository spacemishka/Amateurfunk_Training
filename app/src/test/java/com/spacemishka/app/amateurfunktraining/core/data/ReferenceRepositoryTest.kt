package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.FormulaCategory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReferenceRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun createRepository(): AssetReferenceRepository {
        val formulasJson = File("src/main/assets/formulas_klasse_e.json").readText(Charsets.UTF_8)
        val referenceJson = File("src/main/assets/reference_data.json").readText(Charsets.UTF_8)

        return AssetReferenceRepository(
            formulasJsonProvider = { formulasJson },
            referenceJsonProvider = { referenceJson },
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testFormulasLoadedAndSearchable() = runTest(testDispatcher) {
        val repo = createRepository()
        val allFormulas = repo.getAllFormulas()
        assertFalse("Formeln dürfen nicht leer sein", allFormulas.isEmpty())
        assertTrue("Mindestens 10 Formeln vorhanden", allFormulas.size >= 10)

        // Search for Ohm
        val ohmResults = repo.searchFormulas("Ohm", null)
        assertFalse(ohmResults.isEmpty())
        assertTrue(ohmResults.any { it.id == "formel_ohm" })

        // Filter by category
        val hfResults = repo.searchFormulas("", FormulaCategory.HF_WECHSELSTROM)
        assertFalse(hfResults.isEmpty())
        assertTrue(hfResults.all { it.category == FormulaCategory.HF_WECHSELSTROM })
    }

    @Test
    fun testQGroupsLoadedAndSearchable() = runTest(testDispatcher) {
        val repo = createRepository()
        val allQ = repo.getQGroups()
        assertFalse("Q-Gruppen dürfen nicht leer sein", allQ.isEmpty())
        assertTrue("QTH muss in der Liste sein", allQ.any { it.code == "QTH" })
        assertTrue("QRM muss in der Liste sein", allQ.any { it.code == "QRM" })

        val qthSearch = repo.searchQGroups("Standort")
        assertFalse(qthSearch.isEmpty())
        assertTrue(qthSearch.any { it.code == "QTH" })
    }

    @Test
    fun testCountryPrefixesLoaded() = runTest(testDispatcher) {
        val repo = createRepository()
        val prefixes = repo.getCountryPrefixes()
        assertFalse(prefixes.isEmpty())
        assertTrue("Deutschland (DA-DL) muss vorhanden sein", prefixes.any { it.land.contains("Deutschland") })
        assertTrue("Österreich (OE) muss vorhanden sein", prefixes.any { it.praefix == "OE" })
        assertTrue("Schweiz (HB9) muss vorhanden sein", prefixes.any { it.praefix == "HB9" })

        val searchDL = repo.searchCountryPrefixes("Deutschland")
        assertFalse(searchDL.isEmpty())
    }

    @Test
    fun testBandPlanAndRegulationsLoaded() = runTest(testDispatcher) {
        val repo = createRepository()
        val bandplan = repo.getBandPlan()
        assertFalse("Bandplan darf nicht leer sein", bandplan.isEmpty())
        assertTrue("2m-Band muss vorkommen", bandplan.any { it.band.contains("2m") })
        assertTrue("70cm-Band muss vorkommen", bandplan.any { it.band.contains("70cm") })

        val regs = repo.getRegulations()
        assertFalse("Vorschriften dürfen nicht leer sein", regs.isEmpty())
        assertTrue("AFuG muss vorkommen", regs.any { it.titel.contains("AFuG") })
    }
}
