package com.spacemishka.app.amateurfunktraining.feature.practice.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Renders an official circuit schematic, chart, or diagram from app assets.
 * Circuit schematics are rendered with high contrast on a clean light container
 * ensuring optimal visibility in both light and dark themes.
 */
@Composable
fun QuestionImageView(
    imagePath: String,
    questionId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(imagePath) {
        try {
            context.assets.open(imagePath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Schaltskizze oder Abbildung zur Prüfungsfrage $questionId",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 260.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

