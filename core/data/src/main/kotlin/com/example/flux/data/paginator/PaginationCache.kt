package com.example.flux.data.paginator

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory LRU cache for paginated results.
 * Keyed by (bookId, viewportWidth, viewportHeight, fontSizeSp) so that any change
 * to viewport or preferences transparently invalidates the cached result.
 *
 * [LruCache] is thread-safe — no additional synchronisation required.
 */
@Singleton
class PaginationCache @Inject constructor() {

    private val cache = LruCache<CacheKey, List<String>>(MAX_ENTRIES)

    data class CacheKey(
        val bookId: String,
        val viewportWidth: Int,
        val viewportHeight: Int,
        val fontSizeSp: Int,
    )

    operator fun get(key: CacheKey): List<String>? = cache[key]

    operator fun set(key: CacheKey, pages: List<String>) {
        cache.put(key, pages)
    }

    /** Drops all cached pages for [bookId] (e.g. after file replacement). */
    fun invalidate(bookId: String) {
        cache.snapshot().keys
            .filter { it.bookId == bookId }
            .forEach(cache::remove)
    }

    companion object {
        private const val MAX_ENTRIES = 10
    }
}
