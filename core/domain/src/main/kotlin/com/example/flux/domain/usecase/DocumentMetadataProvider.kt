package com.example.flux.domain.usecase

interface DocumentMetadataProvider {
    fun takePersistablePermission(uriString: String)
    fun getMimeType(uriString: String): String?
    fun getDisplayName(uriString: String): String?
}
