package com.example.flux.data.repository

import com.example.flux.data.local.BookDao
import com.example.flux.data.local.toDomain
import com.example.flux.data.local.toEntity
import com.example.flux.domain.model.Book
import com.example.flux.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }

    override fun getBookById(bookId: String): Flow<Book?> =
        bookDao.getBookById(bookId).map { it?.toDomain() }

    override suspend fun insert(book: Book) = bookDao.insert(book.toEntity())

    override suspend fun delete(bookId: String) = bookDao.delete(bookId)
}
