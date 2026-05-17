package com.example.flux.data.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \n2\u00020\u0001:\u0001\nB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\tH\'\u00a8\u0006\u000b"}, d2 = {"Lcom/example/flux/data/di/RepositoryModule;", "", "()V", "bindBookRepository", "Lcom/example/flux/domain/repository/BookRepository;", "impl", "Lcom/example/flux/data/repository/BookRepositoryImpl;", "bindDocumentMetadataProvider", "Lcom/example/flux/domain/usecase/DocumentMetadataProvider;", "Lcom/example/flux/data/source/DocumentMetadataProviderImpl;", "Companion", "data_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class RepositoryModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.flux.data.di.RepositoryModule.Companion Companion = null;
    
    public RepositoryModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.flux.domain.repository.BookRepository bindBookRepository(@org.jetbrains.annotations.NotNull()
    com.example.flux.data.repository.BookRepositoryImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.flux.domain.usecase.DocumentMetadataProvider bindDocumentMetadataProvider(@org.jetbrains.annotations.NotNull()
    com.example.flux.data.source.DocumentMetadataProviderImpl impl);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\u0007"}, d2 = {"Lcom/example/flux/data/di/RepositoryModule$Companion;", "", "()V", "provideContentResolver", "Landroid/content/ContentResolver;", "context", "Landroid/content/Context;", "data_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @dagger.Provides()
        @org.jetbrains.annotations.NotNull()
        public final android.content.ContentResolver provideContentResolver(@dagger.hilt.android.qualifiers.ApplicationContext()
        @org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}