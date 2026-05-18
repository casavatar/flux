package com.example.flux.domain.usecase

import com.example.flux.domain.repository.BookRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DeleteBookUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val documentMetadataProvider: DocumentMetadataProvider,
) {
    suspend operator fun invoke(bookId: String) {
        val book = bookRepository.getBookById(bookId).firstOrNull() ?: return
        documentMetadataProvider.releasePersistablePermission(book.fileUri)
        bookRepository.delete(bookId)
    }
}
