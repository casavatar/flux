package com.example.flux.domain.usecase

import app.cash.turbine.test
import com.example.flux.domain.exception.PermissionException
import com.example.flux.domain.exception.UnsupportedFormatException
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.repository.BookRepository
import io.mockk.coJustRun
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ImportDocumentUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private val metadataProvider: DocumentMetadataProvider = mockk()
    private val fileStorage: FileStorage = mockk()
    private val useCase = ImportDocumentUseCase(bookRepository, metadataProvider, fileStorage)

    private fun stubHappyPath(uri: String, mimeType: String, displayName: String) {
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns mimeType
        every { metadataProvider.getDisplayName(uri) } returns displayName
        every { metadataProvider.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(0))
        every { fileStorage.openOutputStream(any()) } returns ByteArrayOutputStream()
        every { fileStorage.localUriFor(any()) } returns "file:///data/files/book"
        coJustRun { bookRepository.insert(any()) }
    }

    @Test
    fun `valid EPUB URI emits Importing then Success`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        stubHappyPath(uri, "application/epub+zip", "My Book.epub")

        useCase(uri).test {
            assertIs<ImportResult.Importing>(awaitItem())
            val success = awaitItem()
            assertIs<ImportResult.Success>(success)
            awaitComplete()
        }
    }

    @Test
    fun `valid EPUB import inserts book with correct format and title`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        stubHappyPath(uri, "application/epub+zip", "My Book.epub")

        useCase(uri).test {
            awaitItem() // Importing
            awaitItem() // Success
            awaitComplete()
        }

        coVerify(exactly = 1) {
            bookRepository.insert(match { it.format == BookFormat.EPUB && it.title == "My Book" })
        }
    }

    @Test
    fun `valid PDF URI emits Success with PDF format`() = runTest {
        val uri = "content://com.example.provider/report.pdf"
        stubHappyPath(uri, "application/pdf", "report.pdf")

        useCase(uri).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) { bookRepository.insert(match { it.format == BookFormat.PDF }) }
    }

    @Test
    fun `valid TXT URI emits Success with TXT format`() = runTest {
        val uri = "content://com.example.provider/story.txt"
        stubHappyPath(uri, "text/plain", "story.txt")

        useCase(uri).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) { bookRepository.insert(match { it.format == BookFormat.TXT }) }
    }

    @Test
    fun `unsupported MIME type emits Error with UnsupportedFormatException`() = runTest {
        val uri = "content://com.example.provider/doc.docx"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/msword"

        useCase(uri).test {
            assertIs<ImportResult.Importing>(awaitItem())
            val error = awaitItem()
            assertIs<ImportResult.Error>(error)
            assertIs<UnsupportedFormatException>(error.cause)
            awaitComplete()
        }
    }

    @Test
    fun `null MIME type emits Error with UnsupportedFormatException`() = runTest {
        val uri = "content://com.example.provider/unknown"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns null

        useCase(uri).test {
            assertIs<ImportResult.Importing>(awaitItem())
            val error = awaitItem()
            assertIs<ImportResult.Error>(error)
            assertIs<UnsupportedFormatException>(error.cause)
            awaitComplete()
        }
    }

    @Test
    fun `SecurityException from permission emits Error with PermissionException`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        every { metadataProvider.takePersistablePermission(uri) } throws SecurityException("denied")

        useCase(uri).test {
            assertIs<ImportResult.Importing>(awaitItem())
            val error = awaitItem()
            assertIs<ImportResult.Error>(error)
            assertIs<PermissionException>(error.cause)
            awaitComplete()
        }
    }

    @Test
    fun `null display name falls back to last path segment without extension`() = runTest {
        val uri = "content://com.example.provider/my-book.pdf"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/pdf"
        every { metadataProvider.getDisplayName(uri) } returns null
        every { metadataProvider.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(0))
        every { fileStorage.openOutputStream(any()) } returns ByteArrayOutputStream()
        every { fileStorage.localUriFor(any()) } returns "file:///data/files/book"
        coJustRun { bookRepository.insert(any()) }

        useCase(uri).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) { bookRepository.insert(match { it.title == "my-book" }) }
    }

    @Test
    fun `repository exception emits Error`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/epub+zip"
        every { metadataProvider.getDisplayName(uri) } returns "file.epub"
        every { metadataProvider.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(0))
        every { fileStorage.openOutputStream(any()) } returns ByteArrayOutputStream()
        every { fileStorage.localUriFor(any()) } returns "file:///data/files/book"
        coEvery { bookRepository.insert(any()) } throws RuntimeException("DB error")

        useCase(uri).test {
            assertIs<ImportResult.Importing>(awaitItem())
            val error = awaitItem()
            assertIs<ImportResult.Error>(error)
            assertEquals("DB error", error.cause.message)
            awaitComplete()
        }
    }

    @Test
    fun `book fileUri uses local storage path not original SAF uri`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        stubHappyPath(uri, "application/epub+zip", "file.epub")

        useCase(uri).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) {
            bookRepository.insert(match { it.fileUri == "file:///data/files/book" })
        }
    }
}
