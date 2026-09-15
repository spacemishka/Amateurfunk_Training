package com.spacemishka.app.amateurfunktraining.feature.phonetic

import androidx.lifecycle.ViewModel
import com.spacemishka.app.amateurfunktraining.core.model.PhoneticEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class QuizMode(val displayName: String) {
    LETTER_TO_WORD("Buchstabe ➔ ITU-Wort"),
    CALLSIGN_SPELLING("Rufzeichen buchstabieren")
}

data class QuizQuestion(
    val prompt: String,
    val subtitle: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class PhoneticQuizUiState(
    val mode: QuizMode = QuizMode.LETTER_TO_WORD,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isSubmitted: Boolean = false,
    val score: Int = 0,
    val isFinished: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)
}

class PhoneticQuizViewModel(
    private val random: Random = Random.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneticQuizUiState())
    val uiState: StateFlow<PhoneticQuizUiState> = _uiState.asStateFlow()

    init {
        startQuiz(QuizMode.LETTER_TO_WORD)
    }

    fun setMode(mode: QuizMode) {
        startQuiz(mode)
    }

    fun startQuiz(mode: QuizMode = _uiState.value.mode) {
        val questions = if (mode == QuizMode.LETTER_TO_WORD) {
            generateLetterQuestions(10)
        } else {
            generateCallsignQuestions(10)
        }
        _uiState.value = PhoneticQuizUiState(
            mode = mode,
            questions = questions,
            currentIndex = 0,
            selectedOptionIndex = null,
            isSubmitted = false,
            score = 0,
            isFinished = false
        )
    }

    fun selectOption(index: Int) {
        if (_uiState.value.isSubmitted || _uiState.value.isFinished) return
        _uiState.update { it.copy(selectedOptionIndex = index) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val selected = state.selectedOptionIndex ?: return
        if (state.isSubmitted || state.isFinished) return

        val current = state.currentQuestion ?: return
        val isCorrect = selected == current.correctIndex
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.update {
            it.copy(
                isSubmitted = true,
                score = newScore
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isSubmitted) return

        if (state.currentIndex + 1 < state.questions.size) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOptionIndex = null,
                    isSubmitted = false
                )
            }
        } else {
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    private fun generateLetterQuestions(count: Int): List<QuizQuestion> {
        val shuffledEntries = ITU_ENTRIES.shuffled(random).take(count)
        return shuffledEntries.map { entry ->
            val correctWord = entry.wort
            val distractors = (DISTRACTORS[entry.buchstabe] ?: emptyList()).shuffled(random).take(3)
            val allOptions = (distractors + correctWord).shuffled(random)
            val correctIdx = allOptions.indexOf(correctWord)

            QuizQuestion(
                prompt = entry.buchstabe,
                subtitle = "Wie lautet das offizielle ITU-Codewort für diesen Buchstaben?",
                options = allOptions,
                correctIndex = correctIdx,
                explanation = "Richtig: ${entry.buchstabe} ➔ ${entry.wort} (Aussprache: ${entry.aussprache})"
            )
        }
    }

    private fun generateCallsignQuestions(count: Int): List<QuizQuestion> {
        val callsigns = SAMPLE_CALLSIGNS.shuffled(random).take(count)
        return callsigns.map { call ->
            val lettersInCall = call.filter { it.isLetter() }.map { it.uppercaseChar().toString() }
            val targetLetter = lettersInCall.random(random)
            val correctEntry = ITU_ENTRIES.first { it.buchstabe == targetLetter }
            val distractors = (DISTRACTORS[targetLetter] ?: emptyList()).shuffled(random).take(3)
            val allOptions = (distractors + correctEntry.wort).shuffled(random)
            val correctIdx = allOptions.indexOf(correctEntry.wort)

            QuizQuestion(
                prompt = call,
                subtitle = "Wie wird der Buchstabe '$targetLetter' im Rufzeichen '$call' offiziell nach ITU buchstabiert?",
                options = allOptions,
                correctIndex = correctIdx,
                explanation = "Im Rufzeichen $call wird der Buchstabe '$targetLetter' als '${correctEntry.wort}' gesprochen (${correctEntry.aussprache})."
            )
        }
    }

    companion object {
        val ITU_ENTRIES = listOf(
            PhoneticEntry("A", "Alpha", "AL-FAH"),
            PhoneticEntry("B", "Bravo", "BRAH-VOH"),
            PhoneticEntry("C", "Charlie", "CHAR-LEE"),
            PhoneticEntry("D", "Delta", "DELL-TAH"),
            PhoneticEntry("E", "Echo", "ECK-OH"),
            PhoneticEntry("F", "Foxtrot", "FOKS-TROT"),
            PhoneticEntry("G", "Golf", "GOLF"),
            PhoneticEntry("H", "Hotel", "HOH-TELL"),
            PhoneticEntry("I", "India", "IN-DEE-AH"),
            PhoneticEntry("J", "Juliett", "JEW-LEE-ETT"),
            PhoneticEntry("K", "Kilo", "KEY-LOH"),
            PhoneticEntry("L", "Lima", "LEE-MAH"),
            PhoneticEntry("M", "Mike", "MIKE"),
            PhoneticEntry("N", "November", "NO-VEM-BER"),
            PhoneticEntry("O", "Oscar", "OSS-CAH"),
            PhoneticEntry("P", "Papa", "PAH-PAH"),
            PhoneticEntry("Q", "Quebec", "KEH-BECK"),
            PhoneticEntry("R", "Romeo", "ROW-ME-OH"),
            PhoneticEntry("S", "Sierra", "SEE-AIR-RAH"),
            PhoneticEntry("T", "Tango", "TANG-GO"),
            PhoneticEntry("U", "Uniform", "YOU-NEE-FORM"),
            PhoneticEntry("V", "Victor", "VIK-TAH"),
            PhoneticEntry("W", "Whiskey", "WISS-KEY"),
            PhoneticEntry("X", "X-ray", "ECKS-RAY"),
            PhoneticEntry("Y", "Yankee", "YANG-KEY"),
            PhoneticEntry("Z", "Zulu", "ZOO-LOO")
        )

        private val DISTRACTORS = mapOf(
            "A" to listOf("Anton", "America", "Apple", "Atom"),
            "B" to listOf("Berta", "Boston", "Baker", "Blue"),
            "C" to listOf("Cäsar", "Canada", "Casablanca", "City"),
            "D" to listOf("Dora", "David", "Denver", "Diamond"),
            "E" to listOf("Emil", "England", "Eagle", "Europe"),
            "F" to listOf("Friedrich", "Florida", "Fox", "Frankfurt"),
            "G" to listOf("Gustav", "Geneva", "Germany", "Gold"),
            "H" to listOf("Heinrich", "Hawaii", "Houston", "Hamburg"),
            "I" to listOf("Ida", "Italy", "Indigo", "Island"),
            "J" to listOf("Julius", "Japan", "Jupiter", "Jersey"),
            "K" to listOf("Kaufmann", "King", "Kelvin", "Kentucky"),
            "L" to listOf("Ludwig", "London", "Liberty", "Lemon"),
            "M" to listOf("Martha", "Mexico", "Madrid", "Moon"),
            "N" to listOf("Nordpol", "Norway", "Neptun", "Navy"),
            "O" to listOf("Otto", "Ontario", "Orange", "Ocean"),
            "P" to listOf("Paula", "Paris", "Pacific", "Peter"),
            "Q" to listOf("Quelle", "Queen", "Quasar", "Quick"),
            "R" to listOf("Richard", "Radio", "Roma", "Red"),
            "S" to listOf("Samuel", "Santiago", "Sun", "Silver"),
            "T" to listOf("Theodor", "Texas", "Tokyo", "Tiger"),
            "U" to listOf("Ulrich", "Union", "Uranus", "Utah"),
            "V" to listOf("Viktor", "Vienna", "Venus", "Vektor"),
            "W" to listOf("Wilhelm", "Washington", "Water", "Wave"),
            "X" to listOf("Xanthippe", "Xenon", "Xerox", "Xylophon"),
            "Y" to listOf("Ypsilon", "York", "Yellow", "Yoga"),
            "Z" to listOf("Zacharias", "Zürich", "Zero", "Zebra")
        )

        private val SAMPLE_CALLSIGNS = listOf(
            "DL1ABC", "DO2XYZ", "DA0HQ", "DK5TX", "DF3AA", "DM4KM", "DB7BB",
            "OE1AAA", "HB9XYZ", "F4ABC", "G3TXQ", "PA0RDA", "ON4UN", "EA3JE"
        )
    }
}
