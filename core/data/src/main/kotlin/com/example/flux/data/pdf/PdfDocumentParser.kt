package com.example.flux.data.pdf

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.example.flux.common.di.IoDispatcher
import com.example.flux.data.local.BookDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PdfDocumentParser"

/**
 * Renders PDF pages using the platform-native [PdfRenderer] API.
 *
 * Pages are cached on disk via [PdfPageCache] (50 MB cap). Renders at 2× display
 * density for crisp high-DPI output. All [PdfRenderer] and [PdfRenderer.Page]
 * instances are closed in `finally` blocks to prevent file-descriptor leaks.
 */
@Singleton
class PdfDocumentParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val bookDao: BookDao,
    private val cache: PdfPageCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scaleFactor: Float
        get() = context.resources.displayMetrics.density * 2.0f

    /**
     * Returns a rendered [Bitmap] for [pageIndex] of the book identified by [bookId].
     * Checks the disk cache first; renders and caches on a miss.
     *
     * @throws IllegalStateException if the book is not found in the database.
     */
    suspend fun renderPage(bookId: String, pageIndex: Int): Bitmap = withContext(ioDispatcher) {
        val scale = scaleFactor
        val cacheKey = "${bookId}_page_${pageIndex}_$scale"

        cache.get(cacheKey)?.let { return@withContext it }

        val book = bookDao.getBookByIdOnce(bookId)
            ?: error("Book '$bookId' not found")

        renderAndCache(book.fileUri, bookId, pageIndex, scale, cacheKey)
    }

    /**
     * Returns the total page count for the book identified by [bookId].
     *
     * @throws IllegalStateException if the book is not found in the database.
     */
    suspend fun getPageCount(bookId: String): Int = withContext(ioDispatcher) {
        val book = bookDao.getBookByIdOnce(bookId)
            ?: error("Book '$bookId' not found")
        openRenderer(book.fileUri).use { it.pageCount }
    }

    private fun renderAndCache(
        fileUri: String,
        bookId: String,
        pageIndex: Int,
        scale: Float,
        cacheKey: String,
    ): Bitmap {
        val start = SystemClock.elapsedRealtime()
        val renderer = openRenderer(fileUri)
        try {
            val page = renderer.openPage(pageIndex)
            try {
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888,
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(cacheKey, bitmap)
                Log.d(TAG, "book=$bookId page=$pageIndex rendered in ${SystemClock.elapsedRealtime() - start}ms")
                return bitmap
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
        }
    }

    private fun openRenderer(fileUri: String): PdfRenderer {
        val pfd = contentResolver.openFileDescriptor(Uri.parse(fileUri), "r")
            ?: error("Cannot open file descriptor for URI: $fileUri")
        return PdfRenderer(pfd)
    }
}
