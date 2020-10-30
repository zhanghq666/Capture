package com.candy.capture.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.candy.capture.model.SearchHistory

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/21 16:35
 */
@Dao
interface SearchDao {
    @Query("SELECT * FROM search_history")
    suspend fun getSearchHistory(): List<SearchHistory>

    @Insert
    suspend fun insertSearchHistory(history: SearchHistory)
}