package com.example.flux.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EpubModule {

    @Provides
    @Singleton
    fun provideHttpClient(): DefaultHttpClient = DefaultHttpClient()

    @Provides
    @Singleton
    fun provideAssetRetriever(
        @ApplicationContext context: Context,
        httpClient: DefaultHttpClient,
    ): AssetRetriever = AssetRetriever(
        contentResolver = context.contentResolver,
        httpClient = httpClient,
    )

    @Provides
    @Singleton
    fun providePublicationOpener(): PublicationOpener = PublicationOpener(
        publicationParser = EpubParser()
    )
}
