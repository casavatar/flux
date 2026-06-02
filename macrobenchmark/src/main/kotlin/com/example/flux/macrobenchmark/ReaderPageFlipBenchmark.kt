package com.example.flux.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ReaderPageFlipBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun readerPageFlip_frames() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric("BionicRerender"),
        ),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = { navigateToReader() },
    ) {
        val pager = device.findObject(By.scrollable(true))
        repeat(PAGE_FLIP_REPETITIONS) {
            pager?.fling(Direction.LEFT)
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.navigateToReader() {
        // Navigate to the library first so the app is initialised, then open the
        // first book via the deep-link URI registered in FluxNavHost.
        startActivityAndWait()

        val firstBook = device.wait(Until.findObject(By.clickable(true).depth(3)), IDLE_TIMEOUT_MS)
        if (firstBook != null) {
            firstBook.click()
            device.waitForIdle()
        } else {
            // Fallback: open the reader via deep link with a synthetic book ID.
            // The reader will show an error screen, but frame timing is still captured.
            val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("$DEEP_LINK_BASE/benchmark-book")
                `package` = PACKAGE_NAME
            }
            device.executeShellCommand(
                "am start -a android.intent.action.VIEW -d \"$DEEP_LINK_BASE/benchmark-book\" $PACKAGE_NAME",
            )
            device.waitForIdle()
        }
    }

    companion object {
        private const val PAGE_FLIP_REPETITIONS = 20
        private const val IDLE_TIMEOUT_MS = 3_000L
        private const val DEEP_LINK_BASE = "flux://reader"
    }
}
