package com.example.flux.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun getProgressForBook(bookId: String): Flow<ProgressEntity?>

    @Upsert
    suspend fun upsertProgress(progress: ProgressEntity)
}
