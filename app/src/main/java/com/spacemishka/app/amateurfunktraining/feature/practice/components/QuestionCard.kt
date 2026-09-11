package com.spacemishka.app.amateurfunktraining.feature.practice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacemishka.app.amateurfunktraining.core.model.Category
import com.spacemishka.app.amateurfunktraining.core.model.Question
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryBetrieb
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryTechnik
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryVorschriften
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed

@Composable
fun QuestionCard(
    question: Question,
    modifier: Modifier = Modifier,
    leitnerBox: Int = 1,
    errorCount: Int = 0
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(category = question.category)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (errorCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WrongRed.copy(alpha = 0.15f),
                            modifier = Modifier.semantics {
                                contentDescription = "$errorCount Fehler"
                            }
                        ) {
                            Text(
                                text = "⚠ $errorCount",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = WrongRed
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.semantics {
                            contentDescription = "Leitner Stufe $leitnerBox von 5"
                        }
                    ) {
                        Text(
                            text = "Stufe $leitnerBox/5",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = question.id,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = question.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (category) {
        Category.TECHNIK -> Pair(CategoryTechnik.copy(alpha = 0.15f), CategoryTechnik)
        Category.BETRIEB -> Pair(CategoryBetrieb.copy(alpha = 0.15f), CategoryBetrieb)
        Category.VORSCHRIFTEN -> Pair(CategoryVorschriften.copy(alpha = 0.15f), CategoryVorschriften)
        Category.ALL -> Pair(Color.Gray.copy(alpha = 0.15f), Color.DarkGray)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = category.displayName,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
