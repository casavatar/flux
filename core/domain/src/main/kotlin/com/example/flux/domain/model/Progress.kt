package com.example.flux.domain.model

data class Progress(
    val bookId: String,
    val currentPage: Int,
    val totalPages: Int,
    val lastReadAt: Long,
)
