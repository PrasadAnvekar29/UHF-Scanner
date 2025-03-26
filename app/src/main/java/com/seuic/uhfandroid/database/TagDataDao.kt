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
    suspend fun insert(tagDataEntries: List<TagDataEntry?>)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tagData: TagDataEntry)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert1(tagData: TagDataEntry)

    @Query("DELETE from tag_data_table")
    suspend fun deleteAll()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'tag_data_table'")
    suspend fun truncateAll()

  //  @Query("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE id='tag_data_table'")
  //  fun resetTable()


    /*@Query("DELETE from tag_data_table WHERE epcId = 'epcId' AND rssi = 'rssi' AND times = 'times' AND" +
            "  antenna = 'antenna' AND additionalData = 'additionalData'")
    suspend fun deleteData(epcId : String, rssi : Int,times : Int, antenna : String, additionalData : String)*/



    @Query("SELECT * from tag_data_table")
    suspend fun getList(): List<TagDataEntry>?

    @Query("SELECT * from tag_data_table")
    fun getListLiveData(): LiveData<MutableList<TagDataEntry>>

    @Query("SELECT * from tag_data_table group by epcId ")
    fun getCount(): LiveData<List<TagDataEntry>>

}
