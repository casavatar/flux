package com.example.flux.di

import android.content.Context
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.flux.domain.paginator.LineMeasurer
import com.example.flux.paginator.ComposeLineMeasurer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PaginatorModule {

    @Binds
    @Singleton
    abstract fun bindLineMeasurer(impl: ComposeLineMeasurer): LineMeasurer

    companion object {

        @Provides
        @Singleton
        fun provideTextMeasurer(@ApplicationContext context: Context): TextMeasurer {
            val metrics = context.resources.displayMetrics
            return TextMeasurer(
                defaultFontFamilyResolver = createFontFamilyResolver(context),
                defaultDensity = Density(
                    density = metrics.density,
                    fontScale = metrics.scaledDensity / metrics.density,
                ),
                defaultLayoutDirection = LayoutDirection.Ltr,
            )
        }
    }
}
