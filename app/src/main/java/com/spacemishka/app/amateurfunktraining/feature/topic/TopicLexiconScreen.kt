package com.spacemishka.app.amateurfunktraining.feature.topic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Topic
import com.spacemishka.app.amateurfunktraining.feature.topic.components.TopicContentView
import com.spacemishka.app.amateurfunktraining.ui.layout.isTabletLayout
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryAll
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryBetrieb
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryTechnik
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryVorschriften
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicLexiconScreen(
    viewModel: TopicLexiconViewModel,
    onSelectTopic: (String) -> Unit,
    onStartPractice: ((String, String) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isTablet = isTabletLayout()
    var selectedTopicId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Themenlexikon",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Kurz erklärt • ${state.totalTopicsCount} Themen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        } else if (isTablet) {
            // Master-Detail Tablet Layout
            val effectiveSelectedTopic = state.topics.firstOrNull { it.topic.id == selectedTopicId }
                ?: state.topics.firstOrNull()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Linke Spalte: Suchleiste, Filter und Themenliste
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Themen durchsuchen"
                                    },
                                placeholder = { Text("Themen durchsuchen...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = RadioBlue
                                    )
                                },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Suchtext löschen"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RadioBlue,
                                    focusedLabelColor = RadioBlue
                                )
                            )
                        }

                        // Filter Chips
                        item {
                            val categories = listOf(
                                Category.ALL,
                                Category.TECHNIK,
                                Category.BETRIEB,
                                Category.VORSCHRIFTEN
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { category ->
                                    val selected = state.selectedCategory == category
                                    val (catColor, label) = when (category) {
                                        Category.ALL -> Pair(CategoryAll, "Alle")
                                        Category.TECHNIK -> Pair(CategoryTechnik, "Technik")
                                        Category.BETRIEB -> Pair(CategoryBetrieb, "Betrieb")
                                        Category.VORSCHRIFTEN -> Pair(CategoryVorschriften, "Vorschriften")
                                    }

                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.onCategorySelected(category) },
                                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = catColor.copy(alpha = 0.2f),
                                            selectedLabelColor = catColor
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = if (state.searchQuery.isNotEmpty()) "Suchergebnisse (${state.topics.size})"
                                else "${state.selectedCategory.displayName} (${state.topics.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (state.topics.isEmpty()) {
                            item {
                                EmptyTopicsView(
                                    query = state.searchQuery,
                                    onReset = {
                                        viewModel.onSearchQueryChanged("")
                                        viewModel.onCategorySelected(Category.ALL)
                                    }
                                )
                            }
                        } else {
                            items(state.topics, key = { it.topic.id }) { item ->
                                val isSelected = effectiveSelectedTopic?.topic?.id == item.topic.id
                                TopicListItemCard(
                                    item = item,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedTopicId = item.topic.id
                                        viewModel.markTopicViewed(item.topic.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // Rechte Spalte: Detailansicht des ausgewählten Themas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (effectiveSelectedTopic == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Wählen Sie ein Thema aus der Liste.")
                        }
                    } else {
                        val topic = effectiveSelectedTopic.topic
                        val qCount = effectiveSelectedTopic.questionCount

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 900.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            TopicContentView(
                                topic = topic,
                                onSelectRelatedTopic = { relatedId ->
                                    selectedTopicId = relatedId
                                    viewModel.markTopicViewed(relatedId)
                                }
                            )

                            // Action Card: Wissen trainieren
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "WISSEN TRAINIEREN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = "Zu diesem Thema gibt es $qCount offizielle Prüfungsfragen der Bundesnetzagentur.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Button(
                                        onClick = {
                                            if (onStartPractice != null) {
                                                onStartPractice(topic.id, topic.title)
                                            } else {
                                                onSelectTopic(topic.id)
                                            }
                                        },
                                        enabled = qCount > 0,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RadioBlue)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null
                                            )
                                            Text(
                                                text = "Fragen zu diesem Thema üben ($qCount)",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Themen durchsuchen"
                            },
                        placeholder = { Text("Thema, Formel, Begriff suchen...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Suche löschen"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadioBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Category Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf(Category.ALL, Category.TECHNIK, Category.BETRIEB, Category.VORSCHRIFTEN)
                        categories.forEach { category ->
                            val selected = (state.selectedCategory == category)
                            val (catColor, label) = when (category) {
                                Category.ALL -> Pair(CategoryAll, "Alle")
                                Category.TECHNIK -> Pair(CategoryTechnik, "Technik")
                                Category.BETRIEB -> Pair(CategoryBetrieb, "Betrieb")
                                Category.VORSCHRIFTEN -> Pair(CategoryVorschriften, "Vorschriften")
                            }

                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onCategorySelected(category) },
                                label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(alpha = 0.2f),
                                    selectedLabelColor = catColor
                                )
                            )
                        }
                    }
                }

                // Section: Zuletzt gelesen (Horizontal Scroll)
                if (state.searchQuery.isEmpty() && state.recentTopics.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = RadioBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Zuletzt gelesen",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.recentTopics, key = { it.id }) { topic ->
                                    RecentTopicCard(
                                        topic = topic,
                                        onClick = {
                                            viewModel.markTopicViewed(topic.id)
                                            onSelectTopic(topic.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Header for main list
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "Suchergebnisse (${state.topics.size})"
                            else "${state.selectedCategory.displayName} (${state.topics.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Topic Items
                if (state.topics.isEmpty()) {
                    item {
                        EmptyTopicsView(
                            query = state.searchQuery,
                            onReset = {
                                viewModel.onSearchQueryChanged("")
                                viewModel.onCategorySelected(Category.ALL)
                            }
                        )
                    }
                } else {
                    items(state.topics, key = { it.topic.id }) { item ->
                        TopicListItemCard(
                            item = item,
                            onClick = {
                                viewModel.markTopicViewed(item.topic.id)
                                onSelectTopic(item.topic.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTopicCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (topic.category) {
        Category.TECHNIK -> CategoryTechnik
        Category.BETRIEB -> CategoryBetrieb
        Category.VORSCHRIFTEN -> CategoryVorschriften
        Category.ALL -> CategoryAll
    }

    Card(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Zuletzt gelesen: ${topic.title}"
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = categoryColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = topic.category.displayName,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = categoryColor
                )
            }

            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TopicListItemCard(
    item: TopicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val topic = item.topic
    val categoryColor = when (topic.category) {
        Category.TECHNIK -> CategoryTechnik
        Category.BETRIEB -> CategoryBetrieb
        Category.VORSCHRIFTEN -> CategoryVorschriften
        Category.ALL -> CategoryAll
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${topic.title}, Kategorie ${topic.category.displayName}, ${item.questionCount} Fragen"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = topic.category.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = categoryColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RadioBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${item.questionCount} Fragen",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = RadioBlue
                    )
                }
            }

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = topic.keyTakeaway,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Themenkarte ansehen",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = RadioBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = RadioBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTopicsView(
    query: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = "Keine Themen gefunden",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (query.isNotEmpty()) "Keine Ergebnisse für „$query“ in dieser Kategorie."
                else "Für die gewählte Kategorie liegen keine Themenkarten vor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onReset,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RadioBlue)
            ) {
                Text("Filter zurücksetzen", fontWeight = FontWeight.Bold)
            }
        }
    }
}
