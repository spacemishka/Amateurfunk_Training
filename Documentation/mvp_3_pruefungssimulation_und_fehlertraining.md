# Implementierungsplan: MVP 3 – Prüfungssimulation & Fehlertraining

## 1. Zielsetzung & Umfang (Scope)

Das Ziel von **MVP 3** ist die Bereitstellung einer vollständigen, realistischen **Prüfungssimulation für die Amateurfunkprüfung Klasse E** gemäß den offiziellen Vorgaben der Bundesnetzagentur (BNetzA). Nach Abschluss von MVP 3 können Lernende Probe-Prüfungen mit Zeitbegrenzung ablegen, erhalten eine detaillierte Auswertung pro Fachgebiet mit der offiziellen 75 %-Bestehenshürde, sehen ihren Prüfungsbereitschafts-Score und können alle in der Prüfung falsch beantworteten Fragen mit einem Klick in ein gezieltes Fehlertraining überführen.

### Erfüllte Anforderungen aus der SRS:
- **F2.1**: Realistische Prüfungsbedingungen:
  - Strukturierung in die drei Prüfungsteile: **Technik**, **Betrieb**, **Vorschriften**.
  - Offizielle Bestehensgrenze: Mindestens **75 %** der Punkte in jedem einzelnen Prüfungsteil erforderlich.
  - Einstellbarer Prüfungs-Countdown-Timer.
- **F2.2**: Sofortige Auswertung nach Abgabe (Punktestand pro Teilgebiet, Gesamtstatus: *Bestanden* / *Nicht bestanden*).
- **F2.3**: Gezieltes Fehlertraining direkt aus den falsch beantworteten Prüfungsfragen.
- **F2.4**: Prüfungsbereitschafts-Indikator (Readiness Score) basierend auf dem Verlauf der letzten Prüfungssimulationen.

---

## 2. Prüfungsordnung BNetzA Klasse E (Didaktische Spezifikation)

| Prüfungsteil | Fragenanzahl | Benötigt zum Bestehen (75 %) | Offizielles Zeitlimit |
|---|---|---|---|
| **Teil 1: Technik (Klasse E)** | 34 Fragen | Mindestens 26 richtig | 60 Minuten |
| **Teil 2: Betriebliche Kenntnisse** | 25 Fragen | Mindestens 19 richtig | 45 Minuten |
| **Teil 3: Vorschriften** | 25 Fragen | Mindestens 19 richtig | 45 Minuten |
| **Gesamt** | **84 Fragen** | In **allen 3 Teilen >= 75 %** | **150 Minuten** (konfigurierbar) |

> [!IMPORTANT]
> Eine Prüfung gilt nach den Regeln der BNetzA nur dann als **bestanden**, wenn in **jedem einzelnen der drei Teilgebiete** die 75 %-Marke erreicht wurde. Scheitert der Prüfling in auch nur einem Teilgebiet, gilt die Prüfung als nicht bestanden.

---

## 3. Technische Architektur & Datenmodell

### 3.1 Entitäten & DTOs
```kotlin
enum class ExamStatus {
    PASSED,
    FAILED
}

data class ExamPartConfig(
    val category: Category,
    val questionCount: Int,
    val passThresholdPercentage: Float = 0.75f
)

data class ExamQuestionState(
    val question: Question,
    val selectedAnswerIndex: Int? = null,
    val isMarkedForReview: Boolean = false // Merken für spätere Durchsicht
)

data class ExamResultPart(
    val category: Category,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Float,
    val isPassed: Boolean
)

data class ExamResult(
    val sessionId: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val partResults: List<ExamResultPart>,
    val isOverallPassed: Boolean,
    val wrongQuestions: List<Question>
)
```

### 3.2 SQLDelight Schema-Erweiterung (`Exam.sq`)
```sql
CREATE TABLE ExamHistory (
    session_id TEXT NOT NULL PRIMARY KEY,
    timestamp INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    technik_richtig INTEGER NOT NULL,
    technik_gesamt INTEGER NOT NULL,
    betrieb_richtig INTEGER NOT NULL,
    betrieb_gesamt INTEGER NOT NULL,
    vorschriften_richtig INTEGER NOT NULL,
    vorschriften_gesamt INTEGER NOT NULL,
    gesamt_bestanden INTEGER NOT NULL -- 0 = nein, 1 = ja
);

CREATE TABLE ExamWrongQuestions (
    session_id TEXT NOT NULL,
    frage_id TEXT NOT NULL,
    PRIMARY KEY(session_id, frage_id),
    FOREIGN KEY(session_id) REFERENCES ExamHistory(session_id) ON DELETE CASCADE
);

insertExamHistory:
INSERT INTO ExamHistory(session_id, timestamp, duration_seconds, technik_richtig, technik_gesamt, betrieb_richtig, betrieb_gesamt, vorschriften_richtig, vorschriften_gesamt, gesamt_bestanden)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

insertExamWrongQuestion:
INSERT INTO ExamWrongQuestions(session_id, frage_id) VALUES (?, ?);

selectExamHistory:
SELECT * FROM ExamHistory ORDER BY timestamp DESC LIMIT 20;

selectLatestWrongQuestions:
SELECT DISTINCT frage_id FROM ExamWrongQuestions
ORDER BY rowid DESC LIMIT 100;
```

---

## 4. Detaillierte Arbeitspakete (Work Packages)

### WP 3.1: Prüfungs-Engine & Zufallsgenerator
- **Tasks**:
  1. Implementierung von `ExamGenerator`:
     - Zieht die vorgegebene Anzahl zufälliger Fragen pro Kategorie aus dem Katalog (34 Technik, 25 Betrieb, 25 Vorschriften).
     - Gewährleistet eine gleichmäßige Verteilung über verschiedene Themenblöcke (Topics).
  2. Implementierung von `ExamTimerManager`:
     - Countdown-Zähler via Coroutine `flow`, pause- und stoppbar.
     - Automatisches Einreichen der Prüfung bei Ablauf des Timers (`timeOut`).

### WP 3.2: Prüfungs-UI (Ablauf & Navigation)
- **Tasks**:
  1. **Exam Mode Screen**:
     - Striktes Prüfungs-Layout (im Gegensatz zum Übungsmodus **kein** Sofort-Feedback!).
     - Fragen-Matrix / Navigationsleiste: Schneller Sprung zu Fragen 1..84.
     - Farbkodierung in der Matrix: Unbeantwortet (grau), Beantwortet (blau), Zur Überprüfung markiert (gelb).
     - Countdown-Anzeige in der Top-Bar mit visueller Warnung in den letzten 10 Minuten.
  2. Dialog „Prüfung abgeben“: Warnung, falls noch unbeantwortete Fragen vorliegen.

### WP 3.3: Auswertungs-Screen & Prüfungsbereitschaft
- **Tasks**:
  1. **Exam Result Screen**:
     - Großes Banner: **BESTANDEN (Herzlichen Glückwunsch!)** in Grün bzw. **NICHT BESTANDEN** in Rot.
     - Aufschlüsselung der 3 Fächer mit Fortschrittsbalken und Anzeige der 75 %-Schwelle.
     - Liste aller falsch beantworteten Fragen mit Detailansicht (Was wurde gewählt vs. Was war richtig + Erklärung).
  2. **Fehlertrainings-Shortcut**:
     - Button „Falsche Fragen sofort üben“: Startet eine reguläre Übungssitzung (wie in MVP 1/2) mit genau diesen Fragen.
  3. **Prüfungsbereitschafts-Score**:
     - Logik im `ExamReadinessCalculator`: Gewichteter Durchschnitt der letzten 3 Prüfungssimulationen. Liegen alle 3 bei >= 80 %, zeigt die App „🟢 Sehr gut vorbereitet (Prüfungsbereit)“.

---

## 5. UI/UX Spezifikation

```
+--------------------------------------------------------+
| [X] Abbrechen        01:24:12 verbleibend     [ABGEBEN]|
+--------------------------------------------------------+
| Teil: Technik | Frage 18 / 84          [!] Markieren   |
| [1][2][3]...[17][*18*][19]...[84] (17 beantwortet)     |
+--------------------------------------------------------+
|                                                        |
|  [ Frage TD-202 ]                                      |
|  Welche Einheit hat die elektrische Kapazität?         |
|                                                        |
|  ( ) Henry (H)                                         |
|  (X) Farad (F)                                         |
|  ( ) Ohm (Ω)                                           |
|  ( ) Siemens (S)                                       |
|                                                        |
+--------------------------------------------------------+
| [ < Vorherige Frage ]              [ Nächste Frage > ] |
+--------------------------------------------------------+

--- [ NACH DER ABGABE: ERGEBNIS ] ---

+--------------------------------------------------------+
|                  PRÜFUNGSERGEBNIS                      |
|             [ BESTANDEN - 88 % Gesamt ]                |
+--------------------------------------------------------+
|                                                        |
|  Technik:        29 / 34 richtig (85 %)    [ BESTANDEN]|
|  Betrieb:        23 / 25 richtig (92 %)    [ BESTANDEN]|
|  Vorschriften:   22 / 25 richtig (88 %)    [ BESTANDEN]|
|                                                        |
|  Benötigt je Fach: Mindestens 75 %                     |
|                                                        |
|  [ FEHLERTRAINING STARTEN (10 Falsche Fragen) ]        |
|  [ Zurück zur Übersicht ]                              |
+--------------------------------------------------------+
```

---

## 6. Test- und Verifikationsplan

### 6.1 Automatisierte Tests
| Test | Beschreibung |
|---|---|
| `ExamGeneratorTest` | Stellt sicher, dass exakt 34 Technik-, 25 Betriebs- und 25 Vorschriftenfragen zusammengestellt werden und keine Frage doppelt vorkommt. |
| `ExamEvaluationTest` | Validiert Grenzfälle der 75 %-Schwelle: z. B. 26/34 (76.5% -> Bestanden) vs. 25/34 (73.5% -> Durchgefallen). |
| `ExamTimerTest` | Testet Countdown-Ablauf, Pause und das automatische Auslösen von `submitExam()` bei 0 Sekunden. |

### 6.2 Manueller Akzeptanztest
1. Prüfungssimulation starten: Timer läuft rückwärts.
2. 5 Fragen beantworten, 1 Frage mit Lesezeichen/Review markieren: Matrix zeigt korrekte Farben.
3. Vorzeitig auf „Abgeben“ tippen: Warn-Dialog erscheint wegen unbeantworteter Fragen.
4. Nach Bestätigung: Auswertungsscreen prüfen, Button „Fehlertraining starten“ betätigen -> Übungssitzung mit den Fehlern startet.

---

## 7. Definition of Done (DoD)

- [ ] Zufallsgenerator erzeugt normkonforme Prüfungsbögen (84 Fragen, getrennt nach den 3 Fächern).
- [ ] Timer läuft stabil im Vorder- und Hintergrund während der Prüfung.
- [ ] Prüfungsmatrix erlaubt wahlfreien Zugriff auf alle Fragen.
- [ ] Strikte 75 %-Bestehenslogik je Fachbereich wird korrekt visualisiert und historisiert.
- [ ] Falsch beantwortete Prüfungsfragen können nahtlos im Fehlertraining wiederholt werden.
