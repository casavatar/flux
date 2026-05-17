package com.example.flux

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.flux.domain.repository.BookRepository
import com.example.flux.domain.usecase.DocumentMetadataProvider
import com.example.flux.domain.usecase.ImportDocumentUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.example.flux.common.di.IoDispatcher

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var bookRepository: BookRepository
    @Inject lateinit var documentMetadataProvider: DocumentMetadataProvider
    @Inject lateinit var importDocumentUseCase: ImportDocumentUseCase
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun hiltGraph_injectsAllDependencies() {
        assert(::bookRepository.isInitialized)
        assert(::documentMetadataProvider.isInitialized)
        assert(::importDocumentUseCase.isInitialized)
        assert(::ioDispatcher.isInitialized)
    }
}
