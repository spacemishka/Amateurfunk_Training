# Testplan & Automatisierungsstrategie

## Amateurfunk Training (Klasse E)

| Metadaten | Wert |
|---|---|
| **Dokumentversion** | 1.0 |
| **Datum** | 11. September 2026 |
| **Status** | Aktiv / Freigegeben |
| **Teststufen** | Unit-Tests (JVM), UI-Automator-Tests (On-Device Instrumented Tests) |

---

## 1. Übersicht & Testpyramide

Die Testarchitektur folgt dem Prinzip der agilen Testpyramide:
1. **Unit-Tests (Schnell, autonom auf der JVM)**:
   - Prüfen Datenintegrität des Fragenkatalogs (JSON-Validierung, 4 Optionen, Indexgrenzen).
   - Prüfen Domain-Modelle, Enums und Filter-Algorithmen.
   - Prüfen State-Transitions in ViewModels (`PracticeViewModel`, `HomeViewModel`) via Coroutines Test (`StandardTestDispatcher`, `runTest`).
2. **UI-Automator-Tests (End-to-End auf physischem Gerät / Emulator)**:
   - Nutzen das moderne AndroidX `UiAutomator`-Framework (`UiDevice`, `By`, `Until`).
   - Simulieren echte Benutzerinteraktionen (Taps, State-Transitions, Navigations-Events).
   - Verifizieren die visuelle und semantische Barrierefreiheit (`content-desc="Richtig"`, `content-desc="Falsch"`).

```
          / \
         /   \     UI-Automator Tests (On-Device E2E)
        / E2E \    - AmateurfunkUiAutomatorTest
       /-------\
      /         \  Unit- & State-Tests (JVM)
     /   Unit    \ - QuestionJsonParsingTest, PracticeViewModelTest,
    /    Tests    \- AssetQuestionRepositoryTest, CategoryTest, HomeViewModelTest
   /---------------\
```

---

## 2. Testfall-Matrix

| Testklasse | Typ | Ziel & Prüfgegenstand | Ausführungsdauer |
|---|---|---|---|
| `QuestionJsonParsingTest` | Unit (JVM) | Vollständigkeit & Schema-Validität von `questions_klasse_e.json` (keine doppelten IDs, 4 Antworten, Index 0..3, alle 3 Fächer abgedeckt). | < 500 ms |
| `CategoryTest` | Unit (JVM) | Robustes Parsing von Fachgebiets-Keys (`Technik`, `Betrieb`, `Vorschriften`, `Alle`) inkl. Fallbacks. | < 100 ms |
| `AssetQuestionRepositoryTest` | Unit (JVM) | Asynchrones Einlesen, Caching, Filterung nach Kategorien und ID-Lookup ohne Android-Context-Abhängigkeit. | < 300 ms |
| `HomeViewModelTest` | Unit (JVM) | Korrekte Aggregation und Bereitstellung der Fragenzähler im `HomeUiState`. | < 300 ms |
| `PracticeViewModelTest` | Unit (JVM) | Vollständiger Trainingsablauf: Fragenladen, Richtig-/Falschantwort-Erfassung, Index-Weiterschaltung, Fehlertraining-Pool. | < 400 ms |
| `AmateurfunkUiAutomatorTest` | UI-Automator (Device) | End-to-End-Benutzerreise auf dem Android-Gerät: App-Start, Fachauswahl, Fragenansicht, Sofort-Feedback, Begründung und Zurück-Navigation. | ca. 10–15 s |

---

## 3. Befehle zur separaten automatisierten Ausführung

### A. Ausschließlich Unit-Tests ausführen (ohne Gerät / CI-tauglich)
```powershell
# Alle Unit-Tests auf der JVM ausführen:
.\gradlew.bat test

# Alternativ nur eine spezifische Unit-Testklasse ausführen:
.\gradlew.bat test --tests "com.spacemishka.app.amateurfunktraining.PracticeViewModelTest"
.\gradlew.bat test --tests "com.spacemishka.app.amateurfunktraining.QuestionJsonParsingTest"
```
*HTML-Testbericht*: `app/build/reports/tests/testDebugUnitTest/index.html`

---

### B. Ausschließlich UI-Automator-Tests ausführen (auf angeschlossenem Gerät / Emulator)
```powershell
# Alle instrumentierten UI-Automator-Tests auf dem verbundenen Gerät ausführen:
.\gradlew.bat connectedAndroidTest

# Alternativ nur die UI-Automator-Testklasse ausführen:
.\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.spacemishka.app.amateurfunktraining.AmateurfunkUiAutomatorTest
```
*HTML-Testbericht*: `app/build/reports/androidTests/connected/index.html`

---

### C. Direkt via ADB Instrument (ohne Gradle-Rebuild)
Wenn die Test-APK bereits gebaut ist:
```powershell
# 1. Test-APK und App-APK assembliert bereitstellen
.\gradlew.bat assembleDebug assembleDebugAndroidTest

# 2. Direkt über den Android Test Runner auf dem Gerät starten
adb shell am instrument -w -r -e class com.spacemishka.app.amateurfunktraining.AmateurfunkUiAutomatorTest com.spacemishka.app.amateurfunktraining.test/androidx.test.runner.AndroidJUnitRunner
```

---

## 4. Bereitgestellte Automatisierungs-Skripte

Zur bequemen Ausführung existieren folgende fertige Skripte im Projekt:
- [run_unit_tests.bat](file:///f:/Android/AmateurfunkTraining/scripts/run_unit_tests.bat): Führt alle JVM-Unit-Tests aus und zeigt eine Zusammenfassung.
- [run_ui_automator_tests.bat](file:///f:/Android/AmateurfunkTraining/scripts/run_ui_automator_tests.bat): Prüft adb-Verbindung und startet die UI-Automator-Tests auf dem Zielgerät.
