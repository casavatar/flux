package com.example.flux.domain.usecase

import com.example.flux.domain.model.Progress
import com.example.flux.domain.repository.ProgressRepository
import javax.inject.Inject

class SaveReadingProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(progress: Progress) = progressRepository.upsertProgress(progress)
}
