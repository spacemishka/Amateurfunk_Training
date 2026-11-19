package com.spacemishka.app.amateurfunktraining.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spacemishka.app.amateurfunktraining.R
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryBetrieb
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryTechnik
import com.spacemishka.app.amateurfunktraining.ui.theme.CategoryVorschriften
import com.spacemishka.app.amateurfunktraining.ui.theme.CorrectGreen
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioBlue
import com.spacemishka.app.amateurfunktraining.ui.theme.RadioNavy
import com.spacemishka.app.amateurfunktraining.ui.theme.radioBlueButtonColors
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header Bar: Step indicator & Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge: Schritt X von Y
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RadioBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Schritt ${pagerState.currentPage + 1} von $PAGE_COUNT",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RadioBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (pagerState.currentPage < PAGE_COUNT - 1) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.semantics {
                                contentDescription = "Einführung überspringen"
                            }
                        ) {
                            Text(
                                text = "Überspringen",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp).semantics {
                                contentDescription = "Dialog schließen"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (page) {
                            0 -> WelcomePage()
                            1 -> LeitnerSystemPage()
                            2 -> ExamAndReferencePage()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pager Indicator Dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(PAGE_COUNT) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) RadioBlue
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Gehe zu Schritt ${index + 1}"
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Zurück / Weiter bzw. Los geht's
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Zurück")
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < PAGE_COUNT - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(if (pagerState.currentPage > 0) 1.5f else 1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = radioBlueButtonColors()
                    ) {
                        if (pagerState.currentPage < PAGE_COUNT - 1) {
                            Text("Weiter")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Los geht's!")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Logo
        Surface(
            shape = CircleShape,
            color = RadioNavy,
            shadowElevation = 4.dp,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Willkommen beim\nAmateurfunk-Training!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Dein smarter Begleiter zur erfolgreichen Prüfung für das Amateurfunkzeugnis der Klasse E nach offiziellem BNetzA-Katalog.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Features list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingFeatureCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = RadioBlue,
                title = "839 Originalprüfungsfragen",
                description = "Vollständiger Fragenkatalog mit allen originalen Schaltbildern, Formeln und Kennlinien."
            )
            OnboardingFeatureCard(
                icon = Icons.Default.WifiOff,
                iconTint = CorrectGreen,
                title = "100 % Offline & Werbefrei",
                description = "Funktioniert überall ohne Internetverbindung, ohne störende Werbung und ohne Registrierungszwang."
            )
            OnboardingFeatureCard(
                icon = Icons.Default.Security,
                iconTint = CategoryTechnik,
                title = "Volle Datenkontrolle",
                description = "Dein Lernfortschritt bleibt privat auf deinem Smartphone. Mit einfacher Backup- und Exportfunktion."
            )
        }
    }
}

@Composable
private fun LeitnerSystemPage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = RadioBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = RadioBlue,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Intelligentes\n5-Stufen-Lernsystem",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Effizient einprägen nach dem wissenschaftlich bewährten Leitner-Karteikasten-Prinzip (Spaced Repetition).",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Leitner Box Visual Representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Lernstufen-Aufstieg:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LeitnerLevelBadge(level = 1, label = "Start", color = CategoryTechnik)
                    Text("→", color = MaterialTheme.colorScheme.outline)
                    LeitnerLevelBadge(level = 2, label = "Basis", color = CategoryBetrieb)
                    Text("→", color = MaterialTheme.colorScheme.outline)
                    LeitnerLevelBadge(level = 3, label = "Übung", color = RadioBlue)
                    Text("→", color = MaterialTheme.colorScheme.outline)
                    LeitnerLevelBadge(level = 4, label = "Sicher", color = CategoryVorschriften)
                    Text("→", color = MaterialTheme.colorScheme.outline)
                    LeitnerLevelBadge(level = 5, label = "Meister", color = CorrectGreen)
                }
            }
        }

        // Feature highlights
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingFeatureCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = CorrectGreen,
                title = "Stufenweiser Lernerfolg",
                description = "Jede richtige Antwort befördert eine Frage eine Stufe nach oben – bis sie sicher in Stufe 5 sitzt."
            )
            OnboardingFeatureCard(
                icon = Icons.Default.Refresh,
                iconTint = CategoryTechnik,
                title = "Schwachstellen wiederholen",
                description = "Falsch beantwortete Fragen wandern automatisch zurück auf Stufe 1, damit du sie gezielt festigen kannst."
            )
            OnboardingFeatureCard(
                icon = Icons.Default.Bookmark,
                iconTint = RadioBlue,
                title = "Gezielte Lernmodi",
                description = "Trainiere wahlweise neue Fragen, Fehlerschwerpunkte, einzelne Sachgebiete oder gesetzte Lesezeichen."
            )
        }
    }
}

@Composable
private fun ExamAndReferencePage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = CorrectGreen.copy(alpha = 0.12f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = CorrectGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Prüfungssimulation\n& Nachschlagewerk",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Praxisnahe Vorbereitung auf den Prüfungstag und nützliche Tools für den Funkbetrieb.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Features list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingFeatureCard(
                icon = Icons.Default.Quiz,
                iconTint = RadioBlue,
                title = "Realistische Prüfungssimulation",
                description = "75 zufällige Fragen (Technik, Betrieb, Vorschriften) mit 150-Minuten-Timer und 73 % Bestehensgrenze."
            )
            OnboardingFeatureCard(
                icon = Icons.Default.Functions,
                iconTint = CategoryBetrieb,
                title = "Formelsammlung & Buchstabiertafel",
                description = "Schneller Zugriff auf alle BNetzA-Formeln, Frequenzbänder, Landeskenner und das ICAO-Alphabet."
            )
            OnboardingFeatureCard(
                icon = Icons.Default.Calculate,
                iconTint = CategoryTechnik,
                title = "Integrierter Funkrechner",
                description = "Wissenschaftlicher Taschenrechner direkt in den Fragen für dB-, Resonanz- und Wellenlängen-Berechnungen."
            )
        }
    }
}

@Composable
private fun LeitnerLevelBadge(
    level: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$level",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
