package com.example.flux.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flux.domain.model.Progress

@Entity(
    tableName = "reading_progress",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId")],
)
data class ProgressEntity(
    @PrimaryKey val bookId: String,
    val currentPage: Int,
    val totalPages: Int,
    val lastReadAt: Long,
)

fun ProgressEntity.toDomain() = Progress(
    bookId = bookId,
    currentPage = currentPage,
    totalPages = totalPages,
    lastReadAt = lastReadAt,
)

fun Progress.toEntity() = ProgressEntity(
    bookId = bookId,
    currentPage = currentPage,
    totalPages = totalPages,
    lastReadAt = lastReadAt,
)
