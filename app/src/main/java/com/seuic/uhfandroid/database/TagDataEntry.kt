package com.seuic.uhfandroid.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


@Entity(tableName = "tag_data_table",
    indices = [Index(value = ["epcId","antenna"], unique = true)])
class TagDataEntry(
    @field:ColumnInfo(name = "epcId") @field:SerializedName("epcId") var epcId: String,
    @field:ColumnInfo(name = "antenna") @field:SerializedName("antenna") var antenna: String,
) {
    @Transient
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}
