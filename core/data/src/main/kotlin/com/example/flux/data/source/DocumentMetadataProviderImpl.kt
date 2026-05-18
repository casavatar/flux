package com.example.flux.data.source

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.flux.domain.usecase.DocumentMetadataProvider
import java.io.InputStream
import javax.inject.Inject

class DocumentMetadataProviderImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : DocumentMetadataProvider {

    override fun takePersistablePermission(uriString: String) {
        contentResolver.takePersistableUriPermission(
            Uri.parse(uriString),
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun releasePersistablePermission(uriString: String) {
        try {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Already released or never held — nothing to do.
        }
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

    override fun openInputStream(uriString: String): InputStream? =
        contentResolver.openInputStream(Uri.parse(uriString))
}
