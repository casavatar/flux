package com.example.flux.domain.usecase

import com.example.flux.domain.exception.PermissionException
import com.example.flux.domain.exception.UnsupportedFormatException
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.repository.BookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class ImportDocumentUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val documentMetadataProvider: DocumentMetadataProvider,
    private val fileStorage: FileStorage,
) {
    operator fun invoke(uriString: String): Flow<ImportResult> = flow {
        emit(ImportResult.Importing)
        try {
            try {
                documentMetadataProvider.takePersistablePermission(uriString)
            } catch (e: SecurityException) {
                throw PermissionException(uriString)
            }

            val mimeType = documentMetadataProvider.getMimeType(uriString)
            val format = mimeType?.toBookFormat() ?: throw UnsupportedFormatException(mimeType)
            val displayName = documentMetadataProvider.getDisplayName(uriString)
                ?: uriString.substringAfterLast('/')

            val bookId = UUID.randomUUID().toString()
            val filename = "$bookId.${format.name.lowercase()}"

            val input = documentMetadataProvider.openInputStream(uriString)
                ?: throw IOException("Cannot open stream for $uriString")
            input.buffered(BUFFER_SIZE).use { src ->
                fileStorage.openOutputStream(filename).buffered(BUFFER_SIZE).use { dst ->
                    src.copyTo(dst, bufferSize = BUFFER_SIZE)
                }
            }

            val book = Book(
                id = bookId,
                title = displayName.stripExtension(),
                author = null,
                coverUri = null,
                fileUri = fileStorage.localUriFor(filename),
                format = format,
                addedAt = System.currentTimeMillis(),
            )
            bookRepository.insert(book)
            emit(ImportResult.Success(book.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ImportResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    private fun String.toBookFormat(): BookFormat? = when (this) {
        "application/epub+zip" -> BookFormat.EPUB
        "application/pdf" -> BookFormat.PDF
        "text/plain" -> BookFormat.TXT
        else -> null
    }

    private fun String.stripExtension(): String =
        substringBeforeLast('.').ifEmpty { this }

    companion object {
        private const val BUFFER_SIZE = 16 * 1024
    }
}
