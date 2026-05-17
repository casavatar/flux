package com.example.flux.domain.model

data class ExportJob(
    val id: String,
    val bookId: String,
    val format: BookFormat,
    val status: ExportStatus,
    val outputUri: String?,
    val createdAt: Long,
    val completedAt: Long?,
)
