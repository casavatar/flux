package com.example.flux.feature.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.feature.reader.model.ReaderPage
import com.example.flux.feature.reader.model.ReaderUiState
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        showSystemUi = false,
    )

    // ── snapshots ─────────────────────────────────────────────────────────────

    @Test
    fun reader_loading() {
        paparazzi.snapshot {
            ReaderTheme {
                ReaderContent(
                    uiState = ReaderUiState.Loading,
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    @Test
    fun reader_reading_light() {
        paparazzi.snapshot {
            ReaderTheme {
                ReaderContent(
                    uiState = successState(currentPage = 0, totalPages = 12, fontSizeSp = 18),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    @Test
    fun reader_reading_dark() {
        paparazzi.snapshot {
            ReaderTheme(darkTheme = true) {
                ReaderContent(
                    uiState = successState(currentPage = 0, totalPages = 12, fontSizeSp = 18),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    @Test
    fun reader_reading_font_small() {
        paparazzi.snapshot {
            ReaderTheme {
                ReaderContent(
                    uiState = successState(currentPage = 0, totalPages = 20, fontSizeSp = 12),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    @Test
    fun reader_reading_font_large() {
        paparazzi.snapshot {
            ReaderTheme {
                ReaderContent(
                    uiState = successState(currentPage = 0, totalPages = 6, fontSizeSp = 28),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    @Test
    fun reader_error() {
        paparazzi.snapshot {
            ReaderTheme {
                ReaderContent(
                    uiState = ReaderUiState.Error(
                        message = "The file may have been moved or deleted.",
                        canDelete = true,
                    ),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    @Test
    fun reader_last_page() {
        paparazzi.snapshot {
            ReaderTheme {
                ReaderContent(
                    uiState = successState(
                        currentPage = 11,
                        totalPages = 12,
                        fontSizeSp = 18,
                        controlsVisible = true,
                    ),
                    onNavigateBack = {},
                    onIntent = {},
                )
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun successState(
        currentPage: Int,
        totalPages: Int,
        fontSizeSp: Int,
        controlsVisible: Boolean = false,
    ): ReaderUiState.Success {
        val pages = (0 until totalPages).map { i ->
            ReaderPage(index = i, text = SAMPLE_PAGE_TEXT)
        }
        return ReaderUiState.Success(
            book = FAKE_BOOK,
            pages = pages,
            currentPageIndex = currentPage,
            totalPages = totalPages,
            fontSizeSp = fontSizeSp,
            controlsVisible = controlsVisible,
        )
    }
}

// ── theme ─────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E4057),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF5F6B7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3EBF3),
    onSecondaryContainer = Color(0xFF1B2631),
    tertiary = Color(0xFF7B5E4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC8),
    onTertiaryContainer = Color(0xFF2E1507),
    background = Color(0xFFFAF9F5),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFAF9F5),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE3EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DBDFF),
    onPrimary = Color(0xFF003060),
    primaryContainer = Color(0xFF1B4880),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFFBBC8D8),
    onSecondary = Color(0xFF283644),
    secondaryContainer = Color(0xFF3E505F),
    onSecondaryContainer = Color(0xFFD7E3F3),
    tertiary = Color(0xFFEFB99A),
    onTertiary = Color(0xFF462B16),
    tertiaryContainer = Color(0xFF5E4332),
    onTertiaryContainer = Color(0xFFFFDCC8),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
)

@Composable
private fun ReaderTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}

// ── fixtures ──────────────────────────────────────────────────────────────────

private val FAKE_BOOK = Book(
    id = "snap-1",
    title = "The Great Gatsby",
    author = "F. Scott Fitzgerald",
    coverUri = null,
    fileUri = "file:///gatsby.txt",
    format = BookFormat.TXT,
    addedAt = 0L,
)

private const val SAMPLE_PAGE_TEXT = """In my younger and more vulnerable years my father gave me some advice that I've been turning over in my mind ever since. "Whenever you feel like criticizing anyone," he told me, "just remember that all the people in this world haven't had the advantages that you've had."

He didn't say any more, but we've always been unusually communicative in a reserved way, and I understood that he meant a great deal more than that. In consequence, I'm inclined to reserve all judgments, a habit that has opened up many curious natures to me and also made me the victim of not a few veteran bores.

The abnormal mind is quick to detect and attach itself to this quality when it appears in a normal person, and so it came about that in college I was unjustly accused of being a politician, because I was privy to the secret griefs of wild, unknown men. Most of the confidences were unsought — frequently I have feigned sleep, preoccupation, or a hostile levity when I realized by some unmistakable sign that an intimate revelation was quivering on the horizon."""
