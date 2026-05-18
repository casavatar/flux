package com.example.flux.domain.usecase

sealed class ImportResult {
    data object Importing : ImportResult()
    data class Success(val bookId: String) : ImportResult()
    data class Error(val cause: Exception) : ImportResult()
}
