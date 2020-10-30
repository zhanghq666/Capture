package com.candy.capture.core

import android.app.Application
import android.content.Context
import com.candy.capture.BuildConfig
import com.candy.capture.util.FileUtil
import com.candy.commonlibrary.utils.LogUtil
import com.umeng.analytics.MobclickAgent

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:19
 */
class CaptureApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        FileUtil.initRootFolder(this)
        if (!BuildConfig.DEBUG) {
            MobclickAgent.setDebugMode(true)
            LogUtil.disableLog()
        }
    }

    companion object {
        private var instance: Context? = null
        fun getInstance(): Context {
            return instance!!
        }
    }
}