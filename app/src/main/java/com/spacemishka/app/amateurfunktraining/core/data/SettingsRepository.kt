package com.spacemishka.app.amateurfunktraining.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.spacemishka.app.amateurfunktraining.core.model.SettingsBackupDto

enum class ThemeMode(val displayName: String) {
    SYSTEM("Systemstandard"),
    LIGHT("Hell"),
    DARK("Dunkel")
}

enum class FontScaleOption(val scale: Float, val displayName: String) {
    NORMAL(1.0f, "Standard (100 %)"),
    LARGE(1.15f, "Groß (115 %)"),
    EXTRA_LARGE(1.30f, "Sehr groß (130 %)")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: FontScaleOption = FontScaleOption.NORMAL,
    val isOnboardingCompleted: Boolean = false
)

interface SettingsRepository {
    val settingsFlow: StateFlow<AppSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setFontScale(scale: FontScaleOption)
    fun setOnboardingCompleted(completed: Boolean)
    fun exportSettings(): SettingsBackupDto
    fun importSettings(dto: SettingsBackupDto)
}

class InMemorySettingsRepository(
    initialSettings: AppSettings = AppSettings()
) : SettingsRepository {
    private val _settingsFlow = MutableStateFlow(initialSettings)
    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = mode)
    }

    override fun setFontScale(scale: FontScaleOption) {
        _settingsFlow.value = _settingsFlow.value.copy(fontScale = scale)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(isOnboardingCompleted = completed)
    }

    override fun exportSettings(): SettingsBackupDto {
        val s = _settingsFlow.value
        return SettingsBackupDto(
            dunkelmodus = s.themeMode.name,
            schriftgroesse = s.fontScale.name,
            onboardingAbgeschlossen = s.isOnboardingCompleted
        )
    }

    override fun importSettings(dto: SettingsBackupDto) {
        val theme = try { ThemeMode.valueOf(dto.dunkelmodus) } catch (_: Exception) { ThemeMode.SYSTEM }
        val font = try { FontScaleOption.valueOf(dto.schriftgroesse) } catch (_: Exception) { FontScaleOption.NORMAL }
        setThemeMode(theme)
        setFontScale(font)
        setOnboardingCompleted(dto.onboardingAbgeschlossen)
    }
}

class SharedPrefSettingsRepository(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = mode)
    }

    override fun setFontScale(scale: FontScaleOption) {
        prefs.edit().putString(KEY_FONT_SCALE, scale.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(fontScale = scale)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isOnboardingCompleted = completed)
    }

    override fun exportSettings(): SettingsBackupDto {
        val s = _settingsFlow.value
        return SettingsBackupDto(
            dunkelmodus = s.themeMode.name,
            schriftgroesse = s.fontScale.name,
            onboardingAbgeschlossen = s.isOnboardingCompleted
        )
    }

    override fun importSettings(dto: SettingsBackupDto) {
        val theme = try { ThemeMode.valueOf(dto.dunkelmodus) } catch (_: Exception) { ThemeMode.SYSTEM }
        val font = try { FontScaleOption.valueOf(dto.schriftgroesse) } catch (_: Exception) { FontScaleOption.NORMAL }
        setThemeMode(theme)
        setFontScale(font)
        setOnboardingCompleted(dto.onboardingAbgeschlossen)
    }

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val fontStr = prefs.getString(KEY_FONT_SCALE, FontScaleOption.NORMAL.name)
        val onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        val theme = try {
            ThemeMode.valueOf(themeStr ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val font = try {
            FontScaleOption.valueOf(fontStr ?: FontScaleOption.NORMAL.name)
        } catch (_: Exception) {
            FontScaleOption.NORMAL
        }

        return AppSettings(
            themeMode = theme,
            fontScale = font,
            isOnboardingCompleted = onboardingCompleted
        )
    }

    companion object {
        private const val PREFS_NAME = "amateurfunk_settings_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_FONT_SCALE = "key_font_scale"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
    }
}
