# Implementierungsplan: MVP 2 – Leitner-System & Lernfortschritt

Dieses Dokument beschreibt die Architektur, das Datenmodell, die UI-Erweiterungen und den Verifikationsplan zur Umsetzung von **MVP 2** gemäß der Spezifikation in [mvp_2_leitner_system_und_fortschritt.md](file:///f:/Android/AmateurfunkTraining/Documentation/mvp_2_leitner_system_und_fortschritt.md) und der Gesamt-Roadmap [00_implementation_roadmap.md](file:///f:/Android/AmateurfunkTraining/Documentation/00_implementation_roadmap.md).

---

## 1. Zielsetzung & Umfang (Scope)

Während in MVP 1 die Fragen und Antworten nur flüchtig im Arbeitsspeicher gehalten wurden, führt **MVP 2** die dauerhafte lokale Persistenz und die didaktische Spaced-Repetition-Kernlogik ein:
- **Lokale SQLite-Persistenz** für Lernstände (`QuestionProgress`) und Benutzer-Metadaten (`UserMeta`).
- **5-Stufen-Leitner-Algorithmus**:
  - Box 1: Täglich (Intervall: 1 Tag)
  - Box 2: Alle 3 Tage (Intervall: 3 Tage)
  - Box 3: Wöchentlich (Intervall: 7 Tage)
  - Box 4: Alle 14 Tage (Intervall: 14 Tage)
  - Box 5: Gemeistert (Prüfungsreif)
  - Richtig: Stufenweiser Aufstieg bis Box 5.
  - Falsch: Sofortiger Rückfall in Box 1 + Inkrementierung des `fehlerzaehler`.
- **Lesezeichenfunktion**: Fragen können direkt in der Übung gemerkt und separat trainiert werden.
- **Problemfragen-Training**: Fragen mit `fehlerzaehler >= 2` werden identifiziert und in einer gezielten Übungssitzung bereitgestellt.
- **Täglicher Streak-Zähler**: Berechnet auf Basis des Kalendertags (kontinuierliches Lernen belohnen).
- **Fortschrittsvisualisierung auf dem Home Screen**: Lernstandsbalken pro Fach (Neu, In Arbeit, Gemeistert) und Schnellstart-Karten.

---

## 2. Technische Spezifikation & Datenmodell

### 2.1 SQLite Datenbankschema (`Progress.sq` bzw. SQLite Schema)
```sql
CREATE TABLE IF NOT EXISTS QuestionProgress (
    frage_id TEXT NOT NULL PRIMARY KEY,
    status TEXT NOT NULL,                      -- 'NEU', 'IN_BEARBEITUNG', 'GEMEISTERT'
    fehlerzaehler INTEGER NOT NULL DEFAULT 0,
    letzte_antwort INTEGER NOT NULL DEFAULT 0,  -- Unix Timestamp in Millisekunden
    leitner_box INTEGER NOT NULL DEFAULT 1,     -- 1 bis 5
    ist_lesezeichen INTEGER NOT NULL DEFAULT 0   -- 0 = nein, 1 = ja
);

CREATE TABLE IF NOT EXISTS UserMeta (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);
```

### 2.2 Domain-Modelle (`core:model`)
```kotlin
enum class ProgressStatus {
    NEU,
    IN_BEARBEITUNG,
    GEMEISTERT
}

data class QuestionProgress(
    val questionId: String,
    val status: ProgressStatus,
    val errorCount: Int,
    val lastAnsweredTimestamp: Long,
    val leitnerBox: Int,
    val isBookmarked: Boolean
)

data class CategoryProgress(
    val category: Category,
    val totalCount: Int,
    val newCount: Int,
    val inProgressCount: Int,
    val masteredCount: Int
) {
    val masteredPercentage: Float
        get() = if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f
}
```

### 2.3 Leitner- & Streak-Logik (`core:domain` / `core:leitner`)
- **`LeitnerCalculator`**:
  - `calculateNextState(current: QuestionProgress?, isCorrect: Boolean, timestamp: Long): QuestionProgress`
  - `isDue(progress: QuestionProgress, currentTimestamp: Long): Boolean`
  - Intervalle:
    - Box 1: 1 Tag ($86.400.000\text{ ms}$)
    - Box 2: 3 Tage ($3 \times 86.400.000\text{ ms}$)
    - Box 3: 7 Tage ($7 \times 86.400.000\text{ ms}$)
    - Box 4: 14 Tage ($14 \times 86.400.000\text{ ms}$)
    - Box 5: Gemeistert
- **`StreakManager`**:
  - Verwaltet `last_practice_date` (`YYYY-MM-DD`) und `streak_count` in `UserMeta`.
  - Gleicher Tag: Streak bleibt unverändert.
  - Folgetag: `streak++`.
  - Pause $\ge 2$ Tage: `streak = 1`.

### 2.4 Repository-Schicht (`core:data`)
```kotlin
interface ProgressRepository {
    fun getProgressStream(questionId: String): Flow<QuestionProgress?>
    fun getAllProgressStream(): Flow<List<QuestionProgress>>
    suspend fun getProgressForQuestion(questionId: String): QuestionProgress?
    suspend fun recordAnswer(questionId: String, isCorrect: Boolean, timestamp: Long = System.currentTimeMillis())
    suspend fun toggleBookmark(questionId: String)
    suspend fun getProblemQuestionIds(): List<String>
    suspend fun getBookmarkedQuestionIds(): List<String>
    suspend fun getDueLeitnerQuestionIds(): List<String>
    fun getStreakStream(): Flow<Int>
}
```

---

## 3. Vorgeschlagene Änderungen

### Build-Konfiguration & Abhängigkeiten
#### [MODIFY] [gradle/libs.versions.toml](file:///f:/Android/AmateurfunkTraining/gradle/libs.versions.toml)
- Einbinden von SQLDelight bzw. AndroidX SQLite Treibern für reaktive Abfragen.

#### [MODIFY] [app/build.gradle.kts](file:///f:/Android/AmateurfunkTraining/app/build.gradle.kts)
- Konfigurieren der Bibliotheken.

---

### Kern- & Datenschicht (`core:model`, `core:database`, `core:data`)

#### [NEW] [QuestionProgress.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/core/model/QuestionProgress.kt)
- Datenklasse `QuestionProgress` und Enum `ProgressStatus`.
- Modell `CategoryProgress` für die Fortschrittsaggregation.

#### [NEW] [LeitnerCalculator.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/core/leitner/LeitnerCalculator.kt)
- Reine, zustandslose Business-Logik für Boxen-Aufstieg, Boxen-Rückfall, Fehlerzähler und Intervall-Fälligkeiten.

#### [NEW] [StreakManager.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/core/leitner/StreakManager.kt)
- Datumsberechnung für den täglichen Lernstreak.

#### [NEW] [ProgressRepository.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/core/data/ProgressRepository.kt) & [SqliteProgressRepository.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/core/data/SqliteProgressRepository.kt)
- `ProgressRepository`-Interface und Implementierung mit reaktiven Flows.
- Tabellen-Initialisierung, CRUD-Operationen, `ON CONFLICT REPLACE` und Indizes.

---

### Präsentations- und UI-Schicht (`feature:home`, `feature:practice`)

#### [NEW] [PracticeMode.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/practice/PracticeMode.kt)
- Spezifiziert den Trainingsmodus:
  - `CategoryMode(val category: Category)`
  - `DueLeitner`
  - `Bookmarks`
  - `ProblemQuestions`

#### [MODIFY] [PracticeUiState.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/practice/PracticeUiState.kt)
- Ergänzung um `practiceMode`, `isBookmarked`, `currentLeitnerBox`, `currentErrorCount`.

#### [MODIFY] [PracticeViewModel.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/practice/PracticeViewModel.kt)
- Integration von `ProgressRepository`.
- Starten nach Modus, Filtern nach Lesezeichen, Problemfragen (`fehlerzaehler >= 2`), fälligen Leitner-Fragen.
- Speichern des Antwortstatus via `recordAnswer(...)` und `toggleBookmark()`.

#### [MODIFY] [PracticeScreen.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/practice/PracticeScreen.kt)
- Lesezeichen-Icon in der TopAppBar (`content-desc="Lesezeichen setzen"` / `"Lesezeichen entfernen"`).
- Leitner-Stufen-Badge („Stufe X / 5“) und Fehler-Badge auf der Fragekarte.
- Empty-State Ansicht für leere Lesezeichen-/Problemfragen-Listen.

#### [MODIFY] [HomeViewModel.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/home/HomeViewModel.kt) & [HomeUiState.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/home/HomeUiState.kt)
- Aggregation des Lernfortschritts pro Kategorie (`CategoryProgress`).
- Bereitstellung der Schnellaktions-Zähler (Fällige, Lesezeichen, Problemfragen, Streak).

#### [MODIFY] [HomeScreen.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/feature/home/HomeScreen.kt)
- Streak-Banner mit Flammen-Icon („🔥 X Tage Lernstreak“).
- Schnellstart-Karten („Fällige Wiederholungen“, „Problemfragen“, „Lesezeichen“).
- Detaillierte Lernfortschrittsbalken pro Fach.

#### [MODIFY] [MainActivity.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/MainActivity.kt)
- Dependency-Injection für `ProgressRepository` und Navigation nach `PracticeMode`.

---

## 4. Verifikationsplan

### Automatisierte Tests

#### A. Unit-Tests (JVM)
1. **`LeitnerCalculatorTest`**: Stufenaufstieg (1->5), Rückfall auf 1 bei Fehler, Fälligkeitsberechnung.
2. **`StreakManagerTest`**: Streak-Inkrement, Halten bei Übung am selben Tag, Reset nach Pause.
3. **`ProgressRepositoryTest`**: SQLite-Operationen, Filterung nach Fehlern (`>= 2`) und Lesezeichen.
4. **`PracticeViewModelTest` & `HomeViewModelTest`**: ViewModel-Flows mit Persistenz-Schicht.
5. **Gradle Testausführung**: `.\gradlew.bat test`.

#### B. UI-Automator-Tests (On-Device Instrumented Tests)
Erweiterung von [AmateurfunkUiAutomatorTest.kt](file:///f:/Android/AmateurfunkTraining/app/src/androidTest/java/com/spacemishka/app/amateurfunktraining/AmateurfunkUiAutomatorTest.kt) um:
1. **`testBookmarkToggleAndPersistence`**: Lesezeichen in der TopAppBar setzen (`content-desc="Lesezeichen setzen"` -> `"Lesezeichen entfernen"`), zurück zum Home Screen navigieren, Kachelzähler prüfen und Lesezeichen-Modus starten.
2. **`testProblemQuestionsTraining`**: Frage mehrfach falsch beantworten, zur Home-Ansicht zurückkehren, Kachel „Problemfragen“ prüfen und starten.
3. **`testLeitnerBadgeAndStreakDisplay`**: Anzeige des Stufen-Badges auf der Fragekarte und des Streak-Banners auf dem Home Screen verifizieren.
4. **Ausführung**: `.\gradlew.bat connectedAndroidTest`.
5. **Geräte-Verbleib**: Die finale debuggte App bleibt auf dem Telefon installiert für deine eigenen Tests.
