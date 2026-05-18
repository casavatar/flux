package com.example.flux.domain.usecase

import java.io.InputStream

interface DocumentMetadataProvider {
    fun takePersistablePermission(uriString: String)
    fun releasePersistablePermission(uriString: String)
    fun getMimeType(uriString: String): String?
    fun getDisplayName(uriString: String): String?
    fun openInputStream(uriString: String): InputStream?
}
