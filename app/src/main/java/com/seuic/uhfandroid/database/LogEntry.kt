package com.seuic.uhfandroid.database

import androidx.room.util.TableInfo
import com.google.gson.annotations.SerializedName
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "log_data_table")
class LogEntry(
    @field:ColumnInfo(name = "error") @field:SerializedName("error") var error: String,
    @field:ColumnInfo(name = "time") @field:SerializedName("time") var time: String,
) {
    @Transient
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}