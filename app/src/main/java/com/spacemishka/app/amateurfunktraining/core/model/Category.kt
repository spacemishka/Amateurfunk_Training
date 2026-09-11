package com.spacemishka.app.amateurfunktraining.core.model

enum class Category(
    val idKey: String,
    val displayName: String,
    val shortDescription: String
) {
    ALL(
        idKey = "Alle",
        displayName = "Alle Fächer",
        shortDescription = "Gemischtes Training aus allen Bereichen"
    ),
    TECHNIK(
        idKey = "Technik",
        displayName = "Technik (Klasse E)",
        shortDescription = "Grundlagen, HF-Technik, Bauteile & Schaltungen"
    ),
    BETRIEB(
        idKey = "Betrieb",
        displayName = "Betriebliche Kenntnisse",
        shortDescription = "Q-Gruppen, ITU-Alphabet, Abwicklung & Praxis"
    ),
    VORSCHRIFTEN(
        idKey = "Vorschriften",
        displayName = "Vorschriften & Gesetze",
        shortDescription = "AFuG, AFuV, Bandpläne & Bestimmungen"
    );

    companion object {
        fun fromKey(key: String): Category {
            return entries.firstOrNull { it.idKey.equals(key, ignoreCase = true) } ?: ALL
        }
    }
}
