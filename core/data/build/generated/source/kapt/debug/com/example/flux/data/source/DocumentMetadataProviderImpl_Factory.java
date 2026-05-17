package com.example.flux.data.source;

import android.content.ContentResolver;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class DocumentMetadataProviderImpl_Factory implements Factory<DocumentMetadataProviderImpl> {
  private final Provider<ContentResolver> contentResolverProvider;

  public DocumentMetadataProviderImpl_Factory(Provider<ContentResolver> contentResolverProvider) {
    this.contentResolverProvider = contentResolverProvider;
  }

  @Override
  public DocumentMetadataProviderImpl get() {
    return newInstance(contentResolverProvider.get());
  }

  public static DocumentMetadataProviderImpl_Factory create(
      Provider<ContentResolver> contentResolverProvider) {
    return new DocumentMetadataProviderImpl_Factory(contentResolverProvider);
  }

  public static DocumentMetadataProviderImpl newInstance(ContentResolver contentResolver) {
    return new DocumentMetadataProviderImpl(contentResolver);
  }
}
