package com.example.flux.domain.repository

import com.example.flux.domain.model.Progress

interface ProgressRepository {
    suspend fun getProgress(bookId: String): Progress?
    suspend fun upsertProgress(progress: Progress)
}
