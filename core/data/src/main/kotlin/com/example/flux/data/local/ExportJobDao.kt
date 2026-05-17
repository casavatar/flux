package com.example.flux.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportJobDao {

    @Query("SELECT * FROM export_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<ExportJobEntity>>

    @Query("SELECT * FROM export_jobs WHERE id = :jobId")
    fun getJobById(jobId: String): Flow<ExportJobEntity?>

    @Upsert
    suspend fun upsertJob(job: ExportJobEntity)

    @Query("DELETE FROM export_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)
}
