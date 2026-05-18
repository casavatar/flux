package com.example.flux.data.source

import android.content.Context
import com.example.flux.domain.usecase.FileStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

class FileStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileStorage {

    override fun openOutputStream(filename: String): OutputStream =
        File(context.filesDir, filename).outputStream()

    override fun localUriFor(filename: String): String =
        File(context.filesDir, filename).toURI().toString()
}
