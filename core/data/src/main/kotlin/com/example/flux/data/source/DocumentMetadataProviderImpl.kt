package com.example.flux.data.source

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.flux.domain.usecase.DocumentMetadataProvider
import javax.inject.Inject

class DocumentMetadataProviderImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : DocumentMetadataProvider {

    override fun takePersistablePermission(uriString: String) {
        val uri = Uri.parse(uriString)
        contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun getMimeType(uriString: String): String? =
        contentResolver.getType(Uri.parse(uriString))

    override fun getDisplayName(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }
}
