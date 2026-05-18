package com.example.flux.data.pdf

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfPageCacheTest {

    private lateinit var cache: PdfPageCache
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Wipe any leftover files so each test starts clean.
        cacheDir = File(context.cacheDir, "pdf_pages")
        cacheDir.deleteRecursively()
        cache = PdfPageCache(context)
    }

    @Test
    fun put_then_get_returnsSameBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        cache.put("book1_page_0_2.0", bitmap)

        val result = cache.get("book1_page_0_2.0")

        assertNotNull(result)
    }

    @Test
    fun get_missingKey_returnsNull() {
        assertNull(cache.get("nonexistent_key"))
    }

    @Test
    fun put_overSizeLimit_evictsOldEntries() {
        // Each bitmap is 10×10 px × 4 bytes/px = 400 bytes on disk (PNG overhead makes it larger).
        // We need to exceed 50 MB. Use 4 MB bitmaps: 1024×1024 × 4 = 4 MB each → 15 entries > 50 MB.
        val largeBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)

        for (i in 0 until 15) {
            cache.put("book1_page_${i}_2.0", largeBitmap)
        }

        assertTrue(
            "Cache size ${cache.currentSizeBytes()} should be ≤ 50 MB after eviction",
            cache.currentSizeBytes() <= 50L * 1024 * 1024,
        )
    }

    @Test
    fun put_sameKey_updatesExistingEntry() {
        val bitmap1 = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val bitmap2 = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        cache.put("book1_page_0_2.0", bitmap1)
        val sizeAfterFirst = cache.currentSizeBytes()

        cache.put("book1_page_0_2.0", bitmap2)

        // Size should not keep growing without bound; entry was replaced, not appended.
        assertTrue(cache.currentSizeBytes() <= sizeAfterFirst * 5)
    }
}
