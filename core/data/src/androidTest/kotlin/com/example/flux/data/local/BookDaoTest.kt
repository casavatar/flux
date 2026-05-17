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
class BookDaoTest {

    private lateinit var db: FluxDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FluxDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.bookDao()
    }

    @After
    fun tearDown() = db.close()

    private fun book(id: String = "1") = BookEntity(
        id = id,
        title = "Test Book",
        author = "Author",
        coverUri = null,
        fileUri = "content://test/$id",
        format = BookFormat.EPUB,
        addedAt = 1_000L,
    )

    @Test
    fun getAllBooks_emptyInitially() = runTest {
        assertEquals(emptyList<BookEntity>(), dao.getAllBooks().first())
    }

    @Test
    fun insert_thenGetAll_returnsBook() = runTest {
        dao.insert(book())
        assertEquals(listOf(book()), dao.getAllBooks().first())
    }

    @Test
    fun getBookById_returnsCorrectBook() = runTest {
        dao.insert(book("a"))
        dao.insert(book("b"))
        assertEquals(book("b"), dao.getBookById("b").first())
    }

    @Test
    fun getBookById_missingId_returnsNull() = runTest {
        assertNull(dao.getBookById("missing").first())
    }

    @Test
    fun delete_removesBook() = runTest {
        dao.insert(book())
        dao.delete("1")
        assertNull(dao.getBookById("1").first())
    }

    @Test
    fun insert_replaceOnConflict_updatesBook() = runTest {
        dao.insert(book())
        val updated = book().copy(title = "Updated")
        dao.insert(updated)
        assertEquals("Updated", dao.getBookById("1").first()?.title)
    }
}
