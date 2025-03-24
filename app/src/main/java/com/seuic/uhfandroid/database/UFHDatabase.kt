package com.seuic.uhfandroid.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TagDataEntry::class], version = 1, exportSchema = false)
abstract class UFHDatabase : RoomDatabase() {


    abstract fun tagDataDao(): TagDataDao?


    companion object {
        private const val DATABASE_NAME = "uhf_database"

        private var INSTANCE: UFHDatabase? = null

        fun getDatabase(context: Context): UFHDatabase? {

            Log.d("Database", "Getting the database");
            if (INSTANCE == null) {
                synchronized(UFHDatabase::class.java) {
                    if (INSTANCE == null) {
                        // Create database here
                        INSTANCE = databaseBuilder(context.applicationContext,
                            UFHDatabase::class.java, DATABASE_NAME)
                            .build()

                        Log.d("Database", "Made new database");
                    }
                }
            }
            return INSTANCE
        }

        private val sRoomDatabaseCallback: Callback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
            }
        }
    }
}
