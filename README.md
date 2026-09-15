# Amateurfunk Training – Klasse E

> **Die kostenlose, werbefreie Lern-App für die deutsche Amateurfunk-Prüfung Klasse E.**  
> Entwickelt mit Kotlin & Jetpack Compose. Vollständig Open Source.

---

## Features

| Feature | Beschreibung |
|---|---|
| 📚 **Fragenkatalog** | Alle offiziellen BNetzA-Prüfungsfragen für Klasse E |
| 🃏 **Leitner-System** | Intelligentes Karteikarten-Lernen mit 5 Boxen (Spaced Repetition) |
| 🎯 **Prüfungssimulation** | Realistische Prüfung mit Timer, Auswertung und Bestanden/Nicht-Bestanden |
| 📖 **Themenlexikon** | Alle Themengebiete als strukturierte Kurzartikel zum Nachlesen |
| 📐 **Formelsammlung** | Wichtige Formeln und Einheitenrechner für Technik-Fragen |
| 🔤 **Phonetisches Alphabet** | Interaktive Übersicht des ICAO-Alphabets |
| 📊 **Lernstatistik** | Fortschrittsübersicht, Lernstreak und Fehleranalyse |
| 💾 **Backup & Restore** | Export/Import des kompletten Lernstands als JSON (SAF, kein Cloud-Zwang) |
| ♿ **Barrierefreiheit** | Große Touch-Targets, dynamische Schriftgrößen, Kontrast-optimiertes UI |

---

## Screenshots

> *App auf Android-Gerät oder Emulator starten – keine Screenshots im Repository.*

---

## Technologie-Stack

- **Sprache:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Persistenz:** SQLite (via `AmateurfunkDbHelper`)
- **Serialisierung:** `kotlinx.serialization`
- **Architektur:** MVVM mit Repository-Pattern
- **Minimum Android:** API 31 (Android 12)
- **Target Android:** API 37

---

## Schnellstart

### Voraussetzungen

- Android Studio Ladybug (2024.2) oder neuer
- Android SDK 37
- JDK 17

### Repository klonen & bauen

```bash
git clone https://github.com/spacemishka/amateurfunk-training.git
cd amateurfunk-training
./gradlew assembleDebug
```

Die APK liegt nach dem Build unter:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Auf Gerät/Emulator installieren

```bash
./gradlew installDebug
```

---

## Tests ausführen

```bash
# Unit-Tests
./gradlew test

# Instrumented Tests (Gerät/Emulator erforderlich)
./gradlew connectedAndroidTest
```

---

## Release-Builds (CI/CD)

Bei jedem Git-Tag im Format `vMAJOR.MINOR.PATCH` wird automatisch eine
Release-APK kompiliert und als GitHub/GitLab-Release veröffentlicht.

```bash
# Neues Release erstellen
git tag -a v1.6.0 -m "Release 1.6.0"
git push origin v1.6.0
```

Die Pipeline führt automatisch durch:
1. Unit-Tests
2. `assembleRelease` mit versioniertem APK-Namen
3. Optionale APK-Signierung (wenn Keystore-Secrets gesetzt)
4. Release-Asset-Upload

Details: [`.github/workflows/release.yml`](.github/workflows/release.yml) &
[`.gitlab-ci.yml`](.gitlab-ci.yml)

---

## Beitragen

Beiträge sind willkommen! Bitte beachte die **Upstream-Contribution-Pflicht**
aus der [Lizenz](LICENSE.md) (Abschnitt 3.5):

1. **Fork** des Repositories erstellen
2. **Feature-Branch** anlegen: `git checkout -b feature/mein-beitrag`
3. Änderungen implementieren und **testen** (`./gradlew test`)
4. **Pull Request** (GitHub) oder **Merge Request** (GitLab) öffnen

Bitte beschreibe im PR/MR klar, was geändert wurde und warum.

### Code-Stil

- Kotlin-Konventionen gemäß [kotlinlang.org/docs/coding-conventions.html](https://kotlinlang.org/docs/coding-conventions.html)
- Jetpack Compose Best Practices
- Neue Features mit Unit-Tests absichern

---

## Projektstruktur

```
app/src/main/java/…/amateurfunktraining/
├── core/
│   ├── backup/        # BackupManager (Export/Import)
│   ├── calculator/    # Formelrechner-Logik
│   ├── data/          # Repositories (SQLite, Assets, InMemory)
│   ├── exam/          # Prüfungssimulations-Engine
│   └── model/         # Domänenmodelle & DTOs
└── feature/
    ├── home/          # Startseite & Lernfortschritt
    ├── practice/      # Leitner-Karteikarten-Training
    ├── exam/          # Prüfungssimulation
    ├── topic/         # Themenlexikon
    ├── reference/     # Formelsammlung
    ├── calculator/    # Einheitenrechner
    ├── phonetic/      # Phonetisches Alphabet
    └── settings/      # Einstellungen & Backup
```

---

## Lizenz

Dieses Projekt steht unter der
**[Non-Commercial Open Source License (NC-OSL) v1.1](LICENSE.md)**.

- ✅ Kostenlose Nutzung für immer
- ✅ Quellcode einsehen, lernen, modifizieren
- ✅ Weitergabe unter gleicher Lizenz
- ❌ Keine kommerzielle Nutzung (kein Verkauf, keine Werbung, keine kostenpflichtigen Dienste)
- 🔄 Weiterentwicklungen müssen als PR/MR an dieses Projekt zurückgegeben werden

Copyright © 2024–present spacemishka and contributors
