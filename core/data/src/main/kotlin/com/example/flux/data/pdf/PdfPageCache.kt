package com.example.flux.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfPageCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val cacheDir = File(context.cacheDir, "pdf_pages").also { it.mkdirs() }
    private val maxBytes = 50L * 1024 * 1024 // 50 MB

    private val lock = Any()

    // accessOrder=true gives LRU eviction — least-recently-accessed entry is first
    private val lruIndex = LinkedHashMap<String, Long>(64, 0.75f, true)
    private var totalBytes = 0L

    init {
        // Restore index from any pages left on disk from a previous session,
        // sorted by modification time so the order approximates actual LRU.
        cacheDir.listFiles()
            ?.sortedBy { it.lastModified() }
            ?.forEach { file ->
                lruIndex[file.name] = file.length()
                totalBytes += file.length()
            }
    }

    fun get(key: String): Bitmap? {
        val fileName = key.toFileName()
        val file = File(cacheDir, fileName)
        if (!file.exists()) return null
        synchronized(lock) {
            // Re-insert to move to MRU position in the access-ordered map.
            lruIndex.remove(fileName)?.let { size -> lruIndex[fileName] = size }
        }
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun put(key: String, bitmap: Bitmap) {
        val fileName = key.toFileName()
        val file = File(cacheDir, fileName)
        file.outputStream().buffered().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val fileSize = file.length()
        synchronized(lock) {
            val prev = lruIndex.put(fileName, fileSize) ?: 0L
            totalBytes += fileSize - prev
            evict()
        }
    }

    fun currentSizeBytes(): Long = synchronized(lock) { totalBytes }

    private fun evict() {
        val iter = lruIndex.entries.iterator()
        while (totalBytes > maxBytes && iter.hasNext()) {
            val entry = iter.next()
            if (File(cacheDir, entry.key).delete()) {
                totalBytes -= entry.value
            }
            iter.remove()
        }
    }

    private fun String.toFileName() = replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".png"
}
