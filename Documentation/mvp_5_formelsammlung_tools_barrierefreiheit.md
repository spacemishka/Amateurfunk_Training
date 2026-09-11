# Implementierungsplan: MVP 5 – Formelsammlung, Tools & Barrierefreiheit

## 1. Zielsetzung & Umfang (Scope)

In **MVP 5** wird die Lern-App um essenzielle Prüfungswerkzeuge erweitert und für alle Nutzergruppen uneingeschränkt zugänglich gemacht (**Barrierefreiheit & Inklusion**). Angehende Funkamateure erhalten eine integrierte Formelsammlung mit elektrotechnischen Grundformeln, ein interaktives Quiz für das internationale ITU-Buchstabieralphabet, ein Nachschlagewerk für Q-Gruppen und Landeskenner sowie einen spezialisierten Formel-Rechner. Gleichzeitig wird die App vollständig für Screenreader (TalkBack / VoiceOver) zertifiziert und mit flexibler Schriftgrößenanpassung sowie Dark Mode ausgestattet.

### Erfüllte Anforderungen aus der SRS:
- **F6.1**: Formelsammlung (Elektrotechnik, Wellenlänge, Leistung, Dämpfung, EMV-Sicherheitsabstände).
- **F6.2**: Abkürzungsverzeichnis (Q-Schlüssel wie QTH, QRM, QSO; CW-Kürzel; Landeskenner/Präfixe wie DL, OE, HB9).
- **F6.3**: Interaktives ITU-Buchstabieralphabet-Quiz (z. B. Buchstabe -> Codewort oder Rufzeichen buchstabieren).
- **F6.4**: Gesetzliche Vorschriften (AFuG, AFuV) und Bandpläne (160m - 70cm Klasse E).
- **F6.5**: Integrierter Taschenrechner (schneller Zugriff auf typische Berechnungen wie $R = U / I$, $f = 1 / T$, dB-Umrechnung).
- **F8.1 & F8.2**: Barrierefreiheit: VoiceOver/TalkBack-Unterstützung, Bildbeschreibungen, barrierefreie Touch-Ziele (min. 48x48 dp), Screenreader-Labels.
- **F8.3 & F4.4**: Barrierefreie Einstellungen: Dynamische Schriftgrößen (Dynamic Type / Font Scaling) und Dunkelmodus (Dark Mode).

---

## 2. Detaillierte Tool-Spezifikationen

### 2.1 ITU-Buchstabieralphabet & Q-Gruppen
- **ITU-Alphabet**: Interaktives Multiple-Choice- oder Eingabe-Quiz:
  - Modus 1: Einzelbuchstabe gegeben (z. B. „K“) -> Nutzer wählt „Kilo“ (aus 4 Optionen: Kilo, King, Kelvin, Kentucky).
  - Modus 2: Rufzeichen gegeben (z. B. „DL1ABC“) -> Nutzer muss das vollständige Buchstabier-Muster zuordnen („Delta Lima One Alpha Bravo Charlie“).
- **Q-Gruppen-Lexikon**:
  - Filterbare Liste der wichtigsten Q-Codes (QRA, QRG, QRM, QRN, QRO, QRP, QRT, QRZ, QSL, QSO, QTH, etc.).
  - Umschalter: „Als Frage (Ist Ihr Standort...)“ vs. „Als Antwort (Mein Standort ist...)“.

### 2.2 Formelsammlung & Berechnungs-Helfer
- Formeln kategorisiert in:
  1. *Gleichstromtechnik*: Ohmsches Gesetz ($U = R \cdot I$), Elektrische Leistung ($P = U \cdot I = I^2 \cdot R = U^2 / R$).
  2. *Wechselstrom & HF*: Wellenlänge/Frequenz ($\lambda = c / f$), Periodendauer ($T = 1 / f$), Resonanzfrequenz ($f_0 = 1 / (2\pi \sqrt{LC})$).
  3. *Pegel & Dämpfung*: Spannungs- und Leistungspegel in Dezibel ($dB = 10 \log(P_2/P_1)$ bzw. $20 \log(U_2/U_1)$).
  4. *Antennen & EMV*: Dipollänge ($\lambda / 2 \cdot 0{,}95$), EIRP-Berechnung.
- Integrierter Rechner: Schnelle Eingabemaske für $U$, $R$, $I$, $P$ mit automatischer Einheitenskalierung (mV, V, kV, mA, A, kA).

---

## 3. Barrierefreiheit (Accessibility Guidelines)

1. **Screenreader-Semantik (TalkBack)**:
   - Alle Fragenbilder erhalten eine präzise `contentDescription`, die die Schaltung oder das Diagramm so beschreibt, dass blinde Nutzer die Frage eigenständig lösen können (z. B. *„Schaltskizze: Eine Spannungsquelle von 12 V speist eine Reihenschaltung aus zwei Widerständen R1 = 100 Ohm und R2 = 200 Ohm.“*).
   - Antwortoptionen werden semantisch als Auswahllisten gruppiert (`Modifier.selectableGroup()`).
2. **Visuelle Zugänglichkeit**:
   - Mindestkontrast nach WCAG 2.1 AA (4.5:1 für Fließtext, 3:1 für Überschriften und Icons).
   - Unterstützung des systemweiten Dunkelmodus (`isSystemInDarkTheme()`) sowie manueller Umschalter in den Einstellungen.
   - Textskalierung ohne Layout-Bruch (`TextOverflow`, adaptive Scrollcontainer).

---

## 4. Detaillierte Arbeitspakete (Work Packages)

### WP 5.1: Formelsammlung & Nachschlagewerk
- **Tasks**:
  1. Anlegen von `formulas_klasse_e.json` und `abbreviations.json`.
  2. Erstellung des `ReferenceHubScreen`:
     - Reiter für *Formeln*, *Q-Gruppen*, *Landeskenner*, *Bandplan*.
     - Schnellsuchfeld zur schnellen Begriffssuche während des Lernens.

### WP 5.2: Buchstabier-Quiz (Phonetic Alphabet Trainer)
- **Tasks**:
  1. Implementierung des `PhoneticQuizViewModel`:
     - Rundenbasiertes Quiz (10 oder 20 Fragen).
     - Generierung von Falschantworten aus ähnlichen Wörtern.
  2. `PhoneticQuizScreen` mit audio-visueller Untermalung (optionale Text-to-Speech Aussprache der Codes).

### WP 5.3: Integrierter Rechner
- **Tasks**:
  1. Erstellung des `ExamCalculatorSheet` (aufrufbar per schwebendem Icon oder BottomBar):
     - Standard-Rechenfunktionen (+, -, *, /, Wurzel, log10, Potenz).
     - Formel-Presets: Auswahl einer Formel (z. B. $U = R \cdot I$) belegt Eingabefelder vor und berechnet die fehlende Größe automatisch.

### WP 5.4: Barrierefreiheits-Härtung (TalkBack & Theming)
- **Tasks**:
  1. Audit aller Screens mit dem **Accessibility Scanner** von Android.
  2. Einpflegen detaillierter Alt-Texte für alle technischen Diagramme und Schaltbilder in `questions_klasse_e.json`.
  3. Ergänzung von `Modifier.semantics`:
     - Klare Ansage des aktuellen Zustands (z. B. *„Antwort B ausgewählt. Korrekte Antwort. Begründung verfügbar.“*).
  4. Einstellungen-Screen für Dark Mode (Hell / Dunkel / System) und Schriftgrößenskalierung.

---

## 5. UI/UX Spezifikation

```
+--------------------------------------------------------+
| Nachschlagen & Tools                     [ ⚙ Einstellungen ]
+--------------------------------------------------------+
| [ FORMELN ] [ Q-GRUPPEN ] [ LANDESKENNER ] [ ITU-QUIZ ] |
|                                                        |
| ITU-Buchstabier-Quiz:                                  |
| Buchstabe:                                             |
|                                                        |
|                         [ Q ]                          |
|                                                        |
| Wie lautet das offizielle ITU-Codewort?                |
|                                                        |
| [  Quasar  ]              [  Quebec  ] [ V ]           |
|                                                        |
| [  Queen   ]              [  Quick   ]                 |
|                                                        |
| Aktuelle Runde: 7/10      Richtig: 6                   |
+--------------------------------------------------------+
| [ 🖩 Rechner öffnen ]          [ Nächstes Wort -> ]     |
+--------------------------------------------------------+
```

---

## 6. Test- und Verifikationsplan

### 6.1 Automatisierte Tests
| Test | Beschreibung |
|---|---|
| `FormulaCalculationTest` | Validiert die Berechnungslogiken des Rechners auf mathematische Genauigkeit und Rundungsregeln. |
| `PhoneticQuizGeneratorTest` | Stellt sicher, dass alle 26 Buchstaben des ITU-Alphabets abgedeckt sind und Distraktoren plausibel generiert werden. |
| `AccessibilitySemanticsTest` | Compose UI Test: Prüft, ob alle interaktiven Buttons und Radios über aussagekräftige `contentDescription`-Attribute verfügen. |

### 6.2 Manueller Test mit TalkBack
1. Android-TalkBack-Dienst auf Testgerät aktivieren.
2. App ausschließlich mit Wischgesten und Doppel-Taps blind bedienen:
   - Frage auswählen und vorlesen lassen.
   - Bildbeschreibung anhören.
   - Antwort auswählen und Feedback-Ansage verifizieren.
3. Systemschriftgröße auf Maximum (200 %) stellen: Prüfung auf Überlappungen oder abgeschnittenen Text.

---

## 7. Definition of Done (DoD)

- [ ] Vollständige Formelsammlung und Q-Gruppen-Katalog sind fehlerfrei integriert.
- [ ] ITU-Buchstabier-Quiz ist voll spielbar mit Auswertung.
- [ ] Taschenrechner löst gängige Prüfungsformeln zuverlässig.
- [ ] 100 % der Fragen und Diagramme besitzen adäquate Screenreader-Beschreibungen.
- [ ] Dunkelmodus und Schriftgrößenänderung funktionieren nahtlos ohne Artefakte.
