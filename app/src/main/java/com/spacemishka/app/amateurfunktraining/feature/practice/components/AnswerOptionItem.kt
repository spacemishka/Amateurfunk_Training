package com.spacemishka.app.amateurfunktraining.feature.practice.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreen
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreenBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreenContainer
import com.spacemishka.app.amateurfunktraining.ui.theme.NeutralOptionBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.SelectedOptionContainer
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRed
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRedBorder
import com.spacemishka.app.amateurfunktraining.ui.theme.WrongRedContainer

enum class AnswerItemVisualState {
    DEFAULT,
    SELECTED_PENDING,
    CORRECT,
    WRONG,
    DIMMED
}

@Composable
fun AnswerOptionItem(
    optionLetter: String, // "A", "B", "C", "D"
    optionText: String,
    visualState: AnswerItemVisualState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val targetBorderColor = when (visualState) {
        AnswerItemVisualState.DEFAULT -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        AnswerItemVisualState.SELECTED_PENDING -> RadioBlue
        AnswerItemVisualState.CORRECT -> CorrectGreenBorder
        AnswerItemVisualState.WRONG -> WrongRedBorder
        AnswerItemVisualState.DIMMED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val targetContainerColor = when (visualState) {
        AnswerItemVisualState.DEFAULT -> MaterialTheme.colorScheme.surface
        AnswerItemVisualState.SELECTED_PENDING -> SelectedOptionContainer
        AnswerItemVisualState.CORRECT -> CorrectGreenContainer
        AnswerItemVisualState.WRONG -> WrongRedContainer
        AnswerItemVisualState.DIMMED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "BorderColor"
    )

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 200),
        label = "ContainerColor"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (visualState == AnswerItemVisualState.CORRECT || visualState == AnswerItemVisualState.WRONG) 2.dp else 1.dp,
                color = animatedBorderColor,
                shape = shape
            )
            .background(animatedContainerColor)
            .semantics(mergeDescendants = true) {
                contentDescription = "Option $optionLetter"
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OptionLetterBadge(
                letter = optionLetter,
                visualState = visualState
            )

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp,
                    fontWeight = if (visualState == AnswerItemVisualState.CORRECT) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = when (visualState) {
                    AnswerItemVisualState.CORRECT -> CorrectGreen
                    AnswerItemVisualState.WRONG -> WrongRed
                    AnswerItemVisualState.DIMMED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            if (visualState == AnswerItemVisualState.CORRECT) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Richtig",
                    tint = CorrectGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else if (visualState == AnswerItemVisualState.WRONG) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Falsch",
                    tint = WrongRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionLetterBadge(
    letter: String,
    visualState: AnswerItemVisualState
) {
    val (bgColor, textColor) = when (visualState) {
        AnswerItemVisualState.DEFAULT -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        AnswerItemVisualState.SELECTED_PENDING -> Pair(RadioBlue, Color.White)
        AnswerItemVisualState.CORRECT -> Pair(CorrectGreen, Color.White)
        AnswerItemVisualState.WRONG -> Pair(WrongRed, Color.White)
        AnswerItemVisualState.DIMMED -> Pair(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}
