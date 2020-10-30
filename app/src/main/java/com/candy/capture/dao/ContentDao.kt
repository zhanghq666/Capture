package com.candy.capture.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.candy.capture.model.City
import com.candy.capture.model.Content


/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/14 19:35
 */
@Dao
interface ContentDao {
    @Query("SELECT * FROM content ORDER BY id DESC")
    fun contentsByDate(): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE city_name=:cityName ORDER BY id DESC")
    fun searchContentByCity(cityName: String): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE city_name=:cityName ORDER BY id DESC")
    suspend fun searchContentByCity1(cityName: String): List<Content>

    @Query("SELECT * FROM content WHERE type=:type ORDER BY id DESC")
    fun searchContentByType(type: Int): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE description LIKE :desc ORDER BY id DESC")
//    @Query("SELECT * FROM content WHERE desc LIKE %:description% ORDER BY id DESC")
    fun searchContentByDesc(desc: String): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(city: City, content: Content)

    @Insert
    suspend fun insertContent(content: Content)

    @Delete
    fun deleteContent(vararg content: Content)

    @Delete
    fun deleteContents(content: List<Content>)


    @Query("SELECT * FROM city WHERE city_name=:cityName")
    suspend fun queryCity(cityName: String): City

    @Query("UPDATE city SET content_count=content_count-1 WHERE city_name=:cityName")
    suspend fun decreaseCityContentCount(cityName: String)


    @Transaction
    open suspend fun deleteContentAndUpdateCity(vararg content: Content) {
        content.forEach {
            deleteContent(it)
//            var city = queryCity(it.cityName)
//            city.count--
            decreaseCityContentCount(it.cityName)
        }
    }
    @Transaction
    open suspend fun deleteContentAndUpdateCity(content: List<Content>) {
        content.forEach {
            deleteContent(it)
            decreaseCityContentCount(it.cityName)
        }
    }
}