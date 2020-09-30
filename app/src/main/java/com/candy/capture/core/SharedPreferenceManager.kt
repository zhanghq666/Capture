package com.candy.capture.core

import android.content.Context
import android.content.SharedPreferences

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:24
 */
class SharedPreferenceManager private constructor(val context: Context) {

    init {
        init()
    }

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

    private lateinit var mSharedPreferences: SharedPreferences

    private var mLocationCity: String? = null
    private var mAllowFastCapture = false
    private var mIsFirstRun = false

    private val KEY_LOCATION_CITY = "key_location_city"
    private val KEY_FAST_CAPTURE = "key_fast_capture"
    private val KEY_IS_FIRST_RUN = "key_is_first_run"

    private fun init() {
        mSharedPreferences = context.getSharedPreferences("capture", Context.MODE_PRIVATE)
        mLocationCity = mSharedPreferences.getString(KEY_LOCATION_CITY, ConstantValues.DEFAULT_CITY_NAME)
        mAllowFastCapture = mSharedPreferences.getBoolean(KEY_FAST_CAPTURE, false)
        mIsFirstRun = mSharedPreferences.getBoolean(KEY_IS_FIRST_RUN, true)
    }

    fun getLocationCity(): String? {
        return mLocationCity
    }

    fun setLocationCity(locationCity: String) {
        synchronized(SharedPreferenceManager::class.java) {
            if (mLocationCity != locationCity) {
                mLocationCity = locationCity
                mSharedPreferences!!.edit().putString(KEY_LOCATION_CITY, mLocationCity).apply()
            }
        }
    }

    fun isAllowFastCapture(): Boolean {
        return mAllowFastCapture
    }

    fun setAllowFastCapture(allowFastCapture: Boolean) {
        synchronized(SharedPreferenceManager::class.java) {
            mAllowFastCapture = allowFastCapture
            mSharedPreferences.edit().putBoolean(KEY_FAST_CAPTURE, mAllowFastCapture).commit()
        }
    }

    fun isFirstRun(): Boolean {
        return mIsFirstRun
    }

    fun setFirstRun(firstRun: Boolean) {
        synchronized(SharedPreferenceManager::class.java) {
            mIsFirstRun = firstRun
            mSharedPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, mIsFirstRun).commit()
        }
    }
}