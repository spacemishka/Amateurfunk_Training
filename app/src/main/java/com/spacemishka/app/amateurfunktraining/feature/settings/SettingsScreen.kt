package com.spacemishka.app.amateurfunktraining.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.core.data.FontScaleOption
import com.spacemishka.app.amateurfunktraining.core.data.ThemeMode
import com.spacemishka.app.amateurfunktraining.core.model.BackupSummary
import com.spacemishka.app.amateurfunktraining.core.model.ImportMode
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val pendingBackup by viewModel.pendingBackupForImport.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Export launcher (Create Document)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportBackup(outputStream)
                }
            } catch (e: Exception) {
                // Handled via ViewModel
            }
        }
    }

    // Import launcher (Open Document)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    viewModel.onFileSelectedForImport(inputStream)
                }
            } catch (e: Exception) {
                // Handled via ViewModel
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Einstellungen & Barrierefreiheit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Zurück zum Hauptmenü" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Section
            item {
                SettingsSectionCard(
                    title = "Erscheinungsbild",
                    subtitle = "Wähle zwischen hellem, dunklem oder dem System-Design",
                    icon = Icons.Default.DarkMode
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = settings.themeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(horizontal = 8.dp)
                                .semantics {
                                    this.role = Role.RadioButton
                                    this.contentDescription = "${mode.displayName}${if (isSelected) ", ausgewählt" else ""}"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = RadioBlue)
                            )
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Font Scaling Section
            item {
                SettingsSectionCard(
                    title = "Schriftgröße & Skalierung",
                    subtitle = "Optimiere die Lesbarkeit für alle Bildschirme und Sehanforderungen",
                    icon = Icons.Default.FormatSize
                ) {
                    FontScaleOption.entries.forEach { option ->
                        val isSelected = settings.fontScale == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setFontScale(option) }
                                .padding(horizontal = 8.dp)
                                .semantics {
                                    this.role = Role.RadioButton
                                    this.contentDescription = "${option.displayName}${if (isSelected) ", ausgewählt" else ""}"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setFontScale(option) },
                                colors = RadioButtonDefaults.colors(selectedColor = RadioBlue)
                            )
                            Column {
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vorschau: So wirkt der Fragentext",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (12 * option.scale).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // MVP 6: Data Portability (Backup & Import/Export)
            item {
                SettingsSectionCard(
                    title = "Datensicherung & Synchronisation",
                    subtitle = "Lernstand exportieren oder auf ein anderes Gerät / Web übertragen",
                    icon = Icons.Default.Backup
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sichere deinen gesamten Lernfortschritt, Prüfungshistorien, Lesezeichen und Einstellungen als portable JSON-Datei. Diese kann jederzeit auf Android oder im Web (Wasm) importiert werden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    val filename = "Amateurfunk_KlasseE_Backup_$dateStr.json"
                                    exportLauncher.launch(filename)
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = RadioBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportieren")
                            }

                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importieren")
                            }
                        }

                        if (isProcessing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = RadioBlue
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Verarbeite Daten...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Accessibility / TalkBack Information
            item {
                SettingsSectionCard(
                    title = "Barrierefreiheit & Screenreader",
                    subtitle = "Vollständige Unterstützung für TalkBack und Sprachausgabe",
                    icon = Icons.Default.Info
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "• Alle Schaltflächen besitzen Mindest-Berührungsflächen von 48×48 dp.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Antwortoptionen und Fragenkarten bieten detaillierte Vorlesetexte mit Zustandsansage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Kontraste erfüllen die Richtlinien nach WCAG 2.1 AA.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // App Version Info
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amateurfunk Training Klasse E",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version 1.6.0 (MVP 6) • Datenportabilität & Wasm-Ready • 830 BNetzA-Prüfungsfragen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Import confirmation dialog
    pendingBackup?.let { (_, summary) ->
        ImportConfirmationDialog(
            summary = summary,
            onConfirm = { mode ->
                viewModel.confirmImport(mode)
            },
            onDismiss = {
                viewModel.dismissImportDialog()
            }
        )
    }
}

@Composable
private fun ImportConfirmationDialog(
    summary: BackupSummary,
    onConfirm: (ImportMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(ImportMode.MERGE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Backup-Daten importieren",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Gefundene Datensätze:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RadioBlue
                        )
                        Text(
                            text = "• Fragenfortschritte: ${summary.progressCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Prüfungssitzungen: ${summary.examCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Gelesene Themen: ${summary.topicsCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Plattform: ${summary.clientPlatform} (am ${summary.exportiertAm})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    text = "Wähle die Import-Strategie:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                ImportMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedMode = mode }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedMode = mode },
                            colors = RadioButtonDefaults.colors(selectedColor = RadioBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMode) },
                colors = ButtonDefaults.buttonColors(containerColor = RadioBlue)
            ) {
                Text("Import ausführen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RadioBlue,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            content()
        }
    }
}
