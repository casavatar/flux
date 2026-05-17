package com.example.flux.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\t"}, d2 = {"Lcom/example/flux/data/local/FluxDatabase;", "Landroidx/room/RoomDatabase;", "()V", "bookDao", "Lcom/example/flux/data/local/BookDao;", "exportJobDao", "Lcom/example/flux/data/local/ExportJobDao;", "progressDao", "Lcom/example/flux/data/local/ProgressDao;", "data_debug"})
@androidx.room.Database(entities = {com.example.flux.data.local.BookEntity.class, com.example.flux.data.local.ProgressEntity.class, com.example.flux.data.local.ExportJobEntity.class}, version = 1, exportSchema = true, autoMigrations = {})
@androidx.room.TypeConverters(value = {com.example.flux.data.local.Converters.class})
public abstract class FluxDatabase extends androidx.room.RoomDatabase {
    
    public FluxDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.flux.data.local.BookDao bookDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.flux.data.local.ProgressDao progressDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.flux.data.local.ExportJobDao exportJobDao();
}