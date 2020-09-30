package com.candy.capture.service

import com.candy.capture.activity.BaseActivity

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:54
 */
class Test:BaseActivity() {
    fun tt() {}

    inner class Inner {
        fun innerTT() {
            tt()
        }
    }
}