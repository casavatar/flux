package com.example.flux.data.di

import android.content.ContentResolver
import android.content.Context
import com.example.flux.data.repository.BookRepositoryImpl
import com.example.flux.data.source.DocumentMetadataProviderImpl
import com.example.flux.domain.repository.BookRepository
import com.example.flux.domain.usecase.DocumentMetadataProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    @Singleton
    abstract fun bindDocumentMetadataProvider(impl: DocumentMetadataProviderImpl): DocumentMetadataProvider

    companion object {
        @Provides
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver
    }
}
