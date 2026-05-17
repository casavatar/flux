package com.example.flux.domain.usecase

import com.example.flux.domain.exception.PermissionException
import com.example.flux.domain.exception.UnsupportedFormatException
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportDocumentUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private val metadataProvider: DocumentMetadataProvider = mockk()
    private val useCase = ImportDocumentUseCase(bookRepository, metadataProvider)

    @Test
    fun `valid EPUB URI returns success and inserts book`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/epub+zip"
        every { metadataProvider.getDisplayName(uri) } returns "My Book.epub"
        coJustRun { bookRepository.insert(any()) }

        val result = useCase(uri)

        assertTrue(result.isSuccess)
        assertEquals(BookFormat.EPUB, result.getOrNull()?.format)
        assertEquals("My Book", result.getOrNull()?.title)
        assertEquals(uri, result.getOrNull()?.fileUri)
        coVerify(exactly = 1) { bookRepository.insert(any()) }
    }

    @Test
    fun `valid PDF URI returns success with correct format`() = runTest {
        val uri = "content://com.example.provider/report.pdf"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/pdf"
        every { metadataProvider.getDisplayName(uri) } returns "report.pdf"
        coJustRun { bookRepository.insert(any()) }

        val result = useCase(uri)

        assertTrue(result.isSuccess)
        assertEquals(BookFormat.PDF, result.getOrNull()?.format)
    }

    @Test
    fun `valid TXT URI returns success with correct format`() = runTest {
        val uri = "content://com.example.provider/story.txt"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "text/plain"
        every { metadataProvider.getDisplayName(uri) } returns "story.txt"
        coJustRun { bookRepository.insert(any()) }

        val result = useCase(uri)

        assertTrue(result.isSuccess)
        assertEquals(BookFormat.TXT, result.getOrNull()?.format)
    }

    @Test
    fun `unsupported MIME type returns UnsupportedFormatException`() = runTest {
        val uri = "content://com.example.provider/doc.docx"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/msword"

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertIs<UnsupportedFormatException>(result.exceptionOrNull())
    }

    @Test
    fun `null MIME type returns UnsupportedFormatException`() = runTest {
        val uri = "content://com.example.provider/unknown"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns null

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertIs<UnsupportedFormatException>(result.exceptionOrNull())
    }

    @Test
    fun `SecurityException from permission call returns PermissionException`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        every { metadataProvider.takePersistablePermission(uri) } throws SecurityException("denied")

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertIs<PermissionException>(result.exceptionOrNull())
    }

    @Test
    fun `null display name falls back to last path segment without extension`() = runTest {
        val uri = "content://com.example.provider/my-book.pdf"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/pdf"
        every { metadataProvider.getDisplayName(uri) } returns null
        coJustRun { bookRepository.insert(any()) }

        val result = useCase(uri)

        assertTrue(result.isSuccess)
        assertEquals("my-book", result.getOrNull()?.title)
    }

    @Test
    fun `repository exception propagates as failure`() = runTest {
        val uri = "content://com.example.provider/file.epub"
        justRun { metadataProvider.takePersistablePermission(uri) }
        every { metadataProvider.getMimeType(uri) } returns "application/epub+zip"
        every { metadataProvider.getDisplayName(uri) } returns "file.epub"
        coEvery { bookRepository.insert(any()) } throws RuntimeException("DB error")

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }
}
