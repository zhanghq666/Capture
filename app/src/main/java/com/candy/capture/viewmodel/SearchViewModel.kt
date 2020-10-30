package com.candy.capture.viewmodel

import androidx.lifecycle.ViewModel
import com.candy.capture.core.AppDatabase
import com.candy.capture.core.CaptureApplication
import com.candy.capture.dao.CityDao
import com.candy.capture.dao.SearchDao
import com.candy.capture.model.City
import com.candy.capture.model.SearchHistory

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/27 17:44
 */
class SearchViewModel : ViewModel() {

    private val searchHistoryDao: SearchDao = AppDatabase.get(CaptureApplication.getInstance()).searchDao()
    private val cityDao: CityDao = AppDatabase.get(CaptureApplication.getInstance()).cityDao()

    suspend fun getSearchHistory(): List<SearchHistory> {
        return searchHistoryDao.getSearchHistory()
    }

    suspend fun getAllCity(): List<City> {
        return cityDao.getAllCity()
    }

    suspend fun insertSearchHistory(keyword: String) {
        searchHistoryDao.insertSearchHistory(SearchHistory(keyword = keyword))
    }

}