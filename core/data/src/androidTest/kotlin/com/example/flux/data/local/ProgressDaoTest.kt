package com.example.flux.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.flux.domain.model.BookFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressDaoTest {

    private lateinit var db: FluxDatabase
    private lateinit var bookDao: BookDao
    private lateinit var dao: ProgressDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FluxDatabase::class.java,
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        dao = db.progressDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertBook(id: String = "book1") = bookDao.insert(
        BookEntity(id, "Title", null, null, "content://x/$id", BookFormat.EPUB, 0L)
    )

    private fun progress(bookId: String = "book1") = ProgressEntity(
        bookId = bookId,
        currentPage = 5,
        totalPages = 100,
        lastReadAt = 2_000L,
    )

    @Test
    fun getProgressForBook_noProgress_returnsNull() = runTest {
        insertBook()
        assertNull(dao.getProgressForBook("book1").first())
    }

    @Test
    fun upsert_thenGet_returnsProgress() = runTest {
        insertBook()
        dao.upsertProgress(progress())
        assertEquals(progress(), dao.getProgressForBook("book1").first())
    }

    @Test
    fun upsert_updatesExistingProgress() = runTest {
        insertBook()
        dao.upsertProgress(progress())
        val updated = progress().copy(currentPage = 42)
        dao.upsertProgress(updated)
        assertEquals(42, dao.getProgressForBook("book1").first()?.currentPage)
    }

    @Test
    fun deleteBook_cascadesDeleteToProgress() = runTest {
        insertBook()
        dao.upsertProgress(progress())
        bookDao.delete("book1")
        assertNull(dao.getProgressForBook("book1").first())
    }
}
