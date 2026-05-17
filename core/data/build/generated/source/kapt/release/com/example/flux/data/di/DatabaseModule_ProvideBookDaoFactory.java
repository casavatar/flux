package com.example.flux.data.di;

import com.example.flux.data.local.BookDao;
import com.example.flux.data.local.FluxDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DatabaseModule_ProvideBookDaoFactory implements Factory<BookDao> {
  private final Provider<FluxDatabase> dbProvider;

  public DatabaseModule_ProvideBookDaoFactory(Provider<FluxDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BookDao get() {
    return provideBookDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBookDaoFactory create(Provider<FluxDatabase> dbProvider) {
    return new DatabaseModule_ProvideBookDaoFactory(dbProvider);
  }

  public static BookDao provideBookDao(FluxDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBookDao(db));
  }
}
