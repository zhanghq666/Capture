package com.candy.capture.viewmodel

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.candy.capture.core.AppDatabase
import com.candy.capture.core.CaptureApplication
import com.candy.capture.dao.CityDao
import com.candy.capture.dao.ContentDao
import com.candy.capture.model.City
import com.candy.capture.model.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/21 15:24
 */
class PublishViewModel : ViewModel() {
    private val contentDao: ContentDao = AppDatabase.get(CaptureApplication.getInstance()).contentDao()
    private val cityDao: CityDao = AppDatabase.get(CaptureApplication.getInstance()).cityDao()

    fun saveContent(content: Content) {
        if (!TextUtils.isEmpty(content.cityName)) {
            viewModelScope.launch {
                var city: City?
                withContext(Dispatchers.IO) {
                    city = cityDao.queryCity(content.cityName)
                }
                if (city == null) {
                    city = City(count = 1, cityName = content.cityName)
                } else {
                    city!!.count++
                }

                withContext(Dispatchers.IO) {
                    contentDao.insertContent(city!!, content)
                }
                var contents = contentDao.searchContentByCity1(cityName = content.cityName)
                println("${Thread.currentThread().name} , size: ${contents.size}")

            }
        }
    }

}