package com.seuic.uhfandroid.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface LogDao {




    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tagData: LogEntry)

    @Query("DELETE from log_data_table")
    suspend fun deleteAll()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'log_data_table'")
    suspend fun truncateAll()


  //  @Query("DELETE from log_data_table")
 //   suspend fun deleteAll()

    @Query("SELECT * from log_data_table ORDER BY id DESC ")
    fun getList(): List<LogEntry>



  //  @Query("SELECT * from tag_data_table")
 //   suspend fun getList(): List<LogEntry>?


}
