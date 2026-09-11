package com.spacemishka.app.amateurfunktraining

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacemishka.app.amateurfunktraining.core.data.AssetQuestionRepository
import com.spacemishka.app.amateurfunktraining.core.data.ProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.SqliteProgressRepository
import com.spacemishka.app.amateurfunktraining.feature.home.HomeScreen
import com.spacemishka.app.amateurfunktraining.feature.home.HomeViewModel
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeMode
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeScreen
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeViewModel
import com.spacemishka.app.amateurfunktraining.ui.theme.AmateurfunkTrainingTheme

sealed interface Screen {
    data object Home : Screen
    data class Practice(val mode: PracticeMode) : Screen
}

class MainActivity : ComponentActivity() {

    private val questionRepository by lazy {
        AssetQuestionRepository(applicationContext)
    }

    private val progressRepository by lazy {
        SqliteProgressRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AmateurfunkTrainingTheme {
                AmateurfunkApp(
                    questionRepository = questionRepository,
                    progressRepository = progressRepository
                )
            }
        }
    }
}

@Composable
fun AmateurfunkApp(
    questionRepository: AssetQuestionRepository,
    progressRepository: ProgressRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val homeViewModel = viewModel {
        HomeViewModel(
            questionRepository = questionRepository,
            progressRepository = progressRepository
        )
    }

    val practiceViewModel = viewModel {
        PracticeViewModel(
            questionRepository = questionRepository,
            progressRepository = progressRepository
        )
    }

    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                viewModel = homeViewModel,
                onSelectPracticeMode = { mode ->
                    practiceViewModel.startPractice(mode)
                    currentScreen = Screen.Practice(mode)
                }
            )
        }

        is Screen.Practice -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            PracticeScreen(
                viewModel = practiceViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }
    }
}