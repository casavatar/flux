package com.example.flux.domain.usecase

import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.repository.BookRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.every
import kotlin.test.Test

class DeleteBookUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private val metadataProvider: DocumentMetadataProvider = mockk()
    private val useCase = DeleteBookUseCase(bookRepository, metadataProvider)

    @Test
    fun `valid bookId releases permission then deletes from repo`() = runTest {
        val book = fakeBook("id1")
        every { bookRepository.getBookById("id1") } returns flowOf(book)
        justRun { metadataProvider.releasePersistablePermission(book.fileUri) }
        coJustRun { bookRepository.delete("id1") }

        useCase("id1")

        verify(exactly = 1) { metadataProvider.releasePersistablePermission(book.fileUri) }
        coVerify(exactly = 1) { bookRepository.delete("id1") }
    }

    @Test
    fun `unknown bookId does nothing`() = runTest {
        every { bookRepository.getBookById("missing") } returns flowOf(null)

        useCase("missing")

        verify(exactly = 0) { metadataProvider.releasePersistablePermission(any()) }
        coVerify(exactly = 0) { bookRepository.delete(any()) }
    }
}

private fun fakeBook(id: String) = Book(
    id = id,
    title = "Title $id",
    author = null,
    coverUri = null,
    fileUri = "file:///books/$id",
    format = BookFormat.TXT,
    addedAt = 0L,
)
