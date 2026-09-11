# Implementierungsplan: MVP 1 – Fragenkatalog & Basistraining

Dieses Dokument beschreibt die konkreten technischen Schritte zur Realisierung von **MVP 1** gemäß [mvp_1_fragenkatalog_und_basistraining.md](file:///f:/Android/AmateurfunkTraining/Documentation/mvp_1_fragenkatalog_und_basistraining.md) und [srs_morseapp.md](file:///f:/Android/AmateurfunkTraining/Documentation/srs_morseapp.md).

---

## 1. Übersicht & Zielsetzung

Mit MVP 1 wird der spielbare Kernzyklus (**Core Learning Loop**) umgesetzt:
- Einbindung eines strukturierten BNetzA-Fragenkatalogs für Klasse E als lokales JSON-Asset.
- Domain-Modellierung (`Question`, `Category`, `QuestionDto`).
- Asynchrones Laden und Filtern von Fragen nach Prüfungsfächern (*Technik*, *Betrieb*, *Vorschriften*).
- Interaktiver Übungsmodus mit 4 Antwortoptionen, sofortigem visuellen Feedback (Erfolgs-/Fehlerfarbe) und didaktischer Erklärung.
- Robuste Unit- und Repositorystruktur mit Unit-Tests.

---

## 2. Vorgeschlagene Änderungen

### Build-Konfiguration & Abhängigkeiten
#### [MODIFY] [libs.versions.toml](file:///f:/Android/AmateurfunkTraining/gradle/libs.versions.toml)
- Hinzufügen des Kotlin-Serialization-Plugins: `org.jetbrains.kotlin.plugin.serialization` (`kotlin = "2.2.10"`).
- Hinzufügen der Bibliothek `kotlinx-serialization-json` (z. B. `1.8.0`).
- Hinzufügen von `androidx-lifecycle-viewmodel-compose` (`2.11.0`).
- Hinzufügen von `kotlinx-coroutines-test` (`1.10.1` oder passend) für asynchrone ViewModel-Unit-Tests.

#### [MODIFY] [app/build.gradle.kts](file:///f:/Android/AmateurfunkTraining/app/build.gradle.kts)
- Aktivieren des Plugins `kotlinx-serialization`.
- Einbinden der neuen Dependencies.

---

### Daten- und Ressourcen-Schicht (`core:data` / `core:model`)
#### [NEW] `app/src/main/assets/questions_klasse_e.json`
- Enthält strukturierte Prüfungsfragen der Klasse E gemäß SRS-Abschnitt 6.1:
  - Felder: `id`, `kategorie`, `topic_id`, `frage_text`, `antworten` (4 Optionen), `richtige_antwort` (0-3), `erklaerung`, `bild_svg` (optional).
  - Repräsentativer Katalog für alle drei Fächer (*Technik*, *Betrieb*, *Vorschriften*).

#### [NEW] `core/model/Category.kt`
- Enum `Category` (`TECHNIK`, `BETRIEB`, `VORSCHRIFTEN`, `ALL`) mit deutschen Anzeigenamen und Beschreibungen.

#### [NEW] `core/model/QuestionDto.kt` & `core/model/Question.kt`
- `@Serializable` DTO für JSON-Parsing und sauberes Domain Model.

#### [NEW] `core/data/QuestionRepository.kt` & `AssetQuestionRepository.kt`
- Interface und Implementierung zum asynchronen Einlesen des Katalogs via `Context.assets` mit Coroutine-Dispatching (`Dispatchers.IO`).
- Filter-Methoden: `getQuestionsByCategory(Category)`, `getAllQuestions()`, `getQuestionCount()`.

---

### Präsentations- und UI-Schicht (`feature:home`, `feature:practice`)
#### [NEW] `feature/home/HomeScreen.kt` & `HomeViewModel.kt`
- Modernes Material 3 Dashboard:
  - Header mit App-Titel und Untertitel („Amateurfunk Klasse E Prüfungsvorbereitung“).
  - Übersichtskarten für die Fächer:
    - ⚡ **Technik**
    - 📻 **Betriebliche Kenntnisse**
    - 📜 **Vorschriften & Gesetze**
    - 🎯 **Alle Fächer gemischt**
  - Anzeige der Fragenanzahl pro Fach und Start-Button.

#### [NEW] `feature/practice/PracticeViewModel.kt` & `PracticeUiState.kt`
- Reaktives State Management für die Fragesitzung:
  - `loadQuestions(category: Category)`
  - `selectAnswer(index: Int)`
  - `nextQuestion()`
  - Tracking von Richtig/Falsch-Antworten in der aktuellen Sitzung.

#### [NEW] `feature/practice/PracticeScreen.kt` und Komponenten
- `QuestionCard.kt`: Darstellung der Frage und optionaler Tags/IDs.
- `AnswerOptionItem.kt`: 4 interaktive Antwortkarten mit weichen Übergängen und Material 3 Zustandsfarben:
  - Neutral / Unberührt
  - Richtig: Grüner Akzent, Check-Symbol
  - Falsch: Roter Akzent, X-Symbol
- `ExplanationBox.kt`: Animiertes Ausklappen der Erklärung nach Beantwortung.
- BottomBar mit Fortschritt (`Frage X von Y`) und Button „Weiter“.

#### [MODIFY] [MainActivity.kt](file:///f:/Android/AmateurfunkTraining/app/src/main/java/com/spacemishka/app/amateurfunktraining/MainActivity.kt)
- Verbinden von `HomeScreen` und `PracticeScreen` mit leichtgewichtiger Screen-Navigation (Home <-> Practice).

---

### Tests
#### [NEW] `app/src/test/java/com/spacemishka/app/amateurfunktraining/QuestionJsonParsingTest.kt`
- Validiert das JSON-Schema, Prüfungs-IDs, Antwortenzahl (stets genau 4) und Gültigkeit der Lösungsindizes (0..3).

#### [NEW] `app/src/test/java/com/spacemishka/app/amateurfunktraining/PracticeViewModelTest.kt`
- Testet den Stateflow: Antwortauswahl, Richtig/Falsch-Bewertung, Erklärungssichtbarkeit und Index-Weiterschaltung.

---

## 3. Verifikationsplan

### Automatisierte Tests
- Ausführen aller Unit-Tests via Gradle:
  ```powershell
  .\gradlew.bat test
  ```
- Sicherstellen, dass `QuestionJsonParsingTest` und `PracticeViewModelTest` fehlerfrei durchlaufen.

### Manuelle Verifikation
- Zusammenbau und Start des Debug-Builds:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- Verifikation des visuellen Feedbacks:
  1. Start mit Fach „Technik“.
  2. Klick auf eine Antwort -> Sofortiges Umschalten der Farben und Erscheinen der Erklärung.
  3. Klick auf „Nächste Frage“ -> Frage wechselt, Antworten werden zurückgesetzt.
  4. Zurück zum Hauptmenü und Test mit Fach „Vorschriften“.
