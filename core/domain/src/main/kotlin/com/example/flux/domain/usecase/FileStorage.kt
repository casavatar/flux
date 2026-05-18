package com.example.flux.domain.usecase

import java.io.OutputStream

interface FileStorage {
    fun openOutputStream(filename: String): OutputStream
    fun localUriFor(filename: String): String
}
