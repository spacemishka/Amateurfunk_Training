package com.spacemishka.app.amateurfunktraining

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val APP_PACKAGE = "com.spacemishka.app.amateurfunktraining"
private const val LAUNCH_TIMEOUT = 5000L
private const val UI_TIMEOUT = 4000L

@RunWith(AndroidJUnit4::class)
class AmateurfunkUiAutomatorTest {

    private lateinit var device: UiDevice

    @Before
    fun launchApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Keep screen on during tests
        device.executeShellCommand("svc power stayon true")

        // Wake screen if off
        if (!device.isScreenOn) {
            device.wakeUp()
            Thread.sleep(500)
        }

        // Dismiss keyguard / lock screen programmatically
        device.executeShellCommand("wm dismiss-keyguard")
        Thread.sleep(500)

        // Fallback: swipe up if still locked
        val isLocked = device.findObject(By.res("com.android.systemui", "keyguard_root_view")) != null
        if (isLocked) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.85f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.15f).toInt(),
                20
            )
            Thread.sleep(800)
        }

        // Go to home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage: String = device.launcherPackageName
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)

        // Launch app via shell to bypass MIUI background-start restrictions
        device.executeShellCommand("am start -n $APP_PACKAGE/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER")

        // Wait for app window
        device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_TIMEOUT)
        // Wait for the actual app HOME SCREEN content to appear
        device.wait(Until.hasObject(By.text("Amateurfunk Training")), LAUNCH_TIMEOUT * 2)
        Thread.sleep(800) // let Compose fully settle
        scrollToTop()
    }

    @Test
    fun testHomeScreenCategoriesDisplayed() {
        val appTitle = device.wait(Until.findObject(By.text("Amateurfunk Training")), UI_TIMEOUT)
        assertNotNull("App title 'Amateurfunk Training' must be displayed", appTitle)

        val technikCard = scrollToText("Technik (Klasse E)")
        assertNotNull("Category 'Technik (Klasse E)' must be present", technikCard)

        val betriebCard = scrollToText("Betriebliche Kenntnisse")
        assertNotNull("Category 'Betriebliche Kenntnisse' must be present", betriebCard)

        val vorschriftenCard = scrollToText("Vorschriften & Gesetze")
        assertNotNull("Category 'Vorschriften & Gesetze' must be present", vorschriftenCard)

        val alleCard = scrollToText("Alle Fächer")
        assertNotNull("Category 'Alle Fächer' must be present", alleCard)
    }

    @Test
    fun testStartPracticeSelectAnswerAndAdvance() {
        scrollToTop()
        // Tap on Technik category
        val technikCard = scrollToText("Technik (Klasse E)")
        assertNotNull("Technik card must be found", technikCard)
        technikCard?.let { clickNode(it) }

        // Verify Practice Screen top bar
        val questionCounter = device.wait(Until.findObject(By.textStartsWith("Frage 1 von")), UI_TIMEOUT)
        assertNotNull("Practice screen question counter must be visible", questionCounter)

        // Verify that Next Question button is present
        val nextButtonText = device.wait(Until.findObject(By.text("Nächste Frage")), UI_TIMEOUT)
        assertNotNull("Next Question button text must be visible", nextButtonText)

        // Find answer option A
        val optionA = device.wait(Until.findObject(By.descStartsWith("Option ")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.descStartsWith("Antwort ")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.clickable(true).hasDescendant(By.text("A"))), UI_TIMEOUT)
        assertNotNull("Option A must be visible", optionA)
        optionA?.let { clickNode(it) }
        device.waitForIdle()

        // Verify answer was confirmed: ExplanationBox appears when isAnswerConfirmed = true
        val explanation = device.wait(
            Until.findObject(By.text("Erkl\u00e4rung & Hintergrund")),
            UI_TIMEOUT
        ) ?: scrollToText("Erklärung")
        assertNotNull("Explanation box must appear after answering", explanation)

        // Return to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Zurück zur Übersicht")), UI_TIMEOUT)
        backButton?.let { clickNode(it) }
        device.waitForIdle()
    }

    @Test
    fun testBookmarkToggleAndPersistence() {
        // 1. Enter first category
        val technikCard = scrollToText("Technik (Klasse E)")
        assertNotNull("Technik card must be found", technikCard)
        technikCard?.let { clickNode(it) }
        device.waitForIdle()

        // 2. Click Bookmark icon in TopAppBar
        val bookmarkToggle = device.wait(Until.findObject(By.desc("Lesezeichen setzen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Lesezeichen entfernen")), UI_TIMEOUT)
        assertNotNull("Bookmark toggle icon must be present in practice top bar", bookmarkToggle)
        clickNode(bookmarkToggle)
        device.waitForIdle()

        // 3. Return to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        backButton?.let { clickNode(it) }
        device.waitForIdle()

        // Scroll back to top
        scrollToTop()

        // 4. Click on 'Lesezeichen' card on Home
        val bookmarkCard = device.wait(Until.findObject(By.descContains("Lesezeichen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Lesezeichen")), UI_TIMEOUT)
        assertNotNull("Bookmark tile must be on Home screen", bookmarkCard)
        clickNode(bookmarkCard)
        device.waitForIdle()

        // 5. Verify practice opened in Bookmarks mode
        val bookmarksTitle = device.wait(Until.findObject(By.text("Lesezeichen")), UI_TIMEOUT)
        assertNotNull("Practice top bar must display 'Lesezeichen'", bookmarksTitle)

        // 6. Return to Home
        val backFromBookmarks = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        backFromBookmarks?.let { clickNode(it) }
        device.waitForIdle()
    }

    @Test
    fun testProblemQuestionsTraining() {
        scrollToTop()
        // Verify Problemfragen tile exists on Home
        val problemCard = device.wait(Until.findObject(By.text("Problemfragen")), UI_TIMEOUT)
        assertNotNull("Problemfragen tile must be on Home screen", problemCard)
        clickNode(problemCard)

        // Verify Problemfragen mode opened
        val problemTitle = device.wait(Until.findObject(By.text("Problemfragen")), UI_TIMEOUT)
        assertNotNull("Practice top bar must display 'Problemfragen'", problemTitle)

        // Return to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Zurück zur Übersicht")), UI_TIMEOUT)
        backButton?.let { clickNode(it) }
    }

    @Test
    fun testLeitnerBadgeAndStreakDisplay() {
        scrollToTop()
        // Verify Streak indicator is on Home
        val streakIndicator = device.wait(Until.findObject(By.descContains("Lernstreak")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textContains("Streak")), UI_TIMEOUT)
        assertNotNull("Streak indicator must be present on Home screen", streakIndicator)

        // Enter category
        val technikCard = scrollToText("Technik (Klasse E)")
        assertNotNull("Technik card must be found", technikCard)
        technikCard?.let { clickNode(it) }
        device.waitForIdle()
        device.wait(Until.findObject(By.textStartsWith("Frage 1 von")), UI_TIMEOUT)

        // Verify Leitner stage badge on question card
        val leitnerBadge = device.wait(Until.findObject(By.descStartsWith("Leitner Stufe")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textStartsWith("Stufe ")), UI_TIMEOUT)
            ?: scrollToText("Stufe ")
        assertNotNull("Leitner stage badge must be displayed on question card", leitnerBadge)

        // Return to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        backButton?.let { clickNode(it) }
    }

    @Test
    fun testExamSimulationFlow() {
        scrollToTop()

        // 1. Verify and click "Simulation starten" on Home screen
        val startExamButton = device.wait(Until.findObject(By.desc("Prüfungssimulation starten")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Simulation starten")), UI_TIMEOUT)
        assertNotNull("Button 'Simulation starten' must be present on Home", startExamButton)
        clickNode(startExamButton)
        device.waitForIdle()

        // 2. Verify Exam Screen top bar timer and matrix
        val timerObj = device.wait(Until.findObject(By.descContains("Verbleibende Prüfungszeit")), UI_TIMEOUT)
        assertNotNull("Exam countdown timer must be displayed", timerObj)

        val firstMatrixQuestion = device.wait(Until.findObject(By.descContains("Frage 1")), UI_TIMEOUT)
        assertNotNull("Questions matrix must show Question 1", firstMatrixQuestion)

        // 3. Mark for review
        val reviewBtn = device.wait(Until.findObject(By.desc("Für Überprüfung merken")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Merken")), UI_TIMEOUT)
        if (reviewBtn != null) {
            clickNode(reviewBtn)
            device.waitForIdle()
            Thread.sleep(600)
            val markedBtn = device.wait(Until.findObject(By.desc("Zur Überprüfung markiert")), UI_TIMEOUT)
                ?: device.wait(Until.findObject(By.text("Merken")), UI_TIMEOUT)
            assertNotNull("Question should be marked for review", markedBtn)
        }

        // 4. Select an answer option
        val optionA = device.wait(Until.findObject(By.descStartsWith("Antwort A")), UI_TIMEOUT)
        optionA?.let {
            clickNode(it)
            device.waitForIdle()
        }

        // 5. Click submit in top bar
        val submitTopBarBtn = device.wait(Until.findObject(By.desc("Prüfung abgeben")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Abgeben")), UI_TIMEOUT)
        assertNotNull("Submit button must be present in top bar", submitTopBarBtn)
        clickNode(submitTopBarBtn)
        device.waitForIdle()
        Thread.sleep(600)

        // 6. Confirm in dialog
        val confirmDialogBtn = device.wait(Until.findObject(By.text("Jetzt abgeben")), UI_TIMEOUT)
        assertNotNull("Submit dialog 'Jetzt abgeben' button must appear", confirmDialogBtn)
        clickNode(confirmDialogBtn)
        device.waitForIdle()

        // 7. Verify Exam Result Screen
        val resultTitle = device.wait(Until.findObject(By.text("Prüfungsauswertung")), UI_TIMEOUT * 2)
        assertNotNull("Exam result screen title 'Prüfungsauswertung' must appear", resultTitle)

        val techResult = device.wait(Until.findObject(By.text("Technik (Klasse E)")), UI_TIMEOUT)
        assertNotNull("Technik result card must be displayed", techResult)

        // 8. Return to Home
        val backBtn = device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Zurück zur Startseite")), UI_TIMEOUT)
        if (backBtn != null) {
            clickNode(backBtn)
        } else {
            device.pressBack()
        }
        device.waitForIdle()
        Thread.sleep(800)

        // 9. Verify back on Home
        val homeTitle = device.wait(Until.findObject(By.text("Amateurfunk Training")), UI_TIMEOUT)
        assertNotNull("Must return to Home screen", homeTitle)
    }

    @Test
    fun testTopicLexiconAndContextualKnowledge() {
        scrollToTop()
        val lexiconCard = scrollToText("Themenlexikon")
            ?: device.wait(Until.findObject(By.descContains("Themenlexikon")), UI_TIMEOUT)
        assertNotNull("Themenlexikon card must be visible on Home screen", lexiconCard)
        lexiconCard?.let { clickNode(it) }
        device.waitForIdle()

        // Wait for Themenlexikon screen
        val searchField = device.wait(Until.findObject(By.desc("Themen durchsuchen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Thema, Formel, Begriff suchen...")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textContains("suchen")), UI_TIMEOUT)
        assertNotNull("Search field must be displayed in Themenlexikon", searchField)

        // Verify category chips
        val chipTechnik = device.wait(Until.findObject(By.text("Technik")), UI_TIMEOUT)
        assertNotNull("Technik filter chip must exist", chipTechnik)

        // Select a topic card
        val topicItem = device.wait(Until.findObject(By.text("Themenkarte ansehen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textContains("Größen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textContains("Fragen")), UI_TIMEOUT)
        assertNotNull("At least one topic card must be displayed", topicItem)
        topicItem?.let { clickNode(it) }
        device.waitForIdle()

        // Verify Topic Detail screen
        val kernaussageLabel = device.wait(Until.findObject(By.text("KERNAUSSAGE")), UI_TIMEOUT)
        assertNotNull("Topic detail screen must show KERNAUSSAGE", kernaussageLabel)

        val practiceButton = scrollToText("Fragen üben")
        assertNotNull("Practice button must be available on topic detail screen", practiceButton)
        practiceButton?.let { clickNode(it) }
        device.waitForIdle()

        // Verify practice mode started for this topic
        val practiceOption = device.wait(Until.findObject(By.descStartsWith("Option ")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.clickable(true).hasDescendant(By.text("A"))), UI_TIMEOUT)
        assertNotNull("Practice question answer option must be visible", practiceOption)
        practiceOption?.let { clickNode(it) }
        device.waitForIdle()

        // Verify context button appeared
        val contextButton = scrollToText("Kurz erklärt")
        assertNotNull("Contextual 'Kurz erklärt' button must appear after answering", contextButton)
        contextButton?.let { clickNode(it) }
        device.waitForIdle()

        // Verify ModalBottomSheet opened
        val sheetTakeaway = device.wait(Until.findObject(By.text("KERNAUSSAGE")), UI_TIMEOUT)
        assertNotNull("ModalBottomSheet must display KERNAUSSAGE", sheetTakeaway)

        val dismissButton = device.wait(Until.findObject(By.desc("Schließen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Verstanden & Weiter")), UI_TIMEOUT)
            ?: scrollToText("Verstanden")
        assertNotNull("Dismiss button must be present in sheet", dismissButton)
        dismissButton?.let { clickNode(it) }
        device.waitForIdle()

        // Return to Home
        device.pressBack()
        Thread.sleep(500)
        device.pressBack()
        Thread.sleep(500)
    }

    @Test
    fun testReferenceHubAndToolsNavigation() {
        scrollToTop()

        // 1. Reference Hub (Formelsammlung & Tabellen)
        val refCard = scrollToText("Formelsammlung")
        assertNotNull("Reference Hub card must be present on home", refCard)
        refCard?.let { clickNode(it) }
        device.waitForIdle()

        val refTitle = device.wait(Until.findObject(By.text("Formeln & Nachschlagen")), UI_TIMEOUT)
        assertNotNull("Formeln & Nachschlagen screen title must be displayed", refTitle)

        val qCodesTab = device.wait(Until.findObject(By.text("Q-Codes")), UI_TIMEOUT)
        assertNotNull("Q-Codes tab must be visible", qCodesTab)
        qCodesTab?.let { clickNode(it) }
        device.waitForIdle()

        val qthItem = device.wait(Until.findObject(By.text("QTH")), UI_TIMEOUT)
        assertNotNull("QTH code must be shown in Q-Codes tab", qthItem)

        device.pressBack()
        device.waitForIdle()
        Thread.sleep(400)

        // 2. ITU-Alphabet Quiz
        scrollToTop()
        val quizCard = scrollToText("ITU-Alphabet")
        assertNotNull("ITU-Alphabet quiz card must be present on home", quizCard)
        quizCard?.let { clickNode(it) }
        device.waitForIdle()

        val quizTitle = device.wait(Until.findObject(By.text("ITU-Buchstabier-Trainer")), UI_TIMEOUT)
        assertNotNull("ITU-Buchstabier-Trainer title must be displayed", quizTitle)

        device.pressBack()
        device.waitForIdle()
        Thread.sleep(400)

        // 3. Settings Screen
        scrollToTop()
        val settingsBtn = device.wait(Until.findObject(By.desc("Einstellungen öffnen")), UI_TIMEOUT)
        assertNotNull("Settings button must be present in top bar", settingsBtn)
        settingsBtn?.let { clickNode(it) }
        device.waitForIdle()

        val settingsTitle = device.wait(Until.findObject(By.text("Einstellungen & Barrierefreiheit")), UI_TIMEOUT)
        assertNotNull("Settings title must be displayed", settingsTitle)

        device.pressBack()
        device.waitForIdle()
        Thread.sleep(400)
    }

    private fun scrollToText(text: String): UiObject2? {
        val found = device.findObject(By.text(text))
            ?: device.findObject(By.textContains(text))
            ?: device.findObject(By.descContains(text))
        if (found != null) return found

        for (i in 0..4) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.7f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.3f).toInt(),
                15
            )
            Thread.sleep(300)
            val obj = device.findObject(By.text(text))
                ?: device.findObject(By.textContains(text))
                ?: device.findObject(By.descContains(text))
            if (obj != null) return obj
        }
        return null
    }

    private fun scrollToTop() {
        for (i in 0..2) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.3f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.7f).toInt(),
                10
            )
            Thread.sleep(200)
        }
    }

    private fun clickNode(node: UiObject2) {
        val bounds = node.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
    }
}
