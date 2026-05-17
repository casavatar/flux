package com.example.flux.data.local

import androidx.room.TypeConverter
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.ExportStatus

class Converters {
    @TypeConverter fun fromBookFormat(v: BookFormat): String = v.name
    @TypeConverter fun toBookFormat(v: String): BookFormat = BookFormat.valueOf(v)

    @TypeConverter fun fromExportStatus(v: ExportStatus): String = v.name
    @TypeConverter fun toExportStatus(v: String): ExportStatus = ExportStatus.valueOf(v)
}
