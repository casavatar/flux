package com.example.flux.data.repository

import com.example.flux.data.local.ProgressDao
import com.example.flux.data.local.toDomain
import com.example.flux.data.local.toEntity
import com.example.flux.domain.model.Progress
import com.example.flux.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
) : ProgressRepository {

    override suspend fun getProgress(bookId: String): Progress? =
        progressDao.getProgressForBook(bookId).first()?.toDomain()

    override suspend fun upsertProgress(progress: Progress) =
        progressDao.upsertProgress(progress.toEntity())
}
