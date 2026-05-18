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
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadiumEpubParser @Inject constructor(
    private val assetRetriever: AssetRetriever,
    private val publicationOpener: PublicationOpener,
) {
    suspend fun parse(bookId: String, fileUri: String): BookParseResult {
        val url = AbsoluteUrl(fileUri)
            ?: return BookParseResult.Error.Unknown(IllegalArgumentException("Invalid URI: $fileUri"))

        val asset = when (val r = withContext(Dispatchers.IO) { assetRetriever.retrieve(url) }) {
            is Try.Success -> r.value
            is Try.Failure -> return BookParseResult.Error.Unknown(Exception(r.value.toString()))
        }

        val publication = when (val r = withContext(Dispatchers.IO) {
            publicationOpener.open(asset, allowUserInteraction = false)
        }) {
            is Try.Success -> r.value
            is Try.Failure -> return if (isDrmProtectedError(r.value)) {
                BookParseResult.Error.DrmProtected
            } else {
                BookParseResult.Error.CorruptFile(Exception(r.value.toString()))
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
                href = link.href.toString(),
                position = index,
            )
        }

        val readingOrder = publication.readingOrder.map { link ->
            ReadingOrderItem(
                href = link.href.toString(),
                mediaType = link.mediaType?.toString() ?: "application/xhtml+xml",
            )
        }

        return BookParseResult.Success(
            book = book,
            chapters = chapters,
            readingOrder = readingOrder,
            estimatedPageCount = null, // pageList removed from Publication API in Readium 3.x
        )
    }

    private fun isDrmProtectedError(error: Any): Boolean {
        val msg = error.toString().lowercase()
        return "protection" in msg || "drm" in msg || "lcp" in msg || "forbidden" in msg || "encrypted" in msg
    }
}
