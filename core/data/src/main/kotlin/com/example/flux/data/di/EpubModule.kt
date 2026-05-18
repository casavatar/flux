package com.example.flux.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.readium.r2.streamer.Readium
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EpubModule {

    @Provides
    @Singleton
    fun provideReadium(@ApplicationContext context: Context): Readium = Readium(context)
}
