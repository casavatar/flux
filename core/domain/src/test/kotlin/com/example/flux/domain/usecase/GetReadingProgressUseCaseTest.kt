package com.example.flux.domain.usecase

import com.example.flux.domain.model.Progress
import com.example.flux.domain.repository.ProgressRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetReadingProgressUseCaseTest {

    private val progressRepository: ProgressRepository = mockk()
    private val useCase = GetReadingProgressUseCase(progressRepository)

    @Test
    fun `no prior progress returns null`() = runTest {
        coEvery { progressRepository.getProgress("book1") } returns null

        assertNull(useCase("book1"))
    }

    @Test
    fun `existing progress returns correct Progress`() = runTest {
        val progress = Progress(bookId = "book1", currentPage = 5, totalPages = 100, lastReadAt = 1000L)
        coEvery { progressRepository.getProgress("book1") } returns progress

        assertEquals(progress, useCase("book1"))
    }
}
