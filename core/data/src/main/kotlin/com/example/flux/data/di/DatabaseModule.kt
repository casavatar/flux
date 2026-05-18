package com.example.flux.data.di

import android.content.Context
import androidx.room.Room
import com.example.flux.data.local.BookDao
import com.example.flux.data.local.ExportJobDao
import com.example.flux.data.local.FluxDatabase
import com.example.flux.data.local.ProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFluxDatabase(@ApplicationContext context: Context): FluxDatabase =
        Room.databaseBuilder(context, FluxDatabase::class.java, "flux.db")
            .build()

    @Provides
    fun provideBookDao(db: FluxDatabase): BookDao = db.bookDao()

    @Provides
    fun provideProgressDao(db: FluxDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideExportJobDao(db: FluxDatabase): ExportJobDao = db.exportJobDao()
}
