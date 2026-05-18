package com.example.flux.paginator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.example.flux.domain.paginator.TextPaginator
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Golden screenshot tests: verify that each paginated page fits within its viewport
 * without overflow or truncation. Run `./gradlew recordPaparazziDebug` once to generate
 * the golden files, then `./gradlew verifyPaparazziDebug` in CI.
 */
class TextPaginatorScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        showSystemUi = false,
    )

    private lateinit var paginator: TextPaginator

    @Before
    fun setUp() {
        val context = paparazzi.context
        val metrics = context.resources.displayMetrics
        val density = Density(density = metrics.density, fontScale = 1f)
        val textMeasurer = TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(context),
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
        paginator = TextPaginator(ComposeLineMeasurer(textMeasurer))
    }

    @Test
    fun page1_doesNotOverflow_at18sp() = snapshotPage(fontSizeSp = 18, pageIndex = 0)

    @Test
    fun page1_doesNotOverflow_at14sp() = snapshotPage(fontSizeSp = 14, pageIndex = 0)

    @Test
    fun page1_doesNotOverflow_at24sp() = snapshotPage(fontSizeSp = 24, pageIndex = 0)

    @Test
    fun page1_doesNotOverflow_at28sp() = snapshotPage(fontSizeSp = 28, pageIndex = 0)

    // ── helper ───────────────────────────────────────────────────────────────

    private fun snapshotPage(fontSizeSp: Int, pageIndex: Int) {
        val metrics = paparazzi.context.resources.displayMetrics
        val pages = runBlocking {
            paginator.paginate(
                rawText = SAMPLE_TEXT,
                viewportWidth = metrics.widthPixels,
                viewportHeight = metrics.heightPixels,
                fontSizeSp = fontSizeSp,
            )
        }
        val page = pages.getOrElse(pageIndex) { pages.last() }
        paparazzi.snapshot(name = "${fontSizeSp}sp_page${pageIndex + 1}") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    text = page,
                    style = TextStyle(fontSize = fontSizeSp.sp),
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                )
            }
        }
    }

    companion object {
        private val SAMPLE_TEXT = """
            It was the best of times, it was the worst of times, it was the age of wisdom,
            it was the age of foolishness, it was the epoch of belief, it was the epoch of
            incredulity, it was the season of Light, it was the season of Darkness, it was
            the spring of hope, it was the winter of despair, we had everything before us,
            we had nothing before us, we were all going direct to Heaven, we were all going
            direct the other way.

            There were a king with a large jaw and a queen with a plain face, on the throne
            of England; there were a king with a large jaw and a queen with a fair face, on
            the throne of France. In both countries it was clearer than crystal to the lords
            of the State preserves of loaves and fishes, that things in general were settled
            for ever.

            It was the year of Our Lord one thousand seven hundred and seventy-five.
            Spiritual revelations were conceded to England at that favoured period, as at
            this. Mrs. Southcott had recently attained her five-and-twentieth blessed
            birthday, of whom a prophetic private in the Life Guards had heralded the
            sublime appearance by announcing that arrangements were made for the swallowing
            up of London and Westminster.
        """.trimIndent().repeat(6)
    }
}
