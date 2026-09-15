package com.spacemishka.app.amateurfunktraining.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.amateurfunktraining.core.backup.BackupManager
import com.spacemishka.app.amateurfunktraining.core.data.AppSettings
import com.spacemishka.app.amateurfunktraining.core.data.FontScaleOption
import com.spacemishka.app.amateurfunktraining.core.data.SettingsRepository
import com.spacemishka.app.amateurfunktraining.core.data.ThemeMode
import com.spacemishka.app.amateurfunktraining.core.model.AppBackupDto
import com.spacemishka.app.amateurfunktraining.core.model.BackupSummary
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager? = null
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow

    private val _pendingBackupForImport = MutableStateFlow<Pair<AppBackupDto, BackupSummary>?>(null)
    val pendingBackupForImport: StateFlow<Pair<AppBackupDto, BackupSummary>?> = _pendingBackupForImport.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setFontScale(scale: FontScaleOption) {
        settingsRepository.setFontScale(scale)
    }

    fun exportBackup(outputStream: OutputStream) {
        val manager = backupManager ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val jsonString = manager.createBackupJson()
                outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
                _snackbarMessage.emit("Lernstand erfolgreich als Backup exportiert.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Fehler beim Exportieren: ${e.message ?: "Unbekannter Fehler"}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun onFileSelectedForImport(inputStream: InputStream) {
        val manager = backupManager ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val validationResult = manager.validateBackupJson(jsonString)
                validationResult.fold(
                    onSuccess = { pair ->
                        _pendingBackupForImport.value = pair
                    },
                    onFailure = { error ->
                        _snackbarMessage.emit(error.message ?: "Ungültiges Backup")
                    }
                )
            } catch (e: Exception) {
                _snackbarMessage.emit("Fehler beim Lesen der Datei: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun confirmImport(mode: ImportMode) {
        val manager = backupManager ?: return
        val pending = _pendingBackupForImport.value ?: return

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val restoreResult = manager.restoreBackup(pending.first, mode)
                restoreResult.fold(
                    onSuccess = { msg ->
                        _snackbarMessage.emit("Import erfolgreich: $msg")
                        _pendingBackupForImport.value = null
                    },
                    onFailure = { error ->
                        _snackbarMessage.emit("Fehler beim Wiederherstellen: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _snackbarMessage.emit("Unerwarteter Fehler: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun dismissImportDialog() {
        _pendingBackupForImport.value = null
    }
}
