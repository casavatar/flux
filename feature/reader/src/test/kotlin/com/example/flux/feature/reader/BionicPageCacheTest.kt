package com.example.flux.feature.reader

import androidx.compose.ui.text.AnnotatedString
import com.example.flux.data.bionic.BionicAnnotator
import com.example.flux.domain.bionic.BionicEngine
import com.example.flux.domain.bionic.StyledWord
import com.example.flux.feature.reader.model.PageCacheKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BionicPageCache].
 *
 * Cache sizing: [fakeAnnotated] = AnnotatedString("word") → text.length=4, spanStyles.size=0
 * → sizeOf = 4*2 + 0*32 = 8 bytes. Tests that exercise LRU eviction use maxSizeBytes=16
 * (fits exactly 2 entries) or maxSizeBytes=24 (fits exactly 3).
 */
class BionicPageCacheTest {

    private val engine = mockk<BionicEngine>()
    private val annotator = mockk<BionicAnnotator>()

    private val fakeWord = StyledWord("wo", "rd", false)
    private val fakeAnnotated = AnnotatedString("word") // sizeOf = 8 bytes (4 chars * 2)

    @Before
    fun setUp() {
        every { engine.annotate(any(), any(), any(), any()) } returns listOf(fakeWord)
        every { annotator.annotate(any(), any(), any()) } returns fakeAnnotated
    }

    private fun cache(maxSizeBytes: Int = BionicPageCache.DEFAULT_MAX_SIZE_BYTES) =
        BionicPageCache(engine, annotator, maxSizeBytes)

    private fun key(page: Int = 0, intensity: Float = 0.4f, font: Int = 18) =
        PageCacheKey("book-1", page, intensity, font)

    // ── miss → hit ────────────────────────────────────────────────────────────

    @Test
    fun firstAccess_callsPipelineAndCachesResult() {
        val c = cache()
        val result = c.getOrPut(key(), "word")

        assertEquals(fakeAnnotated, result)
        verify(exactly = 1) { engine.annotate("word", 0.4f, any(), any()) }
        verify(exactly = 1) { annotator.annotate(listOf(fakeWord), any(), any()) }
    }

    @Test
    fun secondAccessSameKey_returnsCachedValue_noPipelineCall() {
        val c = cache()
        c.getOrPut(key(), "word")
        val second = c.getOrPut(key(), "word")

        assertEquals(fakeAnnotated, second)
        verify(exactly = 1) { engine.annotate(any(), any(), any(), any()) }
        verify(exactly = 1) { annotator.annotate(any(), any(), any()) }
    }

    @Test
    fun operatorGet_returnsNullBeforeFirstPut() {
        val c = cache()
        assertNull(c[key(page = 99)])
    }

    @Test
    fun operatorGet_returnsValueAfterGetOrPut() {
        val c = cache()
        c.getOrPut(key(), "word")
        assertNotNull(c[key()])
    }

    // ── key sensitivity ───────────────────────────────────────────────────────

    @Test
    fun differentPageIndex_separateCacheEntries() {
        val a1 = AnnotatedString("page0")
        val a2 = AnnotatedString("page1")
        every { annotator.annotate(any(), any(), any()) } returnsMany listOf(a1, a2)
        val c = cache()

        assertEquals(a1, c.getOrPut(key(page = 0), "page0"))
        assertEquals(a2, c.getOrPut(key(page = 1), "page1"))
        verify(exactly = 2) { engine.annotate(any(), any(), any(), any()) }
    }

    @Test
    fun differentIntensity_separateCacheEntries() {
        val c = cache()
        c.getOrPut(key(intensity = 0.3f), "word")
        c.getOrPut(key(intensity = 0.7f), "word")
        verify(exactly = 2) { engine.annotate(any(), any(), any(), any()) }
    }

    @Test
    fun differentFontSize_separateCacheEntries() {
        val c = cache()
        c.getOrPut(key(font = 18), "word")
        c.getOrPut(key(font = 24), "word")
        verify(exactly = 2) { engine.annotate(any(), any(), any(), any()) }
    }

    @Test
    fun sameKeyDifferentRawText_returnsCachedResult_engineNotCalledAgain() {
        val c = cache()
        c.getOrPut(key(), "first")
        val second = c.getOrPut(key(), "different text")
        assertEquals(fakeAnnotated, second)
        verify(exactly = 1) { engine.annotate(any(), any(), any(), any()) }
    }

    // ── size-based LRU eviction (fakeAnnotated = 8 bytes each) ───────────────

    @Test
    fun maxSizeBytesExceeded_oldestEntryEvicted() {
        val c = cache(maxSizeBytes = 16) // holds exactly 2 entries at 8 bytes each
        c.getOrPut(key(page = 0), "p0")
        c.getOrPut(key(page = 1), "p1")
        c.getOrPut(key(page = 2), "p2") // pushes total to 24 → evicts page 0 (LRU)

        assertNull("page 0 should be evicted", c[key(page = 0)])
        assertNotNull("page 1 should remain", c[key(page = 1)])
        assertNotNull("page 2 should remain", c[key(page = 2)])
    }

    @Test
    fun accessingEntry_promotesItInLruOrder() {
        val c = cache(maxSizeBytes = 16)
        c.getOrPut(key(page = 0), "p0")
        c.getOrPut(key(page = 1), "p1")
        c[key(page = 0)] // promote page 0 → page 1 becomes LRU
        c.getOrPut(key(page = 2), "p2") // evicts page 1

        assertNotNull("page 0 was promoted, should remain", c[key(page = 0)])
        assertNull("page 1 is LRU, should be evicted", c[key(page = 1)])
        assertNotNull("page 2 should remain", c[key(page = 2)])
    }

    // ── evictAll ─────────────────────────────────────────────────────────────

    @Test
    fun evictAll_clearsAllEntries() {
        val c = cache()
        c.getOrPut(key(page = 0), "p0")
        c.getOrPut(key(page = 1), "p1")
        c.evictAll()

        assertNull(c[key(page = 0)])
        assertNull(c[key(page = 1)])
    }

    @Test
    fun afterEvictAll_nextAccessRecomputesPipeline() {
        val c = cache()
        c.getOrPut(key(), "word")
        c.evictAll()
        c.getOrPut(key(), "word")

        verify(exactly = 2) { engine.annotate(any(), any(), any(), any()) }
    }
}
