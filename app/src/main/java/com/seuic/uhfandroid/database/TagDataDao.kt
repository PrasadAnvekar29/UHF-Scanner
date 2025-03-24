package com.seuic.uhfandroid.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface TagDataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tagDataEntries: List<TagDataEntry?>?)

   // @Query("DELETE from tag_data_table")
  //  fun deleteAll()

  //  @Query("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE id='tag_data_table'")
  //  fun resetTable()


    /*@Query("DELETE from tag_data_table WHERE epcId = 'epcId' AND rssi = 'rssi' AND times = 'times' AND" +
            "  antenna = 'antenna' AND additionalData = 'additionalData'")
    suspend fun deleteData(epcId : String, rssi : Int,times : Int, antenna : String, additionalData : String)*/



    @Query("SELECT * from tag_data_table")
    suspend fun getList(): List<TagDataEntry>?


}
