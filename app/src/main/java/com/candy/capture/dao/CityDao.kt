package com.candy.capture.dao

import androidx.room.*
import com.candy.capture.model.City

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/21 15:38
 */
@Dao
interface CityDao {
    @Query("SELECT * FROM city")
    suspend fun getAllCity(): List<City>

    @Query("SELECT * FROM city WHERE city_name=:cityName")
    suspend fun queryCity(cityName: String): City

    @Insert
    fun insertCity(city: City)

    @Update
    fun updateCity(city: City)

    @Delete(entity = City::class)
    fun deleteCity(city: City)
}