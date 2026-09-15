package com.spacemishka.app.amateurfunktraining.feature.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Calculate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacemishka.app.amateurfunktraining.feature.calculator.CalculatorBottomSheet
import com.spacemishka.app.amateurfunktraining.feature.calculator.CalculatorViewModel
import com.spacemishka.app.amateurfunktraining.feature.practice.components.AnswerItemVisualState
import com.spacemishka.app.amateurfunktraining.feature.practice.components.AnswerOptionItem
import com.spacemishka.app.amateurfunktraining.feature.practice.components.ExplanationBox
import com.spacemishka.app.amateurfunktraining.feature.practice.components.QuestionCard
import com.spacemishka.app.amateurfunktraining.feature.topic.components.TopicBottomSheet
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreen
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showTopicSheet by remember { mutableStateOf(false) }
    var showCalculatorSheet by remember { mutableStateOf(false) }
    val calculatorViewModel: CalculatorViewModel = viewModel()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.practiceMode.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!state.isFinished && state.totalCount > 0) {
                            Text(
                                text = "Frage ${state.currentIndex + 1} von ${state.totalCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Zurück zur Übersicht"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (!state.isFinished && state.currentQuestion != null) {
                        IconButton(
                            onClick = { showCalculatorSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Formel-Rechner öffnen" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = RadioBlue
                            )
                        }

                        val bookmarkDesc = if (state.isBookmarked) "Lesezeichen entfernen" else "Lesezeichen setzen"
                        IconButton(
                            onClick = { viewModel.toggleBookmark() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = bookmarkDesc,
                                tint = if (state.isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!state.isFinished && state.currentQuestion != null) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                showTopicSheet = false
                                viewModel.nextQuestion()
                            },
                            enabled = state.isAnswerConfirmed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .semantics {
                                    contentDescription = if (state.currentIndex + 1 >= state.totalCount) "Übung abschließen" else "Nächste Frage"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadioBlue
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (state.currentIndex + 1 >= state.totalCount) "Übung abschließen" else "Nächste Frage",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!state.isFinished && state.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = RadioBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(color = RadioBlue)
                    }

                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage ?: "Ein unbekannter Fehler ist aufgetreten.",
                            color = WrongRed,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    state.isFinished -> {
                        SessionFinishedView(
                            correctCount = state.correctCount,
                            wrongCount = state.wrongCount,
                            totalCount = state.totalCount,
                            onRetryWrong = { viewModel.retryWrongQuestions() },
                            onFinish = onNavigateBack
                        )
                    }

                    state.questions.isEmpty() -> {
                        EmptyPracticeView(
                            mode = state.practiceMode,
                            onFinish = onNavigateBack
                        )
                    }

                    state.currentQuestion != null -> {
                        val question = state.currentQuestion!!
                        val optionLetters = listOf("A", "B", "C", "D")

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            QuestionCard(
                                question = question,
                                leitnerBox = state.currentLeitnerBox,
                                errorCount = state.currentErrorCount
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            question.answers.forEachIndexed { index, answerText ->
                                val visualState = when {
                                    !state.isAnswerConfirmed -> {
                                        if (state.selectedAnswerIndex == index) AnswerItemVisualState.SELECTED_PENDING
                                        else AnswerItemVisualState.DEFAULT
                                    }
                                    index == question.correctAnswerIndex -> AnswerItemVisualState.CORRECT
                                    state.selectedAnswerIndex == index -> AnswerItemVisualState.WRONG
                                    else -> AnswerItemVisualState.DIMMED
                                }

                                AnswerOptionItem(
                                    optionLetter = optionLetters.getOrElse(index) { "?" },
                                    optionText = answerText,
                                    visualState = visualState,
                                    enabled = !state.isAnswerConfirmed,
                                    onClick = { viewModel.selectAnswer(index) }
                                )
                            }

                            ExplanationBox(
                                explanation = question.explanation,
                                visible = state.isAnswerConfirmed
                            )

                            if (state.isAnswerConfirmed && state.currentTopic != null) {
                                val currentTopic = state.currentTopic!!
                                OutlinedButton(
                                    onClick = {
                                        showTopicSheet = true
                                        viewModel.markTopicAsViewed(currentTopic.id)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = "Hintergrundwissen: ${currentTopic.title}"
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RadioBlue),
                                    border = BorderStroke(1.5.dp, RadioBlue.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = RadioBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Kurz erklärt: ${currentTopic.title}",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showTopicSheet && state.currentTopic != null) {
        TopicBottomSheet(
            topic = state.currentTopic!!,
            onDismissRequest = { showTopicSheet = false }
        )
    }

    if (showCalculatorSheet) {
        CalculatorBottomSheet(
            viewModel = calculatorViewModel,
            onDismissRequest = { showCalculatorSheet = false }
        )
    }
}

@Composable
private fun EmptyPracticeView(
    mode: PracticeMode,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, description) = when (mode) {
        is PracticeMode.Bookmarks -> Pair("Keine Lesezeichen gemerkt", "Markiere Fragen während der Übung mit dem Lesezeichen-Symbol, um sie hier gezielt zu wiederholen.")
        is PracticeMode.ProblemQuestions -> Pair("Keine Problemfragen vorhanden", "Fragen, die du 2-mal oder öfter falsch beantwortest, landen automatisch in dieser Liste.")
        is PracticeMode.DueLeitner -> Pair("Keine fälligen Fragen heute", "Großartig! Du hast alle heute fälligen Leitner-Wiederholungen bereits abgeschlossen.")
        is PracticeMode.CategoryMode -> Pair("Keine Fragen gefunden", "Für dieses Fach sind aktuell keine Fragen verfügbar.")
        is PracticeMode.ExamMistakes -> Pair("Keine Fehlfragen vorhanden", "In der letzten Prüfungssimulation gab es keine Fehlfragen oder alle wurden gemeistert.")
        is PracticeMode.TopicQuestions -> Pair("Keine Fragen gefunden", "Für dieses Thema wurden aktuell keine Prüfungsfragen gefunden.")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = RadioBlue,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RadioBlue)
            ) {
                Text("Zurück zur Übersicht", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SessionFinishedView(
    correctCount: Int,
    wrongCount: Int,
    totalCount: Int,
    onRetryWrong: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successRate = if (totalCount > 0) (correctCount.toFloat() / totalCount.toFloat() * 100).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (successRate >= 75) CorrectGreen else WrongRed,
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = if (successRate >= 75) "Klasse Leistung!" else "Übung beendet",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Du hast $correctCount von $totalCount Fragen richtig beantwortet ($successRate %).",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreBadge(title = "Richtig", count = correctCount, color = CorrectGreen)
            ScoreBadge(title = "Falsch", count = wrongCount, color = WrongRed)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (wrongCount > 0) {
            Button(
                onClick = onRetryWrong,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WrongRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Text("Fehler wiederholen ($wrongCount)", fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Zurück zur Übersicht", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScoreBadge(
    title: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}
