package com.candy.capture.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.candy.capture.dao.CityDao
import com.candy.capture.dao.ContentDao
import com.candy.capture.dao.SearchDao
import com.candy.capture.model.City
import com.candy.capture.model.Content
import com.candy.capture.model.SearchHistory

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/14 19:48
 */
@Database(entities = [Content::class, City::class, SearchHistory::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun cityDao(): CityDao
    abstract fun searchDao(): SearchDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun get(context: Context): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "capture.db").build()
            }
            return instance!!
        }
    }
}