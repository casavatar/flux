package com.example.flux.domain.usecase

import com.example.flux.domain.model.Progress
import com.example.flux.domain.repository.ProgressRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SaveReadingProgressUseCaseTest {

    private val progressRepository: ProgressRepository = mockk()
    private val useCase = SaveReadingProgressUseCase(progressRepository)

    @Test
    fun `upserts progress via repository`() = runTest {
        val progress = Progress(bookId = "book1", currentPage = 3, totalPages = 50, lastReadAt = 500L)
        coJustRun { progressRepository.upsertProgress(progress) }

        useCase(progress)

        coVerify(exactly = 1) { progressRepository.upsertProgress(progress) }
    }

    @Test
    fun `second save for same book replaces prior entry`() = runTest {
        val first = Progress(bookId = "book1", currentPage = 3, totalPages = 50, lastReadAt = 500L)
        val second = Progress(bookId = "book1", currentPage = 10, totalPages = 50, lastReadAt = 900L)
        coJustRun { progressRepository.upsertProgress(any()) }

        useCase(first)
        useCase(second)

        coVerify(exactly = 1) { progressRepository.upsertProgress(second) }
    }
}
