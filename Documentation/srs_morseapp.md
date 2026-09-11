# Software Requirements Specification (SRS)

## Lern-App für die Amateurfunkprüfung Klasse E (Deutschland)

| Feld | Wert |
|---|---|
| **Dokumentversion** | 1.0 |
| **Datum** | 11. September 2026 |
| **Status** | Entwurf |
| **Primärplattform** | Android |
| **Sekundärplattform** | Web (Wasm) |

---

## 1. Einleitung

### 1.1 Zweck

Dieses Dokument beschreibt die Anforderungen an eine Lern-App zur Vorbereitung auf die Amateurfunkprüfung der Klasse E in Deutschland. Die App richtet sich primär an Android-Nutzer, eine Web-Version ist als sekundäre Plattform geplant.

### 1.2 Geltungsbereich

Die App ist ein **Prüfungsvorbereitungswerkzeug**, kein Lehrbuch. Der Fokus liegt auf dem effizienten Durcharbeiten des offiziellen Fragenkatalogs der Bundesnetzagentur, ergänzt durch kurze Hintergrundinformationen und eine lokal gespeicherte Lernfortschrittsverfolgung.

### 1.3 Begriffsdefinitionen

| Begriff | Bedeutung |
|---|---|
| **BNetzA** | Bundesnetzagentur für Elektrizität, Gas, Telekommunikation, Post und Eisenbahnen |
| **Klasse E** | Einsteigerklasse im deutschen Amateurfunk (seit Juni 2024 neben N und A) |
| **Fragenkatalog** | Offizieller Prüfungsfragenpool der BNetzA, 3. Auflage März 2024 |
| **Leitner-System** | Lernmethode mit 5-stufiger Intervallwiederholung |
| **KMP** | Kotlin Multiplatform |
| **CMP** | Compose Multiplatform |
| **SRS** | Software Requirements Specification |
| **SAF** | Storage Access Framework (Android) |
| **OPFS** | Origin Private File System (Web) |

### 1.4 Referenzen

- Bundesnetzagentur: Offizieller Fragenkatalog, 3. Auflage März 2024
- DARC: Ausbildungsreferat, Prüfungsinformationen
- 12db.de: Online-Quiz mit allen Fragen
- Bestehende Apps: AFuP Karten, FunkTraining2Go, Hamfisted, 50Ohm-App

---

## 2. Gesamtbeschreibung

### 2.1 Produktperspektive

Die App ist ein eigenständiges Produkt, das den offiziellen Fragenkatalog der BNetzA nutzt und lokal auf dem Gerät speichert. Es besteht keine Abhängigkeit von Servern oder Cloud-Diensten. Die Web-Version ist als Zweitkanal geplant, wobei die Daten über Import/Export synchronisiert werden.

### 2.2 Produktfunktionen (Übersicht)

| Nr. | Funktion | Priorität |
|---|---|---|
| F1 | Intelligentes Fragenübungssystem (kategorisiert, Leitner-System) | Hoch |
| F2 | Simulierte Prüfung mit realistischen Bedingungen | Hoch |
| F3 | Kurzgehaltene Hintergrundinformationen zu Themen | Hoch |
| F4 | Lokale Speicherung des Lernfortschritts | Hoch |
| F5 | Import/Export der Lerndaten (JSON) | Mittel |
| F6 | Merkblätter und Formelsammlung | Mittel |
| F7 | Lernfortschrittsverfolgung und Statistiken | Mittel |
| F8 | Barrierefreiheit (VoiceOver, Bildbeschreibungen) | Mittel |
| F9 | Web-Version (sekundär) | Niedrig |

### 2.3 Zielgruppe

- **Primär**: Personen, die sich auf die Amateurfunkprüfung Klasse E vorbereiten
- **Sekundär**: Personen mit Sehbeeinträchtigungen (VoiceOver-Nutzer)
- **Tertiär**: Nutzer, die die App auf Desktop-Browser nutzen möchten

### 2.4 Betriebsumgebung

| Plattform | Priorität | Technologie |
|---|---|---|
| Android | Primär | Kotlin, Compose Multiplatform, SQLDelight |
| Web (Wasm) | Sekundär | Compose Multiplatform (Wasm), localStorage/IndexedDB |

### 2.5 Annahmen und Abhängigkeiten

- Der offizielle Fragenkatalog der BNetzA liegt als strukturierte JSON-Datei vor.
- Die App benötigt keine Netzwerkverbindung für das Fragenüben.
- Für die Web-Version wird eine separate Speicherlösung verwendet, da SQLDelight auf `wasmJs` nicht offiziell unterstützt wird.

---

## 3. Funktionale Anforderungen

### 3.1 F1: Intelligentes Fragenübungssystem

**F1.1** Die App enthält den vollständigen offiziellen Fragenkatalog der BNetzA (3. Auflage, März 2024) für die Klasse E.

**F1.2** Die App bietet zwei Lernmodi:

- **Einfacher Modus**: Richtig beantwortet = erledigt, falsch = wird erneut gezeigt
- **Leitner-System**: 5-stufige Intervallwiederholung für langfristiges Lernen

**F1.3** Jede Frage ist einer Kategorie (Technik, Betrieb, Vorschriften) und einem Themenbereich zugeordnet. Die App filtert nach Kategorien und Themenbereichen.

**F1.4** Die App zeigt zu jeder Frage eine verständliche Erklärung, warum die richtige Antwort richtig ist und warum die anderen Optionen falsch sind.

**F1.5** Die App bietet gezieltes Üben von Problemfragen (Fragen, die häufig falsch beantwortet werden).

**F1.6** Die App unterstützt Lesezeichen für Fragen, die später wiederholt werden sollen.

### 3.2 F2: Simulierte Prüfung

**F2.1** Die App simuliert realistische Prüfungsbedingungen mit Zeitlimit. Die Klasse-E-Prüfung besteht aus drei Teilen (Technik, Betrieb, Vorschriften) mit einer Bestehensgrenze von 75 %.

**F2.2** Nach Abschluss einer simulierten Prüfung erfolgt eine sofortige Auswertung mit Anzeige der erreichten Punktzahl und Bestehensstatus.

**F2.3** Die App generiert nach der simulierten Prüfung eine Liste der falsch beantworteten Fragen, die direkt ins Fehlertraining übernommen werden können.

**F2.4** Die App zeigt die Bereitschaft für den Prüfungstermin an (basierend auf den letzten Simulationsergebnissen).

### 3.3 F3: Hintergrundinformationen („Kurz erklärt")

**F3.1** Zu jedem Themenbereich existiert eine kurzgehaltene Hintergrundseite mit:

- Einem Satz Kernaussage (das „Warum")
- 2–4 Sätzen Erklärung (der Zusammenhang)
- Optional: Merksatz oder Eselsbrücke
- Optional: Mini-Grafik/Formel

**F3.2** Jede Frage ist über eine `topic_id` mit einem oder mehreren Themenblöcken verknüpft. Mehrere Fragen können auf dasselbe Thema verweisen.

**F3.3** Die Hintergrundseite ist direkt unter der Frage erreichbar (Button „Hintergrund", erscheint nach dem Antworten) sowie als eigenes Nachschlagewerk mit Suche/Filter.

**F3.4** Ein Verlauf der zuletzt gelesenen Hintergrundthemen wird gespeichert.

**F3.5** Die Hintergrundseiten sind so konzipiert, dass sie in ca. 30 Sekunden erfassbar sind.

### 3.4 F4: Lokale Speicherung des Lernfortschritts

**F4.1** Alle Lerndaten werden lokal auf dem Gerät gespeichert. Es besteht keine Account-Pflicht und keine Serverabhängigkeit.

**F4.2** Folgende Daten werden lokal gespeichert:

- Pro Frage: Status (neu / richtig / falsch / gemeistert)
- Fehlerzähler pro Frage
- Letzter Antwortzeitpunkt (für Leitner-Intervalle)
- Lernstreak (Tage in Folge)
- Statistik pro Fach und Thema
- Zuletzt gelesene Hintergrundthemen
- Einstellungen (Schriftgröße, Dunkelmodus, Prüfungstimer)

**F4.3** Auf Android wird SQLDelight zur Speicherung verwendet. Die Datenbank ist migrationsfähig.

**F4.4** Die App unterstützt einen Dunkelmodus.

### 3.5 F5: Import/Export der Lerndaten

**F5.1** Die App bietet eine Export-Funktion, die alle Lerndaten als JSON-Datei speichert.

**F5.2** Die App bietet eine Import-Funktion, die eine zuvor exportierte JSON-Datei einliest und die Lerndaten wiederherstellt.

**F5.3** Auf Android wird das Storage Access Framework (SAF) für den Datei-Export/Import genutzt. Die plattformübergreifende Dateioperation kann über FileKit erfolgen, das KMP, Compose und Wasm unterstützt.

**F5.4** Das JSON-Format wird frühzeitig definiert und ist zwischen Android und Web kompatibel.

### 3.6 F6: Merkblätter und Formelsammlung

**F6.1** Die App enthält eine Formelsammlung mit elektrotechnischen Grundformeln und Berechnungen der elektromagnetischen Verträglichkeit.

**F6.2** Die App enthält ein Abkürzungsverzeichnis (Q-Gruppen, internationale Abkürzungen, Landeskenner).

**F6.3** Die App bietet ein Quiz zum Üben des ITU-Buchstabieralphabets.

**F6.4** Die App enthält relevante Vorschriften und Regelungen sowie technische Diagramme und Schaubilder.

**F6.5** Ein integrierter Taschenrechner unterstützt bei der Lösung von Berechnungsaufgaben.

### 3.7 F7: Lernfortschrittsverfolgung

**F7.1** Die App zeigt den Lernfortschritt pro Kategorie und Themenbereich an.

**F7.2** Die App identifiziert Stärken und Schwächen des Nutzers basierend auf der Genauigkeitsrate pro Themenbereich.

**F7.3** Eine Lernstandsanzeige zeigt, wie viel Übungsbedarf noch besteht.

### 3.8 F8: Barrierefreiheit

**F8.1** Der Amateurfunk-Teil der App ist für VoiceOver-Nutzer optimiert und enthält Bildbeschreibungen für Abbildungen.

**F8.2** Alle interaktiven Elemente sind mit VoiceOver-Gesten navigierbar und verfügen über aussagekräftige Labels.

**F8.3** Die Schriftgröße ist anpassbar.

### 3.9 F9: Web-Version (sekundär)

**F9.1** Die Web-Version teilt die gemeinsame Codebasis mit der Android-Version (KMP/CMP).

**F9.2** Die Web-Version verwendet localStorage oder IndexedDB zur Speicherung, da SQLDelight auf dem `wasmJs`-Target nicht offiziell unterstützt wird.

**F9.3** Bei Verwendung von SQLDelight mit SQL.js im Web ist standardmäßig alles nur im Memory — für Persistenz muss OPFS manuell angebunden werden. Alternativ wird eine einfachere JSON-basierte Speicherung verwendet.

---

## 4. Nicht-funktionale Anforderungen

### 4.1 Leistung

- Die App reagiert innerhalb von 100 ms auf Benutzereingaben.
- Die Fragendatenbank wird vollständig lokal gehalten; keine Netzwerkanfragen für das Fragenüben.
- Die App ist offline vollständig nutzbar.

### 4.2 Zuverlässigkeit

- Die lokale Datenbank wird bei App-Updates migriert.
- Export/Import verhindert Datenverlust bei Gerätewechsel oder Neuinstallation.

### 4.3 Benutzerfreundlichkeit

- Die Benutzeroberfläche ist minimalistisch und auf die drei Kernfunktionen „Fragen üben, Fehler wiederholen, simulierte Prüfung" fokussiert.
- Die App ist werbefrei (optional: In-App-Kauf zur Freischaltung des vollständigen Katalogs).
- Bestehende Apps verwenden ein Modell mit kostenloser Testversion (ca. 10 % der Fragen) und kostenpflichtiger Freischaltung.

### 4.4 Datenschutz

- Es werden keine personenbezogenen Daten erhoben.
- Alle Lerndaten bleiben auf dem Gerät des Nutzers.
- Keine DSGVO-relevanten Datenverarbeitungen.

### 4.5 Wartbarkeit

- Der Fragenkatalog wird als JSON eingebunden und kann bei Aktualisierungen der BNetzA einfach ausgetauscht werden.
- Die Hintergrundtexte sind themenbasiert strukturiert (ca. 100–150 Themenblöcke), nicht pro Frage.

### 4.6 Portabilität

- Die Android-Version ist die Primärplattform und production-ready.
- Die Web-Version ist als sekundäre Plattform geplant und nutzt dieselbe Codebasis, soweit technisch sinnvoll.

---

## 5. Technische Architektur

### 5.1 Technologie-Stack

| Komponente | Android (Primär) | Web (Sekundär) | Gemeinsam |
|---|---|---|---|
| **Sprache** | Kotlin | Kotlin (Wasm) | ✅ `commonMain` |
| **UI** | Compose Multiplatform | Compose Multiplatform (Wasm) | ✅ gemeinsame UI |
| **Datenbank** | SQLDelight | localStorage / IndexedDB | `expect`/`actual`-Schnittstelle |
| **Datei-I/O** | FileKit (SAF) | FileKit (Wasm) | ✅ FileKit |
| **Serialisierung** | kotlinx.serialization | kotlinx.serialization | ✅ `commonMain` |
| **Build** | Gradle | Gradle (`wasmJsBrowserDistribution`) | ein Projekt |

### 5.2 Plattformstatus

| Bereich | Status | Bedeutung |
|---|---|---|
| **Android** | Production-ready | Primärziel, alle Funktionen umsetzbar |
| **Web (Wasm)** | Beta (seit CMP 1.9.0) | Für Lern-App ausreichend stabil |
| **SQLDelight auf Wasm** | Nicht offiziell unterstützt | Web-Speicherung erfordert separate Lösung |
| **FileKit auf Wasm** | Unterstützt | Für Export/Import nutzbar |

### 5.3 Empfohlene Reihenfolge

1. **Android zuerst**: SQLDelight + Compose, alle Funktionen, Export-Funktion direkt mit einbauen.
2. **Web später**: Wenn Android stabil läuft, Web-Version angehen — mit separater Speicherlösung.
3. **Datenformat früh festlegen**: JSON-Struktur jetzt definieren, damit beide Seiten kompatibel bleiben.

---

## 6. Datenmodell (Auszug)

### 6.1 Entitäten

**Frage (Question)**

```
id: String (z.B. "TE-001")
kategorie: String (Technik | Betrieb | Vorschriften)
topic_id: String (Verweis auf Themenblock)
frage_text: String
antworten: List<String> (4 Optionen)
richtige_antwort: Int (0-3)
erklaerung: String (warum richtig/falsch)
bild_svg: String? (optional)
```

**Themenblock (Topic)**

```
topic_id: String
titel: String
kernaussage: String
erklaerung: String
merksatz: String? (optional)
formel: String? (optional)
```

**Lernstand (Progress)**

```
frage_id: String
status: Enum (NEU | RICHTIG | FALSCH | GEMEISTERT)
fehlerzaehler: Int
letzte_antwort: Long (Timestamp)
leitner_box: Int (0-4)
```

**Statistik (Statistics)**

```
kategorie: String
thema: String
gesamt_fragen: Int
richtig: Int
falsch: Int
genauigkeit: Float
```

### 6.2 Export-Format (JSON)

```json
{
  "version": "1.0",
  "exportiert_am": "2026-09-11T12:00:00Z",
  "lernstand": [
    {
      "frage_id": "TE-001",
      "status": "FALSCH",
      "fehlerzaehler": 2,
      "letzte_antwort": 1726056000000,
      "leitner_box": 1
    }
  ],
  "statistiken": {
    "Technik": { "richtig": 120, "falsch": 45 },
    "Betrieb": { "richtig": 80, "falsch": 20 },
    "Vorschriften": { "richtig": 95, "falsch": 15 }
  },
  "einstellungen": {
    "dunkelmodus": true,
    "schriftgroesse": "mittel"
  },
  "gelesene_themen": ["wellenlaenge", "spannungsteiler"],
  "streak": 7
}
```

---

## 7. Schnittstellen

### 7.1 Interne Schnittstellen

| Schnittstelle | Beschreibung |
|---|---|
| **Datenbank-Repository** | `expect`/`actual` für Android (SQLDelight) und Web (localStorage/IndexedDB) |
| **Fragenkatalog-Loader** | Lädt JSON aus `commonMain/resources` |
| **FileKit** | Datei-Export/Import über plattformspezifische Dialoge |
| **Compose Navigation** | Navigation zwischen Screens (Üben, Prüfung, Hintergrund, Statistik, Einstellungen) |

### 7.2 Externe Schnittstellen

| Schnittstelle | Beschreibung |
|---|---|
| **BNetzA Fragenkatalog** | JSON-Datei, manuell aktualisierbar |
| **SAF (Android)** | `ActivityResultContracts.CreateDocument` für Export |

---

## 8. Anhang

### 8.1 Quellen

- Bundesnetzagentur: Offizieller Fragenkatalog, 3. Auflage März 2024
- DARC: Ausbildungsreferat, Prüfungsinformationen
- 12db.de: Online-Quiz mit allen Fragen
- Bestehende Apps: AFuP Karten, FunkTraining2Go, Hamfisted, 50Ohm-App

### 8.2 Referenzierte bestehende Apps

| App | Plattform | Funktionen |
|---|---|---|
| **AFuP Karten** | iOS | Fragenkatalog, Leitner-System, Prüfungssimulation, Formelsammlung |
| **FunkTraining2Go** | iOS | VoiceOver, Offline, Formelsammlung, Q-Gruppen-Quiz |
| **Hamfisted** | Android | Klassen N/E/A, Spaced Repetition, Prüfungssimulation |
| **50Ohm-App** | Android/iOS | Prüfungssimulation, Lernstandsanzeige, Taschenrechner, Videoverknüpfung |

### 8.3 Technische Referenzen

- **SQLDelight**: KMP SQL ORM, auf Android production-ready, auf `wasmJs` nicht offiziell unterstützt.
- **OPFS für Web-Persistenz**: Erfordert manuelle Anbindung an SQLDelight/SQL.js.
- **FileKit**: KMP-Bibliothek für Dateioperationen, unterstützt Android, iOS, JS, Wasm.
- **Compose Multiplatform for Web**: Seit Version 1.9.0 Beta, für Lern-App ausreichend stabil.

