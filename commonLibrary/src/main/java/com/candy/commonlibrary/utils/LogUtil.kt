package com.candy.commonlibrary.utils

import android.util.Log

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 16:20
 */
object LogUtil {
    private var isDebug = true

    fun disableLog() {
        isDebug = false
    }

    fun i(tag: String?, msg: String?) {
        if (isDebug) {
            Log.i(tag, msg)
        }
    }

    fun d(tag: String?, msg: String?) {
        if (isDebug) {
            Log.d(tag, msg)
        }
    }

    fun w(tag: String?, msg: String?) {
        if (isDebug) {
            Log.w(tag, msg)
        }
    }

    fun e(tag: String?, msg: String?) {
        if (isDebug) {
            Log.e(tag, msg)
        }
    }
}