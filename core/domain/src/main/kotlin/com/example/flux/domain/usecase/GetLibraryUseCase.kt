package com.example.flux.domain.usecase

import com.example.flux.domain.model.Book
import com.example.flux.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLibraryUseCase @Inject constructor(
    private val bookRepository: BookRepository,
) {
    operator fun invoke(): Flow<List<Book>> = bookRepository.getAllBooks()
}
