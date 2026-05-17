package com.example.flux.data.di;

import android.content.Context;
import com.example.flux.data.local.FluxDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvideFluxDatabaseFactory implements Factory<FluxDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideFluxDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FluxDatabase get() {
    return provideFluxDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideFluxDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideFluxDatabaseFactory(contextProvider);
  }

  public static FluxDatabase provideFluxDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFluxDatabase(context));
  }
}
