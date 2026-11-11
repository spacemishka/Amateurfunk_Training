package com.spacemishka.app.amateurfunktraining.core.data

import com.spacemishka.app.amateurfunktraining.core.model.SettingsBackupDto
import com.spacemishka.app.amateurfunktraining.feature.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun testSettingsDefaultValues() {
        val repo = InMemorySettingsRepository()
        val settings = repo.settingsFlow.value

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(FontScaleOption.NORMAL, settings.fontScale)
        assertFalse(settings.isOnboardingCompleted)
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

    @Test
    fun testOnboardingCompletedLifecycle() {
        val repo = InMemorySettingsRepository()
        val vm = SettingsViewModel(repo)

        assertFalse(repo.settingsFlow.value.isOnboardingCompleted)

        vm.setOnboardingCompleted(true)
        assertTrue(repo.settingsFlow.value.isOnboardingCompleted)
        assertTrue(vm.settings.value.isOnboardingCompleted)

        vm.setOnboardingCompleted(false)
        assertFalse(repo.settingsFlow.value.isOnboardingCompleted)
    }

    @Test
    fun testExportAndImportSettingsWithOnboarding() {
        val repo = InMemorySettingsRepository()
        repo.setThemeMode(ThemeMode.DARK)
        repo.setFontScale(FontScaleOption.LARGE)
        repo.setOnboardingCompleted(true)

        val exported = repo.exportSettings()
        assertEquals("DARK", exported.dunkelmodus)
        assertEquals("LARGE", exported.schriftgroesse)
        assertTrue(exported.onboardingAbgeschlossen)

        val freshRepo = InMemorySettingsRepository()
        freshRepo.importSettings(exported)

        assertEquals(ThemeMode.DARK, freshRepo.settingsFlow.value.themeMode)
        assertEquals(FontScaleOption.LARGE, freshRepo.settingsFlow.value.fontScale)
        assertTrue(freshRepo.settingsFlow.value.isOnboardingCompleted)
    }
}
