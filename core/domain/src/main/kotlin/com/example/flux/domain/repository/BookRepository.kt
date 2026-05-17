package com.example.flux.domain.repository

import com.example.flux.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getBookById(bookId: String): Flow<Book?>
    suspend fun insert(book: Book)
    suspend fun delete(bookId: String)
}
