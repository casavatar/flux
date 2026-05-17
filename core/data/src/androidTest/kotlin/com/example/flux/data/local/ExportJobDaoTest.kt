package com.example.flux.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.ExportStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportJobDaoTest {

    private lateinit var db: FluxDatabase
    private lateinit var bookDao: BookDao
    private lateinit var dao: ExportJobDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FluxDatabase::class.java,
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        dao = db.exportJobDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertBook(id: String = "book1") = bookDao.insert(
        BookEntity(id, "Title", null, null, "content://x/$id", BookFormat.EPUB, 0L)
    )

    private fun job(id: String = "job1", bookId: String = "book1") = ExportJobEntity(
        id = id,
        bookId = bookId,
        format = BookFormat.PDF,
        status = ExportStatus.PENDING,
        outputUri = null,
        createdAt = 1_000L,
        completedAt = null,
    )

    @Test
    fun getAllJobs_emptyInitially() = runTest {
        assertTrue(dao.getAllJobs().first().isEmpty())
    }

    @Test
    fun upsert_thenGetAll_returnsJob() = runTest {
        insertBook()
        dao.upsertJob(job())
        assertEquals(listOf(job()), dao.getAllJobs().first())
    }

    @Test
    fun getJobById_returnsCorrectJob() = runTest {
        insertBook()
        dao.upsertJob(job("j1"))
        dao.upsertJob(job("j2"))
        assertEquals(job("j1"), dao.getJobById("j1").first())
    }

    @Test
    fun getJobById_missing_returnsNull() = runTest {
        assertNull(dao.getJobById("none").first())
    }

    @Test
    fun upsert_updatesExistingJob() = runTest {
        insertBook()
        dao.upsertJob(job())
        dao.upsertJob(job().copy(status = ExportStatus.DONE, outputUri = "content://out/1"))
        val result = dao.getJobById("job1").first()
        assertEquals(ExportStatus.DONE, result?.status)
        assertEquals("content://out/1", result?.outputUri)
    }

    @Test
    fun deleteJob_removesJob() = runTest {
        insertBook()
        dao.upsertJob(job())
        dao.deleteJob("job1")
        assertNull(dao.getJobById("job1").first())
    }

    @Test
    fun deleteBook_cascadesDeleteToJobs() = runTest {
        insertBook()
        dao.upsertJob(job())
        bookDao.delete("book1")
        assertTrue(dao.getAllJobs().first().isEmpty())
    }
}
