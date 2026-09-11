# Implementierungsplan: MVP 2 – Leitner-System & Lernfortschritt

## 1. Zielsetzung & Umfang (Scope)

Während in MVP 1 die Fragesitzungen nur flüchtig im Speicher lagen, führt **MVP 2** die vollständige lokale Persistenz und das didaktische Kernkonzept der App ein: das **5-Stufen-Leitner-System** (Spaced Repetition). Lernende können Fragen als Lesezeichen vormerken, Problemfragen (mehrfach falsch beantwortet) gezielt trainieren und ihren Lernfortschritt sowie ihren täglichen Streak dauerhaft auf dem Gerät nachverfolgen.

### Erfüllte Anforderungen aus der SRS:
- **F1.2**: 5-stufiges Leitner-System (Intervallwiederholung für nachhaltigen Lernerfolg).
- **F1.5**: Gezieltes Üben von Problemfragen (Fehlerzähler >= 2).
- **F1.6**: Lesezeichenfunktion (Bookmark-Flag pro Frage).
- **F4.1 & F4.2**: Lokale, datenschutzkonforme Speicherung (Status, Fehlerzähler, Zeitstempel, Leitner-Box, Streak).
- **F4.3**: Einsatz von **SQLDelight** auf Android mit schema-migrierbarer SQLite-Datenbank.
- **F7.1 & F7.3**: Lernfortschrittsanzeige nach Kategorien und Lernstand (Neu, In Arbeit, Gemeistert).

---

## 2. Didaktisches Leitner-System: Spezifikation

```
+---------------------------------------------------------------------------------+
|                                5-STUFEN-LEITNER-BOXEN                           |
+-------------------+--------------------+--------------------+-------------------+
| Box 1: Täglich    | Box 2: Alle 3 Tage | Box 3: Wöchentlich | Box 4: Alle 14 T. | Box 5: Gemeistert
| (Intervall: 1 T.) | (Intervall: 3 T.)  | (Intervall: 7 T.)  | (Intervall: 14 T.)| (Prüfungsreif)
+-------------------+--------------------+--------------------+-------------------+
        |                    |                    |                    |                   ^
        | [Richtig]          | [Richtig]          | [Richtig]          | [Richtig]         |
        +------------------->+------------------->+------------------->+-------------------+
        |                                                                                  |
        |<=====================[ Bei JEDEM Fehler: Zurück auf Box 1 ]======================|
```

1. **Einstufung**:
   - Jede neue Frage startet in **Box 0 (Neu)** bzw. **Box 1**.
   - **Richtig beantwortet**: Frage steigt in die nächste Box auf ($Box_{neu} = \min(Box_{alt} + 1, 5)$).
   - **Falsch beantwortet**: Frage fällt sofort in **Box 1** zurück ($Box_{neu} = 1$) und ihr `fehlerzaehler` wird inkrementiert.
2. **Fälligkeit**:
   - Eine Frage ist zur Wiederholung fällig, wenn: $Timestamp_{aktuell} - Timestamp_{letzte\_antwort} \ge Intervall(Box)$.
3. **Gemeistert-Status**:
   - Erreicht eine Frage Box 5, gilt sie als *gemeistert*.

---

## 3. Technische Architektur & Datenbankmodell

### 3.1 SQLDelight Schema (`core/database/src/main/sqldelight/.../Progress.sq`)
```sql
CREATE TABLE QuestionProgress (
    frage_id TEXT NOT NULL PRIMARY KEY,
    status TEXT NOT NULL,             -- 'NEU', 'RICHTIG', 'FALSCH', 'GEMEISTERT'
    fehlerzaehler INTEGER NOT NULL DEFAULT 0,
    letzte_antwort INTEGER NOT NULL DEFAULT 0, -- Unix Timestamp in Millisekunden
    leitner_box INTEGER NOT NULL DEFAULT 1,    -- 1 bis 5
    ist_lesezeichen INTEGER NOT NULL DEFAULT 0  -- 0 = nein, 1 = ja
);

CREATE TABLE UserMeta (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);

-- Queries:
selectAllProgress:
SELECT * FROM QuestionProgress;

getProgressForQuestion:
SELECT * FROM QuestionProgress WHERE frage_id = ?;

upsertAnswerResult:
INSERT INTO QuestionProgress(frage_id, status, fehlerzaehler, letzte_antwort, leitner_box, ist_lesezeichen)
VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT(frage_id) DO UPDATE SET
    status = excluded.status,
    fehlerzaehler = excluded.fehlerzaehler,
    letzte_antwort = excluded.letzte_antwort,
    leitner_box = excluded.leitner_box;

toggleBookmark:
UPDATE QuestionProgress
SET ist_lesezeichen = CASE WHEN ist_lesezeichen = 1 THEN 0 ELSE 1 END
WHERE frage_id = ?;

selectBookmarkedQuestions:
SELECT * FROM QuestionProgress WHERE ist_lesezeichen = 1;

selectProblemQuestions:
SELECT * FROM QuestionProgress WHERE fehlerzaehler >= 2 ORDER BY fehlerzaehler DESC;

selectDueLeitnerQuestions:
SELECT * FROM QuestionProgress
WHERE leitner_box < 5 AND letzte_antwort <= :dueTimestampThreshold;
```

### 3.2 Repositories und Use Cases
```kotlin
interface ProgressRepository {
    fun getProgressStream(questionId: String): Flow<QuestionProgress?>
    fun getAllProgressStream(): Flow<List<QuestionProgress>>
    suspend fun recordAnswer(questionId: String, isCorrect: Boolean)
    suspend fun toggleBookmark(questionId: String)
    suspend fun getProblemQuestionIds(): List<String>
    suspend fun getBookmarkedQuestionIds(): List<String>
    suspend fun getDueLeitnerQuestionIds(category: Category?): List<String>
    fun getStreakStream(): Flow<Int>
}
```

---

## 4. Detaillierte Arbeitspakete (Work Packages)

### WP 2.1: SQLDelight Integration & Persistenzschicht
- **Tasks**:
  1. Integration des SQLDelight Gradle-Plugins in `settings.gradle.kts` und `app/build.gradle.kts`.
  2. Anlegen von `Progress.sq` mit Tabellendefinitionen, Indizes und CRUD-Statements.
  3. Erstellung von `SqlDelightProgressRepository` mit reaktiven Flows (`asFlow().mapToList()`).
  4. Migrationstests für spätere Schema-Änderungen aufsetzen.

### WP 2.2: Leitner-Algorithmus & Streak-Logik
- **Tasks**:
  1. Implementierung der `LeitnerCalculator`-Logik (Berechnung von Ziel-Box, Fälligkeitszeitstempel).
  2. Implementierung der `StreakManager`-Komponente:
     - Speichern des Tagesdatums bei Beantwortung in `UserMeta`.
     - Ist die letzte Übung gestern erfolgt: `streak++`.
     - Liegt die letzte Übung mehr als 48 Stunden zurück: `streak = 1`.
     - Erfolgte heute bereits eine Übung: `streak` bleibt unverändert.

### WP 2.3: Feature-Erweiterungen in der Übungs-UI
- **Tasks**:
  1. **Lesezeichen-Icon** in der `TopAppBar` des `PracticeScreen`: Ein Klick toggelt den Bookmark-Status synchron in der SQLite-Datenbank.
  2. **Leitner-Box-Badge**: Dezente Anzeige in der Fragekarte (z. B. `Box 3 / 5` oder Punkteindikator).
  3. Verknüpfung der Antwortauswertung mit `ProgressRepository.recordAnswer(...)`.

### WP 2.4: Dashboard & Fortschritts-Visualisierung
- **Tasks**:
  1. **Home Screen Redesign**:
     - Streak-Banner mit Flammen-Icon („🔥 4 Tage Lernstreak“).
     - Leitner-Fortschrittsbalken pro Kategorie (Verteilung: Box 1, 2, 3, 4, 5).
     - Aktionskarten:
       - „Fällige Leitner-Wiederholungen (X Fragen bereit)“
       - „Problemfragen üben (Y Fragen mit Fehlern)“
       - „Lesezeichen (Z Fragen gemerkt)“
  2. Separate Übungsmodi im `PracticeViewModel` für Lesezeichen und Problemfragen.

---

## 5. UI/UX Spezifikation

```
+--------------------------------------------------------+
| Amateurfunk Training - Klasse E           [🔥 5 Tage]  |
+--------------------------------------------------------+
|                                                        |
|  [ Fällige Wiederholung ]                              |
|  Du hast 18 fällige Leitner-Fragen heute!              |
|  [ JETZT WIEDERHOLEN (18) ]                            |
|                                                        |
|  [ Prüfungsfächer ]                                    |
|  +--------------------------------------------------+  |
|  | Technik (Klasse E)                 142/240 gelernt|  |
|  | [■■■■■■■■■■■□□□□□□□□] Box 1-4: 80 | Box 5: 62     |  |
|  +--------------------------------------------------+  |
|  | Betrieb & Vorschriften              95/120 gelernt|  |
|  | [■■■■■■■■■■■■■■□□□□□] Box 1-4: 45 | Box 5: 50     |  |
|  +--------------------------------------------------+  |
|                                                        |
|  [ Spezial-Training ]                                  |
|  [★ Lesezeichen (12)]       [⚠ Problemfragen (8)]      |
+--------------------------------------------------------+
```

---

## 6. Test- und Verifikationsplan

### 6.1 Automatisierte Tests
| Test | Beschreibung |
|---|---|
| `LeitnerLogicTest` | Prüft, ob richtige Antworten von Box 1 bis Box 5 stufenweise aufsteigen und ein einziger Fehler die Frage zuverlässig auf Box 1 zurücksetzt. |
| `StreakCalculationTest` | Simuliert Beantwortungen über mehrere Tage hinweg (gleicher Tag, Folgetag, 2 Tage Pause) und validiert den Streak-Wert. |
| `SqlDelightProgressRepositoryTest` | In-Memory SQLite Test: Testet das Schreiben, Updaten und Filtern nach Lesezeichen und Problemfragen (`fehlerzaehler >= 2`). |

### 6.2 Manueller Testdurchlauf
1. Eine Frage 3-mal hintereinander absichtlich falsch beantworten.
2. Prüfen, ob die Frage im Menü „Problemfragen“ gelistet wird.
3. Lesezeichen bei 2 beliebigen Fragen aktivieren; App hart beenden und neu starten.
4. Menü „Lesezeichen“ öffnen: Beide Fragen müssen vorhanden sein.

---

## 7. Definition of Done (DoD)

- [ ] SQLite-Datenbank über SQLDelight initialisiert fehlerfrei und persistiert alle Antworten.
- [ ] 5-Stufen-Leitner-Algorithmus steuert die Fälligkeit von Wiederholungsfragen korrekt.
- [ ] Lesezeichen und Problemfragen können separat geübt werden.
- [ ] Lernstreak und Kategoriefortschritt werden nach jedem Durchlauf reaktiv aktualisiert.
- [ ] Alle Unit- und Integrationstests laufen erfolgreich durch.
