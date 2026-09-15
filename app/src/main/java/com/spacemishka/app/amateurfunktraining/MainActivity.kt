package com.spacemishka.app.amateurfunktraining

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacemishka.app.amateurfunktraining.core.backup.BackupManager
import com.spacemishka.app.amateurfunktraining.core.data.AssetQuestionRepository
import com.spacemishka.app.amateurfunktraining.core.data.AssetReferenceRepository
import com.spacemishka.app.amateurfunktraining.core.data.AssetTopicRepository
import com.spacemishka.app.amateurfunktraining.core.data.ExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.ProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.ReferenceRepository
import com.spacemishka.app.amateurfunktraining.core.data.SettingsRepository
import com.spacemishka.app.amateurfunktraining.core.data.SharedPrefSettingsRepository
import com.spacemishka.app.amateurfunktraining.core.data.SqliteExamRepository
import com.spacemishka.app.amateurfunktraining.core.data.SqliteProgressRepository
import com.spacemishka.app.amateurfunktraining.core.data.TopicRepository
import com.spacemishka.app.amateurfunktraining.core.model.ExamResult
import com.spacemishka.app.amateurfunktraining.feature.calculator.CalculatorScreen
import com.spacemishka.app.amateurfunktraining.feature.calculator.CalculatorViewModel
import com.spacemishka.app.amateurfunktraining.feature.exam.ExamResultScreen
import com.spacemishka.app.amateurfunktraining.feature.exam.ExamScreen
import com.spacemishka.app.amateurfunktraining.feature.exam.ExamViewModel
import com.spacemishka.app.amateurfunktraining.feature.home.HomeScreen
import com.spacemishka.app.amateurfunktraining.feature.home.HomeViewModel
import com.spacemishka.app.amateurfunktraining.feature.phonetic.PhoneticQuizScreen
import com.spacemishka.app.amateurfunktraining.feature.phonetic.PhoneticQuizViewModel
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeMode
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeScreen
import com.spacemishka.app.amateurfunktraining.feature.practice.PracticeViewModel
import com.spacemishka.app.amateurfunktraining.feature.reference.ReferenceHubScreen
import com.spacemishka.app.amateurfunktraining.feature.reference.ReferenceHubViewModel
import com.spacemishka.app.amateurfunktraining.feature.reference.ReferenceTab
import com.spacemishka.app.amateurfunktraining.feature.settings.SettingsScreen
import com.spacemishka.app.amateurfunktraining.feature.settings.SettingsViewModel
import com.spacemishka.app.amateurfunktraining.feature.topic.TopicDetailScreen
import com.spacemishka.app.amateurfunktraining.feature.topic.TopicLexiconScreen
import com.spacemishka.app.amateurfunktraining.feature.topic.TopicLexiconViewModel
import com.spacemishka.app.amateurfunktraining.ui.theme.AmateurfunkTrainingTheme

sealed interface Screen {
    data object Home : Screen
    data class Practice(val mode: PracticeMode) : Screen
    data object Exam : Screen
    data class Result(val examResult: ExamResult) : Screen
    data object TopicLexicon : Screen
    data class TopicDetail(val topicId: String) : Screen
    data class ReferenceHub(val initialTab: ReferenceTab = ReferenceTab.FORMULAS) : Screen
    data object PhoneticQuiz : Screen
    data object Calculator : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    private val questionRepository by lazy {
        AssetQuestionRepository(applicationContext)
    }

    private val progressRepository by lazy {
        SqliteProgressRepository(applicationContext)
    }

    private val examRepository by lazy {
        SqliteExamRepository(applicationContext)
    }

    private val topicRepository by lazy {
        AssetTopicRepository(applicationContext)
    }

    private val referenceRepository by lazy {
        AssetReferenceRepository(applicationContext)
    }

    private val settingsRepository by lazy {
        SharedPrefSettingsRepository(applicationContext)
    }

    private val backupManager by lazy {
        BackupManager(
            progressRepository = progressRepository,
            examRepository = examRepository,
            topicRepository = topicRepository,
            settingsRepository = settingsRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState()

            AmateurfunkTrainingTheme(
                themeMode = settings.themeMode,
                fontScale = settings.fontScale.scale
            ) {
                AmateurfunkApp(
                    questionRepository = questionRepository,
                    progressRepository = progressRepository,
                    examRepository = examRepository,
                    topicRepository = topicRepository,
                    referenceRepository = referenceRepository,
                    settingsRepository = settingsRepository,
                    backupManager = backupManager
                )
            }
        }
    }
}

@Composable
fun AmateurfunkApp(
    questionRepository: AssetQuestionRepository,
    progressRepository: ProgressRepository,
    examRepository: ExamRepository,
    topicRepository: TopicRepository,
    referenceRepository: ReferenceRepository,
    settingsRepository: SettingsRepository,
    backupManager: BackupManager
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val homeViewModel = viewModel {
        HomeViewModel(
            questionRepository = questionRepository,
            progressRepository = progressRepository,
            examRepository = examRepository
        )
    }

    val practiceViewModel = viewModel {
        PracticeViewModel(
            questionRepository = questionRepository,
            progressRepository = progressRepository,
            topicRepository = topicRepository
        )
    }

    val examViewModel = viewModel {
        ExamViewModel(
            questionRepository = questionRepository,
            examRepository = examRepository
        )
    }

    val topicLexiconViewModel = viewModel {
        TopicLexiconViewModel(
            topicRepository = topicRepository,
            questionRepository = questionRepository
        )
    }

    val referenceHubViewModel = viewModel {
        ReferenceHubViewModel(
            referenceRepository = referenceRepository
        )
    }

    val phoneticQuizViewModel = viewModel {
        PhoneticQuizViewModel()
    }

    val calculatorViewModel = viewModel {
        CalculatorViewModel()
    }

    val settingsViewModel = viewModel {
        SettingsViewModel(
            settingsRepository = settingsRepository,
            backupManager = backupManager
        )
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                viewModel = homeViewModel,
                onSelectPracticeMode = { mode ->
                    practiceViewModel.startPractice(mode)
                    currentScreen = Screen.Practice(mode)
                },
                onStartExam = {
                    examViewModel.startExam()
                    currentScreen = Screen.Exam
                },
                onOpenLexicon = {
                    topicLexiconViewModel.loadData()
                    currentScreen = Screen.TopicLexicon
                },
                onOpenReferenceHub = {
                    referenceHubViewModel.setTab(ReferenceTab.FORMULAS)
                    currentScreen = Screen.ReferenceHub(ReferenceTab.FORMULAS)
                },
                onOpenPhoneticQuiz = {
                    phoneticQuizViewModel.startQuiz()
                    currentScreen = Screen.PhoneticQuiz
                },
                onOpenCalculator = {
                    currentScreen = Screen.Calculator
                },
                onOpenSettings = {
                    currentScreen = Screen.Settings
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

        is Screen.Exam -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            ExamScreen(
                viewModel = examViewModel,
                onExamFinished = { result ->
                    currentScreen = Screen.Result(result)
                },
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }

        is Screen.Result -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            ExamResultScreen(
                result = screen.examResult,
                onStartFehlertraining = { mistakeIds ->
                    val mode = PracticeMode.ExamMistakes(mistakeIds)
                    practiceViewModel.startPractice(mode)
                    currentScreen = Screen.Practice(mode)
                },
                onNavigateToHome = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }

        is Screen.TopicLexicon -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            TopicLexiconScreen(
                viewModel = topicLexiconViewModel,
                onSelectTopic = { topicId ->
                    currentScreen = Screen.TopicDetail(topicId)
                },
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }

        is Screen.TopicDetail -> {
            BackHandler {
                currentScreen = Screen.TopicLexicon
                topicLexiconViewModel.loadData()
            }

            TopicDetailScreen(
                topicId = screen.topicId,
                topicRepository = topicRepository,
                questionRepository = questionRepository,
                onStartPractice = { topicId, topicTitle ->
                    val mode = PracticeMode.TopicQuestions(topicId, topicTitle)
                    practiceViewModel.startPractice(mode)
                    currentScreen = Screen.Practice(mode)
                },
                onSelectTopic = { nextTopicId ->
                    currentScreen = Screen.TopicDetail(nextTopicId)
                },
                onNavigateBack = {
                    currentScreen = Screen.TopicLexicon
                    topicLexiconViewModel.loadData()
                }
            )
        }

        is Screen.ReferenceHub -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            ReferenceHubScreen(
                viewModel = referenceHubViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                },
                onOpenCalculator = {
                    currentScreen = Screen.Calculator
                },
                onOpenPhoneticQuiz = {
                    phoneticQuizViewModel.startQuiz()
                    currentScreen = Screen.PhoneticQuiz
                }
            )
        }

        is Screen.PhoneticQuiz -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            PhoneticQuizScreen(
                viewModel = phoneticQuizViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }

        is Screen.Calculator -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            CalculatorScreen(
                viewModel = calculatorViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }

        is Screen.Settings -> {
            BackHandler {
                currentScreen = Screen.Home
                homeViewModel.loadData()
            }

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    currentScreen = Screen.Home
                    homeViewModel.loadData()
                }
            )
        }
    }
}