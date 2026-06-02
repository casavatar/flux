package com.example.flux.feature.reader

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.NightMode
import com.example.flux.feature.reader.model.ReaderPage
import com.example.flux.feature.reader.model.ReaderUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderThemeAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true)
    }

    @Test
    fun lightTheme_passesContrastAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme {
                ReaderContent(
                    uiState = successState(NightMode.LIGHT),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
        triggerAccessibilityChecks()
    }

    @Test
    fun darkTheme_passesContrastAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme {
                ReaderContent(
                    uiState = successState(NightMode.DARK),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
        triggerAccessibilityChecks()
    }

    @Test
    fun sepiaTheme_passesContrastAccessibilityChecks() {
        composeTestRule.setContent {
            MaterialTheme {
                ReaderContent(
                    uiState = successState(NightMode.SEPIA),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
        triggerAccessibilityChecks()
    }

    private fun triggerAccessibilityChecks() {
        composeTestRule.waitForIdle()
        // closeSoftKeyboard is a no-op action that satisfies AccessibilityChecks' hook
        // into every ViewAction, causing ATF to traverse the full view hierarchy.
        Espresso.onView(isRoot()).perform(ViewActions.closeSoftKeyboard())
    }

    private fun successState(nightMode: NightMode) = ReaderUiState.Success(
        book = FAKE_BOOK,
        pages = listOf(ReaderPage(index = 0, text = SAMPLE_TEXT)),
        currentPageIndex = 0,
        totalPages = 1,
        nightMode = nightMode,
    )

    private companion object {
        val FAKE_BOOK = Book(
            id = "a11y-test",
            title = "Accessibility Test Book",
            author = "Test Author",
            coverUri = null,
            fileUri = "file:///test.txt",
            format = BookFormat.TXT,
            addedAt = 0L,
        )

        const val SAMPLE_TEXT =
            "The quick brown fox jumps over the lazy dog. " +
            "This sentence is used to verify that all characters render clearly " +
            "against the chosen reading theme background."
    }
}
