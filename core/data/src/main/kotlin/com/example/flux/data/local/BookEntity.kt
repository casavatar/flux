package com.example.flux.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val coverUri: String?,
    val fileUri: String,
    val format: BookFormat,
    val addedAt: Long,
)

fun BookEntity.toDomain() = Book(
    id = id,
    title = title,
    author = author,
    coverUri = coverUri,
    fileUri = fileUri,
    format = format,
    addedAt = addedAt,
)

fun Book.toEntity() = BookEntity(
    id = id,
    title = title,
    author = author,
    coverUri = coverUri,
    fileUri = fileUri,
    format = format,
    addedAt = addedAt,
)
