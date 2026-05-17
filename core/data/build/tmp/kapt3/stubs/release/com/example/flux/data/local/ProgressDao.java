package com.example.flux.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/example/flux/data/local/ProgressDao;", "", "getProgressForBook", "Lkotlinx/coroutines/flow/Flow;", "Lcom/example/flux/data/local/ProgressEntity;", "bookId", "", "upsertProgress", "", "progress", "(Lcom/example/flux/data/local/ProgressEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "data_release"})
@androidx.room.Dao()
public abstract interface ProgressDao {
    
    @androidx.room.Query(value = "SELECT * FROM reading_progress WHERE bookId = :bookId")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.example.flux.data.local.ProgressEntity> getProgressForBook(@org.jetbrains.annotations.NotNull()
    java.lang.String bookId);
    
    @androidx.room.Upsert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsertProgress(@org.jetbrains.annotations.NotNull()
    com.example.flux.data.local.ProgressEntity progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}