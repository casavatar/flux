package com.example.flux.data.epub

import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.BookParseResult
import com.example.flux.domain.model.Chapter
import com.example.flux.domain.model.ReadingOrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.streamer.Readium
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadiumEpubParser @Inject constructor(
    private val readium: Readium,
) {
    suspend fun parse(bookId: String, fileUri: String): BookParseResult {
        val url = AbsoluteUrl(fileUri)
            ?: return BookParseResult.Error.Unknown(IllegalArgumentException("Invalid URI: $fileUri"))

        val asset = withContext(Dispatchers.IO) {
            readium.assetRetriever.retrieve(url)
        }.getOrElse { error ->
            return BookParseResult.Error.Unknown(Exception(error.toString()))
        }

        val publication = readium.opener
            .open(asset, allowUserInteraction = false)
            .getOrElse { error ->
                return if (isDrmProtectedError(error)) {
                    BookParseResult.Error.DrmProtected
                } else {
                    BookParseResult.Error.CorruptFile(Exception(error.toString()))
                }
            }

        return mapToResult(publication, bookId, fileUri)
    }

    private fun mapToResult(
        publication: Publication,
        bookId: String,
        fileUri: String,
    ): BookParseResult.Success {
        val book = Book(
            id = bookId,
            title = publication.metadata.title ?: "Unknown",
            author = publication.metadata.authors.firstOrNull()?.name,
            coverUri = null,
            fileUri = fileUri,
            format = BookFormat.EPUB,
            addedAt = System.currentTimeMillis(),
        )

        val chapters = publication.tableOfContents.mapIndexed { index, link ->
            Chapter(
                title = link.title ?: "Chapter ${index + 1}",
                href = link.href.string,
                position = index,
            )
        }

        val readingOrder = publication.readingOrder.map { link ->
            ReadingOrderItem(
                href = link.href.string,
                mediaType = link.mediaType?.toString() ?: "application/xhtml+xml",
            )
        }

        val estimatedPageCount = publication.pageList.size.takeIf { it > 0 }

        return BookParseResult.Success(
            book = book,
            chapters = chapters,
            readingOrder = readingOrder,
            estimatedPageCount = estimatedPageCount,
        )
    }

    private fun isDrmProtectedError(error: Any): Boolean {
        val msg = error.toString().lowercase()
        return "protection" in msg || "drm" in msg || "lcp" in msg || "forbidden" in msg || "encrypted" in msg
    }
}
