package com.candy.capture.core

import android.app.Application
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
        FileUtil.initRootFolder(this)
        if (!BuildConfig.DEBUG) {
            MobclickAgent.setDebugMode(true)
            LogUtil.disableLog()
        }
    }
}