package com.spacemishka.app.amateurfunktraining.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class FormulaCategory(val displayName: String) {
    GLEICHSTROM("Gleichstromtechnik"),
    HF_WECHSELSTROM("Wechselstrom & HF"),
    PEGEL_DAEMPFUNG("Pegel & Dämpfung"),
    ANTENNEN_EMV("Antennen & EMV"),
    BAUELEMENTE("Bauelemente")
}

@Serializable
data class FormulaVariable(
    @SerialName("symbol") val symbol: String,
    @SerialName("name") val name: String,
    @SerialName("einheit") val einheit: String
)

@Serializable
data class FormulaDto(
    @SerialName("id") val id: String,
    @SerialName("kategorie") val kategorie: String,
    @SerialName("titel") val titel: String,
    @SerialName("formel") val formel: String,
    @SerialName("beschreibung") val beschreibung: String,
    @SerialName("variablen") val variablen: List<FormulaVariable>,
    @SerialName("umstellungen") val umstellungen: List<String> = emptyList(),
    @SerialName("beispiel") val beispiel: String,
    @SerialName("pruefungs_tipp") val pruefungsTipp: String
) {
    fun toDomain(): Formula {
        val cat = try {
            FormulaCategory.valueOf(kategorie.uppercase())
        } catch (_: Exception) {
            FormulaCategory.GLEICHSTROM
        }
        return Formula(
            id = id,
            category = cat,
            title = titel,
            formula = formel,
            description = beschreibung,
            variables = variablen,
            alternatives = umstellungen,
            example = beispiel,
            examTip = pruefungsTipp
        )
    }
}

data class Formula(
    val id: String,
    val category: FormulaCategory,
    val title: String,
    val formula: String,
    val description: String,
    val variables: List<FormulaVariable>,
    val alternatives: List<String>,
    val example: String,
    val examTip: String
)

@Serializable
data class QGroup(
    @SerialName("code") val code: String,
    @SerialName("frage") val frage: String,
    @SerialName("antwort") val antwort: String,
    @SerialName("bedeutung") val bedeutung: String,
    @SerialName("kategorie") val kategorie: String
)

@Serializable
data class CountryPrefix(
    @SerialName("praefix") val praefix: String,
    @SerialName("land") val land: String,
    @SerialName("kontinent") val kontinent: String,
    @SerialName("itu_zone") val ituZone: Int,
    @SerialName("cq_zone") val cqZone: Int
)

@Serializable
data class BandPlanEntry(
    @SerialName("band") val band: String,
    @SerialName("frequenzbereich") val frequenzbereich: String,
    @SerialName("max_leistung_pep") val maxLeistungPep: String,
    @SerialName("primaer_sekundaer") val status: String,
    @SerialName("bemerkung") val bemerkung: String
)

@Serializable
data class RegulationItem(
    @SerialName("titel") val titel: String,
    @SerialName("paragraph") val paragraph: String,
    @SerialName("inhalt") val inhalt: String
)

@Serializable
data class PhoneticEntry(
    @SerialName("buchstabe") val buchstabe: String,
    @SerialName("wort") val wort: String,
    @SerialName("aussprache") val aussprache: String
)

@Serializable
data class ReferenceDataDto(
    @SerialName("q_gruppen") val qGruppen: List<QGroup> = emptyList(),
    @SerialName("landeskenner") val landeskenner: List<CountryPrefix> = emptyList(),
    @SerialName("bandplan_klasse_e") val bandplanKlasseE: List<BandPlanEntry> = emptyList(),
    @SerialName("gesetze_und_vorschriften") val gesetzeUndVorschriften: List<RegulationItem> = emptyList(),
    @SerialName("itu_alphabet") val ituAlphabet: List<PhoneticEntry> = emptyList()
)
