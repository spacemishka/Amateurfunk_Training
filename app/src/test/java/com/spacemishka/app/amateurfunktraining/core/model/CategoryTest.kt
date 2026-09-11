package com.spacemishka.app.amateurfunktraining.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {

    @Test
    fun testFromKeyMatching() {
        assertEquals(Category.TECHNIK, Category.fromKey("Technik"))
        assertEquals(Category.TECHNIK, Category.fromKey("technik"))
        assertEquals(Category.BETRIEB, Category.fromKey("Betrieb"))
        assertEquals(Category.VORSCHRIFTEN, Category.fromKey("Vorschriften"))
        assertEquals(Category.ALL, Category.fromKey("Alle"))
        assertEquals(Category.ALL, Category.fromKey("Unbekannt"))
    }

    @Test
    fun testCategoryProperties() {
        assertEquals("Technik (Klasse E)", Category.TECHNIK.displayName)
        assertEquals("Betriebliche Kenntnisse", Category.BETRIEB.displayName)
        assertEquals("Vorschriften & Gesetze", Category.VORSCHRIFTEN.displayName)
        assertEquals("Alle Fächer", Category.ALL.displayName)
    }
}
