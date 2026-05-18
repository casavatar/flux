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
import kotlin.test.assertNull

class GetBookByIdUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private val useCase = GetBookByIdUseCase(bookRepository)

    @Test
    fun `existing bookId emits Book`() = runTest {
        val book = fakeBook("abc")
        every { bookRepository.getBookById("abc") } returns flowOf(book)

        useCase("abc").test {
            assertEquals(book, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `unknown bookId emits null`() = runTest {
        every { bookRepository.getBookById("missing") } returns flowOf(null)

        useCase("missing").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `book deleted mid-stream emits null after deletion`() = runTest {
        val book = fakeBook("abc")
        every { bookRepository.getBookById("abc") } returns flowOf(book, null)

        useCase("abc").test {
            assertEquals(book, awaitItem())
            assertNull(awaitItem())
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
    format = BookFormat.PDF,
    addedAt = 0L,
)
