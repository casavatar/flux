package com.example.flux.feature.reader

import android.util.LruCache
import androidx.compose.ui.text.AnnotatedString
import com.example.flux.data.bionic.BionicAnnotator
import com.example.flux.domain.bionic.BionicEngine
import com.example.flux.feature.reader.model.PageCacheKey

/**
 * Size-bounded LRU cache that memoises the bionic annotation pipeline per page.
 *
 * Cache entries are measured in bytes: `text.length * 2` (UTF-16) + a fixed overhead
 * per span. The default [maxSizeBytes] of 8 MB is enough for hundreds of typical pages
 * while keeping the memory footprint well below the 8 MB spec requirement for 5 pages.
 *
 * Invalidation is key-based: a [PageCacheKey] encodes every input that affects the
 * output (bookId, pageIndex, intensity, fontSizeSp). When any parameter changes, old
 * entries are never matched again and are evicted by LRU pressure. Callers should also
 * call [evictAll] on intensity or fontSizeSp changes so stale entries don't hold memory.
 */
class BionicPageCache(
    private val engine: BionicEngine,
    private val annotator: BionicAnnotator,
    maxSizeBytes: Int = DEFAULT_MAX_SIZE_BYTES,
) {
    private val lru = object : LruCache<PageCacheKey, AnnotatedString>(maxSizeBytes) {
        override fun sizeOf(key: PageCacheKey, value: AnnotatedString): Int =
            (value.text.length * 2 + value.spanStyles.size * SPAN_OVERHEAD_BYTES)
                .coerceAtLeast(1)
    }

    /** Returns the cached [AnnotatedString] for [key], or runs the full pipeline and caches it. */
    fun getOrPut(key: PageCacheKey, rawText: String): AnnotatedString =
        lru.get(key) ?: annotator.annotate(engine.annotate(rawText, key.intensity))
            .also { lru.put(key, it) }

    /** Returns the cached value without computing, or null on a miss. */
    operator fun get(key: PageCacheKey): AnnotatedString? = lru.get(key)

    /** Drops all cached entries (call after intensity or fontSizeSp change). */
    fun evictAll() = lru.evictAll()

    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 8 * 1024 * 1024 // 8 MB
        private const val SPAN_OVERHEAD_BYTES = 32
    }
}
