package com.example.flux.domain.usecase

import app.cash.turbine.test
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.repository.BookRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetLibraryUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private val useCase = GetLibraryUseCase(bookRepository)

    @Test
    fun `empty library emits empty list`() = runTest {
        every { bookRepository.getAllBooks() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `non-empty library emits all books`() = runTest {
        val books = listOf(fakeBook("1"), fakeBook("2"))
        every { bookRepository.getAllBooks() } returns flowOf(books)

        useCase().test {
            assertEquals(books, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `multiple emissions reflect library changes`() = runTest {
        val first = listOf(fakeBook("1"))
        val second = listOf(fakeBook("1"), fakeBook("2"))
        every { bookRepository.getAllBooks() } returns flowOf(first, second)

        useCase().test {
            assertEquals(first, awaitItem())
            assertEquals(second, awaitItem())
            awaitComplete()
        }
    }
}

private fun fakeBook(id: String) = Book(
    id = id,
    title = "Title $id",
    author = null,
    coverUri = null,
    fileUri = "file:///books/$id",
    format = BookFormat.EPUB,
    addedAt = 0L,
)
