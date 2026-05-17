package com.example.flux.domain.usecase

import com.example.flux.domain.exception.PermissionException
import com.example.flux.domain.exception.UnsupportedFormatException
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.repository.BookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class ImportDocumentUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val documentMetadataProvider: DocumentMetadataProvider,
) {
    suspend operator fun invoke(uriString: String): Result<Book> {
        return try {
            val book = withContext(Dispatchers.IO) {
                try {
                    documentMetadataProvider.takePersistablePermission(uriString)
                } catch (e: SecurityException) {
                    throw PermissionException(uriString)
                }

                val mimeType = documentMetadataProvider.getMimeType(uriString)
                val format = mimeType?.toBookFormat() ?: throw UnsupportedFormatException(mimeType)
                val displayName = documentMetadataProvider.getDisplayName(uriString)
                    ?: uriString.substringAfterLast('/')

                Book(
                    id = UUID.randomUUID().toString(),
                    title = displayName.stripExtension(),
                    author = null,
                    coverUri = null,
                    fileUri = uriString,
                    format = format,
                    addedAt = System.currentTimeMillis(),
                ).also { bookRepository.insert(it) }
            }
            Result.success(book)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String.toBookFormat(): BookFormat? = when (this) {
        "application/epub+zip" -> BookFormat.EPUB
        "application/pdf" -> BookFormat.PDF
        "text/plain" -> BookFormat.TXT
        else -> null
    }

    private fun String.stripExtension(): String =
        substringBeforeLast('.').ifEmpty { this }
}
