# Implementierungsplan: MVP 1 – Fragenkatalog & Basistraining

## 1. Zielsetzung & Umfang (Scope)

Das Ziel von **MVP 1** ist die Etablierung des technischen Fundaments der App und die Realisierung des ersten spielbaren Lernzyklus (**Core Learning Loop**). Nach Abschluss von MVP 1 kann der Nutzer den BNetzA-Fragenkatalog der Klasse E nach Kategorien (Technik, Betrieb, Vorschriften) filtern, Fragen durchgehen, eine der vier Antworten wählen, erhält unmittelbares visuelles Feedback und sieht die fachliche Erklärung zur richtigen Lösung.

### Erfüllte Anforderungen aus der SRS:
- **F1.1**: Einbindung des BNetzA-Fragenkatalogs (3. Auflage, März 2024) für Klasse E als strukturierte JSON-Ressource.
- **F1.2 (Teil)**: Einfacher Übungsmodus (Richtig beantwortet = weiter, Falsch = verbleibt zur Wiederholung).
- **F1.3**: Filterung nach Kategorien (*Technik*, *Betrieb*, *Vorschriften*) und Themenbereichen.
- **F1.4**: Anzeige verständlicher Erklärungen zur korrekten Antwort nach Auswahl.
- **Nicht-funktionale Vorgaben**: Offline-Fähigkeit, Reaktionszeit < 100 ms, Material 3 Design System.

---

## 2. User Stories & Akzeptanzkriterien

### US1.1: Fragenkatalog durchsuchen und starten
> **Als** Lernender für die Amateurfunkprüfung Klasse E  
> **möchte ich** eine Prüfungskategorie (Technik, Betrieb, Vorschriften) auswählen und sofort mit dem Üben beginnen,  
> **damit ich** mich gezielt auf ein bestimmtes Prüfungsfach vorbereiten kann.

- **Szenario**: Starten einer Übungssitzung
  - **Gegeben sei**: Die App befindet sich auf dem Startbildschirm.
  - **Wenn**: Der Nutzer auf die Kachel „Technik (Klasse E)“ tippt,
  - **Dann**: Wird eine Übungssitzung geladen, die nur Fragen mit Kategorie `TECHNIK` enthält.
  - **Und**: Die erste Frage wird mit Fragestellung und 4 Antwortoptionen angezeigt.

### US1.2: Antwortauswahl und direktes Feedback
> **Als** Lernender  
> **möchte ich** eine Antwort anklicken und sofort sehen, ob sie richtig oder falsch war,  
> **damit ich** einen unmittelbaren Lerneffekt erziele.

- **Szenario**: Richtige Antwort ausgewählt
  - **Gegeben sei**: Frage „TE-101“ mit 4 Antwortmöglichkeiten wird angezeigt.
  - **Wenn**: Der Nutzer auf die korrekte Option tippt,
  - **Dann**: Wird die ausgewählte Option grün hervorgehoben (Erfolgsfarbe).
  - **Und**: Die Erklärung wird eingeblendet.
  - **Und**: Ein Button „Nächste Frage“ wird aktiv.

- **Szenario**: Falsche Antwort ausgewählt
  - **Gegeben sei**: Frage „TE-101“ wird angezeigt.
  - **Wenn**: Der Nutzer auf eine inkorrekte Option tippt,
  - **Dann**: Wird die gewählte Option rot hervorgehoben (Fehlerfarbe).
  - **Und**: Die tatsächlich korrekte Option wird dezent grün markiert.
  - **Und**: Die Erklärung zur Lösung wird eingeblendet.

---

## 3. Technische Architektur & Datenmodell

### 3.1 Paketstruktur (`commonMain` / `app/src/main/java`)
```text
com.spacemishka.app.amateurfunktraining/
├── core/
│   ├── model/
│   │   ├── Category.kt               // ENUM: TECHNIK, BETRIEB, VORSCHRIFTEN
│   │   ├── Question.kt               // Domain Model
│   │   └── QuestionDto.kt            // Serialisierungs-DTO für JSON-Import
│   ├── data/
│   │   ├── QuestionRepository.kt     // Interface
│   │   └── AssetQuestionRepository.kt// Lädt questions_klasse_e.json via Assets / Resources
│   └── util/
│       └── DispatcherProvider.kt     // Coroutine Dispatcher Wrapper
├── feature/
│   ├── home/
│   │   ├── HomeScreen.kt             // Kategoriewahl & Übersicht
│   │   └── HomeViewModel.kt
│   └── practice/
│       ├── PracticeScreen.kt         // Fragenkarte, Antworten, Feedback
│       ├── PracticeViewModel.kt
│       ├── PracticeUiState.kt
│       └── components/
│           ├── QuestionCard.kt
│           ├── AnswerOptionItem.kt
│           └── ExplanationBox.kt
└── ui/
    └── theme/                        // Material 3 Color Schemes & Typography
```

### 3.2 Datenmodelle (Kotlin)
```kotlin
@Serializable
data class QuestionDto(
    val id: String,
    val kategorie: String,
    val topic_id: String,
    val frage_text: String,
    val antworten: List<String>,
    val richtige_antwort: Int,
    val erklaerung: String,
    val bild_svg: String? = null
)

enum class Category(val displayName: String, val identifier: String) {
    TECHNIK("Technik", "Technik"),
    BETRIEB("Betriebliche Kenntnisse", "Betrieb"),
    VORSCHRIFTEN("Vorschriften", "Vorschriften"),
    ALL("Alle Fächer", "Alle")
}

data class Question(
    val id: String,
    val category: Category,
    val topicId: String,
    val text: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val imageAssetPath: String?
)
```

### 3.3 State Management (`PracticeUiState`)
```kotlin
data class PracticeUiState(
    val isLoading: Boolean = true,
    val currentQuestionIndex: Int = 0,
    val totalQuestionsCount: Int = 0,
    val currentQuestion: Question? = null,
    val selectedAnswerIndex: Int? = null,
    val isAnswerConfirmed: Boolean = false,
    val isCorrect: Boolean? = null,
    val errorMessage: String? = null
)
```

---

## 4. Detaillierte Arbeitspakete (Work Packages)

### WP 1.1: Projekt-Setup & JSON-Katalog-Integration
- **Tasks**:
  1. Hinzufügen von `kotlinx.serialization` (JSON) in `gradle/libs.versions.toml` und `app/build.gradle.kts`.
  2. Anlegen von `app/src/main/assets/questions_klasse_e.json` mit vollständigem oder validem Baseline-Katalog (Technik: Fragenreihe TD/TE/TF, Betrieb: BD/BE, Vorschriften: VD/VE).
  3. Erstellung des Repositories `AssetQuestionRepository.kt` mit asynchroner Einleselogik über Kotlin Coroutines (`Dispatchers.IO`).
  4. Unit-Test für den Parser: Validierung von Frage-IDs, Indexgrenzen der Antworten (stets genau 4 Antworten, `richtige_antwort` im Bereich 0..3).

### WP 1.2: Domain- und ViewModel-Schicht
- **Tasks**:
  1. Implementierung von `QuestionRepository` mit Filter-Methoden: `getQuestionsByCategory(Category)`, `getAllQuestions()`, `getQuestionCount()`.
  2. Implementierung von `PracticeViewModel`:
     - Initialisierung mit gefilterter Fragenliste (mit Mischeffekt / Shuffle-Option).
     - Methode `selectAnswer(index: Int)`: Erfassung der Nutzerwahl, Auswertung von `correctAnswerIndex`.
     - Methode `nextQuestion()`: Index-Weiterschaltung, Zurücksetzen des Selektionsstatus.
     - Fehlerbehandlung bei leeren Datenmengen.

### WP 1.3: UI-Implementierung (Compose Material 3)
- **Tasks**:
  1. **Home Screen**:
     - Moderne Kacheln für die drei Prüfungsfächer mit Icon, Fragenanzahl und Farbakzenten.
     - Schnellauswahl „Alle Fächer üben“.
  2. **Practice Screen**:
     - TopAppBar mit Fortschrittsanzeige (`Frage X von Y`) und Abbruch-/Zurück-Navigation.
     - `QuestionCard`: Übersichtlicher Textbereich mit Unterstützung für formatierten Text.
     - `AnswerOptionItem`: Interaktive Antwort-Buttons mit 4 Statuszuständen:
       - Normal / Unberührt
       - Ausgewählt (vor Bestätigung)
       - Richtig (Grüner Akzent, Checkmark-Icon)
       - Falsch (Roter Akzent, X-Icon)
     - `ExplanationBox`: Animiertes Einblenden der Begründung nach Beantwortung.
     - Fester BottomBar-Aktionsbutton „Weiter“.

---

## 5. UI/UX Spezifikation

```
+--------------------------------------------------------+
|  <- Zurück          Frage 14 von 240          (Technik)|
|  [==========================>                          ]
+--------------------------------------------------------+
|                                                        |
|  [ Frage TE-104 ]                                      |
|  Wie verhält sich die Wellenlänge einer elektro-       |
|  magnetischen Welle zur Frequenz?                      |
|                                                        |
|  (A) Die Wellenlänge ist proportional zur Frequenz.    |
|                                                        |
|  (B) Die Wellenlänge ist umgekehrt proportional       |
|      zur Frequenz.  [ V  Richtig ]                     |
|                                                        |
|  (C) Die Wellenlänge bleibt konstant.                 |
|                                                        |
|  (D) Es besteht kein mathematischer Zusammenhang.     |
|                                                        |
+--------------------------------------------------------+
|  [i] Erklärung:                                        |
|  Gemäß c = lambda * f sinkt die Wellenlänge lambda,    |
|  wenn die Frequenz f steigt (umgekehrt proportional).  |
+--------------------------------------------------------+
|  [                     NÄCHSTE FRAGE                  ]|
+--------------------------------------------------------+
```

---

## 6. Test- und Verifikationsplan

### 6.1 Automatisierte Unit-Tests
| Testklasse | Testziel |
|---|---|
| `QuestionJsonParsingTest` | Stellt sicher, dass das JSON ohne Exceptions eingelesen wird, keine IDs doppelt vorkommen und jede Frage exakt 4 Antworten besitzt. |
| `QuestionRepositoryTest` | Prüft, dass die Filter nach `Category.TECHNIK`, `BETRIEB` und `VORSCHRIFTEN` die exakten Teilmengen liefern. |
| `PracticeViewModelTest` | Prüft State-Übergänge: Klick auf richtige/falsche Antwort führt zu `isCorrect == true/false`, `nextQuestion()` erhöht den Zähler. |

### 6.2 Manueller Akzeptanztest
1. App auf Emulator / Physischem Gerät starten.
2. Kategorie „Technik“ wählen: Erste Frage prüfen.
3. Falsche Antwort anklicken: Visuelles Feedback muss sofort (< 100 ms) auf Rot wechseln und die richtige Antwort in Grün anzeigen.
4. Button „Weiter“ drücken: Nächste Frage muss ohne Flackern erscheinen.
5. Drehung des Bildschirms (Konfigurationsänderung): State (ausgewählte Frage und Antwortstatus) bleibt erhalten.

---

## 7. Definition of Done (DoD)

- [x] Alle Fragen der Klasse E liegen als fehlerfreies JSON vor und werden performant geladen.
- [x] Übungsmodus für alle drei Fächer funktioniert ohne Abstürze.
- [x] Sofortiges Feedback und Erklärungen sind verständlich dargestellt.
- [x] Unit-Tests für Parser, Repository und ViewModel sind grün (Coverage > 80 % auf Core Logic).
- [x] Kein UI-Jank, flüssige 60 fps beim Swipen/Navigieren.

