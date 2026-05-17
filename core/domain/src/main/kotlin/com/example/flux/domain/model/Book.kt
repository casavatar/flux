package com.example.flux.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val coverUri: String?,
    val fileUri: String,
    val format: BookFormat,
    val addedAt: Long,
)
