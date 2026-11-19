package com.spacemishka.app.amateurfunktraining.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.core.model.QuestionProgress
import com.spacemishka.app.amateurfunktraining.feature.practice.components.ExplanationBox
import com.spacemishka.app.amateurfunktraining.feature.practice.components.QuestionImageView
import com.spacemishka.app.amateurfunktraining.ui.layout.isTabletLayout
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryBetrieb
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryTechnik
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryVorschriften
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreen
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreenBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreenContainer
import com.spacemishka.app.amateurfunktraining.ui.theme.NeutralOptionBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioNavy
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed
import com.spacemishka.app.amateurfunktraining.ui.theme.radioBlueButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSearchScreen(
    viewModel: QuestionSearchViewModel,
    onNavigateBack: () -> Unit,
    onPracticeQuestions: (questionIds: List<String>, initialQuestionId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val isTablet = isTabletLayout()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fragen suchen",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Zurück" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (state.query.isNotEmpty() || state.selectedCategory != null || state.onlyBookmarked || state.onlyMistakes || state.onlyWithImage || state.selectedLeitnerBox != null) {
                        IconButton(
                            onClick = {
                                viewModel.clearFilters()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.semantics { contentDescription = "Filter zurücksetzen" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            // Search Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isTablet) 800.dp else 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text("Frage-ID (z. B. EI502) oder Text suchen...")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = RadioBlue
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Suche löschen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RadioBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Category Filter Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Alle" FilterChip
                val allCount = state.categoryCounts[null] ?: 0
                FilterChip(
                    selected = state.selectedCategory == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text("Alle ($allCount)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RadioBlue,
                        selectedLabelColor = Color.White
                    )
                )

                // Category chips
                Category.entries.filter { it != Category.ALL }.forEach { category ->
                    val isSelected = state.selectedCategory == category
                    val count = state.categoryCounts[category] ?: 0
                    val badgeColor = when (category) {
                        Category.TECHNIK -> CategoryTechnik
                        Category.BETRIEB -> CategoryBetrieb
                        Category.VORSCHRIFTEN -> CategoryVorschriften
                        else -> RadioBlue
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text("${category.displayName} ($count)") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = badgeColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Quick Toggle Filters (Lesezeichen, Fehler, Grafik, Stufen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nur Gemerkt
                FilterChip(
                    selected = state.onlyBookmarked,
                    onClick = { viewModel.toggleOnlyBookmarked() },
                    label = { Text("⭐ Gemerkt") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFD97706)
                    )
                )

                // Nur Problemfragen
                FilterChip(
                    selected = state.onlyMistakes,
                    onClick = { viewModel.toggleOnlyMistakes() },
                    label = { Text("⚠️ Mit Fehlern") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WrongRed.copy(alpha = 0.2f),
                        selectedLabelColor = WrongRed
                    )
                )

                // Nur mit Grafik
                FilterChip(
                    selected = state.onlyWithImage,
                    onClick = { viewModel.toggleOnlyWithImage() },
                    label = { Text("📷 Mit Grafik") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RadioBlue.copy(alpha = 0.2f),
                        selectedLabelColor = RadioBlue
                    )
                )

                // Leitner Stufen (1..5)
                (1..5).forEach { box ->
                    val isSelected = state.selectedLeitnerBox == box
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onLeitnerBoxSelected(box) },
                        label = { Text("Stufe $box") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadioBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Header: Results count and "Alle Fragen üben" action
            if (state.totalFoundCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.totalFoundCount} ${if (state.totalFoundCount == 1) "Frage gefunden" else "Fragen gefunden"}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val ids = state.results.map { it.question.id }
                            onPracticeQuestions(ids, null)
                        },
                        colors = radioBlueButtonColors(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.totalFoundCount == 1) "Frage üben" else "Ergebnisse üben",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Content: Results List or Loading / Empty state
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RadioBlue)
                }
            } else if (state.results.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Keine Fragen gefunden",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (state.query.isNotBlank())
                                "Für deine Suche „${state.query}“ wurden keine Übungsfragen gefunden."
                            else
                                "Mit den gewählten Filtern stimmen keine Fragen überein.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        OutlinedButton(
                            onClick = { viewModel.clearFilters() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Filter & Suche zurücksetzen")
                        }
                    }
                }
            } else {
                // Results list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.results,
                        key = { it.question.id }
                    ) { item ->
                        SearchResultQuestionCard(
                            item = item,
                            onToggleExpand = { viewModel.toggleCardExpanded(item.question.id) },
                            onToggleBookmark = { viewModel.toggleBookmark(item.question.id) },
                            onPracticeThis = {
                                val allIds = state.results.map { it.question.id }
                                onPracticeQuestions(allIds, item.question.id)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultQuestionCard(
    item: QuestionSearchResultItem,
    onToggleExpand: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPracticeThis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val q = item.question
    val progress = item.progress
    val isExpanded = item.isExpanded

    val categoryColor = when (q.category) {
        Category.TECHNIK -> CategoryTechnik
        Category.BETRIEB -> CategoryBetrieb
        Category.VORSCHRIFTEN -> CategoryVorschriften
        else -> RadioBlue
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) RadioBlue.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: ID, Category, Badges, Bookmark star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Question ID badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RadioNavy
                    ) {
                        Text(
                            text = q.id,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = q.category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // Leitner Stufe badge
                    val box = progress?.leitnerBox ?: 1
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Stufe $box",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // Image badge if present
                    if (!q.imageSvg.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RadioBlue.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = RadioBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Grafik",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RadioBlue
                                )
                            }
                        }
                    }
                }

                // Bookmark icon button
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(36.dp).semantics {
                        contentDescription = if (progress?.isBookmarked == true) "Lesezeichen entfernen" else "Lesezeichen hinzufügen"
                    }
                ) {
                    Icon(
                        imageVector = if (progress?.isBookmarked == true) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (progress?.isBookmarked == true) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Question Text
            Text(
                text = q.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expand/Collapse Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isExpanded) "Details & Lösung verbergen" else "Lösung & Details anzeigen",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = RadioBlue
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = RadioBlue,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation)
                )
            }

            // Expanded Details (Image, 4 Answers, Explanation, Practice Button)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circuit diagram / image if available
                    if (!q.imageSvg.isNullOrBlank()) {
                        QuestionImageView(
                            imagePath = q.imageSvg,
                            questionId = q.id
                        )
                    }

                    // 4 Answers with correct one highlighted
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val letters = listOf("A", "B", "C", "D")
                        q.answers.forEachIndexed { index, answerText ->
                            val isCorrect = index == q.correctAnswerIndex
                            val letter = letters.getOrElse(index) { "$index" }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCorrect) CorrectGreenContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isCorrect) 1.5.dp else 1.dp,
                                    color = if (isCorrect) CorrectGreenBorder else NeutralOptionBorder.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCorrect) CorrectGreen else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = letter,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isCorrect) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = answerText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isCorrect) CorrectGreen else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Richtige Antwort",
                                            tint = CorrectGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Explanation Box
                    if (q.explanation.isNotBlank()) {
                        ExplanationBox(
                            explanation = q.explanation,
                            visible = true
                        )
                    }

                    // Action: Diese Frage üben
                    Button(
                        onClick = onPracticeThis,
                        colors = radioBlueButtonColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Diese Frage üben (${q.id})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
