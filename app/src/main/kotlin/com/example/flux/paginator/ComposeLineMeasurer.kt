package com.example.flux.paginator

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.flux.domain.paginator.LineInfo
import com.example.flux.domain.paginator.LineMeasurer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [LineMeasurer] backed by Compose's [TextMeasurer].
 * [TextMeasurer] is reused across calls (expensive to construct) and is thread-safe.
 */
@Singleton
class ComposeLineMeasurer @Inject constructor(
    private val textMeasurer: TextMeasurer,
) : LineMeasurer {

    override fun measureLines(text: String, fontSizeSp: Int, maxWidthPx: Int): List<LineInfo> {
        if (text.isEmpty()) return emptyList()
        val result = textMeasurer.measure(
            text = AnnotatedString(text),
            style = TextStyle(fontSize = fontSizeSp.sp),
            constraints = Constraints(maxWidth = maxWidthPx),
            softWrap = true,
        )
        return (0 until result.lineCount).map { i ->
            LineInfo(
                start = result.getLineStart(i),
                end = result.getLineEnd(i),
                topPx = result.getLineTop(i),
                bottomPx = result.getLineBottom(i),
            )
        }
    }
}
