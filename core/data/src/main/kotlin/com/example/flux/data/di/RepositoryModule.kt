package com.example.flux.data.di

import android.content.ContentResolver
import android.content.Context
import com.example.flux.data.repository.BookRepositoryImpl
import com.example.flux.data.repository.ProgressRepositoryImpl
import com.example.flux.data.source.DocumentMetadataProviderImpl
import com.example.flux.data.preferences.PreferencesRepositoryImpl
import com.example.flux.data.source.FileStorageImpl
import com.example.flux.domain.repository.BookRepository
import com.example.flux.domain.repository.PreferencesRepository
import com.example.flux.domain.repository.ProgressRepository
import com.example.flux.domain.usecase.DocumentMetadataProvider
import com.example.flux.domain.usecase.FileStorage
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

    @Binds @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds @Singleton
    abstract fun bindDocumentMetadataProvider(impl: DocumentMetadataProviderImpl): DocumentMetadataProvider

    @Binds @Singleton
    abstract fun bindFileStorage(impl: FileStorageImpl): FileStorage

    @Binds @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    companion object {
        @Provides
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver
    }
}
