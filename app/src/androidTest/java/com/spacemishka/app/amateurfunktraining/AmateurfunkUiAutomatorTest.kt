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

        // Start from home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage: String = device.launcherPackageName
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)

        // Launch app
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(APP_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Wait for app to appear
        device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_TIMEOUT)
    }

    @Test
    fun testHomeScreenCategoriesDisplayed() {
        val appTitle = device.wait(Until.findObject(By.text("Amateurfunk Training")), UI_TIMEOUT)
        assertNotNull("App title 'Amateurfunk Training' must be displayed", appTitle)

        val technikCard = device.wait(Until.findObject(By.text("Technik (Klasse E)")), UI_TIMEOUT)
        assertNotNull("Category 'Technik (Klasse E)' must be present", technikCard)

        val betriebCard = device.wait(Until.findObject(By.text("Betriebliche Kenntnisse")), UI_TIMEOUT)
        assertNotNull("Category 'Betriebliche Kenntnisse' must be present", betriebCard)

        // Scroll down to reveal lower items in LazyColumn if necessary
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.75f).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.25f).toInt(),
            10
        )

        val vorschriftenCard = device.wait(Until.findObject(By.text("Vorschriften & Gesetze")), UI_TIMEOUT)
        assertNotNull("Category 'Vorschriften & Gesetze' must be present", vorschriftenCard)

        val alleCard = device.wait(Until.findObject(By.text("Alle Fächer")), UI_TIMEOUT)
        assertNotNull("Category 'Alle Fächer' must be present", alleCard)
    }

    @Test
    fun testStartPracticeSelectAnswerAndAdvance() {
        // Tap on Technik category
        val technikCard = device.wait(Until.findObject(By.text("Technik (Klasse E)")), UI_TIMEOUT)
        assertNotNull("Technik card must be found", technikCard)
        technikCard.click()

        // Verify Practice Screen top bar
        val questionCounter = device.wait(Until.findObject(By.textStartsWith("Frage 1 von")), UI_TIMEOUT)
        assertNotNull("Practice screen question counter must be visible", questionCounter)

        // Verify that Next Question button is present
        val nextButtonText = device.wait(Until.findObject(By.text("Nächste Frage")), UI_TIMEOUT)
        assertNotNull("Next Question button text must be visible", nextButtonText)

        // Find answer option A
        val optionA = device.wait(Until.findObject(By.desc("Option A")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.clickable(true).hasDescendant(By.text("A"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("A")), UI_TIMEOUT)
        assertNotNull("Option A must be visible", optionA)
        clickNode(optionA)

        // Verify visual feedback icon (Richtig or Falsch) appears
        val feedbackIcon = device.wait(Until.findObject(By.desc("Richtig")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Falsch")), UI_TIMEOUT)
        assertNotNull("Visual feedback icon (Richtig or Falsch) must appear after answering", feedbackIcon)

        // Click next question
        val nextButtonContainer = device.wait(Until.findObject(By.desc("Nächste Frage")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.clickable(true).hasDescendant(By.text("Nächste Frage"))), UI_TIMEOUT)
            ?: nextButtonText
        clickNode(nextButtonContainer)

        // Verify index advances to Question 2
        val question2Counter = device.wait(Until.findObject(By.textStartsWith("Frage 2 von")), UI_TIMEOUT)
        assertNotNull("Question counter must advance to 'Frage 2 von'", question2Counter)

        // Navigate back using the back button
        val backButton = device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        assertNotNull("Back button must be visible", backButton)
        clickNode(backButton)

        // Verify we are back on Home
        val homeAppTitle = device.wait(Until.findObject(By.text("Amateurfunk Training")), UI_TIMEOUT)
        assertNotNull("Must return to Home screen", homeAppTitle)
    }

    @Test
    fun testBookmarkToggleAndPersistence() {
        // 1. Enter category practice
        val technikCard = device.wait(Until.findObject(By.text("Technik (Klasse E)")), UI_TIMEOUT)
        assertNotNull("Technik card must be found", technikCard)
        clickNode(technikCard)

        // Ensure practice screen has loaded and navigation transition settled
        val questionCounter = device.wait(Until.findObject(By.textStartsWith("Frage ")), UI_TIMEOUT)
        assertNotNull("Practice screen question counter must be visible", questionCounter)
        device.waitForIdle()

        // 2. Locate bookmark button in top bar
        val bookmarkButton = device.wait(Until.findObject(By.desc("Lesezeichen setzen")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Lesezeichen entfernen")), UI_TIMEOUT)
        assertNotNull("Bookmark button must be present in top bar", bookmarkButton)

        val initialDesc = bookmarkButton.contentDescription

        // Click to toggle bookmark
        clickNode(bookmarkButton)
        device.waitForIdle()

        // Verify description has changed
        val expectedToggledDesc = if (initialDesc == "Lesezeichen setzen") "Lesezeichen entfernen" else "Lesezeichen setzen"
        val toggledNode = device.wait(Until.findObject(By.desc(expectedToggledDesc)), UI_TIMEOUT)
        assertNotNull("Bookmark icon must toggle to '$expectedToggledDesc'", toggledNode)

        // If it is now "Lesezeichen setzen", toggle once more so that at least 1 question is bookmarked
        if (expectedToggledDesc == "Lesezeichen setzen") {
            clickNode(toggledNode)
            device.wait(Until.findObject(By.desc("Lesezeichen entfernen")), UI_TIMEOUT)
        }

        // 3. Navigate back to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        assertNotNull("Back button must be found", backButton)
        clickNode(backButton)

        // 4. Click on 'Lesezeichen' card on Home
        val bookmarkCard = device.wait(Until.findObject(By.text("Lesezeichen")), UI_TIMEOUT)
        assertNotNull("Bookmark tile must be on Home screen", bookmarkCard)
        clickNode(bookmarkCard)

        // 5. Verify practice opened in Bookmarks mode
        val bookmarksTitle = device.wait(Until.findObject(By.text("Lesezeichen")), UI_TIMEOUT)
        assertNotNull("Practice top bar must display 'Lesezeichen'", bookmarksTitle)

        // 6. Return to Home
        val backFromBookmarks = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        backFromBookmarks?.let { clickNode(it) }
    }

    @Test
    fun testProblemQuestionsTraining() {
        // Verify Problemfragen tile exists on Home
        val problemCard = device.wait(Until.findObject(By.text("Problemfragen")), UI_TIMEOUT)
        assertNotNull("Problemfragen tile must be on Home screen", problemCard)
        problemCard.click()

        // Verify Problemfragen mode opened
        val problemTitle = device.wait(Until.findObject(By.text("Problemfragen")), UI_TIMEOUT)
        assertNotNull("Practice top bar must display 'Problemfragen'", problemTitle)

        // Return to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.text("Zurück zur Übersicht")), UI_TIMEOUT)
        backButton?.click()
    }

    @Test
    fun testLeitnerBadgeAndStreakDisplay() {
        // Verify Streak indicator is on Home
        val streakIndicator = device.wait(Until.findObject(By.descContains("Lernstreak")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textContains("Streak")), UI_TIMEOUT)
        assertNotNull("Streak indicator must be present on Home screen", streakIndicator)

        // Enter category
        val technikCard = device.wait(Until.findObject(By.text("Technik (Klasse E)")), UI_TIMEOUT)
        technikCard?.click()

        // Verify Leitner stage badge on question card
        val leitnerBadge = device.wait(Until.findObject(By.descStartsWith("Leitner Stufe")), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.textStartsWith("Stufe ")), UI_TIMEOUT)
        assertNotNull("Leitner stage badge must be displayed on question card", leitnerBadge)

        // Return to Home
        val backButton = device.wait(Until.findObject(By.clickable(true).hasDescendant(By.desc("Zurück zur Übersicht"))), UI_TIMEOUT)
            ?: device.wait(Until.findObject(By.desc("Zurück zur Übersicht")), UI_TIMEOUT)
        backButton?.let { clickNode(it) }
    }

    private fun clickNode(node: UiObject2) {
        val bounds = node.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
    }
}

