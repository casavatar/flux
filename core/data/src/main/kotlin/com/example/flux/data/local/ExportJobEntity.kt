package com.example.flux.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.ExportJob
import com.example.flux.domain.model.ExportStatus

@Entity(
    tableName = "export_jobs",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId")],
)
data class ExportJobEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val format: BookFormat,
    val status: ExportStatus,
    val outputUri: String?,
    val createdAt: Long,
    val completedAt: Long?,
)

fun ExportJobEntity.toDomain() = ExportJob(
    id = id,
    bookId = bookId,
    format = format,
    status = status,
    outputUri = outputUri,
    createdAt = createdAt,
    completedAt = completedAt,
)

fun ExportJob.toEntity() = ExportJobEntity(
    id = id,
    bookId = bookId,
    format = format,
    status = status,
    outputUri = outputUri,
    createdAt = createdAt,
    completedAt = completedAt,
)
