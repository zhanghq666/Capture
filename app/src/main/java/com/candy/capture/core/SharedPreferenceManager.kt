package com.candy.capture.core

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:24
 */
class SharedPreferenceManager private constructor(val context: Context) {

    companion object {
        private var instance: SharedPreferenceManager? = null

        fun getInstance(context: Context): SharedPreferenceManager {
            if (instance == null) {
                synchronized(SharedPreferenceManager::class.java) {
                    if (instance == null) {
                        instance = SharedPreferenceManager(context.applicationContext)
                    }
                }
            }
            return instance!!
        }
    }

    private var mLocationCity: String = ""
    private var mAllowFastCapture = false
    private var mIsFirstRun = false

    private val KEY_LOCATION_CITY = preferencesKey<String>("key_location_city")
    private val KEY_FAST_CAPTURE = preferencesKey<Boolean>("key_fast_capture")
    private val KEY_IS_FIRST_RUN = preferencesKey<Boolean>("key_is_first_run")

    private val dataStore: DataStore<Preferences> = context.createDataStore("capture")

    init {
        GlobalScope.launch {
            dataStore.data.collect {
                mLocationCity = it[KEY_LOCATION_CITY] ?: ConstantValues.DEFAULT_CITY_NAME
                mAllowFastCapture = it[KEY_FAST_CAPTURE] ?: false
                mIsFirstRun = it[KEY_IS_FIRST_RUN] ?: false
            }
        }
    }

    fun getLocationCity(): String? {
        return mLocationCity
    }

    fun setLocationCity(locationCity: String) {
        mLocationCity = locationCity
        GlobalScope.launch {
            dataStore.edit { settings ->
                settings[KEY_LOCATION_CITY] = locationCity
            }
        }
    }

    fun isAllowFastCapture(): Boolean {
        return mAllowFastCapture
    }

    fun setAllowFastCapture(allowFastCapture: Boolean) {
        mAllowFastCapture = allowFastCapture
        GlobalScope.launch {
            dataStore.edit { settings ->
                settings[KEY_FAST_CAPTURE] = allowFastCapture
            }
        }
    }

    fun isFirstRun(): Boolean {
        return mIsFirstRun
    }

    fun setFirstRun(firstRun: Boolean) {
        mIsFirstRun = firstRun
        GlobalScope.launch {
            dataStore.edit { settings ->
                settings[KEY_IS_FIRST_RUN] = firstRun
            }
        }
    }
}