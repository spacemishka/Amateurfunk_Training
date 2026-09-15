package com.spacemishka.app.amateurfunktraining.feature.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.core.model.ExamResultPart
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryBetrieb
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryTechnik
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryVorschriften
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreen
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreenBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreenContainer
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioNavy
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRedBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRedContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    result: ExamResult,
    onStartFehlertraining: (List<String>) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateToHome()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prüfungsauswertung",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateToHome,
                        modifier = Modifier.semantics { contentDescription = "Zurück zur Übersicht" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big Banner: BESTANDEN vs NICHT BESTANDEN
            item {
                ResultBanner(result = result)
            }

            // Category breakdown cards
            item {
                Text(
                    text = "Ergebnisse nach Prüfungsfächern",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(result.partResults) { partResult ->
                CategoryResultCard(partResult = partResult)
            }

            // Quick Actions: Fehlertraining & Back to Home
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (result.wrongQuestions.isNotEmpty()) {
                        Button(
                            onClick = {
                                onStartFehlertraining(result.wrongQuestions.map { it.id })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .semantics {
                                    contentDescription = "Fehlertraining starten mit ${result.wrongQuestions.size} falschen Fragen"
                                }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fehlertraining starten (${result.wrongQuestions.size} ${if (result.wrongQuestions.size == 1) "Frage" else "Fragen"})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToHome,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Zurück zur Startseite", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Detailed mistakes analysis
            if (result.wrongQuestions.isNotEmpty()) {
                item {
                    Text(
                        text = "Detailanalyse der Fehlfragen (${result.wrongQuestions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(result.wrongQuestions) { wrongQuestion ->
                    val userSelectedIndex = result.selectedAnswers[wrongQuestion.id]
                    MistakeDetailCard(
                        question = wrongQuestion,
                        userSelectedIndex = userSelectedIndex
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ResultBanner(result: ExamResult, modifier: Modifier = Modifier) {
    val isPassed = result.isOverallPassed
    val gradient = if (isPassed) {
        Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFB71C1C), Color(0xFFC62828)))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = if (isPassed) "HERZLICHEN GLÜCKWUNSCH!" else "NICHT BESTANDEN",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = if (isPassed) "Prüfungssimulation erfolgreich bestanden" else "Mindestens 75 % in jedem der 3 Teilgebiete erforderlich",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "Gesamtergebnis: ${result.totalCorrectAnswers} / ${result.totalQuestions} richtig (${(result.overallPercentage * 100).toInt()} %)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryResultCard(partResult: ExamResultPart, modifier: Modifier = Modifier) {
    val catColor = when (partResult.category) {
        Category.TECHNIK -> CategoryTechnik
        Category.BETRIEB -> CategoryBetrieb
        Category.VORSCHRIFTEN -> CategoryVorschriften
        else -> RadioNavy
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = catColor,
                        modifier = Modifier.size(10.dp)
                    ) {}

                    Text(
                        text = partResult.category.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (partResult.isPassed) CorrectGreen.copy(alpha = 0.18f) else WrongRed.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = if (partResult.isPassed) "BESTANDEN" else "NICHT BESTANDEN",
                        color = if (partResult.isPassed) Color(0xFF4ADE80) else Color(0xFFF87171),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${partResult.correctAnswers} von ${partResult.totalQuestions} Fragen richtig",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${(partResult.percentage * 100).toInt()} % (Soll: ≥ 75 %)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (partResult.isPassed) Color(0xFF4ADE80) else Color(0xFFF87171)
                )
            }

            LinearProgressIndicator(
                progress = { partResult.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (partResult.isPassed) Color(0xFF22C55E) else Color(0xFFEF4444),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun MistakeDetailCard(
    question: Question,
    userSelectedIndex: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, WrongRedBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = question.id,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = question.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = question.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            // User's Wrong Answer
            val userAnsText = if (userSelectedIndex != null && userSelectedIndex in question.answers.indices) {
                question.answers[userSelectedIndex]
            } else {
                "Keine Antwort ausgewählt (offen gelassen)"
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WrongRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, WrongRedBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            text = "Deine Antwort (falsch):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = userAnsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Correct Answer
            val correctAnsText = question.answers.getOrElse(question.correctAnswerIndex) { "" }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CorrectGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, CorrectGreenBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            text = "Richtige Antwort:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF22C55E)
                        )
                        Text(
                            text = correctAnsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (question.explanation.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
