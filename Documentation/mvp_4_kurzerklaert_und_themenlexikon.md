# Implementierungsplan: MVP 4 – „Kurz erklärt“ & Themenlexikon

## 1. Zielsetzung & Umfang (Scope)

Das didaktische Leitmotiv der App lautet: *Verstehen statt reines Auswendiglernen*. **MVP 4** implementiert das Modul **„Kurz erklärt“**. Zu allen prüfungsrelevanten Themenbereichen existieren kompakte, in ca. 30 Sekunden erfassbare Wissenskarten. Diese sind direkt aus dem Übungsmodus heraus über die `topic_id` verknüpft und stehen zusätzlich in einem eigenständigen, durchsuchbaren Themenlexikon zur Verfügung.

### Erfüllte Anforderungen aus der SRS:
- **F3.1**: Themenblöcke mit einheitlichem Aufbau:
  - 1 Satz Kernaussage (das „Warum“).
  - 2–4 Sätze Erklärung (der technische/rechtliche Zusammenhang).
  - Optional: Merksatz / Eselsbrücke (Mnemonik).
  - Optional: Mini-Formel bzw. Kenndaten.
- **F3.2**: Verknüpfung der Fragen über `topic_id` (n:1 und n:m Beziehung zwischen Fragen und Themen).
- **F3.3**: Kontextueller Zugriff: Button „Hintergrund“ direkt unter der Frage (erscheint nach dem Beantworten) sowie Hauptmenü-Eintrag für das Nachschlagewerk mit Schnellsuche und Kategoriefilter.
- **F3.4**: Speicherung des Verlaufs der zuletzt gelesenen Themen.
- **F3.5**: Konzeptioneller Entwurf für ca. 100–150 modulare Themenblöcke mit max. 30 Sekunden Lesezeit.

---

## 2. Didaktische Struktur der „Kurz erklärt“-Karten

Ein Themenblock folgt einer festen, kognitiv optimierten Mikro-Lernstruktur:

```
+-------------------------------------------------------------------------------+
| THEMENBLOCK: "Wellenlänge & Frequenz" (ID: top_technik_lambda_f)             |
+-------------------------------------------------------------------------------+
| [1] KERNAUSSAGE (Das "Warum" / 1 Satz)                                        |
| Je höher die Frequenz eines Signals ist, desto kürzer wird seine Wellenlänge. |
+-------------------------------------------------------------------------------+
| [2] ERKLÄRUNG (Der Zusammenhang / 2-4 Sätze)                                  |
| Elektromagnetische Wellen breiten sich im Vakuum und in der Luft mit          |
| Lichtgeschwindigkeit c (ca. 300.000 km/s) aus. Die Wellenlänge lambda gibt   |
| den räumlichen Abstand zweier Wellenberge an. Sie berechnet sich aus dem      |
| Verhältnis von Lichtgeschwindigkeit zur Frequenz: lambda = c / f.             |
+-------------------------------------------------------------------------------+
| [3] FORMEL & FAUSTFORMEL                                                      |
| lambda [m] = 300 / f [MHz]                                                    |
+-------------------------------------------------------------------------------+
| [4] ESELSBRÜCKE / MERKSATZ                                                    |
| "Hohe Frequenz = winzige Antenne, tiefe Frequenz = riesiger Draht!"           |
+-------------------------------------------------------------------------------+
```

---

## 3. Technische Architektur & Datenmodell

### 3.1 DTO & Domain Model
```kotlin
@Serializable
data class TopicDto(
    val topic_id: String,
    val kategorie: String,
    val titel: String,
    val kernaussage: String,
    val erklaerung: String,
    val merksatz: String? = null,
    val formel: String? = null,
    val verwandte_themen: List<String> = emptyList()
)

data class Topic(
    val id: String,
    val category: Category,
    val title: String,
    val keyTakeaway: String,
    val explanation: String,
    val mnemonic: String?,
    val formula: String?,
    val relatedTopicIds: List<String>
)
```

### 3.2 SQLDelight Erweiterung für Leseverlauf (`TopicHistory.sq`)
```sql
CREATE TABLE TopicHistory (
    topic_id TEXT NOT NULL PRIMARY KEY,
    gelesen_am INTEGER NOT NULL -- Timestamp
);

insertOrUpdateReadTopic:
INSERT INTO TopicHistory(topic_id, gelesen_am)
VALUES (?, ?)
ON CONFLICT(topic_id) DO UPDATE SET gelesen_am = excluded.gelesen_am;

selectRecentTopics:
SELECT topic_id, gelesen_am FROM TopicHistory
ORDER BY gelesen_am DESC LIMIT 10;
```

### 3.3 Repositories
```kotlin
interface TopicRepository {
    suspend fun getTopicById(id: String): Topic?
    suspend fun searchTopics(query: String, category: Category?): List<Topic>
    suspend fun getAllTopics(): List<Topic>
    fun getRecentlyViewedTopicsStream(): Flow<List<Topic>>
    suspend fun markTopicAsViewed(topicId: String)
}
```

---

## 4. Detaillierte Arbeitspakete (Work Packages)

### WP 4.1: Topic-Katalog & Daten-Ingestion
- **Tasks**:
  1. Anlegen von `app/src/main/assets/topics_klasse_e.json` mit den Basis-Themenblöcken (z. B. Wellenlänge, Ohmsches Gesetz, Modulation, Antennenformen, UTC/Rufzeichen, Bandgrenzen, EMV-Sicherheitsabstand).
  2. Implementierung des `AssetTopicRepository` zum Laden und schnellen Indizieren (In-Memory Lookup Map nach `topic_id`).
  3. Validierungstest: Jede in `questions_klasse_e.json` referenzierte `topic_id` muss in `topics_klasse_e.json` existieren (Integritätsprüfung).

### WP 4.2: Kontext-Button im Übungsmodus
- **Tasks**:
  1. Aktualisierung der `QuestionCard` / `PracticeScreen`:
     - Nach Beantwortung der Frage erscheint neben der Erklärung ein auffälliger Button: `[ 💡 Hintergrundwissen: {Topic-Titel} ]`.
  2. Klick öffnet ein animiertes **ModalBottomSheet** oder wechselt zum `TopicDetailScreen`.
  3. Automatisches Protokollieren des Aufrufs in `TopicHistory` zur Pflege des Leseverlaufs.

### WP 4.3: Nachschlagewerk & Volltextsuche
- **Tasks**:
  1. **TopicListScreen (Lexikon)**:
     - Suchleiste mit Sofortfilterung (`TextField` mit `Debounce 200 ms`).
     - Filter-Chips: *Alle*, *Technik*, *Betrieb*, *Vorschriften*.
     - Bereich „Zuletzt gelesen“ am oberen Rand (Horizontale Scrollreihe).
  2. **TopicDetailScreen**:
     - Modernes Card-Design mit klar abgegrenzten Sektionen (Kernaussage, Erklärung, Formelbox, Eselsbrücke).
     - Liste aller zu diesem Thema gehörenden Fragen mit direktem Button: „Diese Fragen üben“.

---

## 5. UI/UX Spezifikation

```
+--------------------------------------------------------+
| Nachschlagewerk                          [ 🔍 Suche ]  |
+--------------------------------------------------------+
| [Alle] [Technik] [Betrieb] [Vorschriften]              |
|                                                        |
| Zuletzt gelesen:                                       |
| [ Wellenlänge & Freq. ] [ Ohmsches Gesetz ] [ UTC ]    |
|                                                        |
| Alle Themen (Technik):                                 |
| +---------------------------------------------------+  |
| | Wellenlänge & Frequenz                            |  |
| | "Je höher die Frequenz, desto kürzer die Welle."  |  |
| | 12 Fragen verknüpft                         [ > ] |  |
| +---------------------------------------------------+  |
| | Der Schwingkreis (LC)                             |  |
| | "Resonanz entsteht, wenn XL gleich XC ist."       |  |
| | 8 Fragen verknüpft                          [ > ] |  |
| +---------------------------------------------------+  |
+--------------------------------------------------------+
```

---

## 6. Test- und Verifikationsplan

### 6.1 Automatisierte Tests
| Test | Beschreibung |
|---|---|
| `TopicReferentialIntegrityTest` | Iteriert über alle Fragen im Katalog und prüft, ob die referenzierte `topic_id` im Themenkatalog vorhanden ist (keine toten Verweise). |
| `TopicSearchTest` | Testet die Volltextsuche: Suchbegriffe wie „Ohm“, „Wellenlänge“ oder „Relais“ liefern die erwarteten Themenkarten. |
| `TopicHistoryTest` | Verifiziert, dass `markTopicAsViewed` doppelte Einträge verhindert und die Historie zeitlich absteigend sortiert. |

### 6.2 Manueller Akzeptanztest
1. Im Übungsmodus eine Frage beantworten.
2. Prüfen, ob der Button „Hintergrund: [Thema]“ sichtbar wird.
3. Klick auf den Button: Die 30-Sekunden-Themenkarte öffnet sich.
4. Zurück zum Hauptmenü -> Nachschlagewerk aufrufen: Das eben geöffnete Thema steht unter „Zuletzt gelesen“.

---

## 7. Definition of Done (DoD)

- [ ] Mindestens 50 kuratierte Themenblöcke der Klasse E sind strukturiert erfasst.
- [ ] Jede Prüfungsfrage besitzt eine gültige `topic_id`-Verknüpfung.
- [ ] Der Aufruf aus der Frage heraus erfolgt verzögerungsfrei via BottomSheet oder Screen-Navigation.
- [ ] Das Themenlexikon bietet flüssige Suche und Filtermöglichkeiten.
- [ ] Der Leseverlauf wird zuverlässig in der SQLite-Datenbank persistiert.
