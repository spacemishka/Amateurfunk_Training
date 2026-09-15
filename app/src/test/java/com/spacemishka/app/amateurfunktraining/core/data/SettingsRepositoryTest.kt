package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.feature.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun testSettingsDefaultValues() {
        val repo = InMemorySettingsRepository()
        val settings = repo.settingsFlow.value

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(FontScaleOption.NORMAL, settings.fontScale)
    }

    @Test
    fun testUpdateThemeModeAndFontScale() {
        val repo = InMemorySettingsRepository()
        val vm = SettingsViewModel(repo)

        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.settingsFlow.value.themeMode)
        assertEquals(ThemeMode.DARK, vm.settings.value.themeMode)

        vm.setFontScale(FontScaleOption.EXTRA_LARGE)
        assertEquals(FontScaleOption.EXTRA_LARGE, repo.settingsFlow.value.fontScale)
        assertEquals(1.30f, vm.settings.value.fontScale.scale, 0.001f)

        vm.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.settings.value.themeMode)
    }
}
