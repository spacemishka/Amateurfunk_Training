package com.spacemishka.app.amateurfunktraining.feature.phonetic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreen
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneticQuizScreen(
    viewModel: PhoneticQuizViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ITU-Buchstabier-Trainer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Zurück" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startQuiz() },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Quiz neu starten" }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        label = { Text(mode.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadioBlue.copy(alpha = 0.15f),
                            selectedLabelColor = RadioBlue
                        )
                    )
                }
            }

            if (state.isFinished) {
                QuizResultCard(
                    score = state.score,
                    total = state.questions.size,
                    onRestart = { viewModel.startQuiz() },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                val current = state.currentQuestion
                if (current != null) {
                    val progress = (state.currentIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Progress bar & counters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Frage ${state.currentIndex + 1} von ${state.questions.size}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Richtig: ${state.score}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CorrectGreen
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = RadioBlue,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        // Big Prompt Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = current.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, RadioBlue.copy(alpha = 0.3f)),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = current.prompt,
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (current.prompt.length > 2) 32.sp else 48.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = RadioBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4 Option Cards
                        items(current.options.size) { index ->
                            val optionText = current.options[index]
                            val isSelected = state.selectedOptionIndex == index
                            val isCorrect = index == current.correctIndex

                            val (bgColor, borderColor, textColor) = when {
                                state.isSubmitted && isCorrect -> Triple(
                                    CorrectGreen.copy(alpha = 0.15f),
                                    CorrectGreen,
                                    CorrectGreen
                                )
                                state.isSubmitted && isSelected && !isCorrect -> Triple(
                                    WrongRed.copy(alpha = 0.15f),
                                    WrongRed,
                                    WrongRed
                                )
                                isSelected -> Triple(
                                    RadioBlue.copy(alpha = 0.15f),
                                    RadioBlue,
                                    RadioBlue
                                )
                                else -> Triple(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !state.isSubmitted) {
                                        viewModel.selectOption(index)
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Option ${index + 1}: $optionText${
                                            if (state.isSubmitted) {
                                                if (isCorrect) ", Richtig" else if (isSelected) ", Falsch" else ""
                                            } else if (isSelected) ", Ausgewählt" else ""
                                        }"
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                border = BorderStroke(if (isSelected || (state.isSubmitted && isCorrect)) 2.dp else 1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected || (state.isSubmitted && isCorrect)) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = textColor
                                    )

                                    if (state.isSubmitted && isCorrect) {
                                        Icon(Icons.Default.Check, contentDescription = "Richtig", tint = CorrectGreen)
                                    } else if (state.isSubmitted && isSelected && !isCorrect) {
                                        Icon(Icons.Default.Close, contentDescription = "Falsch", tint = WrongRed)
                                    }
                                }
                            }
                        }

                        // Explanation Box after submit
                        item {
                            AnimatedVisibility(
                                visible = state.isSubmitted,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = current.explanation,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        // Action button (Bestätigen or Weiter)
                        item {
                            if (!state.isSubmitted) {
                                Button(
                                    onClick = { viewModel.submitAnswer() },
                                    enabled = state.selectedOptionIndex != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = RadioBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .semantics { contentDescription = "Antwort überprüfen" }
                                ) {
                                    Text("Antwort überprüfen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.nextQuestion() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RadioBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .semantics { contentDescription = "Nächste Frage" }
                                ) {
                                    Text(
                                        text = if (state.currentIndex + 1 < state.questions.size) "Nächste Frage" else "Ergebnis ansehen",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultCard(
    score: Int,
    total: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = (score.toFloat() / total.coerceAtLeast(1) * 100).toInt()
    val isPassed = percentage >= 75

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) CorrectGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPassed) "🏆" else "📚",
                    fontSize = 32.sp
                )
            }

            Text(
                text = if (isPassed) "Hervorragend buchstabiert!" else "Guter Durchgang!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "$score von $total Fragen richtig beantwortet ($percentage %)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = RadioBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Runde wiederholen", fontWeight = FontWeight.Bold)
            }
        }
    }
}
