package com.spacemishka.app.amateurfunktraining.feature.exam

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.core.exam.ExamTimerManager
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryBetrieb
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryTechnik
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryVorschriften
import com.spacemishka.app.amateurfunktraining.ui.theme.NeutralOptionBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioNavy
import com.spacemishka.app.amateurfunktraining.ui.theme.SelectedOptionBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.SelectedOptionContainer
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: ExamViewModel,
    onExamFinished: (ExamResult) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler {
        viewModel.setShowCancelDialog(true)
    }

    LaunchedEffect(state.isFinished, state.examResult) {
        if (state.isFinished && state.examResult != null) {
            onExamFinished(state.examResult!!)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ExamTimerManager.formatTime(state.remainingSeconds),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (state.isWarningZone) WrongRed else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics {
                                contentDescription = "Verbleibende Prüfungszeit ${ExamTimerManager.formatTime(state.remainingSeconds)}"
                            }
                        )
                        if (state.isWarningZone) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WrongRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Zeit knapp!",
                                    color = WrongRed,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setShowCancelDialog(true) },
                        modifier = Modifier.semantics { contentDescription = "Prüfung abbrechen" }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Abbrechen")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setShowSubmitDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = RadioBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .semantics { contentDescription = "Prüfung abgeben" }
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abgeben", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RadioBlue)
            }
        } else if (state.questions.isNotEmpty()) {
            val curQuestionState = state.currentQuestionState ?: return@Scaffold
            val question = curQuestionState.question

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Questions Strip / Matrix
                ExamQuestionMatrix(
                    questions = state.questions,
                    currentIndex = state.currentIndex,
                    onSelectIndex = { viewModel.goToQuestion(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp)
                )

                // Sub-header Bar: Subject, Index, and Review Bookmark
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val catColor = when (question.category) {
                        Category.TECHNIK -> CategoryTechnik
                        Category.BETRIEB -> CategoryBetrieb
                        Category.VORSCHRIFTEN -> CategoryVorschriften
                        else -> RadioNavy
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = question.category.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Frage ${state.currentIndex + 1} von ${state.totalCount}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mark for review toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (curQuestionState.isMarkedForReview) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (curQuestionState.isMarkedForReview) Color(0xFFD97706) else Color.LightGray),
                        modifier = Modifier
                            .clickable { viewModel.toggleMarkForReview() }
                            .semantics {
                                contentDescription = if (curQuestionState.isMarkedForReview) "Zur Überprüfung markiert" else "Für Überprüfung merken"
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (curQuestionState.isMarkedForReview) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (curQuestionState.isMarkedForReview) Color(0xFFD97706) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Merken",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (curQuestionState.isMarkedForReview) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Question Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = question.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 4 Options
                    val optionLetters = listOf("A", "B", "C", "D")
                    question.answers.forEachIndexed { optIndex, optionText ->
                        val isSelected = curQuestionState.selectedAnswerIndex == optIndex

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectAnswer(optIndex) }
                                .semantics {
                                    contentDescription = "Antwort ${optionLetters.getOrElse(optIndex) { "" }}: $optionText"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAnswer(optIndex) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .padding(end = 4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = optionLetters.getOrElse(optIndex) { "" },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom Navigation Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousQuestion() },
                            enabled = state.currentIndex > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.semantics { contentDescription = "Vorherige Frage" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zurück")
                        }

                        Text(
                            text = "${state.answeredCount} / ${state.totalCount} beantwortet",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                if (state.currentIndex < state.totalCount - 1) {
                                    viewModel.nextQuestion()
                                } else {
                                    viewModel.setShowSubmitDialog(true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RadioBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.semantics {
                                contentDescription = if (state.currentIndex < state.totalCount - 1) "Nächste Frage" else "Zur Abgabe"
                            }
                        ) {
                            Text(if (state.currentIndex < state.totalCount - 1) "Weiter" else "Fertig")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                if (state.currentIndex < state.totalCount - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Submit Dialog
    if (state.showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowSubmitDialog(false) },
            title = { Text("Prüfung abgeben?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Du hast ${state.answeredCount} von ${state.totalCount} Fragen beantwortet.")
                    if (state.unansweredCount > 0) {
                        Text(
                            text = "⚠️ Achtung: ${state.unansweredCount} Fragen sind noch unbeantwortet! Unbeantwortete Fragen werden als falsch gewertet.",
                            color = WrongRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text("Alle Fragen wurden beantwortet. Bereit zur offiziellen Auswertung?")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitExam() },
                    colors = ButtonDefaults.buttonColors(containerColor = RadioBlue)
                ) {
                    Text("Jetzt abgeben")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowSubmitDialog(false) }) {
                    Text("Weiterbearbeiten")
                }
            }
        )
    }

    // Cancel Dialog
    if (state.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowCancelDialog(false) },
            title = { Text("Prüfungssimulation abbrechen?") },
            text = {
                Text("Möchtest du die Prüfung wirklich abbrechen? Dein bisheriger Bearbeitungsstand geht dabei verloren.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelExam()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WrongRed)
                ) {
                    Text("Prüfung abbrechen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowCancelDialog(false) }) {
                    Text("Fortsetzen")
                }
            }
        )
    }
}

@Composable
private fun ExamQuestionMatrix(
    questions: List<com.spacemishka.app.amateurfunktraining.core.model.ExamQuestionState>,
    currentIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        val target = (currentIndex - 3).coerceAtLeast(0)
        listState.animateScrollToItem(target)
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(questions.size) { idx ->
            val qState = questions[idx]
            val isCurrent = idx == currentIndex

            val bgColor = when {
                qState.isMarkedForReview -> Color(0xFFF59E0B) // Amber
                qState.isAnswered -> RadioBlue
                else -> Color(0xFFE2E8F0) // Muted gray
            }

            val textColor = when {
                qState.isMarkedForReview || qState.isAnswered -> Color.White
                else -> Color(0xFF334155)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = bgColor,
                border = if (isCurrent) BorderStroke(2.dp, RadioNavy) else null,
                modifier = Modifier
                    .size(width = 36.dp, height = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectIndex(idx) }
                    .semantics {
                        contentDescription = "Frage ${idx + 1}: ${if (qState.isAnswered) "Beantwortet" else "Offen"}${if (qState.isMarkedForReview) ", gemerkt" else ""}"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}
