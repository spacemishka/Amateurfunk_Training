package com.spacemishka.app.amateurfunktraining.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width size classifications based on standard Android Material 3 guidelines.
 */
enum class WindowWidthTier {
    /** Phone portrait (< 600 dp) */
    COMPACT,
    /** Foldables, large phones landscape, small tablets (600 dp .. 839 dp) */
    MEDIUM,
    /** Tablets, landscape tablets, desktops (>= 840 dp) */
    EXPANDED
}

/**
 * Remembers the current window width tier based on screen width in dp.
 */
@Composable
fun rememberWindowWidthTier(): WindowWidthTier {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    return when {
        screenWidth < 600.dp -> WindowWidthTier.COMPACT
        screenWidth < 840.dp -> WindowWidthTier.MEDIUM
        else -> WindowWidthTier.EXPANDED
    }
}

/**
 * Convenience helper to determine if the current window is tablet-sized (>= 600 dp).
 */
@Composable
fun isTabletLayout(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= 600
}

/**
 * Convenience helper to determine if the current window is an expanded tablet / desktop (>= 840 dp).
 */
@Composable
fun isExpandedLayout(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= 840
}

/**
 * Centers content horizontally and constrains its maximum width to prevent over-stretching
 * on large tablet and desktop screens.
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1200.dp,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth)
        ) {
            content()
        }
    }
}

